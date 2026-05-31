package com.datamaster.module.modeling.dal.dataobject.dm;

import lombok.*;
import lombok.experimental.SuperBuilder;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.datamaster.common.core.domain.BaseEntity;

/**
 * 数仓分层-规范管理 DO 对象 MDL_DATA_LAYER_SPECIFICATION
 *
 * @author FXB
 * @date 2026-03-24
 */
@Data
@TableName(value = "MDL_DATA_LAYER_SPECIFICATION")
// 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
// @KeySequence("MDL_DATA_LAYER_SPECIFICATION_seq")
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ModelingDataLayerSpecificationDO extends BaseEntity {
    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

/** 数仓分层ID */
    private Long dataLayerId;

    /** 表前缀 */
    private String prefixName;

    /** 业务大类英文缩写 */
    private String businessEngName;

    /** 负责人ID */
    private Long ownerUserId;

    /** 状态 */
    private String status;

    /** 描述 */
    private String description;

    /** 是否有效 */
    private Boolean validFlag;

    /** 删除标志 */
    @TableLogic
    private Boolean delFlag;


    /**
     * 负责人名称
     */
    @TableField(exist = false)
    private String ownerUserName;
    /**
     * 负责人联系方式
     */
    @TableField(exist = false)
    private String ownerUserPhoneNumber;
}
