

package com.datamaster.module.taxonomy.controller.admin.tagAssetRel.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import com.datamaster.common.core.page.PageParam;

/**
 * 标签与资产关联关系 Request VO 对象 TAX_TAG_ASSET_REL
 *
 * @author DATAMASTER
 * @date 2025-07-11
 */
@Schema(description = "标签与资产关联关系 Request VO")
@Data
public class TaxonomyTagAssetRelPageReqVO extends PageParam {

    private static final long serialVersionUID = 1L;

    @Schema(description = "标签管理id", example = "")
    private String tagId;

    @Schema(description = "资产id", example = "")
    private String assetId;




}
