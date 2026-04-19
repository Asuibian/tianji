package com.tianji.learning.domain.query;

import com.tianji.common.domain.query.PageQuery;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true) // 会重写 equals() 和 hashCode() 方法。
@Data
@ApiModel(description = "互动问题分页查询条件")
public class ReplyPageQuery extends PageQuery {
    // 查询条件
    @ApiModelProperty(value = "问题id")
    private Long questionId;
    @ApiModelProperty(value = "父回答id", example = "1")
    private Long answerId;
}
