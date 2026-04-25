package com.tianji.learning.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.tianji.common.utils.CollUtils;
import com.tianji.common.utils.DateUtils;
import com.tianji.common.utils.UserContext;
import com.tianji.learning.constans.RedisConstans;
import com.tianji.learning.domain.po.PointsRecord;
import com.tianji.learning.domain.vo.PointsStatisticsVO;
import com.tianji.learning.enums.PointsRecordType;
import com.tianji.learning.mapper.PointsRecordMapper;
import com.tianji.learning.service.IPointsRecordService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * 学习积分记录，每个月底清零 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2026-04-19
 */
@Service
@RequiredArgsConstructor
public class PointsRecordServiceImpl extends ServiceImpl<PointsRecordMapper, PointsRecord> implements IPointsRecordService {

    private final StringRedisTemplate redisTemplate;

    @Override
    public void addPointsRecord(Long userId, int point, PointsRecordType type) {
        LocalDateTime now = LocalDateTime.now();
       int maxPoints = type.getMaxPoints();
       int realPoint = point;
       // 1.判断当前方式有没有积分上限
        if (maxPoints > 0) {
            // 2.有，则需要判断是否超过上限
            LocalDateTime begin = now.toLocalDate().atStartOfDay();
            LocalDateTime end = LocalDateTime.of(now.toLocalDate(), LocalTime.MAX);
            // 2.1 查询今日已经得到的积分
            int currentPoints = queryUserPointByTypeAndDate(
                    userId,type,begin,end
            );
            // 2.2判断是否超过上限
            if (currentPoints >= maxPoints) {
                return;
            }
            // 2.3没超过，保存积分记录，判断当前积分加上已经得到的积分，超过上限没
            if (currentPoints + point > maxPoints) {
                realPoint = maxPoints - point;
            }
        }
        // 3.没有，直接保存积分记录
        PointsRecord record = new PointsRecord();
        record.setUserId(userId);
        record.setPoints(realPoint);
        record.setType(type);
        save(record);

        // 4.累积积分数据到Redis的SortedSet中
        String key = RedisConstans.POINTS_BOARD_KEY_PREFIX + now.format(DateUtils.POINTS_BOARD_SUFFIX_FORMATTER);
        redisTemplate.opsForZSet()
                .incrementScore(key,userId.toString(),realPoint);


    }

    @Override
    public List<PointsStatisticsVO> queryPointsToday() {
        // 1.获取用户信息
        Long userId = UserContext.getUser();
        // 2.获取日期
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime begin = LocalDateTime.of(now.toLocalDate(), LocalTime.MIN);
        LocalDateTime end = LocalDateTime.of(now.toLocalDate(), LocalTime.MAX);
        // 3.构建查询条件
        QueryWrapper<PointsRecord> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda()
                .eq(PointsRecord::getUserId, userId)
                .between(PointsRecord::getCreateTime, begin, end);
        // 4.查询
        List<PointsRecord> po = getBaseMapper().queryUserPointByDate(queryWrapper);
        if (po == null || po.isEmpty()) {
            return CollUtils.emptyList();
        }
        // 5.封装返回
        List<PointsStatisticsVO> vos = new ArrayList<>(po.size());
        for (PointsRecord p : po) {
            PointsStatisticsVO vo = new PointsStatisticsVO();
            vo.setMaxPoints(p.getType().getMaxPoints());
            vo.setType(p.getType().getDesc());
            vo.setPoints(p.getPoints());
            vos.add(vo);
        }
        return vos;
    }

    private int queryUserPointByTypeAndDate(
            Long userId, PointsRecordType type, LocalDateTime begin, LocalDateTime end) {
        QueryWrapper<PointsRecord> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda()
                .eq(PointsRecord::getUserId, userId)
                .eq(type!=null,PointsRecord::getType, type)
                .between(begin !=null && end !=null,PointsRecord::getCreateTime, begin, end);
        Integer point = getBaseMapper().queryUserPointByTypeAndDate(queryWrapper);

        return point == null ? 0 : point;

    }
}







































































