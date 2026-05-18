package com.tianji.promotion.controller;


import com.tianji.api.dto.promotion.CouponDiscountDTO;
import com.tianji.api.dto.promotion.OrderCourseDTO;
import com.tianji.promotion.service.IDiscountService;
import com.tianji.promotion.service.IUserCouponService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * <p>
 * 用户领取优惠券的记录，是真正使用的优惠券信息 前端控制器
 * </p>
 *
 * @author 虎哥
 * @since 2026-04-28
 */
@RestController
@RequestMapping("/user-coupons")
@RequiredArgsConstructor
@Api(tags = "优惠券相关接口")
public class UserCouponController {

    private final IUserCouponService userCouponService;

    private final IDiscountService discountService;

    //  获取老师提供的lua脚本视频资料、快速入门lua脚本、、根据脚本改造领取优惠券代码
    //  1、编写手动领取优惠券的lua脚本逻辑
    //  2、编写兑换码领取优惠券的lua脚本逻辑

    @PostMapping("/{couponId}/receive")
    @ApiOperation("领取优惠券接口")
    public void receiveCoupon(@PathVariable("couponId") Long couponId) {
        userCouponService.receiveCoupon(couponId);
    }

    /**  改造兑换码领取优惠券  根据redis  MQ
     * 1、缓存兑换码的数据-》使用Sset数据结构，menber 为 优惠券id，score为兑换码的最大序列号
     *
     * 2、利用redis进行校验
     *
     * 3、mq领取优惠券的逻辑、添加标记兑换码状态为已经兑换
     */
    @PostMapping("/{code}/exchange")
    @ApiOperation("兑换码兑换优惠券接口")
    public void exchangeCoupon(@PathVariable("code") String code) {
        userCouponService.exchangeCoupon(code);
    }


    @ApiOperation("查询我的优惠券可以方案")
    @PostMapping("/available")
    public List<CouponDiscountDTO> findCouponDiscountSolution(@RequestBody List<OrderCourseDTO> orderCourseDTOList) {
       return discountService.findCouponDiscountSolution(orderCourseDTOList);
    }



}
