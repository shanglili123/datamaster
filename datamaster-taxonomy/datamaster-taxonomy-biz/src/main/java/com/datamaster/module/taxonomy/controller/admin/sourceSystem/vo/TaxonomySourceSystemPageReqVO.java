

package com.datamaster.module.taxonomy.controller.admin.sourceSystem.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import com.datamaster.common.core.page.PageParam;

/**
 * 来源系统 Request VO 对象 TAX_SOURCE_SYSTEM
 *
 * @author DATAMASTER
 * @date 2026-04-03
 */
@Schema(description = "来源系统 Request VO")
@Data
public class TaxonomySourceSystemPageReqVO extends PageParam {

    private static final long serialVersionUID = 1L;
        @Schema(description = "ID", example = "")
        private Long id;
    @Schema(description = "系统名称", example = "")
    private String name;

    @Schema(description = "系统类型", example = "")
    private String type;

    @Schema(description = "排序", example = "")
    private Long sortOrder;

    @Schema(description = "描述", example = "")
    private String description;

    @Schema(description = "是否有效;0：无效，1：有效", example = "")
    private Boolean validFlag;

    @Schema(description = "负责人", example = "")
    private String responsiblePerson;

    @Schema(description = "对接人", example = "")
    private String contactPerson;



}
