package com.tianji.promotion.controller;


import cn.hutool.db.Page;
import com.tianji.common.domain.dto.PageDTO;
import com.tianji.promotion.domain.dto.CouponFormDTO;
import com.tianji.promotion.domain.dto.CouponIssueFormDTO;
import com.tianji.promotion.domain.po.Coupon;
import com.tianji.promotion.domain.query.CouponQuery;
import com.tianji.promotion.domain.vo.CouponDetailVO;
import com.tianji.promotion.domain.vo.CouponPageVO;
import com.tianji.promotion.domain.vo.CouponVO;
import com.tianji.promotion.service.ICouponService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

/**
 * <p>
 * 优惠券的规则信息 前端控制器
 * </p>
 *
 * @author 虎哥
 * @since 2026-04-25
 */
@RestController
@RequestMapping("/coupons")
@RequiredArgsConstructor
@Api(tags = "优惠券相关接口")
public class CouponController {

    private final ICouponService couponService;

    @PostMapping
    @ApiOperation("新增优惠券")
    public void saveCoupon(@RequestBody @Valid CouponFormDTO dto) {
        couponService.saveCoupon(dto);
    }


    @GetMapping("/page")
    @ApiOperation("分页查询优惠券")
    public PageDTO<CouponPageVO> queryCouponBypage(CouponQuery query) {
        return couponService.queryCouponBypage(query);
    }

    @ApiOperation("发放优惠券")
    @PutMapping("/{id}/issue")
    public void beginIssueCoupon(@RequestBody @Valid CouponIssueFormDTO dto) {
        couponService.beginIssueCoupon(dto);
    }


    @ApiOperation("暂停优惠券")
    @PutMapping("/{id}/pause")
    public void pauseIssue(@ApiParam("优惠券id") @PathVariable Long id) {
        couponService.pauseIssue(id);
    }



    // 根据id查询优惠券信息
    @GetMapping("/{id}")
    @ApiOperation("根据id查询优惠券信息")
    public CouponDetailVO queryDetailCouponById(@PathVariable Long id) {
        return couponService.queryDetailCouponById(id);
    }

    // 修改优惠券信息
    @ApiOperation("修改优惠券")
    @PutMapping("/{id}")
    public void updateById(@RequestBody @Valid CouponFormDTO dto, @PathVariable("id") Long id){
        couponService.updateCouponById(dto, id);
    }

    // 删除优惠券
    @ApiOperation("删除优惠券")
    @DeleteMapping("{id}")
    public void deleteById(@ApiParam("优惠券id") @PathVariable("id") Long id){
        couponService.deleteById(id);
    }

    @GetMapping("/list")
    @ApiOperation("查询发放中的优惠券列表")
    public List<CouponVO> queryIssuingCoupons() {
        return couponService.queryIssuingCoupons();
    }


}
