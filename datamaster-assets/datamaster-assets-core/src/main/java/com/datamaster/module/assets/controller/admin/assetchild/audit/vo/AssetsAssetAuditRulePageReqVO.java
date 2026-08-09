package com.datamaster.module.assets.controller.admin.assetchild.audit.vo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import com.datamaster.common.core.page.PageParam;
import java.util.Date;/** *  Request VO  DA_ASSET_AUDIT_RULE * * @author DATAMASTER * @date 2025-05-09 */
@Schema(description = " Request VO")
@Data
public class AssetsAssetAuditRulePageReqVO extends PageParam {
    private static final long serialVersionUID = 1L;
@Schema(description = "ID", example = "")        private Long id;
@Schema(description = "ID", example = "")    private Long assetId;
@Schema(description = "", example = "")    private String tableName;
@Schema(description = "/", example = "")    private String columnName;
@Schema(description = "/", example = "")    private String columnComment;
@Schema(description = "", example = "")    private String ruleName;
@Schema(description = "", example = "")    private String qualityDim;
@Schema(description = "", example = "")    private String ruleType;
@Schema(description = "", example = "")    private String ruleLevel;
@Schema(description = "", example = "")    private String ruleDescription;
@Schema(description = "", example = "")    private String ruleConfig;
@Schema(description = "", example = "")    private Long totalCount;
@Schema(description = "", example = "")    private Long issueCount;
@Schema(description = "", example = "")    private Date auditTime;
@Schema(description = "", example = "")    private String batchNo;}
