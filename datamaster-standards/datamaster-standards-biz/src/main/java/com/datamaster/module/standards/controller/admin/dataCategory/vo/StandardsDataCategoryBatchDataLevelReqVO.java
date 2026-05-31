

package com.datamaster.module.standards.controller.admin.dataCategory.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import com.datamaster.common.core.domain.BaseEntity;

import javax.validation.constraints.Size;
import java.util.List;

/**
 * 数据分类 批量設置数据分級 Request VO STD_DATA_CATEGORY
 *
 * @author DATAMASTER
 * @date 2026-04-07
 */
@Schema(description = "批量設置数据分級 Response VO")
@Data
public class StandardsDataCategoryBatchDataLevelReqVO extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @Schema(description = "ids")
    private List<Long> ids;


    @Schema(description = "数据分级", example = "")
    private Long dataLevelId;
}
