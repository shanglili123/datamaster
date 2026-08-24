package com.datamaster.module.ontology.controller.admin.function.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.io.Serializable;
import java.util.Date;

@Schema(description = "函数执行记录 Response VO")
@Data
public class FunctionExecRespVO implements Serializable {
    private static final long serialVersionUID = 1L;
    @Schema(description = "编号") private Long id;
    @Schema(description = "函数ID") private Long functionId;
    @Schema(description = "输入参数") private String inputParams;
    @Schema(description = "输出结果") private String outputResult;
    @Schema(description = "状态") private String status;
    @Schema(description = "审批意见") private String approvalReason;
    @Schema(description = "错误信息") private String errorMessage;
    @Schema(description = "执行耗时(ms)") private Long durationMs;
    @Schema(description = "创建时间") @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") private Date createTime;
}
