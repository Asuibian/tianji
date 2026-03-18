package com.tianji.learning.mq;

import com.tianji.api.dto.trade.OrderBasicDTO;
import com.tianji.common.constants.MqConstants;
import com.tianji.common.utils.CollUtils;
import com.tianji.learning.service.ILearningLessonService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
/**
 * 自动注入：配合 Spring 使用时，Spring 会自动通过构造函数注入 iLearningLessonService 的实例
 *
 * 不可变性：final 关键字确保这个依赖在对象生命周期内不会被改变
 *
 * 代码简洁：不需要手动写构造函数和 @Autowired 注解
 */
public class LessonChangeListener {


    private final ILearningLessonService lessonService;

    // 监听MQ消息队列
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "learning.lesson.pay.queue",durable = "true"),//队列的名字
            exchange = @Exchange(name = MqConstants.Exchange.ORDER_EXCHANGE,type = ExchangeTypes.TOPIC),
            key = MqConstants.Key.ORDER_PAY_KEY
    ))
    // 添加课程到课表里面
    public void listenLessonPay(OrderBasicDTO order) {
        // 1、健壮性处理，订单信息是否正常
        if(order == null || order.getOrderId() == null || CollUtils.isEmpty(order.getCourseIds())) {
            log.error("接受到MQ消息有误，订单数据为空");
            return;
        }
        // 2、添加课程
        log.debug("监听到用户{}的订单{}，需要添加课程{}到课表中",order.getUserId(),order.getOrderId(),order.getCourseIds());
        lessonService.addUserLessons(order.getUserId(),order.getCourseIds());
    }


    //监听MQ消息队列
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "learning.lesson.refund.queue",durable = "true"),
            exchange = @Exchange(MqConstants.Exchange.ORDER_EXCHANGE),
            key = MqConstants.Key.ORDER_REFUND_KEY
    ))
    // 用户退款，从用户课表中删除(多个)课程
    public void listenLessonRefund(OrderBasicDTO order){
        //健壮性处理
        if(order == null || order.getOrderId() == null || CollUtils.isEmpty(order.getCourseIds())) {
            log.error("接受到MQ消息有误，订单数据为空");
            return;
        }

        // 3、删除（多个）课程
        log.debug("监听到用户{}的订单{}，需要删除课程{}到课表中",order.getUserId(),order.getOrderId(),order.getCourseIds());
        lessonService.deleteRefundLearningLesson(order);
    }


}



























