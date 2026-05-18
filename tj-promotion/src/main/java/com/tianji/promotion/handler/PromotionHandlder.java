package com.tianji.promotion.handler;

import com.tianji.common.constants.MqConstants;
import com.tianji.promotion.domain.dto.UserCouponDTO;
import com.tianji.promotion.domain.dto.UserExchangeCouponDTO;
import com.tianji.promotion.service.IUserCouponService;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PromotionHandlder {

    private final IUserCouponService userCouponService;

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = "coupon.receive.queue", durable = "true"),
            exchange = @Exchange(name = MqConstants.Exchange.PROMOTION_EXCHANGE, type = ExchangeTypes.TOPIC),
            key = MqConstants.Key.COUPON_RECEIVE
    ))
    public void listenCouponReceiveMessage(UserCouponDTO uc) {
        // 更新数据库优惠券数量、以及用户和优惠券的中间表
        userCouponService.checkAndCreateUser(uc);
    }

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = "coupon.exchange.receive.queue", durable = "true"),
            exchange = @Exchange(name = MqConstants.Exchange.PROMOTION_EXCHANGE, type = ExchangeTypes.TOPIC),
            key = MqConstants.Key.COUPON_EXCHANGE_RECEIVE
    ))
    public void listenCouponReceiveMessage(UserExchangeCouponDTO userExchangeCouponDTO) {
        // 更新数据库优惠券数量、以及用户和优惠券的中间表
        userCouponService.CreateUserAndUpdateExchange(userExchangeCouponDTO);
    }

}
