package com.tianji.learning.utils;

import cn.hutool.core.thread.ThreadFactoryBuilder;
import cn.hutool.json.JSONUtil;
import com.alibaba.nacos.common.executor.ThreadPoolManager;
import com.tianji.common.utils.JsonUtils;
import com.tianji.common.utils.StringUtils;
import com.tianji.learning.domain.po.LearningLesson;
import com.tianji.learning.domain.po.LearningRecord;
import com.tianji.learning.mapper.LearningRecordMapper;
import com.tianji.learning.service.ILearningLessonService;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.concurrent.*;

@Slf4j
@RequiredArgsConstructor
@Component
public class LearningRecordDelayTaskHandler {

    // 提供redis调用
    private final StringRedisTemplate stringRedisTemplate;
    // 创建延迟队列
    private final DelayQueue<DelayTask<RecordTaskData>> queue = new DelayQueue<>();
    // redis 存储key的模板
    private final static String RECORD_KEY_TEMPLATE = "learning:record:{}";
    // 持久化、修改学习进度相关数据
    private final LearningRecordMapper learningRecordMapper;

    private final ILearningLessonService learningLessonService;
    // volatile 多线程同时看到begin参数，控制延迟任务死循环的开始和结束
    private static volatile boolean begin = true;



    //spring注入，销毁
    @PostConstruct // spring生命周期的注解，当前类注入后，启动这函数
    public void init() {
        // 异步调用，线程
        // 线程的不同方法的创建和调用
        CompletableFuture.runAsync(this::handDelayTask);

    }

    @PreDestroy // spring生命周期的注解，当项目销毁钱，运行这函数
    public void predestroy() {
        begin = false;
        log.debug("延迟任务停止执行！");
    }

    // 处理延迟任务，从延迟队列中取出任务；查询最新的redis数据，比较是否需要更新进度和课表数据
    public void handDelayTask() {
        // 不断循环，从队列中取出，任务，take特性：如果队列为空自动休眠
        while (begin) {
            try {
                // 1. 获取到期的延迟任务
                DelayTask<RecordTaskData> task = queue.take();
                RecordTaskData data = task.getData();

                // 2.查询redis缓存
                LearningRecord record = readLearningRecord(data.getLessonId(), data.getSectionId());

                if (record == null) {
                    continue;
                }

                // 3.比较数据，moment值
                if (Objects.equals(data.getMoment(), record.getMoment())) {
                    // 4.不一致，继续循环
                    continue;
                }

                // 5.一致，持久化到数据库
                // 5.1 更新学习记录的moment
                record.setFinished(null);
                learningRecordMapper.updateById(record);
                // 5.2 更新科比奥的最近学习信息
                LearningLesson lesson = new LearningLesson();
                lesson.setId(record.getLessonId());
                lesson.setLatestSectionId(record.getSectionId());
                lesson.setLatestLearnTime(LocalDateTime.now());
                learningLessonService.updateById(lesson);
            } catch (InterruptedException e) {
                log.error("处理延迟任务发生异常", e);
            }
        }

    }

    // 课程不是第一次，不是学完，添加到redis缓存，提交任务到延迟队列
    public void addLearningRecordTask(LearningRecord record) {
        // 1.添加数据到redis缓存
        writeRecordCache(record);
        // 2.提交任务到延迟队列
        queue.add(new DelayTask<>(new RecordTaskData(record), Duration.ofSeconds(20)));
    }

    public void writeRecordCache(LearningRecord record) {
        log.debug("更新学习记录的缓存数据");
        try {
            // 1.数据转换，属性名字，类型一样
            RecordCacheData cacheData = new RecordCacheData(record);
            String jsonStr = JSONUtil.toJsonStr(cacheData);
            // 2.写入redis
            String key = StringUtils.format(RECORD_KEY_TEMPLATE, record.getLessonId());
            stringRedisTemplate.opsForHash().put(key, record.getSectionId().toString(), jsonStr);
            // 3.添加缓存过期时间
            stringRedisTemplate.expire(key, Duration.ofMillis(1));
        } catch (Exception e) {
            log.error("更新学习记录的缓存数据异常", e);
        }
    }

    public LearningRecord readLearningRecord(Long lessonId, Long sectionId) {
        try {
            // 1.读取redis缓存数据
            // StringUtils.format模板{}，替换
            String key = StringUtils.format(RECORD_KEY_TEMPLATE, lessonId);
            Object cacheData = stringRedisTemplate.opsForHash().get(key, sectionId.toString());
            // 2.数据检查和转换
            if (cacheData == null) {
                return null;
            }
            // JsonUtils.toBean JSON转java类
            LearningRecord record = JsonUtils.toBean(cacheData.toString(), LearningRecord.class);
            return record;
        } catch (Exception e) {
            log.error("缓存数据读取异常", e);
            return null;
        }
    }

    public void cleanLearningRecord(Long lessonId, Long sectionId) {
        String key = StringUtils.format(RECORD_KEY_TEMPLATE, lessonId);
        stringRedisTemplate.opsForHash().delete(key, sectionId.toString());
    }

    @Data
    @NoArgsConstructor
    private static class RecordCacheData {
        private Long id;
        private Integer moment;
        private Boolean finished;

        public RecordCacheData(LearningRecord record) {
            this.id = record.getId();
            this.moment = record.getMoment();
            this.finished = record.getFinished();
        }
    }

    @Data
    @NoArgsConstructor
    private static class RecordTaskData {
        private Integer moment;
        private Long lessonId;
        private Long sectionId;

        public RecordTaskData(LearningRecord record) {
            this.moment = record.getMoment();
            this.lessonId = record.getLessonId();
            this.sectionId = record.getSectionId();
        }
    }


}
































