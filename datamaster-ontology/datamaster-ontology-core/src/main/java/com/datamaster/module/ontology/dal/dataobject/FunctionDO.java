package com.datamaster.module.ontology.dal.dataobject;

import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.datamaster.common.core.domain.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * 函数定义 DO — 对应表 ONT_FUNCTION
 */
@Data
@TableName("ONT_FUNCTION")
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class FunctionDO extends BaseEntity {

    /** 所属本体ID */
    private Long ontologyId;

    /** 函数名称 */
    private String name;

    /** 函数编码 */
    private String code;

    /** 语言：TYPESCRIPT / PYTHON */
    private String lang;

    /** 函数代码体 */
    private String body;

    /** 是否需要审批 */
    private Boolean needsApproval;

    /** 描述 */
    private String description;

    @TableLogic
    private Integer delFlag;
}
