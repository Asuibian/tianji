package com.tianji.learning.controller;


import com.tianji.common.domain.dto.PageDTO;
import com.tianji.common.domain.query.PageQuery;
import com.tianji.learning.domain.dto.ReplayDTO;
import com.tianji.learning.domain.po.InteractionReply;
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
@RequestMapping("/replies")
@RequiredArgsConstructor
@Api(tags = "互动问题的回答或评论")
public class InteractionReplyController {

    private final IInteractionReplyService iInteractionReplyService;

    // 新增回答、评论
    @PostMapping
    @ApiOperation("新增回答、评论")
    public void addAnswerOrComment(@RequestBody ReplayDTO replayDTO) {
        iInteractionReplyService.addAnswerOrComment(replayDTO);
    }

    // 分页查询用户端回答、评论
    @GetMapping("/page")
    @ApiOperation("分页查询用户端回答、评论")
    public PageDTO<ReplyVO> pageMyReplies (ReplyPageQuery pageQuery) {
        return iInteractionReplyService.pageMyReplies(pageQuery);
    }



}
