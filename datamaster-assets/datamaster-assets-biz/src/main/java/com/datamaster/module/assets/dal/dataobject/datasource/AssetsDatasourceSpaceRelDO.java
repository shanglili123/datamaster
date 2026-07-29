package com.datamaster.module.assets.dal.dataobject.datasource;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;
import lombok.experimental.SuperBuilder;
import com.datamaster.common.core.domain.BaseEntity;

/**
 * 数据源与空间关系 DO
 *
 * @author DATAMASTER
 * @date 2025-03-13
 */
@Data
@TableName(value = "AST_DATASOURCE_SPACE_REL")
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class AssetsDatasourceSpaceRelDO extends BaseEntity {
    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

    /** 空间ID */
    private Long spaceId;

    /** 空间名称 */
    @TableField(exist = false)
    private String spaceName;

    /** 空间编码 */
    private String spaceCode;

    /** 数据源ID */
    private Long datasourceId;

    /** 数据源名称 */
    @TableField(exist = false)
    private String datasourceName;

    /** 描述 */
    private String description;

    /** 是否有效 */
    private Boolean validFlag;

    /** 是否已分配到数据加工 */
    private Boolean dppAssigned;
}
