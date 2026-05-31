package com.datamaster.module.assets.controller.admin.assetchild.operate.vo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import com.datamaster.common.core.page.PageParam;
import java.util.Date;/** *  Request VO  DA_ASSET_OPERATE_APPLY * * @author DATAMASTER * @date 2025-05-09 */
@Schema(description = " Request VO")
@Data
public class AssetsAssetOperateApplyPageReqVO extends PageParam {
    private static final long serialVersionUID = 1L;
@Schema(description = "ID", example = "")        private Long id;
@Schema(description = "id", example = "")    private Long assetId;
@Schema(description = "id", example = "")    private Long datasourceId;
@Schema(description = "", example = "")    private String tableName;
@Schema(description = "/", example = "")    private String tableComment;
@Schema(description = "", example = "")    private String operateType;
@Schema(description = "JSON", example = "")    private String operateJson;
@Schema(description = "", example = "")    private Date operateTime;
@Schema(description = "", example = "")    private String executeFlag;
@Schema(description = "", example = "")    private Date executeTime;}
