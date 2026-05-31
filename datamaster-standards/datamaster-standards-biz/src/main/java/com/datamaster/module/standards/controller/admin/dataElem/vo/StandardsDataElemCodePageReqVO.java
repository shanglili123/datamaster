package com.datamaster.module.standards.controller.admin.dataElem.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import com.datamaster.common.core.page.PageParam;

/**
 * 数据元代码 Request VO 对象 STD_DATA_ELEM_CODE
 *
 * @author DATAMASTER
 * @date 2025-01-21
 */
@Schema(description = "数据元代码 Request VO")
@Data
public class StandardsDataElemCodePageReqVO extends PageParam {

    private static final long serialVersionUID = 1L;
        @Schema(description = "ID", example = "")
        private Long id;
    @Schema(description = "数据元id", example = "")
    private String dataElemId;

    @Schema(description = "代码值", example = "")
    private String codeValue;

    @Schema(description = "代码名称", example = "")
    private String codeName;




}
