package com.datamaster.module.ontology.dal.dataobject;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.datamaster.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** JSON contains only a business definition, not a persistence entity. No logical deletion. */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ont_metric")
public class MetricDO extends BaseEntity {
    private Long ontologyId;
    private Long conceptId;
    private String code;
    private String name;
    @TableField(updateStrategy = FieldStrategy.IGNORED)
    private Long conceptTableId;
    private String definitionJson;
    private String status;
    private Integer publishedVersion;
    private Boolean draftChanged;
    private Integer revision;
}
