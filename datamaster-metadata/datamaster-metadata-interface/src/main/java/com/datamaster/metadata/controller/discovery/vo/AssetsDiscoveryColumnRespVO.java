package com.datamaster.metadata.controller.discovery.vo;
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
@Excel(name = "任务 ID")    @Schema(description = "任务 ID", example = "")    private Long taskId;
@Schema(description = "数据源id", example = "")    private Long datasourceId;
@Excel(name = "id")    @Schema(description = "id", example = "")    private Long tableId;
@Excel(name = "/")    @Schema(description = "/", example = "")    private String columnName;
@Excel(name = "/")    @Schema(description = "/", example = "")    private String columnComment;
@Excel(name = "字段类型")    @Schema(description = "字段类型", example = "")    private String columnType;
@Excel(name = "字段长度")    @Schema(description = "字段长度", example = "")    private Long columnLength;
@Excel(name = "字段精度")    @Schema(description = "字段精度", example = "")    private Long columnScale;
@Excel(name = "")    @Schema(description = "", example = "")    private String nullableFlag;
@Excel(name = "")    @Schema(description = "", example = "")    private String pkFlag;
@Excel(name = "")    @Schema(description = "", example = "")    private String defaultValue;
@Excel(name = "是否有效")    @Schema(description = "是否有效", example = "")    private Boolean validFlag;
@Excel(name = "删除标志")    @Schema(description = "删除标志", example = "")    private Boolean delFlag;
@Excel(name = "创建人")    @Schema(description = "创建人", example = "")    private String createBy;
@Excel(name = "创建人 ID")    @Schema(description = "创建人 ID", example = "")    private Long creatorId;
@Excel(name = "创建时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")    @Schema(description = "创建时间", example = "")    private Date createTime;
@Excel(name = "更新人")    @Schema(description = "更新人", example = "")    private String updateBy;
@Excel(name = "更新人 ID")    @Schema(description = "更新人 ID", example = "")    private Long updaterId;
@Excel(name = "更新时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")    @Schema(description = "更新时间", example = "")    private Date updateTime;
@Excel(name = "描述")    @Schema(description = "描述", example = "")    private String description;}
