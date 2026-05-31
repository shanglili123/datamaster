package com.datamaster.module.standards.dal.dataobject.codeMap;

import com.baomidou.mybatisplus.annotation.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import com.datamaster.common.core.domain.BaseEntity;

/**
 * 数据元代码映射 DO 对象 STD_CODE_MAP
 *
 * @author DATAMASTER
 * @date 2025-01-21
 */
@Data
@TableName(value = "STD_CODE_MAP")
// 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
// @KeySequence("STD_CODE_MAP_seq")
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class StandardsCodeMapDO extends BaseEntity {
    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

/** 数据元id */
    private String dataElemId;

    /** 原始值 */
    private String originalValue;

    /** 代码名 */
    private String codeName;

    /** 代码值 */
    private String codeValue;

    /** 是否有效 */
    private Boolean validFlag;

    /** 删除标志 */
    @TableLogic
    private Boolean delFlag;


}
