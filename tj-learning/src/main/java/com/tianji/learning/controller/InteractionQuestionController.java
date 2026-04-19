package com.tianji.learning.controller;


import com.tianji.common.domain.dto.PageDTO;
import com.tianji.learning.domain.dto.QuestionFormDTO;
import com.tianji.learning.domain.po.InteractionQuestion;
import com.tianji.learning.domain.query.QuestionPageQuery;
import com.tianji.learning.domain.vo.QuestionVO;
import com.tianji.learning.service.IInteractionQuestionService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

/**
 * <p>
 * 互动提问的问题表 前端控制器
 * </p>
 *
 * @author 虎哥
 * @since 2026-03-30
 */
@RestController
@RequestMapping("/questions")
@Api(tags = "互动问答的相关接口")
@RequiredArgsConstructor
public class InteractionQuestionController {

    private final IInteractionQuestionService questionService;

    @ApiOperation("新增互动问题")
    @PostMapping
    public void saveQuestion(@Valid //代表让springMVC校验po方法中有校验注解的属性
            @RequestBody QuestionFormDTO questionDTO) {
        questionService.saveQuestion(questionDTO);
    }


    @ApiOperation("分页查询互动问题")
    @GetMapping("/page")
    public PageDTO<QuestionVO> queryQuestionsPage(QuestionPageQuery query){
        // GetMapping url编码传递参数，不需要注解
        return questionService.queryQuestionsPage(query);
    }

    @ApiOperation("根据id查询互动问题")
    @GetMapping("/{id}")
    public QuestionVO queryQuestionsById(@PathVariable("id") Long id) {
        return questionService.queryQuestionsById(id);
    }




}































