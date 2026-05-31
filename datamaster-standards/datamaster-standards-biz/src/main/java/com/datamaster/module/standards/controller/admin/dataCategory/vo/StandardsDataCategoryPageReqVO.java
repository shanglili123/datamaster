

package com.datamaster.module.standards.controller.admin.dataCategory.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import com.datamaster.common.core.page.PageParam;

/**
 * 数据分类 Request VO 对象 STD_DATA_CATEGORY
 *
 * @author DATAMASTER
 * @date 2026-04-07
 */
@Schema(description = "数据分类 Request VO")
@Data
public class StandardsDataCategoryPageReqVO extends PageParam {

    private static final long serialVersionUID = 1L;
    @Schema(description = "ID", example = "")
    private Long id;
    @Schema(description = "类目id", example = "")
    private Long catId;

    @Schema(description = "类目编码", example = "")
    private String catCode;

    @Schema(description = "分类名称", example = "")
    private String name;

    /**
     * 分类名称缩写名
     */
    @Schema(description = "分类名称缩写名", example = "")
    private String shortName;

    @Schema(description = "数据分级", example = "")
    private Long dataLevelId;

    @Schema(description = "任务优先级;HIGHEST,HIGH,MEDIUM,LOW,LOWEST", example = "")
    private String priority;

    @Schema(description = "描述", example = "")
    private String description;

    /**
     * 是否有效
     */
    private Boolean validFlag;

    @Schema(description = "脱敏配置（0:否 1:是）", example = "")
    private String desensitizationRulesFlag;

    @Schema(description = "脱敏规则id")
    private Long desensitizationRulesId;

}
