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

    /** 动作类型：CREATE/UPDATE/DELETE/QUERY/COMPOSITE/FUNCTION */
    private String actionType;

    /** 触发对象类型ID（FUNCTION 类型动作不需要） */
    private Long conceptId;

    /** 多目标动作的有序执行步骤 JSONB；每步定义目标概念、操作类型、目标属性和定位条件 */
    private String executionSteps;

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

    /** 是否需要一次人工确认；false 表示提交后直接执行 */
    private Boolean needsApproval;

    /**
     * 提交时判定条件 (JSONB)：不带/空 = 提交免判定（直接进入审批或执行）；带则提交时对「对象当前状态 + 参数」求值，
     * 决定 PASS（允许继续）/ REJECT（前置检查失败）。是否需要人工确认由动作执行模式独立决定。
     */
    private String submissionCriteria;

    /** 人工确认次数兼容字段：0=直接执行，1=一次人工确认 */
    private Integer approvalLevels;

    /**
     * 唯一人工确认人兼容 JSON 数组：[{"stage":1,"userId":1001,"userName":"张三"}]。
     * 为空表示任意登录用户可确认。
     */
    private String approvalReviewers;

    /** 历史触发标识兼容字段；新执行由运行时 triggerType/triggerRef 描述来源 */
    private String triggerRef;

    /**
     * 执行参数配置(JSON数组):
     * [{"propertyCode":"stock","valueMode":"direct|placeholder|relative|expression",
     *   "valueTemplate":"1","relativeOperator":"SUBTRACT","required":true}]
     * 为空时保持旧行为：直接使用执行入参生成SQL。
     */
    private String paramConfig;

    /** 描述 */
    private String description;

    /** 动作定义版本号：提交执行时冻结到执行记录，执行前复核防定义变更后误执行 */
    private Integer version;

    @TableLogic
    private Integer delFlag;
}
