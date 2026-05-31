

package com.datamaster.module.collector.controller.admin.etl.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import com.datamaster.common.core.page.PageParam;

/**
 * 数据集成SQL模版 Request VO 对象 COL_ETL_SQL_TEMP
 *
 * @author FXB
 * @date 2025-06-25
 */
@Schema(description = "数据集成SQL模版 Request VO")
@Data
public class CollectorEtlSqlTempPageReqVO extends PageParam {

    private static final long serialVersionUID = 1L;
        @Schema(description = "ID", example = "")
        private Long id;
    @Schema(description = "名称", example = "")
    private String name;

    @Schema(description = "类型", example = "")
    private String type;

    @Schema(description = "内容", example = "")
    private String content;

    @Schema(description = "描述", example = "")
    private String description;




}
