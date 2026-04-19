package com.tianji.learning.mq;

import com.tianji.api.dto.remark.LikeTimesDTO;
import com.tianji.learning.domain.po.InteractionReply;
import com.tianji.learning.service.IInteractionReplyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

import static com.tianji.common.constants.MqConstants.Exchange.LIKE_RECORD_EXCHANGE;
import static com.tianji.common.constants.MqConstants.Key.QA_LIKED_TIMES_KEY;


@Slf4j
@Component // 加在类上的注解，让spring管理这个类，注入使用更方便
@RequiredArgsConstructor
public class LikeTimesChangeListener {

    private final IInteractionReplyService replyService;


    // TODO测试 06-08  9:40秒
    // @RabbitListener 加在方法上的注解，监听消息队列，告诉spring 一有消息就调用这个方法
    // @QueueBinding一次性配好队列、交换机、绑定，启动时自动创建
    // @Queue 定义队列的名称和属性，durable,rabbitMq重启后队列还存在，是否需要持久化的说明
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = "qa.liked.times.queue", durable = "true"),
            exchange = @Exchange(name = LIKE_RECORD_EXCHANGE, type = ExchangeTypes.TOPIC),
            key = QA_LIKED_TIMES_KEY
    ))
    public void listenReplyLikedTimesChange(List<LikeTimesDTO> likeTimesDTOS) {
        log.debug("监听到点赞数的变化mq消息");

        List<InteractionReply> po = new ArrayList<>(likeTimesDTOS.size());

        for (LikeTimesDTO dto : likeTimesDTOS) {
            InteractionReply reply = new InteractionReply();
            reply.setId(dto.getBizId());
            reply.setLikedTimes(dto.getLikeTimes());

            po.add(reply);
        }

        replyService.updateBatchById(po);

    }

}
