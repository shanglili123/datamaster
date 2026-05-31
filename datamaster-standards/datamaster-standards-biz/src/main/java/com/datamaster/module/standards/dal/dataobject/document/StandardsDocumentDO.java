package com.datamaster.module.standards.dal.dataobject.document;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;
import lombok.experimental.SuperBuilder;
import com.datamaster.common.core.domain.BaseEntity;

import java.util.Date;

/**
 * 标准信息登记 DO 对象 STD_DOCUMENT
 *
 * @author DATAMASTER
 * @date 2025-08-21
 */
@Data
@TableName(value = "STD_DOCUMENT")
// 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
// @KeySequence("STD_DOCUMENT_seq")
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class StandardsDocumentDO extends BaseEntity {
    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

/** 编码 */
    private String code;

    /** 名称 */
    private String name;

    /** 类目code */
    private String catCode;

    @TableField(exist = false)
    private String catName;

    /** 文件标准类型字段， */
    private String type;

    /** 文件状态 */
    private String status;

    /** 发布机构名称，例如“中国国家标准化管理委员会” */
    private String issuingAgency;

    /** 版本号 */
    private String version;

    /** 发布日期 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date releaseDate;

    /** 实施日期 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date implementationDate;

    /** 废止日期 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date abolitionDate;

    /** 文件url */

    private String fileUrl;
    private String fileName;

    /** 是否有效 */
    private Boolean validFlag;

    /** 删除标志 */
    @TableLogic
    private Boolean delFlag;

    /** 描述 */
    private String description;


}
