package com.datamaster.module.standards.dal.dataobject.dataElem;

import com.baomidou.mybatisplus.annotation.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import com.datamaster.common.core.domain.BaseEntity;

/**
 * 数据元代码 DO 对象 STD_DATA_ELEM_CODE
 *
 * @author DATAMASTER
 * @date 2025-01-21
 */
@Data
@TableName(value = "STD_DATA_ELEM_CODE")
// 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
// @KeySequence("STD_DATA_ELEM_CODE_seq")
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class StandardsDataElemCodeDO extends BaseEntity {
    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

/** 数据元id */
    private String dataElemId;

    /** 代码值 */
    private String codeValue;

    /** 代码名称 */
    private String codeName;

    /** 是否有效 */
    private Boolean validFlag;

    /** 删除标志 */
    @TableLogic
    private Boolean delFlag;


}
