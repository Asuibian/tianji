package com.tianji.promotion.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.OrderItem;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tianji.api.client.course.CategoryClient;
import com.tianji.api.dto.course.CategoryBasicDTO;
import com.tianji.common.domain.dto.PageDTO;
import com.tianji.common.exceptions.BadRequestException;
import com.tianji.common.exceptions.BizIllegalException;
import com.tianji.common.utils.DateUtils;
import com.tianji.common.utils.UserContext;
import com.tianji.promotion.constans.PromotionConstans;
import com.tianji.promotion.domain.dto.CouponFormDTO;
import com.tianji.promotion.domain.dto.CouponIssueFormDTO;
import com.tianji.promotion.domain.po.Coupon;
import com.tianji.promotion.domain.po.CouponScope;
import com.tianji.promotion.domain.po.UserCoupon;
import com.tianji.promotion.domain.query.CouponQuery;
import com.tianji.promotion.domain.vo.CouponDetailVO;
import com.tianji.promotion.domain.vo.CouponPageVO;
import com.tianji.promotion.domain.vo.CouponScopeVO;
import com.tianji.promotion.domain.vo.CouponVO;
import com.tianji.promotion.enums.CouponStatus;
import com.tianji.promotion.enums.ObtainType;
import com.tianji.promotion.enums.UserCouponStatus;
import com.tianji.promotion.mapper.CouponMapper;
import com.tianji.promotion.service.ICouponService;
import com.tianji.promotion.service.IExchangeCodeService;
import com.tianji.promotion.service.IUserCouponService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

/**
 * <p>
 * 优惠券的规则信息 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2026-04-25
 */
@Service
@RequiredArgsConstructor
public class CouponServiceImpl extends ServiceImpl<CouponMapper, Coupon> implements ICouponService {

    private final CouponScopeServiceImpl couponScopeServiceImpl;

    private final IExchangeCodeService codeService;

    private final CategoryClient categoryClient;

    private final IUserCouponService userCouponService;

    private final StringRedisTemplate redisTemplate;

    /**
     * 新增优惠券
     *
     * @param dto
     */
    @Override
    public void saveCoupon(CouponFormDTO dto) {
        // 1.保存优惠券
        // 1.1 dto转 po
        //  Coupon PO 字段名字和数据库关键字重合，插入时候出现BUG,使用TableField注解，添加转意字符
        Coupon coupon = new Coupon();
        BeanUtils.copyProperties(dto, coupon);
        this.save(coupon);

        Boolean specific = dto.getSpecific();
        if (!specific) {
            // 没有限定范围
            return;
        }
        Long couponId = coupon.getId();
        // 2.保存限定范围
        List<Long> scopes = dto.getScopes();
        if (scopes == null || scopes.isEmpty()) {
            throw new BadRequestException("限定范围不能为空"); // 自定义异常不是为了“省掉传参”，而是为了让异常有“类型身份”，便于捕获、统计、运维。
        }
        List<CouponScope> list = new ArrayList<>(scopes.size());
        list = scopes.stream()
                .map(bizId -> new CouponScope().setBizId(bizId).setCouponId(couponId))
                .collect(Collectors.toList());

        couponScopeServiceImpl.saveBatch(list);

    }

    @Override
    public PageDTO<CouponPageVO> queryCouponBypage(CouponQuery query) {
        String name = query.getName();
        Integer type = query.getType();
        Integer status = query.getStatus();

        // 1.分页查询
        Page<Coupon> page = new Page<>(query.getPageNo(), query.getPageSize());
        OrderItem orderItem = new OrderItem();
        orderItem.setAsc(false);
        orderItem.setColumn("create_time");
        page.addOrder(orderItem);
        Page<Coupon> couponPage = lambdaQuery()
                .like(name != null, Coupon::getName, name)
                .eq(type != null, Coupon::getDiscountType, type)
                .eq(status != null, Coupon::getStatus, status)
                .page(page);
        List<Coupon> records = couponPage.getRecords();
        if (records == null || records.isEmpty()) {
            return new PageDTO<>(page.getTotal(), page.getPages(), Collections.emptyList());
        }
        // 2.处理VO返回
        List<CouponPageVO> ts = BeanUtil.copyToList(records, CouponPageVO.class);
        PageDTO<CouponPageVO> pageDTO = new PageDTO<>();
        pageDTO.setTotal(couponPage.getTotal());
        pageDTO.setPages(couponPage.getPages());
        pageDTO.setList(ts);
        return pageDTO;
    }

    @Transactional
    @Override
    public void beginIssueCoupon(CouponIssueFormDTO dto) {
        // 1.查询优惠券
        Coupon coupon = getById(dto.getId());
        // 2.判断优惠券状态、是否是暂停或者待发放
        if (coupon.getStatus() != CouponStatus.DRAFT && coupon.getStatus() != CouponStatus.PAUSE) {
            throw new BizIllegalException("优惠券状态错误！"); // BizIllegalExceptiona表示非法操作
        }
        // 3.判断是否是立刻发放
        LocalDateTime beginTime = dto.getIssueBeginTime();
        boolean isBegin = beginTime == null || !beginTime.isAfter(LocalDateTime.now());
        // 4.更新优惠券
        Coupon c = new Coupon();
        BeanUtils.copyProperties(dto, c);

        if (isBegin) {
            // 立刻发放
            c.setStatus(CouponStatus.ISSUING);
            c.setIssueBeginTime(LocalDateTime.now());
        } else {
            // 定时发放
            c.setStatus(CouponStatus.UN_ISSUE);
        }
        // 5.写入数据库
        updateById(c);

        // 6. 添加优惠券缓存
        if (isBegin) {
            coupon.setIssueBeginTime(c.getIssueBeginTime());
            coupon.setIssueEndTime(c.getIssueEndTime());
            cacheCouponnInfo(coupon);
        }

        // 7.判断是否需要生产兑换码、优惠券类型必须是指定发放（兑换码）、优惠券状态必须是待发放
        if (coupon.getObtainWay() == ObtainType.ISSUE && coupon.getStatus() == CouponStatus.DRAFT) {
            coupon.setIssueEndTime(c.getIssueEndTime());
            codeService.asyncGenerateCode(coupon);
        }

    }

    private void cacheCouponnInfo(Coupon coupon) {
        // 1.组织数据
        Map<String,String> map = new HashMap<>(4);
        //map.put("issueBeginTime", String.valueOf(DateUtils.toEpochMilli(coupon.getIssueBeginTime())));
        map.put("issueBeginTime", String.valueOf(coupon.getIssueBeginTime().toEpochSecond(ZoneOffset.UTC)));
        // 转换成秒级时间戳存储、方便lua脚本比较
        //map.put("issueEndTime", String.valueOf(DateUtils.toEpochMilli(coupon.getIssueEndTime())));
        map.put("issueEndTime", String.valueOf(coupon.getIssueEndTime().toEpochSecond(ZoneOffset.UTC)));
        map.put("totalNum", String.valueOf(coupon.getTotalNum()));
        map.put("userLimit", String.valueOf(coupon.getUserLimit()));

        // 2.写缓存
        redisTemplate.opsForHash().putAll(PromotionConstans.COUPON_CACHE_KEY_PREFIX + coupon.getId(),map);
    }

    /**
     * 根据id查询优惠券详细信息
     *
     * @param id
     * @return
     */
    @Override
    public CouponDetailVO queryDetailCouponById(Long id) {
        CouponDetailVO vo = new CouponDetailVO();
        // 1.查询优惠券实体po
        Coupon coupon = getById(id);
        BeanUtils.copyProperties(coupon, vo);

        // 2.判断是否指定范围
        List<CouponScopeVO> scopes = new ArrayList<>();

        Boolean specific = coupon.getSpecific();
        if (specific) {
            // 2.1是 则根据优惠券id查询中间表
            // 当前优惠券指定的分类的中间表，存储优惠券id 和分类id
            List<CouponScope> list = couponScopeServiceImpl.lambdaQuery()
                    .eq(CouponScope::getCouponId, coupon.getId())
                    .list();
            // 2.2根据分类id 查询分类名称
            Set<Long> bizId = list.stream()
                    .map(CouponScope::getBizId).collect(Collectors.toSet());

            List<CategoryBasicDTO> allOfOneLevel = categoryClient.getAllOfOneLevel();
            // 分类id         分类名称  父分类id

            Map<Long, String> categoryMap = allOfOneLevel.stream().collect(Collectors.toMap(CategoryBasicDTO::getId, CategoryBasicDTO::getName));
            for (Long l : bizId) {
                CouponScopeVO vo1 = new CouponScopeVO();
                vo1.setId(l);
                vo1.setName(categoryMap.get(l));
            }
        }

        // 3.封装VO
        vo.setScopes(scopes);
        return vo;
    }

    @Override
    @Transactional
    public void updateCouponById(CouponFormDTO dto, Long id) {
        Coupon coupon = new Coupon();
        BeanUtils.copyProperties(dto, coupon);
        coupon.setId(id);
        updateById(coupon);

        Boolean specific = dto.getSpecific();
        if (!specific) {
            // 没有限定范围
            return;
        }

        couponScopeServiceImpl.remove(
                new LambdaQueryWrapper<CouponScope>()
                        .eq(CouponScope::getCouponId, coupon.getId())
        );

        Long couponId = coupon.getId();
        // 2.保存限定范围
        List<Long> scopes = dto.getScopes();
        if (scopes == null || scopes.isEmpty()) {
            throw new BadRequestException("限定范围不能为空");
            // 自定义异常不是为了“省掉传参”，而是为了让异常有“类型身份”，便于捕获、统计、运维。
        }
        List<CouponScope> list = new ArrayList<>(scopes.size());
        list = scopes.stream()
                .map(bizId -> new CouponScope().setBizId(bizId).setCouponId(couponId))
                .collect(Collectors.toList());

        couponScopeServiceImpl.saveBatch(list);
    }

    @Override
    public void deleteById(Long id) {
        removeById(id);
    }

    @Override
    public List<CouponVO> queryIssuingCoupons() {
        // 1.查询发放中的 手动领取的 优惠券  po
        List<Coupon> copons = lambdaQuery()
                .eq(Coupon::getObtainWay, ObtainType.PUBLIC)
                .eq(Coupon::getStatus, CouponStatus.ISSUING)
                .list();

        // 2.一次查询当前用户已经领取的优惠券的信息
        List<Long> couponIds = copons.stream().map(Coupon::getId).collect(Collectors.toList());
        List<UserCoupon> userCoupons = userCouponService.lambdaQuery()
                .in(UserCoupon::getCouponId, couponIds)
                .eq(UserCoupon::getUserId, UserContext.getUser())
                .list();

        // 2.1统计当前用户对优惠券已经领取的数量（用于判断是否可以领取）
        // 根据优惠券id进行分组、收集成map，值为优惠券数量
        Map<Long, Long> issueMap = userCoupons.stream().collect(Collectors.groupingBy(UserCoupon::getCouponId, Collectors.counting()));
        // 2.2统计当前用户对优惠券已经领取的数量并未使用（用于判断是否可以使用）
        Map<Long, Long> unuserMap = userCoupons.stream()
                .filter(uc -> uc.getStatus() == UserCouponStatus.UNUSED)
                .collect(Collectors.groupingBy(UserCoupon::getCouponId, Collectors.counting()));

        // 3.封装VO
        List<CouponVO> vos = new ArrayList<>();
        for (Coupon coupon : copons) {
            CouponVO vo = new CouponVO();
            BeanUtils.copyProperties(coupon, vo);
            vos.add(vo);
            // 判断功能性字段
            // 3.1是否可以领取，优惠券总的数量  >  优惠券已经发放的数量  && 当前用户已经领取的数量 < 限领取的数量
            boolean b = coupon.getTotalNum() > coupon.getIssueNum();
            // 可以使用,第一次查询，没有数据为null 使用getOrDefault
            vo.setAvailable(b && issueMap.getOrDefault(coupon.getId(), 0L) < coupon.getUserLimit());
            // 3.2是否可以使用
            Long l = unuserMap.getOrDefault(coupon.getId(), 0L);
            vo.setReceived(l > 0);
        }

        return vos;
    }

    @Transactional
    @Override
    public void pauseIssue(Long id) {
        // 1.查询旧的优惠券
        Coupon coupon = getById(id);
        if (coupon == null) {
            throw new BadRequestException("优惠券不存在");
        }

        // 2.判断当前优惠券是未开始或者进行中
        CouponStatus status = coupon.getStatus();
        if (status != CouponStatus.UN_ISSUE && status != CouponStatus.ISSUING){
            return;
        }
        // 3.更新状态
        boolean update = lambdaUpdate().set(Coupon::getStatus, CouponStatus.PAUSE).eq(Coupon::getId, id).update();
        if (!update) {
            log.error("可能是重复暂停优惠券");
        }

        // 4. 删除优惠券缓存（缓存的作用：判断库存以及是否是发放时间）
        redisTemplate.delete(PromotionConstans.COUPON_CACHE_KEY_PREFIX + id);

    }
}
