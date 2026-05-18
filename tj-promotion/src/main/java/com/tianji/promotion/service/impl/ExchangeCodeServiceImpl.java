package com.tianji.promotion.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tianji.promotion.constans.PromotionConstans;
import com.tianji.promotion.domain.po.Coupon;
import com.tianji.promotion.domain.po.ExchangeCode;
import com.tianji.promotion.mapper.ExchangeCodeMapper;
import com.tianji.promotion.service.IExchangeCodeService;
import com.tianji.promotion.utils.CodeUtil;
import org.springframework.data.redis.core.BoundValueOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * 兑换码 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2026-04-25
 */
@Service
public class ExchangeCodeServiceImpl extends ServiceImpl<ExchangeCodeMapper, ExchangeCode> implements IExchangeCodeService {

    private final StringRedisTemplate redisTemplate;
    private BoundValueOperations<String, String> serialOps;

    public ExchangeCodeServiceImpl(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.serialOps = redisTemplate.boundValueOps(PromotionConstans.COUPON_CODE_SERIAL_KEY);
        //boundValueOps(key) 绑定一个 Redis 的 key
        // 返回一个专门操作这个 key 的 BoundValueOperations 对象
        // 之后对这个 key 的 set / get / increment 等操作都不需要再传 key。
    }

    // 异步生成兑换码
    @Override
    @Async("generateExchangeCodeExecutor")
    public void asyncGenerateCode(Coupon coupon) {
        // 获取优惠券总量：计算最大兑换码的序列号
        Integer totalNum = coupon.getTotalNum();
        // 1.获取redis自增最大序列号
        Long result = serialOps.increment(totalNum); // 自增totalNum
        if (result == null) {
            return;
        }
        int maxSerialNum = result.intValue(); // 最大序列号
        List<ExchangeCode> list = new ArrayList<>(totalNum); // 存储兑换码实体

        // 使用Sset结构、缓存兑换码最大值、以及对应的优惠券id
        String key = PromotionConstans.EXCHANGE_COUPON_CACHE_KEY;
        // ZADD key score member      redis业务key  最大值   优惠券id
        redisTemplate.opsForZSet().add(key, String.valueOf(maxSerialNum), coupon.getId());

        // 2.生产兑换码
        for (int serialNum = maxSerialNum - totalNum + 1; serialNum <= maxSerialNum; serialNum++) {
            String code = CodeUtil.generateCode(serialNum, coupon.getId());
            ExchangeCode exchangeCode = new ExchangeCode();
            exchangeCode.setCode(code);
            exchangeCode.setId(serialNum);
            exchangeCode.setExchangeTargetId(coupon.getId());
            exchangeCode.setExpiredTime(coupon.getIssueEndTime());
            list.add(exchangeCode);
        }
        // 3.保存数据库
        saveBatch(list);

    }

    @Override
    public boolean updateExchangeMark(long seriaNum, boolean b) {
        Boolean boo = redisTemplate.opsForValue().setBit(PromotionConstans.COUPON_CODE_MAPL_KEY, seriaNum, b);
        // 在 COUPON_CODE_MAPL_KEY 这个 Redis key 对应的二进制数组的第 seriaNum 位置
        // 把 bit 设置为 b（0 或 1），并返回这个位置上原来的 bit 值（0 或 1）。
        // 返回旧值，0就是false，1就是true 如果旧值是1就是已经被修改过
        return boo != null && boo;
    }
}
