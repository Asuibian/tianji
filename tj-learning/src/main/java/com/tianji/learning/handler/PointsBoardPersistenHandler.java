package com.tianji.learning.handler;

import com.tianji.common.utils.DateUtils;
import com.tianji.learning.constans.RedisConstans;
import com.tianji.learning.domain.po.PointsBoard;
import com.tianji.learning.service.IPointsBoardSeasonService;
import com.tianji.learning.service.IPointsBoardService;
import com.tianji.learning.utils.TableInfoContext;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class PointsBoardPersistenHandler {

    private final IPointsBoardSeasonService pointsBoardSeasonService;

    private final IPointsBoardService pointsBoardService;

    private final StringRedisTemplate redisTemplate;

//    @Scheduled(cron = "0 0 3 1 * ?")
    @XxlJob("createTableJob")
    public void createPointsBoardTableOflastSeason() {
        // 1.获取上月时间
        LocalDateTime  time = LocalDateTime.now().minusDays(2);
        // 2.查询赛季id
        Integer season = pointsBoardSeasonService.querySeasonByTime(time);
        if (season == null) {
            return;
        }
        // 3.创建表结构
        pointsBoardService.createPointsBoardTableBySeason(season);
    }

    @XxlJob("savePointsBoard2DB")
    public void savePointsBoard2DB() {
        // 1.获取上月时间
        LocalDateTime  time = LocalDateTime.now().minusDays(2);

        // 计算动态表名
        Integer season = pointsBoardSeasonService.querySeasonByTime(time);
        // 存入ThreadLocal
        TableInfoContext.setInfo("points_board_" + season);

        // 2.查询榜单信息
        // 2.1 拼接key
        String key = RedisConstans.POINTS_BOARD_KEY_PREFIX + time.format(DateUtils.POINTS_BOARD_SUFFIX_FORMATTER);

        // 采用分片广播的方式。
        int shardIndex = XxlJobHelper.getShardIndex();
        int shardTotal = XxlJobHelper.getShardTotal();
        int pageNo = shardIndex + 1;
        int pageSize = 1000;
        while (true) {
            // 2.2 分页查询数据
            List<PointsBoard> list = pointsBoardSeasonService.queryCurrentBoardList(key, pageNo, pageSize);
            if (list == null || list.size() == 0) {
                break;
            }
            // 3.持久化到数据库
            // 把排名信息写入id
            list.forEach(pointsBoard -> {
                pointsBoard.setId(pointsBoard.getRank().longValue());
                pointsBoard.setRank(null);
            });
            pointsBoardService.saveBatch(list);
            // 翻页
            pageNo+=shardTotal;
        }
        TableInfoContext.clear();
    }

    @XxlJob("clearPointsBoardFromRedis")
    public void clearPointsBoardFromRedis() {
        // 1.获取上月时间
        LocalDateTime  time = LocalDateTime.now().minusDays(2);
        // 2.拼接key
        String key = RedisConstans.POINTS_BOARD_KEY_PREFIX + time.format(DateUtils.POINTS_BOARD_SUFFIX_FORMATTER);
        // 3.删除 unlink命令，公共命令，创建一个子线程取删除
        redisTemplate.unlink(key);

    }




}
