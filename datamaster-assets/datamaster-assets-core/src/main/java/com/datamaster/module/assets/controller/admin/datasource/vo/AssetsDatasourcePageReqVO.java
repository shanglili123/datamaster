package com.datamaster.module.assets.controller.admin.datasource.vo;

import com.baomidou.mybatisplus.annotation.TableField;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import com.datamaster.common.core.page.PageParam;

import java.util.List;

/**
 * 数据源分页查询 Request VO
 *
 * @author lhs
 * @date 2025-01-21
 */
@Schema(description = "数据源分页查询 Request VO")
@Data
public class AssetsDatasourcePageReqVO extends PageParam {

    private static final long serialVersionUID = 1L;
    @Schema(description = "ID", example = "1")
    private Long id;

    @Schema(description = "数据源名称", example = "核心交易库")
    private String datasourceName;

    @Schema(description = "数据源类型", example = "POSTGRESQL")
    private String datasourceType;

    @Schema(description = "数据源配置(JSON)", example = "{}")
    private String datasourceConfig;

    @Schema(description = "IP", example = "")
    private String ip;

    @Schema(description = "端口", example = "5432")
    private Long port;

    @Schema(description = "表数量", example = "120")
    private Long listCount;

    @Schema(description = "同步次数", example = "3")
    private Long syncCount;

    @Schema(description = "数据量", example = "1000000")
    private Long dataSize;

    @Schema(description = "描述", example = "银行核心业务数据源")
    private String description;

    @Schema(description = "空间编码", example = "bank_risk")
    private String spaceCode;

    @Schema(description = "ID列表", example = "[1,2]")
    private List<Long> idList;

    /**
     * SQL
     */
    @TableField(exist = false)
    private String sqlText;

}
