package com.datamaster.module.modeling.dal.dataobject.businessCategory;

import com.baomidou.mybatisplus.annotation.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import com.datamaster.common.core.domain.BaseEntity;

import java.util.List;

/**
 * 业务分类 DO 对象 MDL_BUSINESS_CATEGORY
 *
 * @author DATAMASTER
 * @date 2026-04-08
 */
@Data
@TableName(value = "MDL_BUSINESS_CATEGORY")
// 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
// @KeySequence("MDL_BUSINESS_CATEGORY_seq")
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ModelingBusinessCategoryDO extends BaseEntity {
    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

/**
     * 层级编码
     */
    private String code;

    /** 业务分类名称 */
    private String name;

    /** 关联上级ID */
    private Long parentId;

    /** 关联上级名称 */
    @TableField(exist = false)
    private String parentName;

    /** 排序 */
    private Long sortOrder;

    /** 描述 */
    private String description;

    /** 英文缩写名 */
    private String engName;

    /** 负责人手机号 */
    private String ownerPhone;

    /** 负责人ID */
    private Long ownerId;

    /** 负责人姓名 */
    @TableField(exist = false)
    private String ownerName;

    /** 数据域ID */
    private Long domainId;
    /** 数据域集合 */
    @TableField(exist = false)
    private List<ModelingBusinessDomainRelDO> domainList;

    @TableField(exist = false)
    private List<String> domainIds;

//    @TableField(exist = false)
//    private List<ModelingDataDomainDO> dataDomainList;

    /** 项目ID */
    private Long projectId;

    /** 项目编码 */
    private String projectCode;

    /** 是否有效;0：无效，1：有效 */
    private Boolean validFlag;

    /** 删除标志;1：已删除，0：未删除 */
    @TableLogic
    private Boolean delFlag;


}
