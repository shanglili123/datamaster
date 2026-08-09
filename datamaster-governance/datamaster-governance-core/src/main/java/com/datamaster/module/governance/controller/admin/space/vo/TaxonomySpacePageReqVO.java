

package com.datamaster.module.governance.controller.admin.space.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import com.datamaster.common.core.page.PageParam;

/**
 * 空间分页查询 Request VO 对象 TAX_SPACE
 *
 * @author shu
 * @date 2025-01-20
 */
@Schema(description = "空间分页查询 Request VO")
@Data
public class TaxonomySpacePageReqVO extends PageParam {

    private static final long serialVersionUID = 1L;

    @Schema(description = "空间名称", example = "")
    private String name;

    @Schema(description = "空间管理员 ID", example = "")
    private Long managerId;

    @Schema(description = "空间 ID")
    private Long spaceId;

    @Schema(description = "空间编码")
    private String spaceCode;

}
