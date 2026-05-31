package com.datamaster.module.assets.dal.dataobject.datasource;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;
import lombok.experimental.SuperBuilder;
import com.datamaster.common.core.domain.BaseEntity;

/**
 *  DO  DA_DATASOURCE_PROJECT_REL
 *
 * @author DATAMASTER
 * @date 2025-03-13
 */
@Data
@TableName(value = "AST_DATASOURCE_PROJECT_REL")
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class AssetsDatasourceProjectRelDO extends BaseEntity {
    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

/** id */
    private Long projectId;

    /**  */
    @TableField(exist = false)
    private String projectName;

    /**  */
    private String projectCode;

    /** id */
    private Long datasourceId;

    /**  */
    @TableField(exist = false)
    private String datasourceName;

    /**  */
    private String description;

    /**  */
    private Boolean validFlag;

    /**  */
    private Boolean dppAssigned;
}
