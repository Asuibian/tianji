package com.tianji.promotion.config;

import com.tianji.common.autoconfigure.xxljob.XxlJobProperties;
import com.tianji.promotion.utils.MyLockAspect;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RedissonClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

@Slf4j
@Configuration
public class PromotionConfig {

    @Bean
    public Executor generateExchangeCodeExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        // 1.核心线程池大小，常驻
        executor.setCorePoolSize(2);
        // 2.最大线程大小 备用
        executor.setMaxPoolSize(5);
        // 3.队列大小
        executor.setQueueCapacity(200);
        // 4.线程名称
        executor.setThreadNamePrefix("exchange-code-handler-");
        // 5.拒绝策略
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy()); // 让原来的线程执行
        // 6.初始化
        executor.initialize();
        return executor;
    }
//
//    @Bean
//    public MyLockAspect myLockAspect(RedissonClient redissonClient) {
//        return new MyLockAspect(redissonClient);
//    }

    @Bean
    public Executor discountSolutionExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        // 1.核心线程池大小，常驻
        executor.setCorePoolSize(12);
        // 2.最大线程大小 备用
        executor.setMaxPoolSize(13);
        // 3.队列大小
        executor.setQueueCapacity(99999);
        // 4.线程名称
        executor.setThreadNamePrefix("discount-solution-calculator-");
        // 5.拒绝策略
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy()); // 直接拒绝、防止占用过多
        // 6.初始化
        executor.initialize();
        return executor;
    }


}
