package com.datamaster.common.core.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.datamaster.common.core.domain.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.List;
import java.util.ArrayList;

@Schema(description = "数据源")
@Data
@TableName(value = "AST_DATASOURCE")
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class DatasourceDO extends BaseEntity {

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

    @Schema(description = "数据源名称")
    private String datasourceName;

    @Schema(description = "数据源类型")
    private String datasourceType;

    @Schema(description = "数据源配置(JSON)")
    private String datasourceConfig;

    @Schema(description = "IP地址")
    private String ip;

    @Schema(description = "端口")
    private Long port;

    @Schema(description = "表数量")
    private Long listCount;

    @Schema(description = "同步次数")
    private Long syncCount;

    @Schema(description = "数据量")
    private Long dataSize;

    @Schema(description = "描述")
    private String description;

    @Schema(description = "是否有效")
    private Boolean validFlag;

    @TableLogic
    private Boolean delFlag;

    @Schema(description = "DolphinScheduler数据源ID")
    private Long dsDatasourceId;

    @Schema(description = "AI问数数据源ID")
    private Long dbgptDatasourceId;

    @Schema(description = "AI问数同步状态")
    private String dbgptSyncStatus;

    @Schema(description = "AI问数同步消息")
    private String dbgptSyncMessage;

    @TableField(exist = false)
    @Schema(description = "空间关系列表")
    private List<DatasourceSpaceRelDO> spaceList;

    @TableField(exist = false)
    @Schema(description = "空间名称")
    private String spaceName;

    @TableField(exist = false)
    @Schema(description = "是否允许管理员添加")
    private Boolean adminAddTo;
}
