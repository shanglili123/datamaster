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
 * 动作定义 DO
 *
 * 对应表 ONT_ACTION
 */
@Data
@TableName("ONT_ACTION")
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ActionDO extends BaseEntity {

    /** 所属本体ID */
    private Long ontologyId;

    /** 动作名称 */
    private String name;

    /** 动作类型：CREATE/UPDATE/DELETE/QUERY/FUNCTION */
    private String actionType;

    /** 绑定概念ID（FUNCTION 类型动作不需要概念） */
    private Long conceptId;

    /** 绑定共享函数ID，actionType=FUNCTION 时必填，来自 ONT_FUNCTION 表 */
    private Long functionId;

    /** 动作绑定函数的读取来源概念ID（FUNCTION 类型动作选填：绑定后执行时按属性查询该概念物理表数据注入 input.source.rows） */
    private Long sourceConceptId;

    /** 可选关联关系ID JSON数组（FUNCTION 类型动作选填：按关系关联表查询数据注入 input.source.relations） */
    private String sourceRelationIds;

    /** 输出目标概念ID（FUNCTION 类型动作选填：脚本 JSON 数组结果按主键 UPSERT 到该概念物理表） */
    private Long outputConceptId;

    /** 数据来源读取行数上限（默认 5000，防大批量读取拖垮执行） */
    private Integer readLimit;

    /** 是否需要审批 */
    private Boolean needsApproval;

    /**
     * 执行参数配置(JSON数组):
     * [{"propertyCode":"phone","valueMode":"direct|placeholder|expression","valueTemplate":"${newPhone}","required":true}]
     * 为空时保持旧行为：直接使用执行入参生成SQL。
     */
    private String paramConfig;

    /** 描述 */
    private String description;

    @TableLogic
    private Integer delFlag;
}
