package com.tianji.remark.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tianji.api.dto.remark.LikeTimesDTO;
import com.tianji.common.autoconfigure.mq.RabbitMqHelper;
import com.tianji.common.utils.CollUtils;
import com.tianji.common.utils.StringUtils;
import com.tianji.common.utils.UserContext;
import com.tianji.remark.constants.RedisConstants;
import com.tianji.remark.domain.dto.LikeRecordFormDTO;
import com.tianji.remark.domain.po.LikedRecord;
import com.tianji.remark.mapper.LikedRecordMapper;
import com.tianji.remark.service.ILikedRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.StringRedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.tianji.common.constants.MqConstants.Exchange.LIKE_RECORD_EXCHANGE;
import static com.tianji.common.constants.MqConstants.Key.LIKED_TIMES_KEY_TEMPLATE;

/**
 * <p>
 * 点赞记录表 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2026-04-14
 */
//基于redis实现的点赞、取消点赞功能
@Service
@RequiredArgsConstructor
public class LikedRecordServiceRedisImpl extends ServiceImpl<LikedRecordMapper, LikedRecord> implements ILikedRecordService {

    private final RabbitMqHelper mqHelper;
    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public void addLikedRecord(LikeRecordFormDTO likeRecordFormDTO) {
        // 1.判断是点赞还是取消点赞
        Boolean success = likeRecordFormDTO.getLiked() ? like(likeRecordFormDTO) : unLike(likeRecordFormDTO);

        // 2.执行失败直接结束即可
        if (!success) {
            return;
        }
        // 3.统计点赞数量
        Long count = stringRedisTemplate.opsForSet().
                size(RedisConstants.LIKES_BIZ_KEY_PREFIX + likeRecordFormDTO.getBizId());
        if (count == null) {
            return;
        }
        // 4.缓存点赞总数到Redis
        // redis ZSET数据结构
        stringRedisTemplate.opsForZSet().add(RedisConstants.LIKES_TIMES_KEY_PREFIX + likeRecordFormDTO.getBizType(), // key
                likeRecordFormDTO.getBizId().toString(), // member
                count);// score
    }


    // 说清楚功能 + 为什么用 Pipeline + Set 的优势
    // redis版本查询当前用户点赞过的业务id
    @Override
    public Set<Long> isBizLiked(List<Long> bizIds) {
        // 1.获取当前登录用户
        Long userId = UserContext.getUser();
        // 2.根据用户id以及回答的id（业务id），查询点赞记录存不存在，返回bizId
        String key = RedisConstants.LIKES_BIZ_KEY_PREFIX;
//        Set<Long> ids = new HashSet<>();
        // redis使用传输效率低 多次让redis执行命令，多次网路传输延迟，性能低
//        // Pipelined批处理
//        for (Long bizId : bizIds) {
//            Boolean member = stringRedisTemplate.opsForSet().isMember(key + bizId, userId.toString());
//            if (member != null && member) {
//                ids.add(bizId);
//            }
//        }
        List<Object> objects = stringRedisTemplate.executePipelined(new RedisCallback<Object>() {
            @Override
            public Object doInRedis(RedisConnection connection) throws DataAccessException {
                StringRedisConnection src = (StringRedisConnection) connection;
                for (Long bizId : bizIds) {
                    src.sIsMember(key + bizId,userId.toString());
                }
                return null;
            }
        });

//        // 得到的objects集合，根据小标索引，可以判断当前用户对bizId点赞与否
//        for (int i = 0; i < objects.size(); i++) {
//            Boolean o = (Boolean) objects.get(i);
//            if (o) {
//                ids.add(bizIds.get(i));
//            }
//        }

        return IntStream.range(0, objects.size())
                .filter(i -> (Boolean) objects.get(i))
                .mapToObj(bizIds::get)
                .collect(Collectors.toSet());
    }

    // 通过biztype，maxsize查询SZET，并删除，发送mq
    @Override
    public void readLikedTimesAndSendMessage(String bizType, int maxBizSize) {
        // 1.读取数据
        Set<ZSetOperations.TypedTuple<String>> typedTuples =
                stringRedisTemplate.opsForZSet().popMin(RedisConstants.LIKES_TIMES_KEY_PREFIX + bizType, maxBizSize);
        if (CollUtils.isEmpty(typedTuples)) {
            return;
        }
        // 2.数据转换成LikedTimesDTO
        List<LikeTimesDTO> likeTimesDTOS = new ArrayList<>(typedTuples.size());
        for (ZSetOperations.TypedTuple<String> typedTuple : typedTuples) {
            String bizId = typedTuple.getValue();
            Double liedTimes = typedTuple.getScore();
            if(bizId == null || liedTimes == null){
                continue;
            }
            likeTimesDTOS.add(LikeTimesDTO.of(Long.valueOf(bizId),liedTimes.intValue()));
        }
        // 3.发送MQ消息
        mqHelper.send(
                LIKE_RECORD_EXCHANGE,// 交换机
                StringUtils.format(LIKED_TIMES_KEY_TEMPLATE,bizType),// 点赞的RoutingKey
                likeTimesDTOS// 消息体，dto传输类，map
        );

    }


//      数据库版本，查询点赞
//
//    @Override
//    public Set<Long> isBizLiked(List<Long> bizIds) {
//        // 1.获取当前登录用户
//        Long userId = UserContext.getUser();
//        // 2.根据用户id以及回答的id（业务id），查询点赞记录存不存在，返回bizId
//        List<LikedRecord> list = lambdaQuery()
//                .eq(LikedRecord::getUserId, userId)
//                .in(LikedRecord::getBizId, bizIds)
//                .list();
//        return list.stream().map(LikedRecord::getBizId).collect(Collectors.toSet());
//    }

    private Boolean unLike(LikeRecordFormDTO likeRecordFormDTO) {
        // 1.获取用户id
        Long userId = UserContext.getUser();

        // 2.获取key
        String key = RedisConstants.LIKES_BIZ_KEY_PREFIX + likeRecordFormDTO.getBizId();

        // 3.执行SADD命令，set数据结构
        Long s = stringRedisTemplate.opsForSet().remove(key, userId.toString());
        return s != null && s > 0;
    }

    private Boolean like(LikeRecordFormDTO likeRecordFormDTO) {
        // 1.获取用户id
        Long userId = UserContext.getUser();

        // 2.获取key
        String key = RedisConstants.LIKES_BIZ_KEY_PREFIX + likeRecordFormDTO.getBizId();

        // 3.执行SADD命令，set数据结构
        Long s = stringRedisTemplate.opsForSet().add(key, userId.toString());
        return s != null && s > 0;
    }
}
