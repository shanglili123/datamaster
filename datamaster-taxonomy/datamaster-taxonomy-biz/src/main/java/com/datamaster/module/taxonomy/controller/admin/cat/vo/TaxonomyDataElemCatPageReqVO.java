

package com.datamaster.module.taxonomy.controller.admin.cat.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import com.datamaster.common.core.page.PageParam;

/**
 * 数据元类目管理 Request VO 对象 TAX_DATA_ELEM_CAT
 *
 * @author DATAMASTER
 * @date 2025-01-20
 */
@Schema(description = "数据元类目管理 Request VO")
@Data
public class TaxonomyDataElemCatPageReqVO extends PageParam {

    private static final long serialVersionUID = 1L;
        @Schema(description = "ID", example = "")
        private Long id;
    @Schema(description = "类别名称", example = "")
    private String name;

    @Schema(description = "关联上级ID", example = "")
    private Long parentId;

    @Schema(description = "类别排序", example = "")
    private Long sortOrder;

    @Schema(description = "描述", example = "")
    private String description;

    @Schema(description = "层级编码", example = "")
    private String code;

    private Boolean validFlag;




}
