package com.tianji.learning.service.impl;

import com.tianji.learning.domain.po.PointsBoard;
import com.tianji.learning.mapper.PointsBoardMapper;
import com.tianji.learning.service.IPointsBoardService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 学霸天梯榜 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2026-04-19
 */
@Service
public class PointsBoardServiceImpl extends ServiceImpl<PointsBoardMapper, PointsBoard> implements IPointsBoardService {

    @Override
    public void createPointsBoardTableBySeason(Integer season) {

        getBaseMapper().createPointsBoardTable("points_board_" + season);

    }
}
