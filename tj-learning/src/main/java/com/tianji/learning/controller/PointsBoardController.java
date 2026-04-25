package com.tianji.learning.controller;


import com.tianji.learning.domain.query.PointsBoardQuery;
import com.tianji.learning.domain.vo.PointsBoardVO;
import com.tianji.learning.service.IPointsBoardSeasonService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.RestController;

/**
 * <p>
 * 学霸天梯榜 前端控制器
 * </p>
 *
 * @author 虎哥
 * @since 2026-04-19
 */
@RestController
@RequestMapping("/boards")
@Api(tags = "积分相关接口")
@RequiredArgsConstructor
public class PointsBoardController {

    private final IPointsBoardSeasonService pointsBoardSeasonService;

    @GetMapping
    @ApiOperation("分页查询指定赛季的积分排行榜")
    public PointsBoardVO queryPointsBoardBySeason(PointsBoardQuery query) {
        return pointsBoardSeasonService.queryPointsBoardBySeason(query);
    }

}
