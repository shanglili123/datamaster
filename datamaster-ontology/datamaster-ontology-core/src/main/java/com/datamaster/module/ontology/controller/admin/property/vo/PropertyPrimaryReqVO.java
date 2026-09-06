package com.datamaster.module.ontology.controller.admin.property.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

@Schema(description = "概念主属性设置 Request VO")
@Data
public class PropertyPrimaryReqVO {

    @NotNull(message = "概念ID不能为空")
    private Long conceptId;

    @NotEmpty(message = "请至少选择一个主属性")
    private List<Long> propertyIds;
}
