package com.tianji.learning.mapper;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.tianji.learning.domain.po.PointsRecord;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.aspectj.apache.bcel.classfile.ConstantString;

import java.util.List;

/**
 * <p>
 * 学习积分记录，每个月底清零 Mapper 接口
 * </p>
 *
 * @author 虎哥
 * @since 2026-04-19
 */
public interface PointsRecordMapper extends BaseMapper<PointsRecord> {

    @Select("SELECT SUM(points) FROM tj_learning.points_record ${ew.customSqlSegment}")
    Integer queryUserPointByTypeAndDate(@Param("ew") QueryWrapper<PointsRecord> queryWrapper);

    @Select("SELECT tj_learning.points_record.type , SUM(points) as points FROM tj_learning.points_record ${ew.customSqlSegment} GROUP BY tj_learning.points_record.type")
    List<PointsRecord> queryUserPointByDate(@Param("ew")QueryWrapper<PointsRecord> queryWrapper);
}
