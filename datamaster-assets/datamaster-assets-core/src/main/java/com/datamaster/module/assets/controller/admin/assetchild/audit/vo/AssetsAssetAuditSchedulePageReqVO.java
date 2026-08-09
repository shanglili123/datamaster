package com.datamaster.module.assets.controller.admin.assetchild.audit.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import com.datamaster.common.core.page.PageParam;

/**
 *  Request VO  DA_ASSET_AUDIT_SCHEDULE
 *
 * @author DATAMASTER
 * @date 2025-05-09
 */
@Schema(description = " Request VO")
@Data
public class AssetsAssetAuditSchedulePageReqVO extends PageParam {

    private static final long serialVersionUID = 1L;
        @Schema(description = "ID", example = "")
        private Long id;
    @Schema(description = "ID", example = "")
    private Long assetId;

    @Schema(description = "", example = "")
    private String scheduleFlag;

    @Schema(description = "cron", example = "")
    private String cronExpression;

    @Schema(description = "id", example = "")
    private Long nodeId;

    @Schema(description = "", example = "")
    private String nodeCode;

    @Schema(description = "id", example = "")
    private Long taskId;

    @Schema(description = "", example = "")
    private String taskCode;

    @Schema(description = "id", example = "")
    private Long systemJobId;

}
