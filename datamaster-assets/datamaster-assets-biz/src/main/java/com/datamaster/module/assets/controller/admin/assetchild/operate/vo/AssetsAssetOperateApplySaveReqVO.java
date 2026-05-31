package com.datamaster.module.assets.controller.admin.assetchild.operate.vo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import com.datamaster.common.core.domain.BaseEntity;
import javax.validation.constraints.Size;
import java.util.Date;/** *  / Request VO DA_ASSET_OPERATE_APPLY * * @author DATAMASTER * @date 2025-05-09 */
@Schema(description = " Response VO")
@Data
public class AssetsAssetOperateApplySaveReqVO extends BaseEntity {
    private static final long serialVersionUID = 1L;
@Schema(description = "ID")    private Long id;
@Schema(description = "id", example = "")    private Long assetId;
@Schema(description = "id", example = "")    private Long datasourceId;
@Schema(description = "", example = "")    @Size(max = 256, message = "256")    private String tableName;
@Schema(description = "/", example = "")    @Size(max = 256, message = "/256")    private String tableComment;
@Schema(description = "", example = "")    @Size(max = 256, message = "256")    private String operateType;
@Schema(description = "JSON", example = "")    @Size(max = 256, message = "JSON256")    private String operateJson;
@Schema(description = "", example = "")    private Date operateTime;
@Schema(description = "", example = "")    @Size(max = 256, message = "256")    private String executeFlag;
@Schema(description = "", example = "")    private Date executeTime;
@Schema(description = "", example = "")    @Size(max = 256, message = "256")    private String remark;}
