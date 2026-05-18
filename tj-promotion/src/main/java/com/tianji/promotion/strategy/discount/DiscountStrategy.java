package com.tianji.promotion.strategy.discount;

import com.tianji.promotion.enums.DiscountType;

import java.util.EnumMap;

// 使用工厂模式、调用者调用工厂获取对应的实现类、符合开闭原则、修改实现类时候、调用方不需要修改代码
public class DiscountStrategy {
    
    // 定义map
    private final static EnumMap<DiscountType,Discount> strategies;

    // 构造map数据
    // 纯工具类、不需要依赖外部、所以不需要传参
    static {
        strategies = new EnumMap<>(DiscountType.class);
        strategies.put(DiscountType.NO_THRESHOLD, new NoThresholdDiscount());
        strategies.put(DiscountType.PER_PRICE_DISCOUNT, new PerPriceDiscount());
        strategies.put(DiscountType.RATE_DISCOUNT, new RateDiscount());
        strategies.put(DiscountType.PRICE_DISCOUNT, new PriceDiscount());
    }

    
    // 提供调用方法返回实现类
    public static Discount getDiscount(DiscountType type) {
        return strategies.get(type);
    }
    
}
