package com.datamaster.module.ontology.dal.dataobject;

import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.datamaster.common.core.domain.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@TableName("ONT_ACTION_WORKFLOW")
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ActionWorkflowDO extends BaseEntity {
    private Long ontologyId;
    private String code;
    private String name;
    private Long triggerConceptId;
    private Integer version;
    private String status;
    private String triggerRef;
    private String inputSchema;
    private String outputSchema;
    private String failurePolicy;
    private Boolean enabled;
    private String description;
    @TableLogic
    private Integer delFlag;
}
