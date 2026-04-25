package com.tianji.learning.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.CollectionUtil;
import com.tianji.common.autoconfigure.mq.RabbitMqHelper;
import com.tianji.common.constants.MqConstants;
import com.tianji.common.exceptions.BizIllegalException;
import com.tianji.common.utils.CollUtils;
import com.tianji.common.utils.DateUtils;
import com.tianji.common.utils.UserContext;
import com.tianji.learning.constans.RedisConstans;
import com.tianji.learning.domain.vo.SignResultVO;
import com.tianji.learning.mq.message.SignInMessage;
import com.tianji.learning.service.ISignRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.connection.BitFieldSubCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


@Service
@RequiredArgsConstructor
public class SignRecordServiceImpl implements ISignRecordService {

    private final StringRedisTemplate stringRedisTemplate;
    private final RabbitMqHelper mqHelper;

    @Override
    public SignResultVO addSignRecords() {
        // 1.签到
        // 1.1获取用户id
        Long userId = UserContext.getUser();
        // 1.2获取当前日期
        LocalDate now = LocalDate.now();
        // 1.3拼接key
        String key = RedisConstans.SIGN_RECORD_KEY_PREFIX + userId + now.format(DateUtils.SIGN_DATE_SUFFIX_FORMATTER);
        // 1.4计算offset
        int offset = now.getDayOfMonth() - 1;
        // 1.5保存
        Boolean b = stringRedisTemplate.opsForValue().setBit(key, offset, true);
        if (b != null && b) {
            throw new BizIllegalException("不允许重复签到！");
        }

        // 2.计算连续签到天数
        int signDays = countSignDays(key, now.getDayOfMonth());

        //  3.计算签到得分
        int rewardPoints = 0;
        switch (signDays){
            case 7:
                rewardPoints = 10;
                break;
            case 14:
                rewardPoints = 20;
                break;
            case 28:
                rewardPoints = 40;
                break;
        }
        // 4.保存积分明细记录
        mqHelper.send(
                MqConstants.Exchange.LEARNING_EXCHANGE,
                MqConstants.Key.SIGN_IN,
                SignInMessage.of(userId,rewardPoints + 1)
        );



        SignResultVO signResultVO = new SignResultVO();
        signResultVO.setSignDays(signDays);
        signResultVO.setRewardPoints(rewardPoints);
        // 5.封装返回
        return signResultVO;


    }

    @Override
    public List<Integer> getSignRecordsByMonth() {
        List<Integer> r = new ArrayList<>();
        // 1.从redis中，根据key和Bitfiemling查询，

        // 1.1计算当前是几号，减一，得到命令BitFie“encodeing”后面所需要填的值
        LocalDate now = LocalDate.now();
        int dayOfMonth = now.getDayOfMonth() - 1;

        // 1.2获取redis   key的值
        String key = RedisConstans.SIGN_RECORD_KEY_PREFIX + UserContext.getUser() + now.format(DateUtils.SIGN_DATE_SUFFIX_FORMATTER);

        // bitfield命令从第0valueAt(0))位开始取，取多少位dayOfMonth
        List<Long> list = stringRedisTemplate.opsForValue()
                .bitField(key, BitFieldSubCommands.create().get(BitFieldSubCommands.BitFieldType.unsigned(dayOfMonth)).valueAt(0));
        if (list == null || list.isEmpty()) {
            return Collections.emptyList();
        }
        int l = list.get(0).intValue();
        // 2.无符号整数，通过循环与1做与运算，然后右移一位改变最后一位的值，
        while (l != 0){
            int i = l & 1;
            r.add(i);
            l >>>= 1;
        }

        // 3。封装成集合，反转集合。就可以从当前日期往前获得签到记录
        Collections.reverse(r);
        return r;
    }

    private int countSignDays(String key, int dayOfMonth) {
        // 1.获取签到记录
        List<Long> list = stringRedisTemplate.opsForValue()
                .bitField(key, BitFieldSubCommands.create().get(BitFieldSubCommands.BitFieldType.unsigned(dayOfMonth)).valueAt(0));
        if (list == null || list.isEmpty()) {
            return 0;
        }
        int l = (list.get(0)).intValue();
        // 2.定义计数器
        int count = 0;
        // 3.循环与1做与运算，得到最后一个bit，判断是否为0，0则终止，1则继续
        while ((l & 1) == 1) {
            // 4.计数器加1
            count++;
            // 5.吧数字右移一位，切换最后一位的数字，重新比较
            l >>>= 1;
        }
        return count;
    }
}































