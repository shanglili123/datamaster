package com.datamaster.module.assets.controller.admin.datasource.vo;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import com.datamaster.common.annotation.Excel;
import com.datamaster.module.assets.dal.dataobject.datasource.AssetsDatasourceProjectRelDO;
import java.io.Serializable;
import java.util.Date;
import java.util.List;/** *  Response VO  DA_DATASOURCE * * @author lhs * @date 2025-01-21 */
@Schema(description = " Response VO")
@Data
public class AssetsDatasourceRespVO implements Serializable {
    private static final long serialVersionUID = 1L;
@Excel(name = "ID")    @Schema(description = "ID")    private Long id;
@Excel(name = "")    @Schema(description = "", example = "")    private String datasourceName;
@Excel(name = "")    @Schema(description = "", example = "")    private String datasourceType;
@Excel(name = "(json)")    @Schema(description = "(json)", example = "")    private String datasourceConfig;
@Excel(name = "")    @Schema(description = "", example = "")    private List<AssetsDatasourceProjectRelDO> projectList;
@Excel(name = "")    @Schema(description = "", example = "")    private String projectName;
@Excel(name = "")    @Schema(description = "", example = "")    private Boolean isAdminAddTo;
@Excel(name = "IP")    @Schema(description = "IP", example = "")    private String ip;
@Excel(name = "")    @Schema(description = "", example = "")    private Long port;
@Excel(name = "", readConverterExp = "=")    @Schema(description = "", example = "")    private Long listCount;
@Excel(name = "", readConverterExp = "=")    @Schema(description = "", example = "")    private Long syncCount;
@Excel(name = "", readConverterExp = "=")    @Schema(description = "", example = "")    private Long DataSize;
@Excel(name = "")    @Schema(description = "", example = "")    private String description;
@Excel(name = "")    @Schema(description = "", example = "")    private Boolean validFlag;
@Excel(name = "")    @Schema(description = "", example = "")    private String createBy;
@Excel(name = "id")    @Schema(description = "id", example = "")    private Long creatorId;
@Excel(name = "", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")    @Schema(description = "", example = "")    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")    private Date createTime;
@Excel(name = "")    @Schema(description = "", example = "")    private String updateBy;
@Excel(name = "id")    @Schema(description = "id", example = "")    private Long updaterId;
@Excel(name = "", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")    @Schema(description = "", example = "")    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")    private Date updateTime;
@Excel(name = "")    @Schema(description = "", example = "")    private String remark;}
