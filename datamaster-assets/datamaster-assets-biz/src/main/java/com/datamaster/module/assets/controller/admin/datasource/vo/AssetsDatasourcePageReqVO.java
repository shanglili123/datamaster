package com.datamaster.module.assets.controller.admin.datasource.vo;

import com.baomidou.mybatisplus.annotation.TableField;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import com.datamaster.common.core.page.PageParam;

import java.util.List;

/**
 *  Request VO  DA_DATASOURCE
 *
 * @author lhs
 * @date 2025-01-21
 */
@Schema(description = " Request VO")
@Data
public class AssetsDatasourcePageReqVO extends PageParam {

    private static final long serialVersionUID = 1L;
        @Schema(description = "ID", example = "")
        private Long id;
    @Schema(description = "", example = "")
    private String datasourceName;

    @Schema(description = "", example = "")
    private String datasourceType;

    @Schema(description = "(json)", example = "")
    private String datasourceConfig;

    @Schema(description = "IP", example = "")
    private String ip;

    @Schema(description = "", example = "")
    private Long port;

    @Schema(description = "", example = "")
    private Long listCount;

    @Schema(description = "", example = "")
    private Long syncCount;

    @Schema(description = "", example = "")
    private Long DataSize;

    @Schema(description = "", example = "")
    private String description;

    @Schema(description = "", example = "")
    private String projectCode;

    @Schema(description = "id", example = "")
    private List<Long> idList;

    /**
     * SQL
     */
    @TableField(exist = false)
    private String sqlText;

}
