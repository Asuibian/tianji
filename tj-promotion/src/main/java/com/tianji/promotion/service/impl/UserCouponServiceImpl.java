package com.tianji.promotion.service.impl;

import cn.hutool.core.bean.copier.CopyOptions;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tianji.common.autoconfigure.mq.RabbitMqHelper;
import com.tianji.common.autoconfigure.redisson.annotations.Lock;
import com.tianji.common.constants.MqConstants;
import com.tianji.common.exceptions.BizIllegalException;
import com.tianji.common.utils.BeanUtils;
import com.tianji.common.utils.UserContext;
import com.tianji.promotion.constans.PromotionConstans;
import com.tianji.promotion.domain.dto.UserCouponDTO;
import com.tianji.promotion.domain.dto.UserExchangeCouponDTO;
import com.tianji.promotion.domain.po.Coupon;
import com.tianji.promotion.domain.po.ExchangeCode;
import com.tianji.promotion.domain.po.UserCoupon;
import com.tianji.promotion.enums.ExchangeCodeStatus;
import com.tianji.promotion.mapper.CouponMapper;
import com.tianji.promotion.mapper.UserCouponMapper;
import com.tianji.promotion.service.IExchangeCodeService;
import com.tianji.promotion.service.IUserCouponService;
import com.tianji.promotion.utils.CodeUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Map;

/**
 * <p>
 * 用户领取优惠券的记录，是真正使用的优惠券信息 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2026-04-28
 */
@Service
@RequiredArgsConstructor
public class UserCouponServiceImpl extends ServiceImpl<UserCouponMapper, UserCoupon> implements IUserCouponService {

    private final CouponMapper couponMapper;

    private final IExchangeCodeService exchangeCodeService;

    private final StringRedisTemplate redisTemplate;

    private final RabbitMqHelper mqHelper;

    // TODO 判断在微服务项目中是否可以这样子 构造
    // 手动领取优惠券的lua脚本的加载
    private static final DefaultRedisScript<Long> receiveCouponScript;

    static {
        receiveCouponScript = new DefaultRedisScript<>();

        // 告诉lua脚本的位置、从Resource包去找
        receiveCouponScript.setLocation(new ClassPathResource("receive_coupon.lua"));

        // 设置lua脚本的返回值类型
        receiveCouponScript.setResultType(Long.class);
    }

    // TODO 判断在微服务项目中是否可以这样子 构造
    // 手动领取优惠券的lua脚本的加载
    private static final DefaultRedisScript<Long> receiveExchangeCouponScript;

    static {
        receiveExchangeCouponScript = new DefaultRedisScript<>();

        // 告诉lua脚本的位置、从Resource包去找
        receiveExchangeCouponScript.setLocation(new ClassPathResource("exchange_coupon.lua"));

        // 设置lua脚本的返回值类型
        receiveExchangeCouponScript.setResultType(Long.class);
    }

    @Override
    /**
     *     @Transactional 声明式事物管理  底层基于springAOP动态代理
     *Spring 事务是通过代理机制实现的，而你在同类中直接调用了 checkAndCreateUser，
     * 调用的是原对象方法，没有经过代理——事务切面不会被触发，所以事务失效。
     *
     * 在同类中调用原对象方法，没有经过代理。
     */
    //  使用lua脚本改造 校验代码
    @Lock(name = "#{couponId}")
    public void receiveCoupon(Long couponId) {
        // 1、调用lua脚本、去校验redis缓存的优惠券以及用户领取优惠券的信息
        // KEYS[1] 表示 优惠券基本信息的redis中的 唯一的key
        String couponInfoKey = PromotionConstans.COUPON_CACHE_KEY_PREFIX + couponId;

        // KEYS[2] 表示 优惠券以及对应的用户 领取的优惠券的数量的 结构的key
        String userCouponKey = PromotionConstans.USER_COUPON_CACHE_KEY_PREFIX + couponId;

        // ARGV[1] 表示 操作key2hash结构中的field、就是hashKey，用户id
        String userIdStr = UserContext.getUser().toString();

        Long result = redisTemplate.execute(
                receiveCouponScript,
                Arrays.asList(couponInfoKey, userCouponKey),
                userIdStr
        );

        // TODO 调用lua脚本完成之后 对不同 返回值的处理逻辑
        switch (result.intValue()) {
            case 0:
                // 执行lua脚本成功
                // 6.发送mq消息
                UserCouponDTO uc = new UserCouponDTO();
                uc.setCouponId(couponId);
                uc.setUserId(UserContext.getUser());
                mqHelper.send(
                        MqConstants.Exchange.PROMOTION_EXCHANGE,
                        MqConstants.Key.COUPON_RECEIVE,
                        uc
                );
                break;
            case 1:
                throw new BizIllegalException("优惠券不存在");
            case 2:
                throw new BizIllegalException("库存不足");
            case 3:
                throw new BizIllegalException("优惠券发放未开始或者已经结束");
            case 4:
                throw new BizIllegalException("超出每人限领数量");
            default:
                throw new BizIllegalException("未知错误，请联系管理员");
        }
//
//        // 1.查询优惠券
//        Coupon coupon = queryCouponBycache(couponId);
//        if (coupon == null) {
//            throw new BadRequestException("优惠券不存在");
//        }
//        // 2.校验发放时间
//        LocalDateTime now = LocalDateTime.now();
//        if (now.isBefore(coupon.getIssueBeginTime()) || now.isAfter(coupon.getIssueEndTime())) {
//            throw new BadRequestException("优惠券发放未开始或者已经结束");
//        }
//        // 3.校验库存
//        if (coupon.getTotalNum() <= 0) {
//            throw new BadRequestException("优惠券库存不足");
//        }
//        // 4.4 获取成功、执行业务
//        // 4.校验每人限领数量 并保存优惠券已经发放的数量以及用户券
//        String key = PromotionConstans.USER_COUPON_CACHE_KEY_PREFIX + couponId;
//        // 自增并返回增后的值
//        Long count = redisTemplate.opsForHash()
//                .increment(key, UserContext.getUser().toString(), 1);
//        if (count > coupon.getUserLimit()) {
//            throw new BadRequestException("超出领取数量");
//        }
//        // 5.扣减优惠券库存
//        redisTemplate.opsForHash().increment(PromotionConstans.COUPON_CACHE_KEY_PREFIX + couponId,
//                "totalNum",
//                -1);

//        // 6.发送mq消息
//        UserCouponDTO uc = new UserCouponDTO();
//        uc.setCouponId(couponId);
//        uc.setUserId(UserContext.getUser());
//        mqHelper.send(
//                MqConstants.Exchange.PROMOTION_EXCHANGE,
//                MqConstants.Key.COUPON_RECEIVE,
//                uc
//        );
//


//
//        // 1.查询优惠券
//        Coupon coupon = queryCouponBycache(couponId);
//        if (coupon == null) {
//            throw new BadRequestException("优惠券不存在");
//        }
//        // 2.校验发放时间
//        LocalDateTime now = LocalDateTime.now();
//        if (now.isBefore(coupon.getIssueBeginTime()) || now.isAfter(coupon.getIssueEndTime())) {
//            throw new BadRequestException("优惠券发放未开始或者已经结束");
//        }
//        // 3.校验库存
//        if (coupon.getTotalNum() <= 0) {
//            throw new BadRequestException("优惠券库存不足");
//        }
//        // 4.4 获取成功、执行业务
//        // 4.校验每人限领数量 并保存优惠券已经发放的数量以及用户券
//        String key = PromotionConstans.USER_COUPON_CACHE_KEY_PREFIX + couponId;
//        // 自增并返回增后的值
//        Long count = redisTemplate.opsForHash()
//                .increment(key, UserContext.getUser().toString(), 1);
//        if (count > coupon.getUserLimit()) {
//            throw new BadRequestException("超出领取数量");
//        }
//        // 5.扣减优惠券库存
//        redisTemplate.opsForHash().increment(PromotionConstans.COUPON_CACHE_KEY_PREFIX + couponId,
//                "totalNum",
//                -1);
//
//        // 6.发送mq消息
//        UserCouponDTO uc = new UserCouponDTO();
//        uc.setCouponId(couponId);
//        uc.setUserId(UserContext.getUser());
//        mqHelper.send(
//                MqConstants.Exchange.PROMOTION_EXCHANGE,
//                MqConstants.Key.COUPON_RECEIVE,
//                uc
//        );

    }


    // 从缓存中查询优惠券
    private Coupon queryCouponBycache(Long couponId) {
        // 1.准备key
        String key = PromotionConstans.COUPON_CACHE_KEY_PREFIX + couponId;
        // 2.查询
        Map<Object, Object> objMap = redisTemplate.opsForHash().entries(key);
        if (objMap.isEmpty()) {
            return null;
        }
        // 3.数据反序列化、查询hash结构、得到map类型转成类
        return BeanUtils.mapToBean(objMap, Coupon.class, false, CopyOptions.create());
    }

    /**
     * synchronized：锁的是同一个对象；获取不到锁，阻塞等待
     * 单实例部署下多线程并发安全处理方法；
     * synchronized 在同步代码块/方法结束后（无论正常或异常）会自动释放锁，无需手动调用任何释放方法。
     * <p>
     * <p>
     * 实例方法上的 synchronized 锁的是当前对象实例，在 Spring 单例 Bean 中会导致所有请求串行执行；
     * <p>
     * u         serId.toString() 每次调用都会新建一个 String 对象
     * <p>
     * 由于开启了事物，释放锁后还没提交 事物，数据库数据修改其它线程看不见就会导致线程安全问题、顺序交换
     * <p>
     * 同一个用户提交大量请求、同时判断限领数量成功后，还没修改数据库并提交事物。中间友来一个线程判断成功、导致同一个用户领取多张优惠券
     * <p>
     * 对同一个用户id加锁，并且是先加锁后加事物。
     * <p>
     * intern();获取常量池的string对象，只要值一样对象就是同一个
     */
    //  @MyLock() @Transactional 都是切入点 指定切入的顺序可以在切面类上实现接口 implements Ordered {
    //    @Override
    //    public int getOrder() {
    //        return 0;
    //    }
    // @MyLock(name = "") 保证校验用户限领数量的串行执行
    @Transactional
    @Override
    public void checkAndCreateUser(UserCouponDTO uc) {
        // UserContext.getUser().toString().intern(); 锁字符串的话 锁常量池的唯一变量。因为tostring方法是重新new一个string
//        // 4.1查询当前用户领取的数量
//        Integer count = lambdaQuery()
//                .eq(UserCoupon::getCouponId, couponId)
//                .eq(UserCoupon::getUserId, UserContext.getUser())
//                .count();
//        // 4.2校验
//        if (count != null && count >= coupon.getUserLimit()) {
//            throw new BadRequestException("领取次数太多");
//        }

        Long couponId = uc.getCouponId();

        // 5.更新优惠券实体已经发放数量+1
        int i = couponMapper.incrIssueNum(couponId);
        if (i == 0) {
            // 更新0行数据，失败，触发乐观锁
            throw new BizIllegalException("库存不足");
        }

        Coupon coupon = couponMapper.selectById(couponId);
        LocalDateTime now = LocalDateTime.now();

        // 6.新增一个用户券
        saveUserCoupon(coupon, now);
    }

    @Override
    @Transactional
    // 基于redis校验
    // TODO 使用lua脚本改造 兑换码领取优惠券的 代码
    // TODO 判断lua脚本是否正确、然后调用改造
    // 减少多次对redis访问所消耗的网络
    public void exchangeCoupon(String code) {
        // 1.使用工具类，校验并解析兑换码
        long seriaNum = CodeUtil.parseCode(code);


        // 1、调用lua脚本、去校验redis缓存的优惠券以及用户领取优惠券的信息
        // KEYS[1] 表示 位图的key、校验兑换码是否为1、就是被兑换过了
        String bitMapKey = PromotionConstans.COUPON_CODE_MAPL_KEY;

        // KEYS[2] 表示 获取自增序列号对应的优惠券的id
        String exchangeCouponKey = PromotionConstans.EXCHANGE_COUPON_CACHE_KEY;

        // ARGV[1] 表示 自增序列号
        String seriaNumStr = String.valueOf(seriaNum);

        // ARGV[2] 表示 ZRANGEBYSCORE 命令中的最大分数
        String maxSeriaNumStr = String.valueOf(seriaNum + 5000);

        // ARGV[3] 表示 hash结构中的field 为用户id
        String userIdStr = UserContext.getUser().toString();

        Long result = redisTemplate.execute(
                receiveExchangeCouponScript,
                Arrays.asList(bitMapKey, exchangeCouponKey),
                seriaNumStr, maxSeriaNumStr, userIdStr
        );
        //因为 Redis 的 EVAL 命令要求先列出所有 KEYS（影响集群槽位的关键参数），再列出 ARGV；Spring Data Redis 为了严格遵循这个协议
        // ，并且让框架能正确识别哪些参数是键（用于路由、监控等），
        // 所以 execute 方法强制将 KEYS 封装成 List，而将 ARGV 作为可变参数直接传递。这样既清晰又安全。


        if (result > 5) {
            // 成功：result 就是优惠券 ID
            Long couponId = result;

            // 发送mq消息
            UserExchangeCouponDTO userExchangeCouponDTO = new UserExchangeCouponDTO();
            userExchangeCouponDTO.setCouponId(couponId);
            userExchangeCouponDTO.setUserId(UserContext.getUser());
            userExchangeCouponDTO.setExchangeId(seriaNum);
            mqHelper.send(
                    MqConstants.Exchange.PROMOTION_EXCHANGE,
                    MqConstants.Key.COUPON_EXCHANGE_RECEIVE,
                    userExchangeCouponDTO
            );

            // 可选：记录日志或返回结果
        } else {
            // 失败：根据错误码抛出对应异常
            switch (result.intValue()) {
                case 1:
                    throw new BizIllegalException("兑换码已被使用");
                case 2:
                    throw new BizIllegalException("无可用的优惠券");
                case 3:
                    throw new BizIllegalException("优惠券信息不存在");
                case 4:
                    throw new BizIllegalException("优惠券不在有效期内");
                case 5:
                    throw new BizIllegalException("超出每人限领数量");
                default:
                    throw new BizIllegalException("未知错误，请联系管理员");
            }
        }




        //旧的代码
//
//        // 1.使用工具类，校验并解析兑换码
//        long seriaNum = CodeUtil.parseCode(code);
//        try {
//            // 2.得到自增长序列，到redisbitMap中校验是否已经兑换
//            // getbit命令，于setbt命令间隔多个操作时间长。同时进入多个线程。导致一个
//            //兑换码，兑换多张券。线程不安全。直接一开始使用setbit代替getbit，会返回
//            //赋值前的数字，也就是说只有第一个命令会得到“0”，没有兑换
//            //手动捕获异常，回滚redis
//            boolean exchanged = exchangeCodeService.updateExchangeMark(seriaNum, true);
//            // 返回旧值，0就是false，1就是true，如果旧值是1就是已经被修改过
//            if (exchanged) {
//                throw new BizIllegalException("兑换码已经被兑换过了");
//            }
//
//            // 3.查询兑换码。根据自增序列号得到对应的目标优惠券的id
//            String key = PromotionConstans.EXCHANGE_COUPON_CACHE_KEY;
//            // Set 是无序的 没有 get(0)，取第一个元素用 set.iterator().next()  取序列号 取第一个
//            Set<String> couponIds = redisTemplate.opsForZSet()
//                    .rangeByScore(key, seriaNum, seriaNum + 5000, 0, 1);
//            // 操作redis中zset数据结构、redisKey  最低分数  最高分数  从0开始  只能取一条（离最低分数最近的那个score） 返回menber
//
//            if (couponIds == null || couponIds.isEmpty()) {
//                throw new BizIllegalException("兑换码不存在");
//            }
//            String couponId = couponIds.iterator().next();
//
//            // 从redis中、获取缓存的目标优惠券的信息
//            Map<Object, Object> couponInfo = redisTemplate.opsForHash()
//                    .entries(PromotionConstans.COUPON_CACHE_KEY_PREFIX + couponId);
//
//            // 4.检验过期时间
//            LocalDateTime expiredTime = (LocalDateTime) couponInfo.get("issueEndTime");
//            LocalDateTime now = LocalDateTime.now();
//            if (now.isAfter(expiredTime)) {
//                throw new BizIllegalException("兑换码已经过期");
//            }
//
//            // 5. 检验限领数量
//            String limitKey = PromotionConstans.USER_COUPON_CACHE_KEY_PREFIX + couponId;
//            // 自增并返回原来的值,如果不存在就会创建并从0开始自增
//            // INCR、HINCRBY → 返回增加后的新值。
//            //GETSET、SETBIT → 返回旧值。
//            Long count = redisTemplate.opsForHash().increment(limitKey, UserContext.getUser().toString(), 1);
//            if (count > Long.parseLong((String) couponInfo.get("userLimit"))) {
//                throw new BadRequestException("超出领取数量");
//            }
//
//
//            // 6.发送mq消息
//            UserExchangeCouponDTO userExchangeCouponDTO = new UserExchangeCouponDTO();
//            userExchangeCouponDTO.setCouponId(Long.valueOf(couponId));
//            userExchangeCouponDTO.setUserId(UserContext.getUser());
//            userExchangeCouponDTO.setExchangeId(seriaNum);
//            mqHelper.send(
//                    MqConstants.Exchange.PROMOTION_EXCHANGE,
//                    MqConstants.Key.COUPON_EXCHANGE_RECEIVE,
//                    userExchangeCouponDTO
//            );
//
//        } catch (Exception e) {
//            // 重置兑换的标记 0
//            exchangeCodeService.updateExchangeMark(seriaNum, false);
//            throw e;
//        }

    }

    @Override
    @Transactional
    public void CreateUserAndUpdateExchange(UserExchangeCouponDTO userExchangeCouponDTO) {
        Long couponId = userExchangeCouponDTO.getCouponId();

        // 1.更新优惠券实体已经发放数量+1
        int i = couponMapper.incrIssueNum(couponId);
        if (i == 0) {
            // 更新0行数据，失败，触发乐观锁
            throw new BizIllegalException("库存不足");
        }

        Coupon coupon = couponMapper.selectById(couponId);
        LocalDateTime now = LocalDateTime.now();

        // 2.新增一个用户券
        saveUserCoupon(coupon, now);

        // 3.更新兑换码状态
        exchangeCodeService.lambdaUpdate()
                .eq(ExchangeCode::getId, userExchangeCouponDTO.getExchangeId())
                .set(ExchangeCode::getStatus, ExchangeCodeStatus.USED)
                .update();
    }

    private void saveUserCoupon(Coupon coupon, LocalDateTime now) {
        UserCoupon userCoupon = new UserCoupon();
        userCoupon.setCouponId(coupon.getId());
        userCoupon.setUserId(UserContext.getUser());
        LocalDateTime termBeginTime = coupon.getTermBeginTime();
        LocalDateTime termEndTime = coupon.getTermEndTime();

        if (termBeginTime == null) {
            termBeginTime = now;
            termEndTime = now.plusDays(coupon.getTermDays());
        }

        userCoupon.setTermBeginTime(termBeginTime);
        userCoupon.setTermEndTime(termEndTime);
        // 保存信息
        save(userCoupon);
    }
}
