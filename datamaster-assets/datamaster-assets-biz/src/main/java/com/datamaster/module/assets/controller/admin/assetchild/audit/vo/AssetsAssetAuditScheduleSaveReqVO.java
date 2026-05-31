package com.datamaster.module.assets.controller.admin.assetchild.audit.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import com.datamaster.common.core.domain.BaseEntity;

import javax.validation.constraints.Size;

/**
 *  / Request VO DA_ASSET_AUDIT_SCHEDULE
 *
 * @author DATAMASTER
 * @date 2025-05-09
 */
@Schema(description = " Response VO")
@Data
public class AssetsAssetAuditScheduleSaveReqVO extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "ID", example = "")
    private Long assetId;

    @Schema(description = "", example = "")
    @Size(max = 256, message = "256")
    private String scheduleFlag;

    @Schema(description = "cron", example = "")
    @Size(max = 256, message = "cron256")
    private String cronExpression;

    @Schema(description = "id", example = "")
    private Long nodeId;

    @Schema(description = "", example = "")
    @Size(max = 256, message = "256")
    private String nodeCode;

    @Schema(description = "id", example = "")
    private Long taskId;

    @Schema(description = "", example = "")
    @Size(max = 256, message = "256")
    private String taskCode;

    @Schema(description = "id", example = "")
    private Long systemJobId;

    @Schema(description = "", example = "")
    @Size(max = 256, message = "256")
    private String remark;

}
