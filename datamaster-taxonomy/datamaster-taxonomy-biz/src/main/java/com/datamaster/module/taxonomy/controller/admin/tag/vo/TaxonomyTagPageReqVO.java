

package com.datamaster.module.taxonomy.controller.admin.tag.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import com.datamaster.common.core.page.PageParam;

/**
 * 标签管理 Request VO 对象 TAX_TAG
 *
 * @author DATAMASTER
 * @date 2025-07-11
 */
@Schema(description = "标签管理 Request VO")
@Data
public class TaxonomyTagPageReqVO extends PageParam {

    private static final long serialVersionUID = 1L;
        @Schema(description = "ID", example = "")
        private Long id;
        private Long [] ids;

    @Schema(description = "名称", example = "")
    private String name;

    @Schema(description = "描述", example = "")
    private String description;

    @Schema(description = "类目编码", example = "")
    private String catCode;

   @Schema(description = "类目名称", example = "")
    private String catName;

    @Schema(description = "资产数量", example = "")
    private Long aeestCount;
    private Long aeestId;

    @Schema(description = "状态", example = "")
    private String status;

    @Schema(description = "扩展信息别名", example = "")
    private String alias;

    @Schema(description = "近义词", example = "")
    private String nearSynonyms;

    @Schema(description = "同义词", example = "")
    private String synonyms;

    @Schema(description = "项目ID")
    private Long projectId;

    @Schema(description = "项目编码")
    private String projectCode;

}
