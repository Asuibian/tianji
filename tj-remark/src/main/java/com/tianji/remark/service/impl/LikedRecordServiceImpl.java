package com.tianji.remark.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tianji.api.dto.remark.LikeTimesDTO;
import com.tianji.common.autoconfigure.mq.RabbitMqHelper;
import com.tianji.common.utils.StringUtils;
import com.tianji.common.utils.UserContext;
import com.tianji.remark.domain.dto.LikeRecordFormDTO;
import com.tianji.remark.domain.po.LikedRecord;
import com.tianji.remark.mapper.LikedRecordMapper;
import com.tianji.remark.service.ILikedRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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
//@Service 基于mysql实现的点赞、取消点赞功能
@RequiredArgsConstructor
public class LikedRecordServiceImpl extends ServiceImpl<LikedRecordMapper, LikedRecord> implements ILikedRecordService {

    private final RabbitMqHelper mqHelper;


    // TODO 测试点赞  取消点赞功能是否正确
    @Override
    public void addLikedRecord(LikeRecordFormDTO likeRecordFormDTO) {
        // 1.判断是点赞还是取消点赞
        Boolean success = likeRecordFormDTO.getLiked() ? like(likeRecordFormDTO) : unLike(likeRecordFormDTO);

        // 2.执行失败直接结束即可
        if (!success) {
            return;
        }
        // 3.统计点赞数量
        Integer count = lambdaQuery()
                .eq(LikedRecord::getBizId, likeRecordFormDTO.getBizId())
                .count();
        // 4.生产MQ消息
        mqHelper.send(
                LIKE_RECORD_EXCHANGE,// 交换机
                StringUtils.format(LIKED_TIMES_KEY_TEMPLATE,likeRecordFormDTO.getBizType()),// 点赞的RoutingKey
                LikeTimesDTO.of(likeRecordFormDTO.getBizId(),count)// 消息体，dto传输类，map
        );
    }

    // TODO 测试 是否能正确查询点赞业务的id
    @Override
    public Set<Long> isBizLiked(List<Long> bizIds) {
        // 1.获取当前登录用户
        Long userId = UserContext.getUser();
        // 2.根据用户id以及回答的id（业务id），查询点赞记录存不存在，返回bizId
        List<LikedRecord> list = lambdaQuery()
                .eq(LikedRecord::getUserId, userId)
                .in(LikedRecord::getBizId, bizIds)
                .list();
        return list.stream().map(LikedRecord::getBizId).collect(Collectors.toSet());
    }

    @Override
    public void readLikedTimesAndSendMessage(String bizType, int maxBizSize) {

    }

    private Boolean unLike(LikeRecordFormDTO likeRecordFormDTO) {
        return remove(new QueryWrapper<LikedRecord>().lambda()
                .eq(LikedRecord::getUserId, UserContext.getUser())
                .eq(LikedRecord::getBizId, likeRecordFormDTO.getBizId()));

    }

    private Boolean like(LikeRecordFormDTO likeRecordFormDTO) {
        Long userId = UserContext.getUser();
        // 1.判断记录是否存在
        Integer count = lambdaQuery()
                .eq(LikedRecord::getUserId, userId)
                .eq(LikedRecord::getBizId, likeRecordFormDTO.getBizId())
                .count();
        // 2.已经存在，重复点赞，结束
        if (count > 0) {
            return false;
        }
        // 3.不存在，新增po
        LikedRecord likedRecord = new LikedRecord();
        BeanUtils.copyProperties(likeRecordFormDTO, likedRecord);
        likedRecord.setUserId(userId);
        save(likedRecord);
        return true;
    }
}
