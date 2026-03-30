package com.tianji.learning.utils;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.DelayQueue;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
class DelayTaskTest {
    @Test
    void testDelayQueuee() throws InterruptedException {
        // 初始话延迟队列
        DelayQueue<DelayTask<String>> delayQueue = new DelayQueue<>();
        // 向队列中添加任务
        log.info("开始初始化延迟任务。。。。");
        delayQueue.add(new DelayTask<>("3", Duration.ofSeconds(3)));
        delayQueue.add(new DelayTask<>("1", Duration.ofSeconds(1)));
        delayQueue.add(new DelayTask<>("2", Duration.ofSeconds(2)));
        while (true){
            DelayTask<String> task = delayQueue.take();
            log.info("开始执行任务，{}", task.getData());
        }
    }
}
































