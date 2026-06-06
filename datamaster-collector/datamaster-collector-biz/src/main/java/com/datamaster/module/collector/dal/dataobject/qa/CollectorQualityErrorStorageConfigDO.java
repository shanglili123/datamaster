package com.datamaster.module.collector.dal.dataobject.qa;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.datamaster.common.core.domain.BaseEntity;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("QUALITY_ERROR_STORAGE_CONFIG")
public class CollectorQualityErrorStorageConfigDO extends BaseEntity {

    @TableField("datasource_id")
    private Long datasourceId;

    @TableField("table_name")
    private String tableName;

    @TableField("enabled")
    private String enabled;

    @TableField("remark")
    private String remark;
}
