package com.datamaster.module.assets.controller.admin.discovery.vo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import com.datamaster.common.annotation.Excel;
import java.io.Serializable;
import java.util.Date;/** *  Response VO  DA_DISCOVERY_TABLE * * @author DATAMASTER * @date 2025-02-11 */
@Schema(description = " Response VO")
@Data
public class AssetsDiscoveryTableRespVO implements Serializable {
    private static final long serialVersionUID = 1L;
@Excel(name = "ID")    @Schema(description = "ID")    private Long id;
@Excel(name = "任务 ID")    @Schema(description = "任务 ID", example = "")    private Long taskId;
@Schema(description = "数据源id", example = "")    private Long datasourceId;
@Schema(description = "已创建资产id", example = "")    private Long assetId;
@Schema(description = "是否已创建资产", example = "")    private Boolean assetCreatedFlag;
@Excel(name = "表名称")    @Schema(description = "表名称", example = "")    private String tableName;
@Excel(name = "表注释")    @Schema(description = "表注释", example = "")    private String tableComment;
@Excel(name = "数据量")    @Schema(description = "数据量", example = "")    private Long dataCount;
@Excel(name = "字段数量")    @Schema(description = "字段数量", example = "")    private Long fieldCount;
@Excel(name = "变更标志")    @Schema(description = "变更标志", example = "")    private String changeFlag;
@Excel(name = "任务状态")    @Schema(description = "任务状态", example = "")    private String status;
@Excel(name = "忽略标志")    @Schema(description = "忽略标志", example = "")    private String ignoreFlag;
@Excel(name = "是否有效")    @Schema(description = "是否有效", example = "")    private Boolean validFlag;
@Excel(name = "删除标志")    @Schema(description = "删除标志", example = "")    private Boolean delFlag;
@Excel(name = "创建人")    @Schema(description = "创建人", example = "")    private String createBy;
@Excel(name = "创建人 ID")    @Schema(description = "创建人 ID", example = "")    private Long creatorId;
@Excel(name = "创建时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")    @Schema(description = "创建时间", example = "")    private Date createTime;
@Excel(name = "更新人")    @Schema(description = "更新人", example = "")    private String updateBy;
@Excel(name = "更新人 ID")    @Schema(description = "更新人 ID", example = "")    private Long updaterId;
@Excel(name = "更新时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")    @Schema(description = "更新时间", example = "")    private Date updateTime;
@Excel(name = "描述")    @Schema(description = "描述", example = "")    private String description;}
