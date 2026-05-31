package com.datamaster.module.assets.dal.dataobject.assetchild.audit;import com.baomidou.mybatisplus.annotation.*;import lombok.*;
import lombok.experimental.SuperBuilder;
import com.datamaster.common.core.domain.BaseEntity;import java.util.Date;
/** *  DO  DA_ASSET_AUDIT_RULE * * @author DATAMASTER * @date 2025-05-09 */

@Data
@TableName(value = "AST_ASSET_AUDIT_RULE")
// 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
// @KeySequence("DA_ASSET_AUDIT_RULE_seq")
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)public class AssetsAssetAuditRuleDO extends BaseEntity {    @TableField(exist = false)    private static final long serialVersionUID = 1L;/** ID */
    private Long assetId;    /**  */
    private String tableName;    /** / */
    private String columnName;    /** / */
    private String columnComment;    /**  */
    private String ruleName;    /**  */
    private String qualityDim;    /**  */
    private String ruleType;    /**  */
    private String ruleLevel;    /**  */
    private String ruleDescription;    /**  */
    private String ruleConfig;    /**  */
    private Long totalCount;    /**  */
    private Long issueCount;    /**  */
    private Date auditTime;    /**  */
    private String batchNo;    /**  */
    private Boolean validFlag;    /**  */

@TableLogic    private Boolean delFlag;
}