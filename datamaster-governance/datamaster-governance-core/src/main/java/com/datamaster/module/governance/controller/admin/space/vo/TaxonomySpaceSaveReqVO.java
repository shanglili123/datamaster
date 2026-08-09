

package com.datamaster.module.governance.controller.admin.space.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import com.datamaster.common.core.domain.BaseEntity;

import javax.validation.constraints.Size;

/**
 * 空间创建/修改 Request VO TAX_SPACE
 *
 * @author shu
 * @date 2025-01-20
 */
@Schema(description = "空间创建/修改 Request VO")
@Data
public class TaxonomySpaceSaveReqVO extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "空间名称", example = "")
    @Size(max = 256, message = "空间名称长度不能超过256个字符")
    private String name;

    @Schema(description = "空间编码", example = "")
    private String code;

    @Schema(description = "空间管理员 ID", example = "")
    private Long managerId;

    @Schema(description = "历史空间管理员 ID", example = "")
    private Long managerHistoryId;

    @Schema(description = "有效状态", example = "")
    private Boolean validFlag;

    @Schema(description = "空间描述", example = "")
    @Size(max = 256, message = "空间描述长度不能超过256个字符")
    private String description;


}
