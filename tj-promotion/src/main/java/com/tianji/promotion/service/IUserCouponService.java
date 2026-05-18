package com.tianji.promotion.service;

import com.tianji.promotion.domain.dto.UserCouponDTO;
import com.tianji.promotion.domain.dto.UserExchangeCouponDTO;
import com.tianji.promotion.domain.po.Coupon;
import com.tianji.promotion.domain.po.UserCoupon;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * <p>
 * 用户领取优惠券的记录，是真正使用的优惠券信息 服务类
 * </p>
 *
 * @author 虎哥
 * @since 2026-04-28
 */
public interface IUserCouponService extends IService<UserCoupon> {

    void receiveCoupon(Long couponId);

    void checkAndCreateUser(UserCouponDTO uc);

    void exchangeCoupon(String code);

    void CreateUserAndUpdateExchange(UserExchangeCouponDTO userExchangeCouponDTO);
}
