package com.datamaster.module.assets.controller.admin.assetchild.audit.vo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import com.datamaster.common.core.domain.BaseEntity;
import javax.validation.constraints.Size;
import java.util.Date;/** * - / Request VO DA_ASSET_AUDIT_ALERT * * @author DATAMASTER * @date 2025-05-09 */
@Schema(description = "- Response VO")
@Data
public class AssetsAssetAuditAlertSaveReqVO extends BaseEntity {
    private static final long serialVersionUID = 1L;
@Schema(description = "ID")    private Long id;
@Schema(description = "ID", example = "")    private Long assetId;
@Schema(description = "", example = "")    @Size(max = 256, message = "256")    private String batchNo;
@Schema(description = "", example = "")    private Date auditTime;
@Schema(description = "", example = "")    private Date alertTime;
@Schema(description = "", example = "")    @Size(max = 256, message = "256")    private String alertMessage;
@Schema(description = "JSON", example = "")    @Size(max = 256, message = "JSON256")    private String alertChannels;
@Schema(description = "", example = "")    @Size(max = 256, message = "256")    private String alertChannelResult;
@Schema(description = "", example = "")    @Size(max = 256, message = "256")    private String remark;}
