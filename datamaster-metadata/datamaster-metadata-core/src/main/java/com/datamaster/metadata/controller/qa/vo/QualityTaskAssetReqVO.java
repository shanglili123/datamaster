

package com.datamaster.metadata.controller.qa.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import com.datamaster.common.core.page.PageParam;

@Data
public class QualityTaskAssetReqVO  extends PageParam {

    private static final long serialVersionUID = 1L;
    @Schema(description = "ID", example = "")
    private Long id;

    @Schema(description = "资产id")
    private Long assetId;
}
