package com.tianji.promotion.strategy.discount;


import com.tianji.promotion.domain.po.Coupon;

// 根据优惠券类型、选择不同的规则、一个接口、多个实现类（不同策略） + 工厂模式。符合开闭原则，对扩展代码开放，对修改代码关闭。
public interface Discount {

    // 判断当前订单的总金额，是否满足使用的门槛
    boolean canUse(int totalAmout, Coupon coupon);

    // 计算折扣的金额
    int calculateDiscount(int totalAmout, Coupon coupon);

    // 规则描述信息
    String getRule(Coupon coupon);

}
