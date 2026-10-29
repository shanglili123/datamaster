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
    private String aggregation;
    private Long conceptTableId;
    private Long valuePropertyId;
    private Long timePropertyId;
    private List<Long> dimensionPropertyIds = new ArrayList<>();
    @Valid private List<MetricFilterVO> fixedFilters = new ArrayList<>();
    private Integer revision;
}
