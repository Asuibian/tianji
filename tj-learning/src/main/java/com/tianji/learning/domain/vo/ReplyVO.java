package com.tianji.learning.domain.vo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@ApiModel(description = "回答、评论信息")
public class ReplyVO {
    @ApiModelProperty("主键id")
    private Long id;

    @ApiModelProperty("回答者id")
    private Long userId;
    @ApiModelProperty("回答者头像")
    private String userIcon;
    @ApiModelProperty("回答者昵称")
    private String userName;
    @ApiModelProperty("评论的目标用户昵称")
    private String targetUserName;
    @ApiModelProperty("回答内容")
    private String content;
    @ApiModelProperty("回答下的评论数量")
    private Integer replyTimes;
    @ApiModelProperty("是否匿名")
    private Boolean anonymity;


    @ApiModelProperty("创建时间")
    private LocalDateTime createTime;
    @ApiModelProperty("点赞数量")
    private Integer likedTimes;
    @ApiModelProperty("当前用户是否点赞过")
    private Boolean liked;


}