package com.tianji.learning.controller;


import com.tianji.common.domain.dto.PageDTO;
import com.tianji.learning.domain.dto.ReplayDTO;
import com.tianji.learning.domain.query.ReplyPageQuery;
import com.tianji.learning.domain.vo.ReplyVO;
import com.tianji.learning.service.IInteractionReplyService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * <p>
 * 互动问题的回答或评论 前端控制器
 * </p>
 *
 * @author 虎哥
 * @since 2026-03-30
 */
@RestController
@RequestMapping("/admin/replies")
@RequiredArgsConstructor
@Api(tags = "管理端互动问题的回答或评论")
public class InteractionReplyAdminController {

    private final IInteractionReplyService iInteractionReplyService;


    // 分页查询管理端回答、评论
    @GetMapping("/page")
    @ApiOperation("分页查询管理端回答、评论")
    public PageDTO<ReplyVO> queryAdminRepliesPage(ReplyPageQuery replyPageQuery) {
        return iInteractionReplyService.queryAdminRepliesPage(replyPageQuery);

    }


    // 管理端隐藏指定回答、评论
    @PutMapping("/{id}/hidden/{hidden}")
    @ApiOperation("管理端隐藏指定回答、评论")
    public void hideReplyById(@PathVariable Long id, @PathVariable Boolean hidden) {
        iInteractionReplyService.hideReplyById(id,hidden);
    }


}
