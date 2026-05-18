package com.tianji.promotion.constans;

public interface PromotionConstans {
    String COUPON_CODE_SERIAL_KEY = "coupon:code:serial"; // 全部兑换码使用一个key保证序列号唯一，兑换码不重复
    String COUPON_CODE_MAPL_KEY = "coupon:code:map"; // 表示兑换码是否被兑换过的位图
    String COUPON_CACHE_KEY_PREFIX = "prs:coupon:"; // 用于查缓存 领取优惠券的信息，判断当前用户是否有资格去领取、后在发通知
    String USER_COUPON_CACHE_KEY_PREFIX = "prs:user:coupon:"; // 缓存优惠券被哪几个用户领取过，hash机构hash值为用户领取券的数量


    String EXCHANGE_COUPON_CACHE_KEY = "prs:coupon:range"; // 缓存兑换码的最大值以及对应的优惠券id
}
