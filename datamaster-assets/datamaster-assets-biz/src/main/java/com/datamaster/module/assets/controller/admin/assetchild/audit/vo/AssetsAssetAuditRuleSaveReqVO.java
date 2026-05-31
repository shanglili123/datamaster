package com.datamaster.module.assets.controller.admin.assetchild.audit.vo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import com.datamaster.common.core.domain.BaseEntity;
import javax.validation.constraints.Size;
import java.util.Date;/** *  / Request VO DA_ASSET_AUDIT_RULE * * @author DATAMASTER * @date 2025-05-09 */
@Schema(description = " Response VO")
@Data
public class AssetsAssetAuditRuleSaveReqVO extends BaseEntity {
    private static final long serialVersionUID = 1L;
@Schema(description = "ID")    private Long id;
@Schema(description = "ID", example = "")    private Long assetId;
@Schema(description = "", example = "")    @Size(max = 256, message = "256")    private String tableName;
@Schema(description = "/", example = "")    @Size(max = 256, message = "/256")    private String columnName;
@Schema(description = "/", example = "")    @Size(max = 256, message = "/256")    private String columnComment;
@Schema(description = "", example = "")    @Size(max = 256, message = "256")    private String ruleName;
@Schema(description = "", example = "")    @Size(max = 256, message = "256")    private String qualityDim;
@Schema(description = "", example = "")    @Size(max = 256, message = "256")    private String ruleType;
@Schema(description = "", example = "")    @Size(max = 256, message = "256")    private String ruleLevel;
@Schema(description = "", example = "")    @Size(max = 256, message = "256")    private String ruleDescription;
@Schema(description = "", example = "")    @Size(max = 256, message = "256")    private String ruleConfig;
@Schema(description = "", example = "")    private Long totalCount;
@Schema(description = "", example = "")    private Long issueCount;
@Schema(description = "", example = "")    private Date auditTime;
@Schema(description = "", example = "")    @Size(max = 256, message = "256")    private String batchNo;
@Schema(description = "", example = "")    @Size(max = 256, message = "256")    private String remark;}
