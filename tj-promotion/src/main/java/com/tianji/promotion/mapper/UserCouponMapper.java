package com.tianji.promotion.mapper;

import com.tianji.promotion.domain.po.Coupon;
import com.tianji.promotion.domain.po.UserCoupon;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * <p>
 * 用户领取优惠券的记录，是真正使用的优惠券信息 Mapper 接口
 * </p>
 *
 * @author 虎哥
 * @since 2026-04-28
 */
public interface UserCouponMapper extends BaseMapper<UserCoupon> {

    // @Param("userId") 告诉 MyBatis：这个方法参数的名字叫 userId，在 SQL 中用 #{userId} 就能拿到它的值。
    List<Coupon> queryMyCoupons(@Param("userId") Long userId);
}
