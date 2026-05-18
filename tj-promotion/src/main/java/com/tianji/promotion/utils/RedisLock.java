package com.tianji.promotion.utils;

import cn.hutool.core.util.BooleanUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.concurrent.TimeUnit;

/** redis setnx锁的局限性
 * 1、不可重入：同一个线程执行业务前获取锁后，执行业务中也需要获取锁的情况下、setnx肯定获取失败、业务无法继续执行。
 * 2、失败重试机制
 * 3、一致性问题、redis在主从模式下，数据是最终一致、假设在主节点获取锁后，当前主节点宕机了、推出一个新的主节点上没有锁的信息，会导致其它线程也获取到锁、并行执行
 * 4、原子性
 * 5、超时问题
 */
// 创建分布式锁和释放锁
// 可见性 互斥性
@RequiredArgsConstructor
public class RedisLock {

    /**
     * 将 key 作为成员变量，本质上是用对象实例代表“对某个资源的锁”
     * 保证 tryLock 和 unlock 操作同一个资源
     * 降低调用复杂度，减少人为错误。
     */
    private final String key;
    private final StringRedisTemplate redisTemplate;

    public boolean tryLock(long time, TimeUnit timeUnit) {
        // 1.获取线程名称
        String value = Thread.currentThread().getName();
        // 2.获取锁
        Boolean success = redisTemplate.opsForValue().setIfAbsent(key, value, time, timeUnit);
        // 3.返回结果
        return Boolean.TRUE.equals(success);
    }

    public void unlock() {
        redisTemplate.delete(key);
    }


}
