package com.tianji.promotion.utils;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

/**
 * 自定义注解、标记增强的方法，在方法运行前后去操作
 */


@Retention(RetentionPolicy.RUNTIME) //  让自定义注解在程序运行时仍然存活，AOP 才能通过反射拦截它。
@Target(ElementType.METHOD) // 注解作用在方法上
public @interface MyLock {
    String name();

    long waitTime() default 1;

    long leaseTime() default -1; // 过期时间-1使用redission默认值 30 秒 以及打开看门狗

    TimeUnit unit() default TimeUnit.SECONDS; // 上面使用时间的单位

    MyLockType lockType() default MyLockType.RE_ENTRANT_LOCK;

    MyLockStrategy lockStrategy() default MyLockStrategy.FAIL_AFTER_RETRY_TIMEOUT;

}
