package com.tianji.promotion.strategy.discount;

import com.tianji.common.utils.NumberUtils;
import com.tianji.common.utils.SignUtils;
import com.tianji.common.utils.StringUtils;
import com.tianji.promotion.domain.po.Coupon;
import lombok.RequiredArgsConstructor;

// 每满减
@RequiredArgsConstructor
public class PerPriceDiscount implements Discount {

    // 规则的模板
    private static final String RULE_TEMPLATE = "每满{}减{}，上限{}";


    @Override
    public boolean canUse(int totalAmout, Coupon coupon) {
        return totalAmout >= coupon.getThresholdAmount();
    }

    // 返回优惠金额
    @Override
    public int calculateDiscount(int totalAmout, Coupon coupon) {
        int discount = 0;
        // 满多少
        Integer thresholdAmount = coupon.getThresholdAmount();
        // 减多少
        Integer discountValue = coupon.getDiscountValue();

        while (totalAmout >= thresholdAmount) {
            discount += discountValue;
            totalAmout -= thresholdAmount;
        }

        return Math.min(discount,coupon.getMaxDiscountAmount());
    }

    @Override
    public String getRule(Coupon coupon) {
        return StringUtils.format(RULE_TEMPLATE,
                NumberUtils.scaleToStr(coupon.getThresholdAmount(), 2),
                NumberUtils.scaleToStr(coupon.getDiscountValue(), 2),
                NumberUtils.scaleToStr(coupon.getMaxDiscountAmount(), 2)
                );
    }
}
