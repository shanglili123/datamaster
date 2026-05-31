

package com.datamaster.module.taxonomy.controller.admin.tagAssetRel.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import com.datamaster.common.core.domain.BaseEntity;

import javax.validation.constraints.Size;
import java.util.List;

/**
 * 标签与资产关联关系 创建/修改 Request VO TAX_TAG_ASSET_REL
 *
 * @author DATAMASTER
 * @date 2025-07-11
 */
@Schema(description = "标签与资产关联关系 Response VO")
@Data
public class TaxonomyTagAssetRelSaveReqVO extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "标签管理id", example = "")
    @Size(max = 256, message = "标签管理id长度不能超过256个字符")
    private String tagId;
    private List<String> tagIds;

    @Schema(description = "资产id", example = "")
    @Size(max = 256, message = "资产id长度不能超过256个字符")
    private String assetId;


    @Schema(description = "备注", example = "")
    @Size(max = 256, message = "备注长度不能超过256个字符")
    private String remark;


}
