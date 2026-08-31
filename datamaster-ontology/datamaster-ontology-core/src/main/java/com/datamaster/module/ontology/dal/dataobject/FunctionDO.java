package com.datamaster.module.ontology.dal.dataobject;

import com.baomidou.mybatisplus.annotation.TableField;
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

    /** 所属本体ID（共享函数可空，不强制绑定本体） */
    private Long ontologyId;

    /** 函数名称 */
    private String name;

    /** 函数编码 */
    private String code;

    /** 语言：TYPESCRIPT / PYTHON */
    private String lang;

    /** 函数代码体 */
    private String body;

    /** 参数声明(JSON数组)，如 ["name","amount"]；绑定主概念后为属性 code 列表 */
    @TableField("PARAMS")
    private String paramNames;

    /** 数据来源主概念ID（可空：绑定后执行时按属性查询该概念物理表数据注入 input.source.rows） */
    private Long sourceConceptId;

    /** 可选关联关系ID JSON数组（可空：按关系关联表查询数据注入 input.source.relations） */
    private String sourceRelationIds;

    /** 输出目标概念ID（可空：脚本 JSON 数组结果按主键 UPSERT 到该概念物理表） */
    private Long outputConceptId;

    /** 主概念/关系数据读取行数上限（默认 5000） */
    private Integer readLimit;

    /** 是否需要审批 */
    private Boolean needsApproval;

    /** 描述 */
    private String description;

    @TableLogic
    private Integer delFlag;
}
