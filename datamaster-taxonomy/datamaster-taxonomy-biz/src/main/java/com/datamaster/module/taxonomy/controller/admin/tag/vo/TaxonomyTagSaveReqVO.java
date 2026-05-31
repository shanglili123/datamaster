

package com.datamaster.module.taxonomy.controller.admin.tag.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import com.datamaster.common.core.domain.BaseEntity;

import javax.validation.constraints.Size;

/**
 * 标签管理 创建/修改 Request VO TAX_TAG
 *
 * @author DATAMASTER
 * @date 2025-07-11
 */
@Schema(description = "标签管理 Response VO")
@Data
public class TaxonomyTagSaveReqVO extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @Schema(description = "id")
    private Long id;

    @Schema(description = "名称", example = "")
    @Size(max = 256, message = "名称长度不能超过256个字符")
    private String name;

    @Schema(description = "描述", example = "")
    @Size(max = 256, message = "描述长度不能超过256个字符")
    private String description;

    @Schema(description = "类目编码", example = "")
    @Size(max = 256, message = "类目编码长度不能超过256个字符")
    private String catCode;

    @Schema(description = "类目名称", example = "")
    @Size(max = 256, message = "类目编码长度不能超过256个字符")
    private String catName;

    @Schema(description = "资产数量", example = "")
    private Long aeestCount;

    @Schema(description = "状态", example = "")
    @Size(max = 256, message = "状态长度不能超过256个字符")
    private String status;

    @Schema(description = "扩展信息别名", example = "")
    @Size(max = 256, message = "扩展信息别名长度不能超过256个字符")
    private String alias;

    @Schema(description = "近义词", example = "")
    @Size(max = 256, message = "近义词长度不能超过256个字符")
    private String nearSynonyms;

    @Schema(description = "同义词", example = "")
    @Size(max = 256, message = "同义词长度不能超过256个字符")
    private String synonyms;

    @Schema(description = "备注", example = "")
    @Size(max = 256, message = "备注长度不能超过256个字符")
    private String remark;


}
