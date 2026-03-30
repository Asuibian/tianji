package com.tianji.learning.controller;


import com.tianji.common.domain.dto.PageDTO;
import com.tianji.common.domain.query.PageQuery;
import com.tianji.learning.domain.dto.LearningPlanDTO;
import com.tianji.learning.domain.vo.LearningLessonVO;
import com.tianji.learning.domain.vo.LearningNowVO;
import com.tianji.learning.domain.vo.LearningPlanPageVO;
import com.tianji.learning.domain.vo.LearningPlanVO;
import com.tianji.learning.service.ILearningLessonService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

/**
 * <p>
 * 学生课程表 前端控制器
 * </p>
 *
 * @author 虎哥
 * @since 2026-03-12
 */
@RestController
@RequestMapping("/lessons")
@Api(tags = "我的课表相关接口")
@RequiredArgsConstructor
@Slf4j
public class LearningLessonController {

    private final ILearningLessonService learningLessonService;

    @GetMapping("/page")
    @ApiOperation("分页查询我的课表")
    public PageDTO<LearningLessonVO> queryMyLessons(PageQuery query) {
        return learningLessonService.queryMyLessons(query);
    }

    @GetMapping("/now")
    @ApiOperation("查询最近正在学习的课程")
    public LearningNowVO queryLearningNow() {
        LearningNowVO vo = new LearningNowVO();
        vo = learningLessonService.queryLearningNow();
        return vo;
    }

    @GetMapping("/{courseId}")
    @ApiOperation("查询指定课程学习状态")
    public LearningLessonVO queryLearningLesson(@PathVariable("courseId") Long courseId) {
        log.info("查询指定课程学习状态........................");
        LearningLessonVO vo = new LearningLessonVO();
        vo = learningLessonService.queryCourseLearningStatus(courseId);
        return vo;
    }


    @DeleteMapping("/{courseId}")
    @ApiOperation("删除课表中的某课程")
    public void deleteLearningLesson(@PathVariable("courseId") Long courseId) {
        learningLessonService.deleteLearningLesson(courseId);
    }

    /**
     * 统计课程学习人数
     *
     * @param courseId 课程id
     * @return 学习人数
     */
    @GetMapping("/lessons/{courseId}/count")
    @ApiOperation("统计课程学习人数")
    public Integer countLearningLessonByCourse(@PathVariable("courseId") Long courseId) {
        Integer count = learningLessonService.countLearningLessonByCourse(courseId);
        return count;
    }


    /**
     * 校验当前用户是否可以学习当前课程
     *
     * @param courseId 课程id
     * @return lessonId，如果是报名了则返回lessonId，否则返回空
     */
    @GetMapping("/lessons/{courseId}/valid")
    @ApiOperation("校验当前用户是否可以学习当前课程")
    public Long isLessonValid(@PathVariable("courseId") Long courseId) {
        return learningLessonService.isLessonValid(courseId);
    }


    @ApiOperation("创建学习计划")
    @PostMapping("/plans")
    public void createLearningPlans(@Valid @RequestBody LearningPlanDTO planDTO){
        learningLessonService.createLearningPlans(planDTO.getFreq(),planDTO.getCourseId());
    }

    @ApiOperation("查询我的学习计划")
    @GetMapping("/plans")
    public LearningPlanPageVO queryMyPlans(PageQuery query){
        return learningLessonService.queryMyPlans(query);
    }










































}
