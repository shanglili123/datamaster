package com.datamaster.module.assets.dal.dataobject.assetchild.audit;

import com.baomidou.mybatisplus.annotation.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import com.datamaster.common.core.domain.BaseEntity;

import java.util.Date;

/**
 *  DO  AST_ASSET_AUDIT_ALERT
 *
 * @author DATAMASTER
 * @date 2025-05-09
 */
@Data
@TableName(value = "AST_ASSET_AUDIT_ALERT")
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class AssetsAssetAuditAlertDO extends BaseEntity {
    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

    /** ID */
    private Long assetId;

    private String batchNo;

    private Date auditTime;

    private Date alertTime;

    private String alertMessage;

    /** JSON */
    private String alertChannels;

    private String alertChannelResult;

    private Boolean validFlag;

    @TableLogic
    private Boolean delFlag;
}
