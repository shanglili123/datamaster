

package com.datamaster.module.modeling.controller.admin.dm.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import com.datamaster.common.core.page.PageParam;

/**
 * 数据域管理 Request VO 对象 Modeling_DATA_DOMAIN
 *
 * @author FXB
 * @date 2026-03-24
 */
@Schema(description = "数据域管理 Request VO")
@Data
public class ModelingDataDomainPageReqVO extends PageParam {

    private static final long serialVersionUID = 1L;
        @Schema(description = "ID", example = "")
        private Long id;
    @Schema(description = "名称", example = "")
    private String name;

    @Schema(description = "英文缩写", example = "")
    private String engName;

    @Schema(description = "负责人ID", example = "")
    private Long ownerUserId;

    @Schema(description = "描述", example = "")
    private String description;


    @Schema(description = "业务域ID", example = "")
    private Long businessDomainId;


    @Schema(description = "业务分类ID", example = "")
    private Long businessCategoryId;


}
