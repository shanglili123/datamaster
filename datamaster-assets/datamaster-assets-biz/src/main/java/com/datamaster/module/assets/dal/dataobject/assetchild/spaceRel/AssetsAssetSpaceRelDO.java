package com.datamaster.module.assets.dal.dataobject.assetchild.spaceRel;

import com.baomidou.mybatisplus.annotation.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import com.datamaster.common.core.domain.BaseEntity;

/**
 * 数据资产与空间关联关系 DO
 *
 * @author DATAMASTER
 * @date 2025-04-18
 */
@Data
@TableName(value = "AST_ASSET_SPACE_REL")
// 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
// @KeySequence("AST_ASSET_SPACE_REL_seq")
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class AssetsAssetSpaceRelDO extends BaseEntity {
    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

    /** 资产 ID */
    private Long assetId;

    /** 空间 ID */
    private Long spaceId;

    /** 空间编码 */
    private String spaceCode;

    /** 描述 */
    private String description;

    /** 是否有效 */
    private Boolean validFlag;

    /** 删除标志 */
    @TableLogic
    private Boolean delFlag;

}
