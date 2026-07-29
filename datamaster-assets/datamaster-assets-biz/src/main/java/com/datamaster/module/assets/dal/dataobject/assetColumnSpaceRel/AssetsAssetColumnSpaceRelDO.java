package com.datamaster.module.assets.dal.dataobject.assetColumnSpaceRel;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.datamaster.common.core.domain.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * 数据资产字段与空间关联关系 DO
 *
 * @author DATAMASTER
 */
@Data
@TableName(value = "AST_ASSET_COLUMN_SPACE_REL")
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class AssetsAssetColumnSpaceRelDO extends BaseEntity {
    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

    /** 资产id */
    private Long assetId;

    /** 字段id */
    private Long columnId;

    /** 空间id */
    private Long spaceId;

    /** 空间编码 */
    private String spaceCode;

    /** 描述 */
    private String description;

    /** 是否有效 */
    private Boolean validFlag;

    /** 删除标识 */
    @TableLogic
    private Boolean delFlag;
}
