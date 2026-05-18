package com.tianji.promotion.strategy.discount;

import com.tianji.common.utils.NumberUtils;
import com.tianji.common.utils.StringUtils;
import com.tianji.promotion.domain.po.Coupon;
import lombok.RequiredArgsConstructor;

// 无门槛
@RequiredArgsConstructor
public class NoThresholdDiscount implements Discount {

    // 规则的模板
    private static final String RULE_TEMPLATE = "无门槛抵{}元";

    @Override
    public boolean canUse(int totalAmout, Coupon coupon) {
        return totalAmout > coupon.getDiscountValue(); // 无门槛、但是抵值后得大于1
    }

    @Override
    public int calculateDiscount(int totalAmout, Coupon coupon) {
        return coupon.getDiscountValue();
    }

    @Override
    public String getRule(Coupon coupon) {
        return StringUtils.format(RULE_TEMPLATE, NumberUtils.scaleToStr(coupon.getDiscountValue(), 2));
    }
}
