package com.datamaster.module.system.dal.dataobject.auth;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;
import com.datamaster.common.core.domain.BaseEntity;
import lombok.experimental.SuperBuilder;
/**
 * 用户与认证中心关系 DO 对象 rel_user_auth_product
 *
 * @author DATAMASTER
 * @date 2024-11-07
 */
@Data
@TableName(value = "rel_user_auth_product")
// 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
// @KeySequence("rel_user_auth_product_seq")
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class RelUserAuthProductDO extends BaseEntity {

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

    /** 用户ID */
    private Long userId;

    /** 统一身份认证id */
    private String authId;

    /** 认证平台类型 */
    private Integer authProductType;


}
