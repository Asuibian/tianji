package com.tianji.learning.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tianji.api.client.course.CourseClient;
import com.tianji.api.dto.course.CourseFullInfoDTO;
import com.tianji.api.dto.leanring.LearningLessonDTO;
import com.tianji.api.dto.leanring.LearningRecordDTO;
import com.tianji.common.exceptions.BizIllegalException;
import com.tianji.common.exceptions.DbException;
import com.tianji.common.utils.UserContext;
import com.tianji.learning.domain.dto.LearningRecordFormDTO;
import com.tianji.learning.domain.po.LearningLesson;
import com.tianji.learning.domain.po.LearningRecord;
import com.tianji.learning.enums.LessonStatus;
import com.tianji.learning.enums.SectionType;
import com.tianji.learning.mapper.LearningRecordMapper;
import com.tianji.learning.service.ILearningLessonService;
import com.tianji.learning.service.ILearningRecordService;
import com.tianji.learning.utils.LearningRecordDelayTaskHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static cn.hutool.core.bean.BeanUtil.copyToList;

/**
 * <p>
 * 学习记录表 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2026-03-18
 */
@Service
@RequiredArgsConstructor
public class LearningRecordServiceImpl extends ServiceImpl<LearningRecordMapper, LearningRecord> implements ILearningRecordService {

    private final ILearningLessonService learningLessonService;

    private final CourseClient courseClient;

    private final LearningRecordDelayTaskHandler learningRecordDelayTaskHandler;

    @Override
    public LearningLessonDTO queryLearningRecordByCourse(Long courseId) {
        // 1、获取用户id
        Long userId = UserContext.getUser();
        // 2、查询课表
        LearningLesson lesson = learningLessonService.queryLessonIdByUserIdCouresId(userId, courseId);
        // 3、查询学习记录（每个人小节的学习情况）
        // 3.1根据课表id查询不同小节的学习情况
        List<LearningRecord> records = this.lambdaQuery()
                .eq(LearningRecord::getLessonId, lesson.getId())
                .list();
        // 4、封装dto
        LearningLessonDTO dto = new LearningLessonDTO();
        dto.setId(lesson.getId());
        dto.setLatestSectionId(lesson.getLatestSectionId());
//
//        List<LearningRecordDTO> recordDTOS = records.stream()
//                .map(learningRecord -> {
//                    LearningRecordDTO dto1 = new LearningRecordDTO();
//                    BeanUtils.copyProperties(learningRecord, dto1);
//                    return dto1;
//                })
//                .collect(Collectors.toList());

        List<LearningRecordDTO> learningRecordDTOS = copyToList(records, LearningRecordDTO.class);

        dto.setRecords(learningRecordDTOS);

        return dto;
    }

    @Override
    @Transactional
    public void addLearningRecord(LearningRecordFormDTO learningRecordFormDTO) {
        // 获取用户Id
        Long userId = UserContext.getUser();

        boolean finished = false; //判断是否是第一次学完，是否需要更新课表数据
        // 1、处理学习记录
        if (learningRecordFormDTO.getSectionType() == SectionType.VIDEO) {
            // 1、1处理视频
            finished = handleVideoRecord(userId, learningRecordFormDTO);
        } else {
            // 1、2处理考试
            finished = handleExamRecord(userId, learningRecordFormDTO);
        }

        if (!finished) {
            return;
        }

        // 2、处理课表数据
        handleLessonRecord(learningRecordFormDTO);

    }

    private void handleLessonRecord(LearningRecordFormDTO learningRecordFormDTO) {
        // 判断是否学完小节
        // 判断是否学完全部小节

        // 查询课表
        LearningLesson lesson = learningLessonService.getById(learningRecordFormDTO.getLessonId());
        if (lesson == null) {
            throw new BizIllegalException("课表不存在，无法更新数据！");
        }

        int learnedSections = lesson.getLearnedSections() + 1;

        boolean allLessoned = false;

        // 查询课程
        CourseFullInfoDTO cinfo = courseClient.getCourseInfoById(lesson.getCourseId(), false, false);
        if (cinfo == null) {
            throw new BizIllegalException("课程不存在，无法更新数据！");
        }
        // 比对
        allLessoned = learnedSections >= cinfo.getSectionNum();

        learningLessonService.lambdaUpdate()
                .setSql("learned_sections = learned_sections + 1")
                .set(lesson.getLearnedSections() == 0, LearningLesson::getStatus, LessonStatus.LEARNING)
                .set(allLessoned, LearningLesson::getStatus, LessonStatus.FINISHED)
                .eq(LearningLesson::getId, lesson.getId())
                .update();

    }

    private boolean handleVideoRecord(Long userId, LearningRecordFormDTO learningRecordFormDTO) {
        // 1.查询旧的学习记录  缓存
        LearningRecord oldRecord = queryOldRecord(learningRecordFormDTO.getLessonId(), learningRecordFormDTO.getSectionId());
        ;        // 2.判断是否存在
        if (oldRecord == null) {
            // 2.1不存在，新增
            // DTO封装为PO
            LearningRecord po = new LearningRecord();
            BeanUtils.copyProperties(learningRecordFormDTO, po);

            po.setUserId(userId);

            boolean save = save(po);
            if (!save) {
                throw new DbException("新增学习记录失败");
            }
            return false;
        }

        // 2.2存在，更新
        // 判断是否学习完成,学完才给添加，true表示学完
        //falsee 表示没有学完，表示之前没有学完过
        boolean finish = !oldRecord.getFinished() && oldRecord.getMoment() << 1 >= learningRecordFormDTO.getDuration();

        // 如果不是第一次、、学完
        if (!finish) {
            LearningRecord re = new LearningRecord();
            re.setLessonId(oldRecord.getLessonId());
            re.setSectionId(oldRecord.getSectionId());
            re.setId(oldRecord.getId());
            re.setMoment(oldRecord.getMoment());
            re.setFinished(oldRecord.getFinished());

            learningRecordDelayTaskHandler.addLearningRecordTask(re);
            return false;
        }

        Integer moment = learningRecordFormDTO.getMoment();

        boolean succuss = lambdaUpdate()
                .set(LearningRecord::getMoment, moment)
                .set(LearningRecord::getFinished, true)
                .set(LearningRecord::getFinishTime, learningRecordFormDTO.getCommitTime())
                .eq(LearningRecord::getId, oldRecord.getId())
                .update();
        if (!succuss) {
            throw new DbException("修改学习记录失败");
        }

        // 清理缓存
        learningRecordDelayTaskHandler.cleanLearningRecord(learningRecordFormDTO.getLessonId(), learningRecordFormDTO.getSectionId());


        return true;
    }


    private LearningRecord queryOldRecord(Long lessonId, Long sectionId) {
        // 1.查询缓存
        LearningRecord learningRecord = learningRecordDelayTaskHandler.readLearningRecord(lessonId, sectionId);
        // 2.命中，直接返回
        if (learningRecord != null) {
            return learningRecord;
        }
        // 3.未命中，查询数据库
        LearningRecord record = this.lambdaQuery()
                .eq(LearningRecord::getSectionId, sectionId)
                .eq(LearningRecord::getLessonId, lessonId)
                .one();
        // 4.写入缓存
        learningRecordDelayTaskHandler.writeRecordCache(record);
        return record;
    }

    private boolean handleExamRecord(Long userId, LearningRecordFormDTO learningRecordFormDTO) {
        // 1、DTO封装为PO
        LearningRecord po = new LearningRecord();
        BeanUtils.copyProperties(learningRecordFormDTO, po);

        po.setUserId(userId);
        po.setFinished(true);
        po.setFinishTime(learningRecordFormDTO.getCommitTime());

        boolean save = save(po);
        if (!save) {
            throw new DbException("新增考试记录失败");
        }

        return true;
    }
}
