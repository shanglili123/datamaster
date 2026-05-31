package com.datamaster.module.assets.controller.admin.discovery.vo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import com.datamaster.common.annotation.Excel;
import java.io.Serializable;
import java.util.Date;/** *  Response VO  DA_DISCOVERY_COLUMN * * @author DATAMASTER * @date 2025-02-11 */
@Schema(description = " Response VO")
@Data
public class AssetsDiscoveryColumnRespVO implements Serializable {
    private static final long serialVersionUID = 1L;
@Excel(name = "ID")    @Schema(description = "ID")    private Long id;
@Excel(name = "id")    @Schema(description = "id", example = "")    private Long taskId;
@Excel(name = "id")    @Schema(description = "id", example = "")    private Long tableId;
@Excel(name = "/")    @Schema(description = "/", example = "")    private String columnName;
@Excel(name = "/")    @Schema(description = "/", example = "")    private String columnComment;
@Excel(name = "")    @Schema(description = "", example = "")    private String columnType;
@Excel(name = "")    @Schema(description = "", example = "")    private Long columnLength;
@Excel(name = "")    @Schema(description = "", example = "")    private Long columnScale;
@Excel(name = "")    @Schema(description = "", example = "")    private String nullableFlag;
@Excel(name = "")    @Schema(description = "", example = "")    private String pkFlag;
@Excel(name = "")    @Schema(description = "", example = "")    private String defaultValue;
@Excel(name = "")    @Schema(description = "", example = "")    private Boolean validFlag;
@Excel(name = "")    @Schema(description = "", example = "")    private Boolean delFlag;
@Excel(name = "")    @Schema(description = "", example = "")    private String createBy;
@Excel(name = "id")    @Schema(description = "id", example = "")    private Long creatorId;
@Excel(name = "", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")    @Schema(description = "", example = "")    private Date createTime;
@Excel(name = "")    @Schema(description = "", example = "")    private String updateBy;
@Excel(name = "id")    @Schema(description = "id", example = "")    private Long updaterId;
@Excel(name = "", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")    @Schema(description = "", example = "")    private Date updateTime;
@Excel(name = "")    @Schema(description = "", example = "")    private String remark;}
