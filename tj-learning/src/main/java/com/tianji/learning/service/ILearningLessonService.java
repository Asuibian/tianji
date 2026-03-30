package com.tianji.learning.service;

import com.tianji.api.dto.trade.OrderBasicDTO;
import com.tianji.common.domain.dto.PageDTO;
import com.tianji.common.domain.query.PageQuery;
import com.tianji.learning.domain.po.LearningLesson;
import com.baomidou.mybatisplus.extension.service.IService;
import com.tianji.learning.domain.vo.LearningLessonVO;
import com.tianji.learning.domain.vo.LearningNowVO;
import com.tianji.learning.domain.vo.LearningPlanPageVO;
import org.hibernate.validator.constraints.Range;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * <p>
 * 学生课程表 服务类
 * </p>
 *
 * @author 虎哥
 * @since 2026-03-12
 */
public interface ILearningLessonService extends IService<LearningLesson> {

    /**
     * 监听MQ消息异步添加课程到当前用户课表中
     * @param userId
     * @param courseIds
     */
    void addUserLessons(Long userId, List<Long> courseIds);

    /**
     * 分页查询我的课表
     * @param query
     * @return
     */
    PageDTO<LearningLessonVO> queryMyLessons(PageQuery query);

    LearningNowVO queryLearningNow();

    LearningLessonVO queryCourseLearningStatus(Long courseId);

    void deleteLearningLesson(Long courseId);

    void deleteRefundLearningLesson(OrderBasicDTO order);

    Integer countLearningLessonByCourse(Long courseId);

    Long isLessonValid(Long courseId);

    LearningLesson queryLessonIdByUserIdCouresId(Long userId, Long courseId);

    void createLearningPlans(@NotNull @Range(min = 1, max = 50) Integer freq, @NotNull @Min(1) Long courseId);

    LearningPlanPageVO queryMyPlans(PageQuery query);
}
