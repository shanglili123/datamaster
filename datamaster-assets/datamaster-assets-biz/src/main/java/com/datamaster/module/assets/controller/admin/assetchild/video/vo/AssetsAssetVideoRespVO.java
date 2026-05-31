package com.datamaster.module.assets.controller.admin.assetchild.video.vo;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import com.datamaster.common.annotation.Excel;
import java.io.Serializable;
import java.util.Date;/** * - Response VO  DA_ASSET_VIDEO * * @author DATAMASTER * @date 2025-04-14 */
@Schema(description = "- Response VO")
@Data
public class AssetsAssetVideoRespVO implements Serializable {
    private static final long serialVersionUID = 1L;
@Excel(name = "ID")    @Schema(description = "ID")    private Long id;
@Excel(name = "id")    @Schema(description = "id", example = "")    private Long assetId;
@Excel(name = "IP")    @Schema(description = "IP", example = "")    private String ip;
@Excel(name = "")    @Schema(description = "", example = "")    private Long port;
@Excel(name = "")    @Schema(description = "", example = "")    private String protocol;
@Excel(name = "")    @Schema(description = "", example = "")    private String platform;
@Excel(name = "JSON")    @Schema(description = "JSON", example = "")    private String config;
@Excel(name = "")    @Schema(description = "", example = "")    private Boolean validFlag;
@Excel(name = "")    @Schema(description = "", example = "")    private Boolean delFlag;
@Excel(name = "")    @Schema(description = "", example = "")    private String createBy;
@Excel(name = "id")    @Schema(description = "id", example = "")    private Long creatorId;
@Excel(name = "", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")    @Schema(description = "", example = "")    private Date createTime;
@Excel(name = "")    @Schema(description = "", example = "")    private String updateBy;
@Excel(name = "id")    @Schema(description = "id", example = "")    private Long updaterId;
@Excel(name = "", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")    @Schema(description = "", example = "")    private Date updateTime;
@Excel(name = "")    @Schema(description = "", example = "")    private String remark;}
