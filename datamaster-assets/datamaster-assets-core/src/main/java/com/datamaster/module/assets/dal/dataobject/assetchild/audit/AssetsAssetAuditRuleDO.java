package com.datamaster.module.assets.dal.dataobject.assetchild.audit;

import com.baomidou.mybatisplus.annotation.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import com.datamaster.common.core.domain.BaseEntity;

import java.util.Date;

/**
 *  DO  AST_ASSET_AUDIT_RULE
 *
 * @author DATAMASTER
 * @date 2025-05-09
 */
@Data
@TableName(value = "AST_ASSET_AUDIT_RULE")
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class AssetsAssetAuditRuleDO extends BaseEntity {
    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

    /** ID */
    private Long assetId;

    private String tableName;

    private String columnName;

    private String columnComment;

    private String ruleName;

    private String qualityDim;

    private String ruleType;

    private String ruleLevel;

    private String ruleDescription;

    private String ruleConfig;

    private Long totalCount;

    private Long issueCount;

    private Date auditTime;

    private String batchNo;

    private Boolean validFlag;

    @TableLogic
    private Boolean delFlag;
}
