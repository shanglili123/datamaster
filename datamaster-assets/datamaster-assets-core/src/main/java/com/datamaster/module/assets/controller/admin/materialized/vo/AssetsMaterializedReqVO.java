package com.datamaster.module.assets.controller.admin.materialized.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import com.datamaster.common.core.domain.BaseEntity;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.List;

/**
 * 资产物化方法 Request VO
 *
 * @author DATAMASTER
 * @date 2026-08-01
 */
@Schema(description = "资产物化方法 Request VO")
@Data
public class AssetsMaterializedReqVO extends BaseEntity {

    @Schema(description = "模型ID列表", example = "[1, 2, 3]", required = true)
    @NotNull(message = "模型ID列表不能为空")
    private List<Long> modelId;

    @Schema(description = "数据源ID", example = "1", required = true)
    @NotNull(message = "数据源ID不能为空")
    private Long datasourceId;

    @Schema(description = "数据源名称", example = "DataSource1")
    @Size(max = 256, message = "数据源名称长度不能超过256个字符")
    private String datasourceName;

    /**
     * 库名（预留，当前建表未使用）
     */
    private String dbName;

    @Schema(description = "数据源类型", example = "MySQL")
    @Size(max = 256, message = "数据源类型长度不能超过256个字符")
    private String datasourceType;

    @Schema(description = "数据源配置(json字符串)", example = "{\"username\":\"root\",\"password\":\"123456\"}")
    private String datasourceConfig;

    @Schema(description = "IP地址", example = "192.168.1.1")
    @Size(max = 15, message = "IP地址长度不能超过15个字符")
    private String ip;

    @Schema(description = "端口号", example = "3306")
    private Long port;

}
