package com.datamaster.module.governance.dal.dataobject.space;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;
import lombok.experimental.SuperBuilder;
import com.datamaster.common.core.domain.BaseEntity;

/**
 * 空间 DO 对象 TAX_SPACE
 *
 * @author shu
 * @date 2025-01-20
 */
@Data
@TableName(value = "TAX_SPACE")
// 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
// @KeySequence("TAX_SPACE_seq")
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class TaxonomySpaceDO extends BaseEntity {
    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

    /** 空间名称 */
    private String name;

    /** 空间编码 */
    private String code;

    /** DS 专属工作组 ID */
    private Integer workerGroupId;

    /** DS 专属工作组 */
    private String workerGroup;

    /** 空间管理员 ID */
    private Long managerId;

    /** 空间管理员昵称 */
    @TableField(exist = false)
    private String nickName;

    /** 空间管理员手机号 */
    @TableField(exist = false)
    private String managerPhone;

    /** 空间描述 */
    private String description;

    /** 是否有效 */
    private Boolean validFlag;

    /** 删除标志 */
    @TableLogic
    private Boolean delFlag;


}
