package com.datamaster.module.assets.controller.admin.assetchild.api.vo;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import com.datamaster.common.annotation.Excel;
import java.io.Serializable;
import java.util.Date;/** * -API Response VO  DA_ASSET_API * * @author DATAMASTER * @date 2025-04-14 */
@Schema(description = "-API Response VO")
@Data
public class AssetsAssetApiRespVO implements Serializable {
    private static final long serialVersionUID = 1L;
@Excel(name = "ID")    @Schema(description = "ID")    private Long id;
@Excel(name = "id")    @Schema(description = "id", example = "")    private Long assetId;
@Excel(name = "API")    @Schema(description = "API", example = "")    private String url;
@Schema(description = "", example = "")    private String developerName;
@Schema(description = "", example = "")    private String appName;
@Excel(name = "")    @Schema(description = "", example = "")    private String httpMethod;
@Excel(name = "")    @Schema(description = "", example = "")    private Boolean validFlag;
@Excel(name = "")    @Schema(description = "", example = "")    private Boolean delFlag;
@Excel(name = "")    @Schema(description = "", example = "")    private String createBy;
@Excel(name = "id")    @Schema(description = "id", example = "")    private Long creatorId;
@Excel(name = "", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")    @Schema(description = "", example = "")    private Date createTime;
@Excel(name = "")    @Schema(description = "", example = "")    private String updateBy;
@Excel(name = "id")    @Schema(description = "id", example = "")    private Long updaterId;
@Excel(name = "", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")    @Schema(description = "", example = "")    private Date updateTime;
@Excel(name = "")    @Schema(description = "", example = "")    private String remark;}
