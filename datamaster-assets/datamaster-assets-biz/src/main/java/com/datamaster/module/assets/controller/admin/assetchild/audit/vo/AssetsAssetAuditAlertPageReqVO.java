package com.datamaster.module.assets.controller.admin.assetchild.audit.vo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import com.datamaster.common.core.page.PageParam;
import java.util.Date;/** * - Request VO  DA_ASSET_AUDIT_ALERT * * @author DATAMASTER * @date 2025-05-09 */
@Schema(description = "- Request VO")
@Data
public class AssetsAssetAuditAlertPageReqVO extends PageParam {
    private static final long serialVersionUID = 1L;
@Schema(description = "ID", example = "")        private Long id;
@Schema(description = "ID", example = "")    private Long assetId;
@Schema(description = "", example = "")    private String batchNo;
@Schema(description = "", example = "")    private Date auditTime;
@Schema(description = "", example = "")    private Date alertTime;
@Schema(description = "", example = "")    private String alertMessage;
@Schema(description = "JSON", example = "")    private String alertChannels;
@Schema(description = "", example = "")    private String alertChannelResult;}
