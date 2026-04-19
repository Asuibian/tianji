package com.tianji.remark.task;

import com.tianji.remark.service.ILikedRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class LikedTimesCheckTask {

    private static final List<String> BIZ_TYPES = List.of("QA","NOTE");
    private static final int MAX_BIZ_SIZE = 30;

    private final ILikedRecordService likedRecordService;

    // TODO 生产mq消息
    @Scheduled(fixedDelay = 20000) // 上一次执行结束后，等待 20000 毫秒（20秒），再开始下一次执行
    public void checkLikedTimes() {
        // 通过业务类型，去执行
        for (String bizType : BIZ_TYPES) {
            likedRecordService.readLikedTimesAndSendMessage(bizType,MAX_BIZ_SIZE); // 操作Szet数据结构，取，删
        }
    }

}
