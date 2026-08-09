package com.datamaster.module.assets.controller.admin.datasource.vo;

import com.datamaster.common.annotation.Excel;
import com.datamaster.common.core.domain.entity.DatasourceSpaceRelDO;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 数据源 Response VO
 *
 * @author lhs
 * @date 2025-01-21
 */
@Schema(description = "数据源 Response VO")
@Data
public class AssetsDatasourceRespVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Excel(name = "ID")
    @Schema(description = "ID")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @Excel(name = "数据源名称")
    @Schema(description = "数据源名称", example = "核心交易库")
    private String datasourceName;

    @Excel(name = "数据源类型")
    @Schema(description = "数据源类型", example = "POSTGRESQL")
    private String datasourceType;

    @Excel(name = "数据源配置")
    @Schema(description = "数据源配置(JSON)", example = "{}")
    private String datasourceConfig;

    @Excel(name = "所属空间")
    @Schema(description = "所属空间关系列表")
    private List<DatasourceSpaceRelDO> spaceList;

    @Excel(name = "所属空间")
    @Schema(description = "所属空间名称", example = "风险管理部")
    private String spaceName;

    @Excel(name = "是否管理员添加")
    @Schema(description = "是否管理员添加", example = "true")
    private Boolean adminAddTo;

    @Excel(name = "IP")
    @Schema(description = "IP", example = "127.0.0.1")
    private String ip;

    @Excel(name = "端口")
    @Schema(description = "端口", example = "5432")
    private Long port;

    @Excel(name = "表数量", readConverterExp = "=")
    @Schema(description = "表数量", example = "120")
    private Long listCount;

    @Excel(name = "同步次数", readConverterExp = "=")
    @Schema(description = "同步次数", example = "3")
    private Long syncCount;

    @Excel(name = "数据量", readConverterExp = "=")
    @Schema(description = "数据量", example = "1000000")
    private Long dataSize;

    @Excel(name = "描述")
    @Schema(description = "描述", example = "银行核心业务数据源")
    private String description;

    @Excel(name = "是否有效")
    @Schema(description = "是否有效", example = "true")
    private Boolean validFlag;

    @Schema(description = "AI问数数据源ID", example = "")
    private Long dbgptDatasourceId;

    @Schema(description = "AI问数同步状态", example = "")
    private String dbgptSyncStatus;

    @Schema(description = "AI问数同步消息", example = "")
    private String dbgptSyncMessage;

    @Excel(name = "创建人")
    @Schema(description = "创建人", example = "admin")
    private String createBy;

    @Excel(name = "创建人ID")
    @Schema(description = "创建人ID", example = "1")
    private Long creatorId;

    @Excel(name = "创建时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间", example = "2026-07-28 10:00:00")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    @Excel(name = "更新人")
    @Schema(description = "更新人", example = "admin")
    private String updateBy;

    @Excel(name = "更新人ID")
    @Schema(description = "更新人ID", example = "1")
    private Long updaterId;

    @Excel(name = "更新时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "更新时间", example = "2026-07-28 10:00:00")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;

}
