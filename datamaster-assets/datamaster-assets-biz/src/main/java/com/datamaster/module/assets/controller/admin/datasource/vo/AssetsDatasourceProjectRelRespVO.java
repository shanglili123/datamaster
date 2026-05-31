package com.datamaster.module.assets.controller.admin.datasource.vo;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import com.datamaster.common.annotation.Excel;
import java.io.Serializable;
import java.util.Date;/** *  Response VO  DA_DATASOURCE_PROJECT_REL * * @author DATAMASTER * @date 2025-03-13 */
@Schema(description = " Response VO")
@Data
public class AssetsDatasourceProjectRelRespVO implements Serializable {
    private static final long serialVersionUID = 1L;
@Excel(name = "ID")    @Schema(description = "ID")    private Long id;
@Excel(name = "id")    @Schema(description = "id", example = "")    private Long projectId;
@Excel(name = "")    @Schema(description = "", example = "")    private String projectCode;
@Excel(name = "")    @Schema(description = "", example = "")    private Boolean dppAssigned;
@Excel(name = "id")    @Schema(description = "id", example = "")    private Long datasourceId;
@Excel(name = "")    @Schema(description = "", example = "")    private String description;
@Excel(name = "")    @Schema(description = "", example = "")    private Boolean validFlag;
@Excel(name = "")    @Schema(description = "", example = "")    private String createBy;
@Excel(name = "id")    @Schema(description = "id", example = "")    private Long creatorId;
@Excel(name = "", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")    @Schema(description = "", example = "")    private Date createTime;
@Excel(name = "")    @Schema(description = "", example = "")    private String updateBy;
@Excel(name = "id")    @Schema(description = "id", example = "")    private Long updaterId;
@Excel(name = "", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")    @Schema(description = "", example = "")    private Date updateTime;
@Excel(name = "")    @Schema(description = "", example = "")    private String remark;}
