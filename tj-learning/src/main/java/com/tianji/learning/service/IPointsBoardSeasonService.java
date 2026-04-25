package com.tianji.learning.service;

import com.tianji.learning.domain.po.PointsBoard;
import com.tianji.learning.domain.po.PointsBoardSeason;
import com.baomidou.mybatisplus.extension.service.IService;
import com.tianji.learning.domain.query.PointsBoardQuery;
import com.tianji.learning.domain.vo.PointsBoardVO;

import javax.validation.constraints.Min;
import java.time.LocalDateTime;
import java.util.List;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author 虎哥
 * @since 2026-04-19
 */
public interface IPointsBoardSeasonService extends IService<PointsBoardSeason> {

    PointsBoardVO queryPointsBoardBySeason(PointsBoardQuery query);

    Integer querySeasonByTime(LocalDateTime time);

    // 分页查询当前赛季榜单
    List<PointsBoard> queryCurrentBoardList(String key, @Min(value = 1, message = "页码不能小于1") Integer pageNo, @Min(value = 1, message = "每页查询数量不能小于1") Integer pageSize);
}
