package com.tianji.promotion.utils;

import org.redisson.api.RLock;

/**
 * 锁的类型的枚举，根据枚举调用不同的方法，获取不同的锁的对象
 */
public enum MyLockType {
    RE_ENTRANT_LOCK,
    FAIR_LOCK,
    READ_LOCK,
    WRITE_LOCK,
    ;

}
