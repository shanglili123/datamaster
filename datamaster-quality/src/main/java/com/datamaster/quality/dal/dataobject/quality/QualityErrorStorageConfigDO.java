package com.datamaster.quality.dal.dataobject.quality;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableField;
import com.datamaster.common.core.domain.BaseEntity;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("QUALITY_ERROR_STORAGE_CONFIG")
public class QualityErrorStorageConfigDO extends BaseEntity {

    @TableField("datasource_id")
    private Long datasourceId;

    @TableField("table_name")
    private String tableName;

    @TableField("enabled")
    private String enabled;

    @TableField("remark")
    private String remark;
}
