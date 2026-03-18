package com.tianji.learning.domain.vo;

import com.tianji.learning.enums.LessonStatus;
import com.tianji.learning.enums.PlanStatus;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@ApiModel(description = "最近学习的课程信息")
public class LearningNowVO {

    @ApiModelProperty("主键lessonId")
    private Long id;

    @ApiModelProperty("课程id")
    private Long courseId;

    @ApiModelProperty("课程名称")
    private String courseName;

    @ApiModelProperty("课程封面")
    private String courseCoverUrl;

    @ApiModelProperty("课程总课时数")
    private Integer sections;

    @ApiModelProperty("已学习课时数量")
    private Integer learnedSections;

    @ApiModelProperty("总已报名课程数")
    private Integer courseAmount;

    @ApiModelProperty("课程购买时间")
    private LocalDateTime createTime;

    @ApiModelProperty("课程过期时间，如果为null代表课程永久有效")
    private LocalDateTime expireTime;

    @ApiModelProperty("最近学习的小节名")
    private String latestSectionName;

    @ApiModelProperty("最近学习的小节编号")
    private Integer latestSectionIndex;
}
