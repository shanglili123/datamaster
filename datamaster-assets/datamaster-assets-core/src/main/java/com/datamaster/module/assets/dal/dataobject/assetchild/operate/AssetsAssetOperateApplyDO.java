package com.datamaster.module.assets.dal.dataobject.assetchild.operate;

import com.baomidou.mybatisplus.annotation.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import com.datamaster.common.core.domain.BaseEntity;

import java.util.Date;

/**
 *  DO  AST_ASSET_OPERATE_APPLY
 *
 * @author DATAMASTER
 * @date 2025-05-09
 */
@Data
@TableName(value = "AST_ASSET_OPERATE_APPLY")
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class AssetsAssetOperateApplyDO extends BaseEntity {
    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

    /** id */
    private Long assetId;

    /** id */
    private Long datasourceId;

    private String tableName;

    private String tableComment;

    private String operateType;

    /** JSON */
    private String operateJson;

    private Date operateTime;

    private String executeFlag;

    private Date executeTime;

    /** 审批与执行状态。老数据为空时按历史 executeFlag 兼容处理。 */
    private String approvalStatus;

    /** 最近一次审批意见。 */
    private String approvalComment;

    /** 审批人。 */
    private String approvedBy;

    /** 审批人 ID。 */
    private Long approvedId;

    /** 审批时间。 */
    private Date approvedTime;

    /** 最近一次执行结果或错误信息。 */
    private String executionMessage;

    private Boolean validFlag;

    @TableLogic
    private Boolean delFlag;
}
