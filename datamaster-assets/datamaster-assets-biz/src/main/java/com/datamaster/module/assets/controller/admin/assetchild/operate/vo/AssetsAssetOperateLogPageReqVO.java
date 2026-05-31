package com.datamaster.module.assets.controller.admin.assetchild.operate.vo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import com.datamaster.common.core.page.PageParam;
import java.util.Date;/** *  Request VO  DA_ASSET_OPERATE_LOG * * @author DATAMASTER * @date 2025-05-09 */
@Schema(description = " Request VO")
@Data
public class AssetsAssetOperateLogPageReqVO extends PageParam {
    private static final long serialVersionUID = 1L;
@Schema(description = "ID", example = "")        private Long id;
@Schema(description = "id", example = "")    private Long assetId;
@Schema(description = "id", example = "")    private Long datasourceId;
@Schema(description = "", example = "")    private String tableName;
@Schema(description = "/", example = "")    private String tableComment;
@Schema(description = "", example = "")    private String operateType;
@Schema(description = "", example = "")    private Date operateTime;
@Schema(description = "", example = "")    private Date executeTime;
@Schema(description = "(JSON)", example = "")    private String updateBefore;
@Schema(description = "JSON MD5", example = "")    private String updateWhereMd5;
@Schema(description = "(JSON)", example = "")    private String updateAfter;
@Schema(description = "", example = "")    private String fieldNames;
@Schema(description = "URL", example = "")    private String fileUrl;
@Schema(description = "", example = "")    private String fileName;
@Schema(description = "", example = "")    private String status;
@Schema(description = "", example = "")    private Date startTime;
@Schema(description = "", example = "")    private Date endTime;}
