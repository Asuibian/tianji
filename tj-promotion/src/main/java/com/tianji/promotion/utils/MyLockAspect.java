package com.tianji.promotion.utils;

import com.tianji.common.exceptions.BizIllegalException;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;


//  枚举结合自定义工厂

@Aspect // 告诉 Spring：这个类里的方法（如 @Around）定义了何时何地插入额外逻辑，请为我生成代理并织入增强。
@Component
@RequiredArgsConstructor
public class MyLockAspect implements Ordered {
    @Override
    public int getOrder() {
        return 0;
    }

    private final MyLockFactory myLockFactory;

    @Around("@annotation(myLock)")
    public Object tryLock(ProceedingJoinPoint joinPoint, MyLock myLock) throws Throwable {
        // 1.创建锁对象
        // 获取锁的类型
        // 根据锁的类型获取不同的锁，类似if，使用工厂模式处理
        RLock lock = myLockFactory.getLock(myLock.lockType(), myLock.name());   // 根据不同的枚举选择不同的锁对象
        // 2.尝试获取锁
        boolean isLock = myLock.lockStrategy().tryLock(lock,myLock);
        // 3.失败、快速结束
        if (!isLock) {
            return null;
            //throw new BizIllegalException("请求太频繁"); // BizIllegalException 表示 “业务非法异常”
        }
        // 3.1 成功执行业务
        try {
            return joinPoint.proceed();
        } finally {
            // 4. 释放锁
            lock.unlock();
        }
    }

}




































