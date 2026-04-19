package com.tianji.learning.domain.dto;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.NotNull;

@Data
@ApiModel(description = "回答、评论表单信息")
public class ReplayDTO {
    /**
     * 互动问题问题id
     */
    @ApiModelProperty("互动问题问题id")
    private Long questionId;

    /**
     * 回复的上级回答id
     */
    @ApiModelProperty("回复的上级回答id")
    private Long answerId;

    /**
     * 回答内容
     */
    @ApiModelProperty("回答内容")
    private String content;

    /**
     * 回复的目标用户id
     */
    @ApiModelProperty("回复的目标用户id")

    private Long targetUserId;

    /**
     * 回复的目标回复id
     */
    @ApiModelProperty("回复的目标回复id")

    private Long targetReplyId;
    /**
     * 是否匿名，默认false
     */
    @ApiModelProperty("是否匿名")
    private Boolean anonymity;
    /**
     * 是否是学生提交
     */
    @ApiModelProperty("是否是学生提交")
    private Boolean isStudent;


}
