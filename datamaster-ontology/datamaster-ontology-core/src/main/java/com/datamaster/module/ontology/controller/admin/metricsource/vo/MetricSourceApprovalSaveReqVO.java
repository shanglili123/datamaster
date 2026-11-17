package com.datamaster.module.ontology.controller.admin.metricsource.vo;

import lombok.Data;
import javax.validation.constraints.*;

/** Only target IDs and optimistic preconditions; source identity and actor are server-owned. */
@Data
public class MetricSourceApprovalSaveReqVO {
    @NotNull @Positive private Long ontologyId;
    @NotNull @Positive private Long bindingId;
    @NotNull @Min(0) private Integer revision;
    @NotBlank @Pattern(regexp = "[a-f0-9]{64}") private String sourceToken;
    @NotBlank @Size(max = 500) private String reason;
    private Boolean readonlyConfirmed;
}
