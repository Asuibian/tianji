package com.tianji.remark.controller;


import com.tianji.remark.domain.dto.LikeRecordFormDTO;
import com.tianji.remark.service.ILikedRecordService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.Set;

/**
 * <p>
 * 点赞记录表 前端控制器
 * </p>
 *
 * @author 虎哥
 * @since 2026-04-14
 */
@RestController
@RequestMapping("/likes")
@Api(tags = "点赞相关接口")
@RequiredArgsConstructor
public class LikedRecordController {

    private final ILikedRecordService likedRecordService;

    // TODO 测试点赞  取消点赞功能是否正确
    @PostMapping
    @ApiOperation("点赞或者取消点赞")
    public void addLikedRecord(@Valid @RequestBody LikeRecordFormDTO likeRecordFormDTO) {
        likedRecordService.addLikedRecord(likeRecordFormDTO);
    }


    // TODO 测试 是否能正确查询点赞业务的id
    // 提供FeignClient接口，给分页查询回答提供指定业务id的点赞状态
    @ApiOperation("分页查询回答提供指定业务id的点赞状态")
    @GetMapping("/list")
    public Set<Long> isBizLiked(
            @RequestParam("bizIds") List<Long> bizIds // 集合类型不加 @RequestParam，Spring 可能无法正确绑定所有值；加上后能保证获取完整的参数列表。
    ) {
        return likedRecordService.isBizLiked(bizIds); // 返回的set集合 表示 当前用户已经点赞的业务id
    }


}
