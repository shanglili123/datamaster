package com.datamaster.module.assets.controller.admin.datasource.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import com.datamaster.common.core.domain.BaseEntity;
import com.datamaster.common.core.domain.entity.DatasourceSpaceRelDO;

import javax.validation.constraints.Size;
import java.util.List;

/**
 * 数据源保存 Request VO
 *
 * @author lhs
 * @date 2025-01-21
 */
@Schema(description = "数据源保存 Request VO")
@Data
public class AssetsDatasourceSaveReqVO extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "数据源名称", example = "核心交易库")
    @Size(max = 256, message = "数据源名称不能超过256个字符")
    private String datasourceName;

    @Schema(description = "数据源类型", example = "POSTGRESQL")
    @Size(max = 256, message = "数据源类型不能超过256个字符")
    private String datasourceType;

    @Schema(description = "数据源配置(JSON)", example = "{}")
    @Size(max = 256, message = "数据源配置不能超过256个字符")
    private String datasourceConfig;

    @Schema(description = "原所属空间ID列表", example = "[1,2]")
    private List<Long> spaceListOld;

    @Schema(description = "所属空间关系列表")
    private List<DatasourceSpaceRelDO> spaceList;

    @Schema(description = "IP", example = "")
    @Size(max = 256, message = "IP不能超过256个字符")
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
    @Size(max = 256, message = "描述不能超过256个字符")
    private String description;

}
