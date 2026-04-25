package com.tianji.learning.mq;

import com.tianji.learning.enums.PointsRecordType;
import com.tianji.learning.mq.message.SignInMessage;
import com.tianji.learning.service.IPointsRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import static com.tianji.common.constants.MqConstants.Exchange.LEARNING_EXCHANGE;
import static com.tianji.common.constants.MqConstants.Key.*;

@Component
@RequiredArgsConstructor
public class LearningPointsListener {

    private final IPointsRecordService pointsRecordService;

    // 问答
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = "qa.points.queue" ,durable = "true"),
            exchange = @Exchange(name = LEARNING_EXCHANGE,type = ExchangeTypes.TOPIC),
            key = WRITE_REPLY
    ))
    public void listenWriteReplyMessage(Long userId){
        pointsRecordService.addPointsRecord(userId,5, PointsRecordType.QA);
    }


    // 签到
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = "sign.points.queue" ,durable = "true"),
            exchange = @Exchange(name = LEARNING_EXCHANGE,type = ExchangeTypes.TOPIC),
            key = SIGN_IN
    ))
    public void listenSignInMessage(SignInMessage message){
        pointsRecordService.addPointsRecord(message.getUserId(),message.getPoints(), PointsRecordType.SIGN);
    }


    // 课程学习
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = "section.points.queue" ,durable = "true"),
            exchange = @Exchange(name = LEARNING_EXCHANGE,type = ExchangeTypes.TOPIC),
            key = LEARN_SECTION
    ))
    public void listenSectionMessage(Long userId){
        pointsRecordService.addPointsRecord(userId,10, PointsRecordType.LEARNING);
    }


    // 写笔记
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = "note.points.queue" ,durable = "true"),
            exchange = @Exchange(name = LEARNING_EXCHANGE,type = ExchangeTypes.TOPIC),
            key = WRITE_NOTE
    ))
    public void listenNoteMessage(Long userId){
        pointsRecordService.addPointsRecord(userId,3, PointsRecordType.NOTE);
    }


    // 笔记被采集
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = "noteGathered.points.queue" ,durable = "true"),
            exchange = @Exchange(name = LEARNING_EXCHANGE,type = ExchangeTypes.TOPIC),
            key = NOTE_GATHERED
    ))
    public void listenNoteGatheredMessage(Long userId){
        pointsRecordService.addPointsRecord(userId,2, PointsRecordType.NOTE);
    }















































}
