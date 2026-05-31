

package com.datamaster.module.standards.controller.admin.dataCategoryCat.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import com.datamaster.common.core.page.PageParam;

/**
 * 数据分类-类目 Request VO 对象 STD_DATA_CATEGORY_CAT
 *
 * @author FXB
 * @date 2026-04-07
 */
@Schema(description = "数据分类-类目 Request VO")
@Data
public class StandardsDataCategoryCatPageReqVO extends PageParam {

    private static final long serialVersionUID = 1L;
        @Schema(description = "ID", example = "")
        private Long id;
    @Schema(description = "类别名称", example = "")
    private String name;

    @Schema(description = "关联上级ID", example = "")
    private Long parentId;

    @Schema(description = "类别排序", example = "")
    private Long sortOrder;

    @Schema(description = "层级编码", example = "")
    private String code;

    @Schema(description = "描述", example = "")
    private String description;




}
