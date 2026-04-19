package com.tianji.learning.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tianji.api.client.remark.RemarkClient;
import com.tianji.api.client.user.UserClient;
import com.tianji.api.dto.user.UserDTO;
import com.tianji.common.domain.dto.PageDTO;
import com.tianji.common.utils.CollUtils;
import com.tianji.common.utils.UserContext;
import com.tianji.learning.domain.dto.ReplayDTO;
import com.tianji.learning.domain.po.InteractionQuestion;
import com.tianji.learning.domain.po.InteractionReply;
import com.tianji.learning.domain.query.ReplyPageQuery;
import com.tianji.learning.domain.vo.ReplyVO;
import com.tianji.learning.enums.QuestionStatus;
import com.tianji.learning.mapper.InteractionQuestionMapper;
import com.tianji.learning.mapper.InteractionReplyMapper;
import com.tianji.learning.service.IInteractionReplyService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * <p>
 * 互动问题的回答或评论 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2026-03-30
 */
@Service
@RequiredArgsConstructor
public class InteractionReplyServiceImpl extends ServiceImpl<InteractionReplyMapper, InteractionReply> implements IInteractionReplyService {

    private final InteractionQuestionMapper interactionQuestionMapper;

    private final UserClient userClient;

    private final RemarkClient remarkClient;

    @Override
    @Transactional
    public void addAnswerOrComment(ReplayDTO replayDTO) {
        InteractionReply reply = new InteractionReply();
        // 1.获取当前用户id
        Long userId = UserContext.getUser();
        // 2.封装po，提交
        BeanUtils.copyProperties(replayDTO, reply);
        reply.setUserId(userId);
        save(reply);

        // MyBatis-Plus 默认会在 save() 执行后将生成的主键自动回填到实体对象中
        Long id = reply.getId();

        // 3.判断是回答还是评论
        Long answerId = replayDTO.getAnswerId(); // 回复的上级id
        Long questionId = replayDTO.getQuestionId();
        if (answerId == null || answerId == 0L) {
            //3.1.1回答，根据问题id修改该问题下的最近一次回答id

            interactionQuestionMapper.update(null,
                    new LambdaUpdateWrapper<InteractionQuestion>()
                            .eq(InteractionQuestion::getId, questionId)  // 修改问题表
                            .set(InteractionQuestion::getLatestAnswerId, id) // 修改最近一次回答id ，本身的id
                            // 3.1.2 累计问题下的回答次数
                            .setSql("answer_times = answer_times + 1")
            );

//
//            iInteractionQuestionService.lambdaUpdate()
//                    .eq(InteractionQuestion::getId, questionId)
//                    .set(InteractionQuestion::getLatestAnswerId, id)
//                    // 3.1.2 累计问题下的回答次数
//                    .setSql("answer_times = answer_times + 1")
//                    .update();

        } else {
            // 3.2.1评论，累计问题下的评论次数
            this.lambdaUpdate()
                    .eq(InteractionReply::getId, answerId)
                    .setSql("reply_times = reply_times + 1")
                    .update();
        }
        // 4.判断是否是学生提交
        Boolean isStudent = replayDTO.getIsStudent();
        // 4.1 是，修改问题状态为未查看
        if (isStudent) {
            interactionQuestionMapper.update(null,
                    new LambdaUpdateWrapper<InteractionQuestion>()
                            .eq(InteractionQuestion::getId, questionId)
                            .set(InteractionQuestion::getStatus, QuestionStatus.UN_CHECK)
            );

        }


    }

    @Override
    public void hideReplyById(Long id, Boolean hidden) {
        lambdaUpdate()
                .eq(InteractionReply::getId, id)
                .set(InteractionReply::getHidden, hidden)
                .update();
    }

    @Override
    public PageDTO<ReplyVO> pageMyReplies(ReplyPageQuery pageQuery) {
        Long questionId = pageQuery.getQuestionId();
        Long answerId = pageQuery.getAnswerId();

        if ((answerId == null || answerId == 0L) && (questionId == null || questionId == 0L)) {
            return new PageDTO<>(0L, 0L, CollUtils.emptyList());
        }

        // 用户端分页查询
        // 已经判断为回答的查询
        if (questionId != null && questionId != 0L) {
            // 1.1根据questionId以及空的answerId查询回答，以及hidden查询不被隐藏的回答
            Page<InteractionReply> paged = lambdaQuery()
                    .eq(InteractionReply::getQuestionId, questionId)
                    .eq(InteractionReply::getAnswerId, 0L)
                    .eq(InteractionReply::getHidden, false)
                    .page(pageQuery.toMpPageDefaultSortByCreateTimeDesc());
            List<InteractionReply> records = paged.getRecords(); // po
            if (CollUtils.isEmpty(records)) {
                return new PageDTO<>(0L, 0L, CollUtils.emptyList());
            }

            Set<Long> uids = new HashSet<>(records.size()); //回答者id

            uids = records.stream()
                    .map(InteractionReply::getUserId)
                    .collect(Collectors.toSet());


            List<UserDTO> answers = userClient.queryUserByIds(uids); //适用于问题下的回答

            // 判断查询到了数据，key 为id，值为本身，转换成map型
            Map<Long, UserDTO> userDTOMap = answers.stream().collect(Collectors.toMap(UserDTO::getId, userDTO -> userDTO));

            // 查询到被隐藏的评论
            Integer count1 = lambdaQuery()
                    .eq(InteractionReply::getQuestionId, questionId)
                    .eq(InteractionReply::getAnswerId, 0L)
                    .eq(InteractionReply::getHidden, true)
                    .count();

            // 1.2 查询得到po
            // 1.3 不匿名的回答，需要查询用户头像和昵称
            Set<Long> id = records.stream().map(InteractionReply::getId).collect(Collectors.toSet());
            List<ReplyVO> voList = new ArrayList<>(records.size());

            // 得到当前用户已经点赞的回答的id，根据bizId的集合，修改VO liked为true 表示已经点赞
            Set<Long> isLikeBizIds = remarkClient.isBizLiked(id);// 传参 全部的业务id

            // 匿名的用户不用返回
            int count = 0;
            for (InteractionReply reply : records) {
                ReplyVO vo = new ReplyVO();
                BeanUtils.copyProperties(reply, vo);

                if (!reply.getAnonymity()) {
                    // false表示不匿名，需要封装
                    vo.setUserName(userDTOMap.get(reply.getUserId()).getUsername());
                    vo.setUserIcon(userDTOMap.get(reply.getUserId()).getIcon());
                }
                // 1.4 回答下的评论数量，累加的真实数据，用户端减去被隐藏的评论
                vo.setReplyTimes(reply.getReplyTimes() - count1);

                // 如果当前id存在于 isLikeBizIds中代表当前用户 点赞了当前回答
                if (isLikeBizIds.contains(reply.getId())) {
                    vo.setLiked(true);
                }
                voList.add(vo);
            }


            return new PageDTO<>(paged.getTotal(), paged.getPages(), voList);
        }



        // --------------------------------

        // 1.1根据answerId查询评论，以及hidden查询不被隐藏的回答
        Page<InteractionReply> pagedComment = lambdaQuery()
                .eq(InteractionReply::getAnswerId, answerId)
                .eq(InteractionReply::getHidden, false)
                .page(pageQuery.toMpPageDefaultSortByCreateTimeDesc());
        List<InteractionReply> records = pagedComment.getRecords(); // po
        if (CollUtils.isEmpty(records)) {
            return new PageDTO<>(0L, 0L, CollUtils.emptyList());
        }

        Set<Long> uids = new HashSet<>(records.size()); //回答者id

        Set<Long> targetUids = new HashSet<>(records.size()); //评论的目标用户的id
        Set<Long> targetReplyIds = new HashSet<>(records.size()); //评论的目标用户的当前回复的id


        uids = records.stream()
                .map(InteractionReply::getUserId)
                .collect(Collectors.toSet());


        targetUids = records.stream()
                .map(InteractionReply::getTargetUserId)
                .collect(Collectors.toSet());

        targetReplyIds = records.stream()
                .map(InteractionReply::getTargetReplyId)
                .collect(Collectors.toSet());

        List<UserDTO> answers = userClient.queryUserByIds(uids); //适用于问题下的回答
        List<UserDTO> targetAnswers = userClient.queryUserByIds(targetUids); //评论的目标用户昵称
        List<InteractionReply> targetReplyIdsList = lambdaQuery() //判断目标用户的回复是否匿名
                .in(InteractionReply::getId, targetReplyIds)
                .list();

        // 判断查询到了数据，key 为id，值为本身，转换成map型
        Map<Long, UserDTO> userDTOMap = answers.stream().collect(Collectors.toMap(UserDTO::getId, userDTO -> userDTO));// 回答者
        Map<Long, UserDTO> targetAnswersMap = targetAnswers.stream().collect(Collectors.toMap(UserDTO::getId, userDTO -> userDTO)); // 回答的目标
        Map<Long, Boolean> targetReplyIdsListMap = targetReplyIdsList.stream().collect(Collectors.toMap(InteractionReply::getId, InteractionReply::getAnonymity)); // 回答的目标是否匿名


        // 1.2 查询得到po
        // 1.3 不匿名的回答，需要查询用户头像和昵称
        List<ReplyVO> voList = new ArrayList<>(records.size());

        Set<Long> id = records.stream().map(InteractionReply::getId).collect(Collectors.toSet());
        Set<Long> isLikeBizIds = remarkClient.isBizLiked(id);// 传参 全部的业务id


        // 匿名的用户不用返回
        int count = 0;
        for (InteractionReply reply : records) {
            ReplyVO vo = new ReplyVO();
            BeanUtils.copyProperties(reply, vo);

            if (!reply.getAnonymity()) {
                // false表示不匿名，需要封装
                vo.setUserName(userDTOMap.get(reply.getUserId()).getName());
                vo.setUserIcon(userDTOMap.get(reply.getUserId()).getIcon());
            }

            // 判断评论的目标回答用户是否匿名，false为不匿名设置目标用户的昵称
            Boolean b = targetReplyIdsListMap.get(reply.getTargetReplyId());
            if (!b) {
                vo.setTargetUserName(targetAnswersMap.get(reply.getTargetUserId()).getName());
            }

            // 得到当前用户已经点赞的评论的id，根据bizId的集合，修改VO liked为true 表示已经点赞
            // 如果当前id存在于 isLikeBizIds中代表当前用户 点赞了当前回答
            if (isLikeBizIds.contains(reply.getId())) {
                vo.setLiked(true);
            }

            voList.add(vo);
        }


        return new PageDTO<>(pagedComment.getTotal(), pagedComment.getPages(), voList);


    }

    @Override
    public PageDTO<ReplyVO> queryAdminRepliesPage(ReplyPageQuery replyPageQuery) {

        Long questionId = replyPageQuery.getQuestionId();
        Long answerId = replyPageQuery.getAnswerId();

        if ((answerId == null || answerId == 0L) && (questionId == null || questionId == 0L)) {
            return new PageDTO<>(0L, 0L, CollUtils.emptyList());
        }

        // 管理端分页查询
        // 已经判断为回答的查询
        if (questionId != null && questionId != 0L) {
            // 1.1根据questionId以及空的answerId查询回答，以及hidden查询不被隐藏的回答
            Page<InteractionReply> paged = lambdaQuery()
                    .eq(InteractionReply::getQuestionId, questionId)
                    .eq(InteractionReply::getAnswerId, 0L)
                    .page(replyPageQuery.toMpPageDefaultSortByCreateTimeDesc());

            List<InteractionReply> records = paged.getRecords(); // po

            if (CollUtils.isEmpty(records)) {
                return new PageDTO<>(0L, 0L, CollUtils.emptyList());
            }


            Set<Long> uids = new HashSet<>(records.size()); //回答者id

            uids = records.stream()
                    .map(InteractionReply::getUserId)
                    .collect(Collectors.toSet());


            List<UserDTO> answers = userClient.queryUserByIds(uids); //适用于问题下的回答

            // 判断查询到了数据，key 为id，值为本身，转换成map型
            Map<Long, UserDTO> userDTOMap = answers.stream().collect(Collectors.toMap(UserDTO::getId, userDTO -> userDTO));


            // 1.2 查询得到po
            // 1.3 不匿名的回答，需要查询用户头像和昵称
            List<ReplyVO> voList = new ArrayList<>(records.size());


            Set<Long> id = records.stream().map(InteractionReply::getId).collect(Collectors.toSet());
            Set<Long> isLikeBizIds = remarkClient.isBizLiked(id);// 传参 全部的业务id

            // 匿名的用户不用返回
            int count = 0;
            for (InteractionReply reply : records) {
                ReplyVO vo = new ReplyVO();
                BeanUtils.copyProperties(reply, vo);

                // false表示不匿名，需要封装
                vo.setUserName(userDTOMap.get(reply.getUserId()).getUsername());
                vo.setUserIcon(userDTOMap.get(reply.getUserId()).getIcon());

                if (isLikeBizIds.contains(reply.getId())) {
                    vo.setLiked(true);
                }

                voList.add(vo);
            }


            return new PageDTO<>(paged.getTotal(), paged.getPages(), voList);
        }

        // 1.1根据questionId以及空的answerId查询回答，以及hidden查询不被隐藏的回答
        Page<InteractionReply> pagedComment = lambdaQuery()
                .eq(InteractionReply::getAnswerId, answerId)
                .page(replyPageQuery.toMpPageDefaultSortByCreateTimeDesc());
        List<InteractionReply> records = pagedComment.getRecords(); // po
        if (CollUtils.isEmpty(records)) {
            return new PageDTO<>(0L, 0L, CollUtils.emptyList());
        }

        Set<Long> uids = new HashSet<>(records.size()); //回答者id

        Set<Long> targetUids = new HashSet<>(records.size()); //评论的目标用户的id


        uids = records.stream()
                .map(InteractionReply::getUserId)
                .collect(Collectors.toSet());


        targetUids = records.stream()
                .map(InteractionReply::getTargetUserId)
                .collect(Collectors.toSet());


        List<UserDTO> answers = userClient.queryUserByIds(uids); //适用于问题下的回答

        List<UserDTO> targetAnswers = userClient.queryUserByIds(targetUids); //评论的目标用户昵称


        // 判断查询到了数据，key 为id，值为本身，转换成map型
        Map<Long, UserDTO> userDTOMap = answers.stream().collect(Collectors.toMap(UserDTO::getId, userDTO -> userDTO));// 回答者
        Map<Long, UserDTO> targetAnswersMap = targetAnswers.stream().collect(Collectors.toMap(UserDTO::getId, userDTO -> userDTO)); // 回答的目标


        // 1.2 查询得到po
        // 1.3 不匿名的回答，需要查询用户头像和昵称
        List<ReplyVO> voList = new ArrayList<>(records.size());

        Set<Long> id = records.stream().map(InteractionReply::getId).collect(Collectors.toSet());
        Set<Long> isLikeBizIds = remarkClient.isBizLiked(id);// 传参 全部的业务id

        // 匿名的用户不用返回
        int count = 0;
        for (InteractionReply reply : records) {
            ReplyVO vo = new ReplyVO();
            BeanUtils.copyProperties(reply, vo);


            vo.setUserName(userDTOMap.get(reply.getUserId()).getName());
            vo.setUserIcon(userDTOMap.get(reply.getUserId()).getIcon());


            vo.setTargetUserName(targetAnswersMap.get(reply.getTargetUserId()).getName());
            if (isLikeBizIds.contains(reply.getId())) {
                vo.setLiked(true);
            }

            voList.add(vo);
        }


        return new PageDTO<>(pagedComment.getTotal(), pagedComment.getPages(), voList);


    }


}
