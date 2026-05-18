package com.tianji.promotion.domain.dto;

import lombok.Data;

/**
 * mq消息传递的dto，异步生成优惠券需要的数据
 */
@Data
public class UserExchangeCouponDTO {
    /**
     * 用户id
     */
    private Long userId;
    /**
     * 优惠券id
     */
    private Long couponId;
    /**
     * 兑换码id
     */
    private  Long exchangeId;
}
