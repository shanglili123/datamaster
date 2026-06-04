

package com.datamaster.module.standards.controller.admin.dataLevel.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import com.datamaster.common.core.page.PageParam;

/**
 * 数据分级 Request VO 对象 STD_DATA_LEVEL
 *
 * @author DATAMASTER
 * @date 2026-04-03
 */
@Schema(description = "数据分级 Request VO")
@Data
public class StandardsDataLevelPageReqVO extends PageParam {

    private static final long serialVersionUID = 1L;
        @Schema(description = "ID", example = "")
        private Long id;
    @Schema(description = "分级名称", example = "")
    private String name;

    @Schema(description = "分级缩写名", example = "")
    private String shortName;

    @Schema(description = "敏感等级", example = "")
    private Long sensitiveLevel;

    @Schema(description = "排序", example = "")
    private Long sortOrder;

    @Schema(description = "描述", example = "")
    private String description;

    @Schema(description = "是否有效;0：无效，1：有效", example = "")
    private Boolean validFlag;

    @Schema(description = "项目ID")
    private Long projectId;

    @Schema(description = "项目编码")
    private String projectCode;

}
