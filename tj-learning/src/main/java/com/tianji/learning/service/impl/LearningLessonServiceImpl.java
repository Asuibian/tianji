package com.tianji.learning.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tianji.api.client.course.CatalogueClient;
import com.tianji.api.client.course.CourseClient;
import com.tianji.api.dto.course.CataSimpleInfoDTO;
import com.tianji.api.dto.course.CourseFullInfoDTO;
import com.tianji.api.dto.course.CourseSimpleInfoDTO;
import com.tianji.api.dto.trade.OrderBasicDTO;
import com.tianji.common.domain.dto.PageDTO;
import com.tianji.common.domain.query.PageQuery;
import com.tianji.common.exceptions.BadRequestException;
import com.tianji.common.exceptions.BizIllegalException;
import com.tianji.common.utils.CollUtils;
import com.tianji.common.utils.UserContext;
import com.tianji.learning.domain.po.LearningLesson;
import com.tianji.learning.domain.vo.LearningLessonVO;
import com.tianji.learning.domain.vo.LearningNowVO;
import com.tianji.learning.enums.LessonStatus;
import com.tianji.learning.mapper.LearningLessonMapper;
import com.tianji.learning.service.ILearningLessonService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <p>
 * 学生课程表 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2026-03-12
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LearningLessonServiceImpl extends ServiceImpl<LearningLessonMapper, LearningLesson> implements ILearningLessonService {

    private final CourseClient courseClient;

    private final CatalogueClient catalogueClient;

    @Override
    @Transactional
    public void addUserLessons(Long userId, List<Long> courseIds) {
        // 1、需要以下字段数据  学员id 课程id 创建时间  过期时间
        //1.1查询课程过期时间
        List<CourseSimpleInfoDTO> cInfoList = courseClient.getSimpleInfoList(courseIds);
        if(CollUtils.isEmpty(cInfoList)){
            log.error("课程id查询不到课程信息，无法添加到课表中");
            return;
        }
        // 2、批量封装LearningLesson
        List<LearningLesson> learningLessons = new ArrayList<>();
        for (CourseSimpleInfoDTO cInfo : cInfoList) {
            //封装
            LearningLesson learningLesson = new LearningLesson();
            // 过期时间不是每个课程都有，存在免费课程
            LocalDateTime now = LocalDateTime.now();
            learningLesson.setCreateTime(now);

            Integer validDuration = cInfo.getValidDuration();
            if(validDuration != null && validDuration > 0){
                learningLesson.setExpireTime(now.plusMonths(validDuration));
            }

            learningLesson.setUserId(userId);
            learningLesson.setCourseId(cInfo.getId());
            learningLessons.add(learningLesson);
        }
        // 3、批量新增
        saveBatch(learningLessons);

    }

    /**
     * 分页查询我的课表
     * @param query
     * @return
     */
    public PageDTO<LearningLessonVO> queryMyLessons(PageQuery query) {
        // 1、获取当前登录用户
        Long userId = UserContext.getUser();

        // 2、分页查询
        Page<LearningLesson> page = lambdaQuery()
                .eq(LearningLesson::getUserId, userId)
                .page(query.toMpPage("latest_learn_time",false));
        List<LearningLesson> records = page.getRecords();
        if(CollUtils.isEmpty(records)){
            return PageDTO.empty(page);
        }
        // 3、获取课程信息
        // 3.1获取课程id
        Set<Long> cIds = records.stream().map(LearningLesson::getCourseId).collect(Collectors.toSet());
        /**
         * map 是中间操作，将每个元素转换为另一种形式
         *
         * LearningLesson::getCourseId 是方法引用，等价于 lesson -> lesson.getCourseId()
         *
         * 将每个 LearningLesson 对象映射为它的 courseId（Long 类型）
         */

        // 3.2查询课程信息
        List<CourseSimpleInfoDTO> cInfoList = courseClient.getSimpleInfoList(cIds);
        if(CollUtils.isEmpty(cInfoList)){
           throw new BadRequestException("课程信息不存在");
        }

        // 3.3把课程集合处理成Map，key是课程id，值是本身
        Map<Long, CourseSimpleInfoDTO> cInfoMap = cInfoList.stream().collect(Collectors.toMap(CourseSimpleInfoDTO::getId, c -> c));

        // 4、封装VO返回
        List<LearningLessonVO> list = new ArrayList<>(records.size());//集合扩容会增加消耗的资源
        for (LearningLesson r : records) {
            // 属性拷贝
            LearningLessonVO vo = new LearningLessonVO();
            BeanUtils.copyProperties(r,vo);

            // 课程信息对应，r里面已经属性拷贝了课程id，
            CourseSimpleInfoDTO cInfo = cInfoMap.get(r.getCourseId());
            vo.setCourseName(cInfo.getName());
            vo.setCourseCoverUrl(cInfo.getCoverUrl());
            vo.setSections(cInfo.getSectionNum());

            list.add(vo);
        }
        // 总条数，总页数，集合
        return new PageDTO<>(page.getTotal(),page.getPages(),list);
    }

    @Override
    public LearningNowVO queryLearningNow() {
        LearningNowVO vo = new LearningNowVO();

        //获取当前用户
        Long userId = UserContext.getUser();
        //查询当前用户最近学习的课程
        //根据需要查询的表的PO去查询
        LearningLesson lesson = this.lambdaQuery()
                .eq(LearningLesson::getUserId,userId)
                .eq(LearningLesson::getStatus, LessonStatus.LEARNING)
                .orderByDesc(LearningLesson::getLatestLearnTime)
                .last("limit 1") //限制只返回一条数据
                .one();
        if(lesson == null){
            return null;
        }

        // 查询到封装VO对象的数据
        // 根据课程id去查询，课程名称，封面，总节数
        CourseFullInfoDTO cinfo = courseClient.getCourseInfoById(lesson.getCourseId(), false, false);
        if (cinfo == null) {
            throw new BizIllegalException("课程不存在");
        }

        // 获取用户学习的总课程数量
        Integer count = this.lambdaQuery()
                .eq(LearningLesson::getUserId,userId)
                .count();

        // 根据小节ID查询目录详细信息
        Long latestSectionId = lesson.getLatestSectionId();
        List<CataSimpleInfoDTO> cataSimpleInfoDTOS = catalogueClient.batchQueryCatalogue(CollUtils.singletonList(latestSectionId));
        if (cataSimpleInfoDTOS == null){
            throw new BizIllegalException("小节不存在");
        }

        BeanUtils.copyProperties(lesson,vo);
        vo.setCourseName(cinfo.getName());
        vo.setCourseCoverUrl(cinfo.getCoverUrl());
        vo.setSections(cinfo.getSectionNum());

        CataSimpleInfoDTO cataSimpleInfoDTO = cataSimpleInfoDTOS.get(0);
        vo.setLatestSectionName(cataSimpleInfoDTO.getName());
        vo.setLatestSectionIndex(cataSimpleInfoDTO.getCIndex());

        return vo;
    }



    public LearningLessonVO queryCourseLearningStatus(Long courseId) {
        Long userId = UserContext.getUser();
        // 得到课程学习状态，已经学习课时数，加入课表时间，过期时间
        LearningLesson lesson = this.lambdaQuery()
                .eq(LearningLesson::getUserId,userId)
                .eq(LearningLesson::getCourseId,courseId)
                .one();
        if(lesson == null){
            return null;
        }


        LearningLessonVO vo = new LearningLessonVO();
        BeanUtils.copyProperties(lesson,vo);

        return vo;
    }


    public void deleteLearningLesson(Long courseId) {
        Long userId = UserContext.getUser();
        this.lambdaUpdate()
                .eq(LearningLesson::getUserId,userId)
                .eq(LearningLesson::getCourseId,courseId)
                .remove();
    }


    public void deleteRefundLearningLesson(OrderBasicDTO order) {
        this.lambdaUpdate()
                .in(LearningLesson::getCourseId,order.getCourseIds())
                .eq(LearningLesson::getUserId,order.getUserId())
                .remove();
    }


    public Integer countLearningLessonByCourse(Long courseId) {
        Integer count = this.lambdaQuery()
                .eq(LearningLesson::getCourseId, courseId)
                .count();
        return count;
    }


    public Long isLessonValid(Long courseId) {
        //用户是否报名
        Long userId = UserContext.getUser();
        LearningLesson vo = this.lambdaQuery()
                .eq(LearningLesson::getUserId,userId)
                .eq(LearningLesson::getCourseId,courseId)
                .one();
        // 没有报名
        if (vo == null) {
            return null;
        }

        //用户课程是否过期
        LocalDateTime expireTime = vo.getExpireTime();
        if(expireTime != null && LocalDateTime.now().isAfter(expireTime)){
            return null;
        }
        return vo.getId();
    }
}
























