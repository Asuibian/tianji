package com.tianji.promotion.service.impl;

import com.tianji.api.dto.promotion.CouponDiscountDTO;
import com.tianji.api.dto.promotion.OrderCourseDTO;
import com.tianji.common.utils.CollUtils;
import com.tianji.common.utils.UserContext;
import com.tianji.promotion.domain.po.Coupon;
import com.tianji.promotion.domain.po.CouponScope;
import com.tianji.promotion.mapper.UserCouponMapper;
import com.tianji.promotion.service.ICouponScopeService;
import com.tianji.promotion.service.IDiscountService;
import com.tianji.promotion.strategy.discount.Discount;
import com.tianji.promotion.strategy.discount.DiscountStrategy;
import com.tianji.promotion.utils.PermuteUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DiscountServiceImpl implements IDiscountService {

    private final UserCouponMapper userCouponMapper;

    private final ICouponScopeService scopeService;

    private final Executor discountSolutionExecutor;

    @Override
    public List<CouponDiscountDTO> findCouponDiscountSolution(List<OrderCourseDTO> orderCourseDTOList) {
        // 1、根据当前用户id查询用户所有的券、在查询券的信息
        // 用户券表和优惠券表的联查、返回用户 券中的主键、以及优惠券的折扣信息相关的字段
        // 使用po实体中的Long形creater接收用户券的主键
        List<Coupon> coupons = userCouponMapper.queryMyCoupons(UserContext.getUser());
        if (CollUtils.isEmpty(coupons)) {
            return CollUtils.emptyList();
        }
        // 2、初筛
        // 2.1、计算全部课程的价格之和
        int totalAmount = orderCourseDTOList.stream().mapToInt(OrderCourseDTO::getPrice).sum();
        // 2.2、根据工厂得到不同枚举的实现类、根据实现类的方法进行过滤
        List<Coupon> availableCoupons = coupons.stream()
                .filter(c -> DiscountStrategy.getDiscount(c.getDiscountType()).canUse(totalAmount, c))
                .collect(Collectors.toList());
        if (availableCoupons.isEmpty()) {
            return CollUtils.emptyList();
        }
        // 3、排列出优惠方案
        // 3.1、细筛（找出每一个优惠券的指定分类的课程、）判断指定分类的课程的总结是否达到优惠券的使用门槛
        // 优惠券以及对应的的课程集合
        Map<Coupon, List<OrderCourseDTO>> availableCouponsMap = findAvailableCoupon(availableCoupons, orderCourseDTOList);
        if (availableCouponsMap.isEmpty()) {
            return CollUtils.emptyList();
        }
        // 4.计算每种方案、、根据coupon中主键全排列
        availableCoupons = new ArrayList<>(availableCouponsMap.keySet());
        List<List<Coupon>> permute = PermuteUtil.permute(availableCoupons);

        for (Coupon c : availableCoupons) {
            permute.add(List.of(c));
        }

        // 多线程写的情况下、使用Arraylist是、线程、不安全的。使用synchronizedList
        List<CouponDiscountDTO> list = Collections.synchronizedList(new ArrayList<>(permute.size()));

        // 使用异步 多线程的方法 提高计算的性能、并主线程要等待全部计算的完成、线程计算需要存在返回值
        // 4.1 定义闭锁。用于判断是否全部线程执行完成
        CountDownLatch latch = new CountDownLatch(permute.size());
        // 对每一种方案进行循环，一次添加一种CouponDiscountDTO
        for (List<Coupon> s : permute) {
            // 4.2 调用线程池进行异步计算
            CompletableFuture
                    // 无参数有返回值的。可以使用匿名的方式进行传参
                    .supplyAsync(() -> (calculateSolutionDiscount(availableCouponsMap, orderCourseDTOList, s))) // 调用线程池、异步执行计算
                    .thenAccept(dto -> { // 每一次计算完成后的执行
                        // 4.3 得到计算结果、添加到集合中、并latch减减
                        list.add(dto);
                        latch.countDown();
                    });
        }
        // 4.4等待运算结果
        try {
            latch.await(1, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            log.error("优惠方案计算被中断，{}", e.getMessage());
        }

        // 5.筛选出最优的解
        return findBestSolution(list);
    }

    // 计算出优惠券数量相同时候、金额最大的、、、、金额相同时候、优惠券数量最少的券、、2个map的交集
    private List<CouponDiscountDTO> findBestSolution(List<CouponDiscountDTO> list) {
        // 1.准备map去记录最优解
        // 组合的id      当前最优
        Map<String, CouponDiscountDTO> moreDiscountMap = new HashMap<>();
        // 不同的金额    当前金额的最优
        Map<Integer, CouponDiscountDTO> lessCouponMap = new HashMap<>();

        // 2.遍历、循环、得到最优解
        for (CouponDiscountDTO sulution : list) {
            // 2.1不同的排序、组合是唯一的、按照id升序排序、相同组合key就是唯一的
            List<Long> ids = sulution.getIds();
            String idsKey = ids.stream()// 相同组合的ids是唯一的
                    .sorted(Long::compare)
                    .map(String::valueOf)
                    .collect(Collectors.joining(","));
            // 2.2key相同的时候、就是说明用券相同、比较获取组合相同的时候、优惠的金额是不是最大的
            CouponDiscountDTO best = moreDiscountMap.get(idsKey);
            // 已经存在组合、去比较金额
            if (best != null && best.getDiscountAmount() >= sulution.getDiscountAmount()) {
                continue;
            }
            // 2.3前一个比较通过、是组合中优惠金额最大的、然后继续比较找到金额相同的时候、使用的优惠券是不是最少的
            best = lessCouponMap.get(sulution.getDiscountAmount());// 表示获取出来旧的实体
            if (sulution.getIds().size() > 1 && best != null && best.getIds().size() <= sulution.getIds().size()) {
                continue;
            }

            // 2.4 更新最优解
            moreDiscountMap.put(idsKey, sulution);
            lessCouponMap.put(sulution.getDiscountAmount(), sulution);
        }
        // 3. 求交集
        Collection<CouponDiscountDTO> values = moreDiscountMap.values();
        Collection<CouponDiscountDTO> values1 = lessCouponMap.values();
        Collection<CouponDiscountDTO> bestSolutions = CollUtils.intersection(values, values1);

        // 4.排序、按优惠金额降序
        return bestSolutions.stream()
                .sorted(Comparator.comparingInt(CouponDiscountDTO::getDiscountAmount).reversed())
                .collect(Collectors.toList());
    }

    // 计算当前方案的     订单的可用优惠券及折扣信息
    private CouponDiscountDTO calculateSolutionDiscount(
            Map<Coupon, List<OrderCourseDTO>> availableCouponsMap, // 优惠券实体对应的可以使用的用户购买的课程
            List<OrderCourseDTO> orderCourseDTOList, // 用户购买的全部课程、用于初始化map映射
            List<Coupon> solution) // 全排列、优惠券不同的组合以及顺序
    {
        // 1、初始化 dto
        CouponDiscountDTO dto = new CouponDiscountDTO();
        // 2、初始化折扣明细的映射(课程、以及当前课程对应的折扣)
        Map<Long, Integer> detailMap = orderCourseDTOList.stream()
                .collect(Collectors.toMap(OrderCourseDTO::getId, oc -> 0));
        // 3、循环计算 当前优惠券的 每一个优惠券的折扣
        for (Coupon coupon : solution) {
            // 3.1获取优惠券限定范围以及对应的课程
            List<OrderCourseDTO> availableCourses = availableCouponsMap.get(coupon);
            // 3.2计算课程总价（课程原价 - 折扣明细）
            int totalAmount = availableCourses.stream().mapToInt(oc -> oc.getPrice() - detailMap.get(oc.getId())).sum();
            // 3.3判断是否可以使用
            Discount discount = DiscountStrategy.getDiscount(coupon.getDiscountType());
            if (!discount.canUse(totalAmount, coupon)) {
                continue;
            }
            // 3.4计算优惠金额
            int discountAmount = discount.calculateDiscount(totalAmount, coupon); // 当前优惠券对可以被使用的课程的总价格、然后获取全部折扣
            // 3.5 在循环中处理  全排列  中的一个优惠券  的明细、对课程参数折扣的明细（按每一个课程的比例去分配计算折扣明细）
            calculateDiscountDetails(detailMap, availableCourses, totalAmount, discountAmount);
            // 3.6 更新dto
            dto.getIds().add(coupon.getId());  // 得到ids集合、然后去添加
            dto.getRules().add(discount.getRule(coupon));
            dto.setDiscountAmount(discountAmount + dto.getDiscountAmount());
        }
        return dto;
    }

    // TODO this 计算折扣明细，并由于依赖参数直接修改map、、map用于总价一直维持是减去折扣后的课程的总价
    private void calculateDiscountDetails(
            Map<Long, Integer> detailMap, // 课程以及对应的折扣明细
            List<OrderCourseDTO> availableCourses,  // 当前优惠券可以使用的课程
            int totalAmount, // 已经从原价中减去折扣价格的总价格
            int discountAmount // 优惠的全部金额
    ) {
        int times = 0; // 计数器、判断是不是计算、最后一个、的课程
        int remainDiscount = discountAmount; // remain 表示、剩余的折扣金额是多少
        for (OrderCourseDTO c : availableCourses) {
            times++;
            int discount = 0; // 用户记录当前课程的折扣
            // 判断是不是最后一个课程的计算
            if (times == availableCourses.size()) {
                discount = remainDiscount;
            } else {
                discount = totalAmount * (c.getPrice() / totalAmount); //因为价格比例不会更变、怎么减折扣都是按照比例减的、所以直接使用原价即可。比例一样
                remainDiscount -= discount;
            }
            detailMap.put(c.getId(), discount + detailMap.get(c.getId())); // 更新、累加折扣金额（不同优惠券都会有折扣）
        }

    }

    // key表示优惠券、、、、value表示指定的分类的课程的集合
    private Map<Coupon, List<OrderCourseDTO>> findAvailableCoupon(
            // 优惠券                                      全部课程
            List<Coupon> availableCoupons, List<OrderCourseDTO> orderCourseDTOList) {

        Map<Coupon, List<OrderCourseDTO>> map = new HashMap<>(availableCoupons.size());

        // 再循环中 依次装配每一个 优惠券  以及对应的 提交的课程中 可以使用的课程
        for (Coupon coupon : availableCoupons) {
            // 如果有分类就会重新覆盖指定的分类课程
            // 没有就是都可以使用
            List<OrderCourseDTO> availableCourses = orderCourseDTOList; //最终在循环中得到、当前优惠券可以使用的指定的分类  的课程

            // 得到当前优惠券的分类的课程
            if (coupon.getSpecific()) {
                // 1.找出当前优惠券的可以使用的课程分类
                List<CouponScope> scopes = scopeService.lambdaQuery().eq(CouponScope::getCouponId, coupon.getId()).list();
                // 2.获取范围对应的分类的id,当前优惠券可以使用的分类id
                Set<Long> scopeIds = scopes.stream().map(CouponScope::getBizId).collect(Collectors.toSet());
                // 3.筛选课程，对全部用户需要购买的课程进行过滤、只保留被scopeIds包含的id。就是判断每一个课程是否再ids中，再就是true保留就行
                availableCourses = orderCourseDTOList.stream()
                        .filter(c -> scopeIds.contains(c.getCateId()))
                        .collect(Collectors.toList());
            }

            // 在循环中计算当前优惠券的 课程总价availableCourses表示当前优惠券可以使用的指定分类的课程、根据课程实体中的价格属性做累加即可、得到当前优惠券去判断是否可以使用的门槛
            int totalAmount = availableCourses.stream().mapToInt(OrderCourseDTO::getPrice).sum();
            // 在循环当前去判断 当前优惠券 是否可用
            Discount discount = DiscountStrategy.getDiscount(coupon.getDiscountType()); // 通过工厂获取实体
            boolean b = discount.canUse(totalAmount, coupon); // 调用实体的方法 判断当前优惠券是否可以使用（全部价格够不够门槛）
            if (b) {
                // 够
                map.put(coupon, availableCourses);
            }

        }


        return map;
    }
}












