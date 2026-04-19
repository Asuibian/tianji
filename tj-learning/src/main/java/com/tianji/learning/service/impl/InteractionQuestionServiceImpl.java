package com.tianji.learning.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.metadata.OrderItem;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tianji.api.cache.CategoryCache;
import com.tianji.api.client.course.CatalogueClient;
import com.tianji.api.client.course.CourseClient;
import com.tianji.api.client.search.SearchClient;
import com.tianji.api.client.user.UserClient;
import com.tianji.api.dto.course.CataSimpleInfoDTO;
import com.tianji.api.dto.course.CourseFullInfoDTO;
import com.tianji.api.dto.course.CourseSimpleInfoDTO;
import com.tianji.api.dto.user.UserDTO;
import com.tianji.common.constants.Constant;
import com.tianji.common.domain.dto.PageDTO;
import com.tianji.common.exceptions.BadRequestException;
import com.tianji.common.utils.UserContext;
import com.tianji.learning.domain.dto.QuestionFormDTO;
import com.tianji.learning.domain.po.InteractionQuestion;
import com.tianji.learning.domain.po.InteractionReply;
import com.tianji.learning.domain.query.QuestionAdminPageQuery;
import com.tianji.learning.domain.query.QuestionPageQuery;
import com.tianji.learning.domain.vo.QuestionAdminVO;
import com.tianji.learning.domain.vo.QuestionVO;
import com.tianji.learning.enums.QuestionStatus;
import com.tianji.learning.mapper.InteractionQuestionMapper;
import com.tianji.learning.service.IInteractionQuestionService;
import com.tianji.learning.service.IInteractionReplyService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * <p>
 * 互动提问的问题表 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2026-03-30
 */
@Service
@RequiredArgsConstructor
public class InteractionQuestionServiceImpl extends ServiceImpl<InteractionQuestionMapper, InteractionQuestion> implements IInteractionQuestionService {

    private final IInteractionReplyService replyService;
    private final UserClient userClient;

    private final SearchClient searchClient; // 根据课程名称查询课程id

    private final CatalogueClient catalogueClient; // 查询目录信息 得到 目录id  目录名称

    private final CategoryCache categoryCache; // 查询 课程 分类 所属 各级 分类

    private final CourseClient courseClient; // 查询课程相关信息


    @Override //标记注解，说明此方法重写父类接口的方法
    public void saveQuestion(QuestionFormDTO questionDTO) {
        // 1.获取登录用户id
        Long userId = UserContext.getUser();
        // 2.数据封装
        InteractionQuestion interactionQuestion = new InteractionQuestion();
        BeanUtils.copyProperties(questionDTO, interactionQuestion);
        interactionQuestion.setUserId(userId);
        // 3.写入数据库
        save(interactionQuestion);
    }


    @Override
    public PageDTO<QuestionVO> queryQuestionsPage(QuestionPageQuery query) {
        // 1.参数校验。。防止非法操作
        Long courseId = query.getCourseId();
        Long sectionId = query.getSectionId();
        if (courseId == null && sectionId == null) {
            throw new BadRequestException("sectionId  courseId都为空");
        }

        // 2.分页查询
        Page<InteractionQuestion> page = new Page<>(query.getPageNo(), query.getPageSize());
        OrderItem orderItem = new OrderItem();
        orderItem.setAsc(false);
        orderItem.setColumn("create_time");
        page.addOrder(orderItem);

        if (query.getOnlyMine() == null) {
            query.setOnlyMine(false);
        }
        Page<InteractionQuestion> paged = lambdaQuery()
                .select(InteractionQuestion.class, info -> !info.getProperty().equals("description")) // getProperty获得属性名字，等于，然后取反
                .eq(query.getOnlyMine(), InteractionQuestion::getUserId, UserContext.getUser())
                .eq(courseId != null, InteractionQuestion::getCourseId, courseId)
                .eq(sectionId != null, InteractionQuestion::getSectionId, sectionId)
                .eq(InteractionQuestion::getHidden, false)
                .page(page);
        // 判空，如果为空返回空的结果
        List<InteractionQuestion> records = paged.getRecords();
        if (CollUtil.isEmpty(records)) {
            new PageDTO<>(paged.getTotal(), paged.getPages(), Collections.emptyList());
        }


        // 3.根据userId查询提问者的名称和头像。。。。。根据latest_answer_id查询最新回答者的名称和回答内容
        Set<Long> userIds = new HashSet<>();
        Set<Long> answerIds = new HashSet<>();

        // 3.1得到id
        for (InteractionQuestion q : records) {
            // 若提问者匿名则不需要查询
            if (!q.getAnonymity()) {
                userIds.add(q.getUserId());
            }
            answerIds.add(q.getLatestAnswerId());
        }

        // 3.1 执行数据库操作，去除空的数据，最后保存为map，key表示id，内容为本身
        //最近一次回答者
        answerIds.remove(null);
        // 通过回答者的id，得到回答po
        Map<Long, InteractionReply> replyMap = new HashMap<>(answerIds.size());

        if (!CollectionUtil.isEmpty(answerIds)) {
            List<InteractionReply> replies = replyService.listByIds(answerIds);
            for (InteractionReply reply : replies) {
                replyMap.put(reply.getId(), reply);

                if (!reply.getAnonymity()) {
                    Long latestAnswerId = reply.getUserId();
                    userIds.add(latestAnswerId);
                }

            }
        }

        //当前提问者
        userIds.remove(null);
        //通过提问者的id，得到当前提问者的信息
        Map<Long, UserDTO> userMap = new HashMap<>(userIds.size());

        if (!CollectionUtil.isEmpty(userIds)) {
            List<UserDTO> users = userClient.queryUserByIds(userIds);
            userMap = users.stream()
                    .collect(Collectors.toMap(UserDTO::getId, user -> user));
        }

        // 4.封装VO
        List<QuestionVO> voList = new ArrayList<>(records.size());
        for (InteractionQuestion r : records) {
            // 4.1 将PO转为VO
            QuestionVO vo = new QuestionVO();
            BeanUtils.copyProperties(r, vo);

            // 4.2封装提问者用户信息
            if (r.getAnonymity() != null) {
                UserDTO userDTO = userMap.get(r.getUserId());
                if (userDTO != null) {
                    vo.setUserName(userDTO.getName());
                    vo.setUserIcon(userDTO.getIcon());
                }
            }

            // 4.3封装最近一次回答的用户信息，名称和内容
            InteractionReply reply = replyMap.get(r.getLatestAnswerId());
            if (reply != null) {
                vo.setLatestReplyContent(reply.getContent());
                if (!reply.getAnonymity()) {
                    // 获得最近一次回答的用户的名称
                    UserDTO dto = userMap.get(reply.getUserId());
                    vo.setLatestReplyUser(dto.getName());
                }
            }

            voList.add(vo);
        }
        return PageDTO.of(page, voList);
        //return new PageDTO<>(paged.getTotal(), paged.getPages(), voList);
    }

    @Override
    public QuestionVO queryQuestionsById(Long id) {
        // 1.根据id查询数据
        InteractionQuestion question = getById(id); // 数据库操作
        // 2.数据校验
        if (question == null || question.getHidden()) {
            // 数据为空 或者 被隐藏
            return null;
        }
        // 3.查询提问者信息
        UserDTO user = null;
        if (!question.getAnonymity()) {
            user = userClient.queryUserById(question.getUserId());// 数据库操作
        }
        // 4.封装VO
        QuestionVO vo = new QuestionVO();
        BeanUtils.copyProperties(question, vo);
        if (user != null) {
            vo.setUserName(user.getName());
            vo.setUserIcon(user.getIcon());
        }
        return vo;
    }

    @Override
    public PageDTO<QuestionAdminVO> queryQuestionsPageAdmin(QuestionAdminPageQuery query) {
        Integer status = query.getStatus();
        LocalDateTime beginTime = query.getBeginTime();
        LocalDateTime endTime = query.getEndTime();
        String courseName = query.getCourseName();
        // 1.处理课程名称，得到课程id
        List<Long> coursesIds = null;
        if (StrUtil.isNotBlank(courseName)) {
            coursesIds = searchClient.queryCoursesIdByName(courseName);
            if (CollUtil.isEmpty(coursesIds)) {
                return PageDTO.empty(0L, 0L);
            }
        }
        Page<InteractionQuestion> page = new Page<>(query.getPageNo(), query.getPageSize());
        OrderItem orderItem = new OrderItem();
        orderItem.setAsc(false);
        orderItem.setColumn(Constant.DATA_FIELD_NAME_CREATE_TIME);
        page.addOrder(orderItem);

        // 2.分页查询
        Page<InteractionQuestion> paged = lambdaQuery()
                .in(coursesIds != null, InteractionQuestion::getCourseId, coursesIds)
                .eq(status != null, InteractionQuestion::getStatus, status)
                .gt(beginTime != null, InteractionQuestion::getCreateTime, beginTime)
                .le(endTime != null, InteractionQuestion::getCreateTime, endTime)
                .page(page);
        List<InteractionQuestion> records = paged.getRecords();
        if (CollUtil.isEmpty(records)) {
            return new PageDTO<>(paged.getTotal(), paged.getPages(), Collections.emptyList());
        }

        // 3.准备VO需要的数据，用户数据，课程数据，章节数据，分类数据
        Set<Long> userIds = new HashSet<>(); // 存储用户id集合
        Set<Long> cIds = new HashSet<>(); // 存储课程id集合
        Set<Long> cataIds = new HashSet<>(); // 存储章和小节id集合
        // 3.1 获取各种数据的id集合
        for (InteractionQuestion q : records) {
            userIds.add(q.getUserId());
            cIds.add(q.getCourseId());
            cataIds.add(q.getChapterId());
            cataIds.add(q.getSectionId());
        }

        // 3.2 根据id查询用户
        userIds.remove(null);
        List<UserDTO> users = userClient.queryUserByIds(userIds);
        Map<Long, UserDTO> userDTOMap = new HashMap<>(users.size());
        if (CollUtil.isNotEmpty(users)) {
            userDTOMap = users.stream().collect(Collectors.toMap(UserDTO::getId, user -> user));
        }

        // 3.3 根据id查询课程
        cIds.remove(null);
        List<CourseSimpleInfoDTO> cinfos = courseClient.getSimpleInfoList(cIds);
        Map<Long, CourseSimpleInfoDTO> cinfoDTOMap = new HashMap<>(cinfos.size());
        if (CollUtil.isNotEmpty(cinfos)) {
            cinfoDTOMap = cinfos.stream().collect(Collectors.toMap(CourseSimpleInfoDTO::getId, c -> c));
        }

        // 3.4 根据id查询章节
        cataIds.remove(null);
        List<CataSimpleInfoDTO> catas = catalogueClient.batchQueryCatalogue(cataIds);
        Map<Long, String> catasMap = new HashMap<>(catas.size());
        if (CollUtil.isNotEmpty(catas)) {
            catasMap = catas.stream()
                    .collect(Collectors.toMap(CataSimpleInfoDTO::getId, CataSimpleInfoDTO::getName));
        }

        // 4.封装VO
        List<QuestionAdminVO> volist = new ArrayList<>(records.size());
        for (InteractionQuestion q : records) {
            QuestionAdminVO vo = new QuestionAdminVO();
            BeanUtils.copyProperties(q, vo);
            volist.add(vo);

            // 用户信息
            UserDTO dto = userDTOMap.get(q.getUserId());
            if (dto != null) {
                vo.setUserName(dto.getName());
            }

            // 课程以及分类信息
            CourseSimpleInfoDTO courseSimpleInfoDTO = cinfoDTOMap.get(q.getCourseId());
            if (courseSimpleInfoDTO != null) {
                vo.setCourseName(courseSimpleInfoDTO.getName());
                List<Long> categoryIds = courseSimpleInfoDTO.getCategoryIds();
                String categoryNames = categoryCache.getCategoryNames(categoryIds);
                vo.setCategoryName(categoryNames);
            }
            // 章节信息
            vo.setCategoryName(catasMap.getOrDefault(q.getChapterId(), ""));
            vo.setSectionName(catasMap.getOrDefault(q.getSectionId(), ""));
        }

        return new PageDTO<>(page.getTotal(), page.getPages(), volist);
    }

    @Override
    public QuestionAdminVO queryQuestionAdminByid(Long id) {
        QuestionAdminVO vo = new QuestionAdminVO();

        InteractionQuestion q = getById(id);
        BeanUtils.copyProperties(q, vo);


        // 1.用户信息
        List<UserDTO> users = userClient.queryUserByIds(Collections.singleton(q.getUserId()));
        if (users != null && !users.isEmpty()) {
            vo.setUserName(users.get(0).getName());
            vo.setUserIcon(users.get(0).getIcon());
        }

        // 1.1获取老师列表ids，通过课程id
        CourseFullInfoDTO infoById = courseClient.getCourseInfoById(q.getCourseId(), false, true);
        // 1.1查询老师用户信息
        if (infoById != null) {
            List<Long> teacherIds = infoById.getTeacherIds();
            if (teacherIds != null) {
                List<UserDTO> teacherInfo = userClient.queryUserByIds(teacherIds);
                if (teacherInfo != null) {
                    String teacherString = teacherInfo.stream().map(UserDTO::getName)
                            .collect(Collectors.joining(","));
                    vo.setTeacherName(teacherString);
                }
            }
        }

        // 2.课程以及分类信息
        List<CourseSimpleInfoDTO> cinfos = courseClient.getSimpleInfoList(Collections.singleton(q.getCourseId()));

        if (cinfos != null) {
            vo.setCourseName(cinfos.get(0).getName());

            List<Long> categoryIds = cinfos.get(0).getCategoryIds();
            String categoryNames = categoryCache.getCategoryNames(categoryIds);
            vo.setCategoryName(categoryNames);
        }

        // 3.章节信息
        Set<Long> cataIds = new HashSet<>();
        cataIds.add(q.getSectionId());
        cataIds.add(q.getChapterId());

        List<CataSimpleInfoDTO> catas = catalogueClient.batchQueryCatalogue(cataIds);
        Map<Long, String> catasMap = catas.stream().collect(Collectors.toMap(CataSimpleInfoDTO::getId, CataSimpleInfoDTO::getName));

        vo.setCategoryName(catasMap.getOrDefault(q.getChapterId(), ""));
        vo.setSectionName(catasMap.getOrDefault(q.getSectionId(), ""));

        // 4.修改状态，为老师已查看
        // boolean s = q.getStatus().equals(QuestionStatus.CHECKED); 常量放在判断前面，因为变量可能会为空
        boolean s = QuestionStatus.CHECKED.equals(q.getStatus());
        if (!s) {
            lambdaUpdate()
                    .eq(InteractionQuestion::getId, id)
                    .set(InteractionQuestion::getStatus, QuestionStatus.CHECKED)
                    .update();
        }
        return vo;

    }


}


























