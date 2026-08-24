package com.datamaster.module.ontology.controller.admin.function.vo;

import com.datamaster.common.annotation.Excel;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.io.Serializable;
import java.util.Date;

@Schema(description = "函数 Response VO")
@Data
public class FunctionRespVO implements Serializable {
    private static final long serialVersionUID = 1L;
    @Schema(description = "编号") @Excel(name = "编号")
    private Long id;
    @Schema(description = "本体ID") @Excel(name = "本体ID")
    private Long ontologyId;
    @Schema(description = "函数名称") @Excel(name = "函数名称")
    private String name;
    @Schema(description = "函数编码") @Excel(name = "函数编码")
    private String code;
    @Schema(description = "语言") @Excel(name = "语言")
    private String lang;
    @Schema(description = "函数代码体")
    private String body;
    @Schema(description = "是否需要审批") @Excel(name = "需要审批")
    private Boolean needsApproval;
    @Schema(description = "描述") @Excel(name = "描述")
    private String description;
    @Schema(description = "创建者") @Excel(name = "创建者")
    private String createBy;
    @Schema(description = "创建时间") @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "创建时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;
}
