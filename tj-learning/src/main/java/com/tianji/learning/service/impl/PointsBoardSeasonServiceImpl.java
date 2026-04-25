package com.tianji.learning.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tianji.api.client.user.UserClient;
import com.tianji.api.dto.user.UserDTO;
import com.tianji.common.utils.CollUtils;
import com.tianji.common.utils.DateUtils;
import com.tianji.common.utils.UserContext;
import com.tianji.learning.constans.RedisConstans;
import com.tianji.learning.domain.po.PointsBoard;
import com.tianji.learning.domain.po.PointsBoardSeason;
import com.tianji.learning.domain.query.PointsBoardQuery;
import com.tianji.learning.domain.vo.PointsBoardItemVO;
import com.tianji.learning.domain.vo.PointsBoardVO;
import com.tianji.learning.mapper.PointsBoardSeasonMapper;
import com.tianji.learning.service.IPointsBoardSeasonService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.BoundZSetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import javax.validation.constraints.Min;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2026-04-19
 */
@Service
@RequiredArgsConstructor
public class PointsBoardSeasonServiceImpl extends ServiceImpl<PointsBoardSeasonMapper, PointsBoardSeason> implements IPointsBoardSeasonService {

    private final StringRedisTemplate redisTemplate;

    private final UserClient userClient;

    /**
     * 分页查询指定赛季的积分排行榜 和 我都排行和积分
     *
     * @param query
     * @return
     */
    @Override
    public PointsBoardVO queryPointsBoardBySeason(PointsBoardQuery query) {
        // 1.判断是否时当前赛季
        Long season = query.getSeason();
        boolean isCurrent = season == null || season == 0;  // true表示season为0或者null，表示查询当前赛季
        // 获取redis的key
        LocalDateTime now = LocalDateTime.now();
        String key = RedisConstans.POINTS_BOARD_KEY_PREFIX + now.format(DateUtils.POINTS_BOARD_SUFFIX_FORMATTER);
        // 2.查询我的积分和排名
        PointsBoard myBoard = isCurrent ?
                queryCurrentBoard(key) :// 当前用户的积分和排名存储在redis
                queryHistoryBoard(season);// TODO mysql

        // 3.查询榜单列表
        List<PointsBoard> list = isCurrent ?
                queryCurrentBoardList(key, query.getPageNo(), query.getPageSize()) :
                queryHistoryBoardList(query);//TODO
        // 4.封装VO
        PointsBoardVO vo = new PointsBoardVO();

        // 4.1 处理我都信息
        if(myBoard != null){
            vo.setRank(myBoard.getRank());
            vo.setPoints(myBoard.getPoints());
        }

        // 处理VO榜单信息
        if(CollUtils.isEmpty(list)){
            return vo;
        }

        // 4.2 获取用户信息
        Set<Long> uIds = list.stream().map(PointsBoard::getUserId).collect(Collectors.toSet());
        List<UserDTO> userDTOS = userClient.queryUserByIds(uIds);
        Map<Long,String> userMap = new HashMap<>(uIds.size());
        if(CollUtils.isNotEmpty(userDTOS)){
            userMap = userDTOS.stream().collect(Collectors.toMap(UserDTO::getId, UserDTO::getName));
        }
        // 4.3封装榜单列表
        List<PointsBoardItemVO> boardList = new ArrayList<>(list.size());
        for (PointsBoard p : list) {
            PointsBoardItemVO voItem = new PointsBoardItemVO();
            boardList.add(voItem);
            voItem.setName(userMap.get(p.getUserId()));
            voItem.setRank(p.getRank());
            voItem.setPoints(p.getPoints());
        }

        vo.setBoardList(boardList);

        return vo;
    }

    @Override
    public Integer querySeasonByTime(LocalDateTime time) {
        Optional<PointsBoardSeason> optional = lambdaQuery()
                .le(PointsBoardSeason::getBeginTime, time)
                .ge(PointsBoardSeason::getEndTime, time)
                .oneOpt();

        return optional.map(PointsBoardSeason::getId).orElse(null);

    }

    private List<PointsBoard> queryHistoryBoardList(PointsBoardQuery query) {
        return null;
    }

    @Override
    public List<PointsBoard> queryCurrentBoardList(String key, @Min(value = 1, message = "页码不能小于1") Integer pageNo, @Min(value = 1, message = "每页查询数量不能小于1") Integer pageSize) {
        // 1.计算分页角标
        int form = (pageNo - 1) * pageSize;
        // 2.查询
        Set<ZSetOperations.TypedTuple<String>> tuples = redisTemplate.opsForZSet().reverseRangeWithScores(key, form, form + pageSize - 1);
        if (CollUtils.isEmpty(tuples)) {
            return Collections.emptyList();
        }
        // 3.封装
        int rank = form + 1;
        List<PointsBoard> result = new ArrayList<>(tuples.size());
        for (ZSetOperations.TypedTuple<String> tuple : tuples) {
            String userId = tuple.getValue();
            Double points = tuple.getScore();
            if (userId == null || points == null) {
                continue;
            }
            PointsBoard p = new PointsBoard();
            p.setUserId(Long.valueOf(userId));
            p.setRank(rank++);
            p.setPoints(points.intValue());
            result.add(p);
        }
        return result;
    }

    private PointsBoard queryHistoryBoard(Long season) {
        return null;
    }

    // 从redis中  查询当前用户的 积分和排名 封装为PO类型
    private PointsBoard queryCurrentBoard(String key) {
        // 1.绑定key
        BoundZSetOperations<String, String> ops = redisTemplate.boundZSetOps(key);
        // 2.获取当前用户信息
        Long userId = UserContext.getUser();
        // 3.查询积分
        Double points = ops.score(userId);
        // 4.查询排名
        Long rank = ops.reverseRank(userId);
        // 5.封装返回
        PointsBoard p = new PointsBoard();
        p.setPoints(points == null ? 0 : points.intValue());
        p.setRank(rank == null ? 0 : rank.intValue() + 1);
        return p;
    }
}

































