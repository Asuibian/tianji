package com.tianji.promotion.utils;

import com.tianji.common.exceptions.BizIllegalException;
import org.redisson.api.RLock;

public enum MyLockStrategy {
    SKIP_FAST() {
        @Override
        boolean tryLock(RLock lock, MyLock prop) throws InterruptedException {
            return lock.tryLock(0,prop.leaseTime(),prop.unit());
        }
    }, // 不重试直接返回
    FAIL_FAST() {
        @Override
        boolean tryLock(RLock lock, MyLock prop) throws InterruptedException {
            boolean isLock = lock.tryLock(0, prop.leaseTime(), prop.unit());
            if (!isLock) {
                throw new BizIllegalException("请求太频繁");
            }
            return true;
        }
    }, // 不重试 抛异常
    KEEP_TRYING() {
        @Override
        boolean tryLock(RLock lock, MyLock prop) throws InterruptedException {
            lock.lock(prop.leaseTime(), prop.unit());
            return true;
        }
    }, // 一直获取锁
    SKIP_AFTER_RETRY_TIMEOUT() {
        @Override
        boolean tryLock(RLock lock, MyLock prop) throws InterruptedException {
            return lock.tryLock(prop.waitTime(),prop.leaseTime(),prop.unit());
        }
    }, // 重试一段时间 直接返回
    FAIL_AFTER_RETRY_TIMEOUT() {
        @Override
        boolean tryLock(RLock lock, MyLock prop) throws InterruptedException {
            boolean isLock = lock.tryLock(prop.waitTime(), prop.leaseTime(), prop.unit());
            if (!isLock) {
                throw new BizIllegalException("请求太频繁");
            }
            return true;
        }
    }, // 重试一段时间 直接抛异常
    ;


    abstract boolean tryLock(RLock lock,MyLock prop) throws InterruptedException;

}
