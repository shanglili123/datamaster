

package com.datamaster.module.standards.api.dataLevel.dto;

import lombok.*;

/**
 * 数据分级 DTO 对象 STD_DATA_LEVEL
 *
 * @author DATAMASTER
 * @date 2026-04-03
 */
@Data
public class StandardsDataLevelRespDTO {

    private static final long serialVersionUID = 1L;

    /** ID */
    private Long id;

    /** 分级名称 */
    private String name;

    /** 分级缩写名 */
    private String shortname;

    /** 敏感等级 */
    private Long sensitiveLevel;

    /** 排序 */
    private Long sortOrder;

    /** 描述 */
    private String description;

    /** 是否有效;0：无效，1：有效 */
    private Boolean validFlag;

    /** 删除标志;1：已删除，0：未删除 */
    private Boolean delFlag;


}
