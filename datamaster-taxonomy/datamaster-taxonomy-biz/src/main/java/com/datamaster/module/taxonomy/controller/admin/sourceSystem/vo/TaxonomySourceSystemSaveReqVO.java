

package com.datamaster.module.taxonomy.controller.admin.sourceSystem.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import com.datamaster.common.core.domain.BaseEntity;

import javax.validation.constraints.Size;

/**
 * 来源系统 创建/修改 Request VO TAX_SOURCE_SYSTEM
 *
 * @author DATAMASTER
 * @date 2026-04-03
 */
@Schema(description = "来源系统 Response VO")
@Data
public class TaxonomySourceSystemSaveReqVO extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "系统名称", example = "")
    @Size(max = 256, message = "系统名称长度不能超过256个字符")
    private String name;

    @Schema(description = "系统类型", example = "")
    @Size(max = 256, message = "系统类型长度不能超过256个字符")
    private String type;

    @Schema(description = "排序", example = "")
    private Long sortOrder;

    @Schema(description = "描述", example = "")
    @Size(max = 256, message = "描述长度不能超过256个字符")
    private String description;

    @Schema(description = "是否有效;0：无效，1：有效", example = "")
    private Boolean validFlag;

    @Schema(description = "负责人", example = "")
    @Size(max = 256, message = "负责人长度不能超过256个字符")
    private String responsiblePerson;

    @Schema(description = "对接人", example = "")
    @Size(max = 256, message = "对接人长度不能超过256个字符")
    private String contactPerson;

    @Schema(description = "备注", example = "")
    @Size(max = 256, message = "备注长度不能超过256个字符")
    private String remark;


}
