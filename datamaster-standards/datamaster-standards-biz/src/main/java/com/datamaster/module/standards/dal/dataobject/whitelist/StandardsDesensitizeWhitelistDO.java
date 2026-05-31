package com.datamaster.module.standards.dal.dataobject.whitelist;

import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.SuperBuilder;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.datamaster.common.core.domain.BaseEntity;

/**
 * 脱敏白名单 DO 对象 STD_DESENSITIZE_WHITELIST
 *
 * @author DATAMASTER
 * @date 2026-04-09
 */
@Data
@TableName(value = "STD_DESENSITIZE_WHITELIST")
// 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
// @KeySequence("STD_DESENSITIZE_WHITELIST_seq")
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class StandardsDesensitizeWhitelistDO extends BaseEntity {
    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

/** 白名单名称 */
    private String name;

    /** 数据分类 */
    private Long dataCategoryId;
    /** 数据分类名称 */
    @TableField(exist = false)
    private String dataCategoryName;

    /** 用户集合 */
    @TableField(exist = false)
    private List<StandardsDesensitizeUserRelDO> userList;

    /** 生效分类;1：用户 2：角色 3：部门 */
    private String effectiveCategory;

    /** 开始时间 */
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private Date startTime;

    /** 结束时间 */
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private Date endTime;

    /** 排序 */
    private Long sortOrder;

    /** 描述 */
    private String description;

    /** 是否有效;0：无效，1：有效 */
    private Boolean validFlag;

    /** 删除标志;1：已删除，0：未删除 */
    @TableLogic
    private Boolean delFlag;


}
