package com.tianji.learning.mapper;

import com.tianji.api.dto.IdAndNumDTO;
import com.tianji.learning.domain.po.LearningRecord;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.lettuce.core.dynamic.annotation.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * <p>
 * 学习记录表 Mapper 接口
 * </p>
 *
 * @author 虎哥
 * @since 2026-03-18
 */
public interface LearningRecordMapper extends BaseMapper<LearningRecord> {

    List<IdAndNumDTO> countLearningSections(
            @Param("userId") Long userId,
            @Param("beginTime") LocalDateTime beginTime,
            @Param("endTime") LocalDateTime endTime);
}
