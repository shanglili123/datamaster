package com.datamaster.module.ontology.controller.admin.metric.vo;

import lombok.Data;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.util.ArrayList;
import java.util.List;

@Data
public class MetricSaveReqVO {
    private Long id;
    @NotNull private Long ontologyId;
    @NotNull private Long conceptId;
    @NotBlank @Size(max = 100) @Pattern(regexp = "[a-z][a-z0-9_]*") private String code;
    @NotBlank @Size(max = 200) private String name;
    @NotBlank private String description;
    private String owner;
    private String unit;
    private String grain;
    /** Explicit fixed offsets, e.g. +00:00 / +08:00. Both omitted preserves legacy source-local time. */
    private String sourceTimeOffset;
    private String businessTimeOffset;
    private String aggregation;
    private Long conceptTableId;
    private Long valuePropertyId;
    /** Inline, same-source aggregate derivation; no arbitrary formula or live metric dependency. */
    private String derivedOperation;
    private String secondaryAggregation;
    private Long secondaryValuePropertyId;
    private String primaryUnit;
    private String secondaryUnit;
    private Long timePropertyId;
    private List<Long> dimensionPropertyIds = new ArrayList<>();
    @Valid private List<MetricFilterVO> fixedFilters = new ArrayList<>();
    private Integer revision;
}
