package com.tianji.learning.utils;

import lombok.Data;

import java.time.Duration;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

/**
 * 延时任务工具类，学习进度的最后一次更新
 */
@Data
public class DelayTask<D> implements Delayed {

    private D data;

    // 创建任务时，构造任务指定具体的到期时间
    private long deadlineNanos;

    // 给我周期，我帮你转换
    public DelayTask(D data, Duration delayTime) {
        this.data = data;
        this.deadlineNanos = System.nanoTime() + delayTime.toNanos();
    }

    /**
     * 返回任务的到期时间
     * @param unit the time unit
     * @return
     */
    @Override
    public long getDelay(TimeUnit unit) {
        return unit.convert(Math.max(deadlineNanos - System.nanoTime(), 0), TimeUnit.NANOSECONDS);

    }

    /**
     * 比较this当前任务的过期时间 与 指定任务，返回过期时间短的任务
     * @param o the object to be compared.
     * @return
     */
    @Override
    public int compareTo(Delayed o) {
        long l = getDelay(TimeUnit.NANOSECONDS) - o.getDelay(TimeUnit.NANOSECONDS);
        if (l > 0){
            return 1;
        }else if (l < 0){
            return -1;
        }else {
            return 0;
        }
    }
}


































