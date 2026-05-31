

package com.datamaster.module.standards.api.dataCategory.dto;

import lombok.*;

/**
 * 数据分类 DTO 对象 STD_DATA_CATEGORY
 *
 * @author DATAMASTER
 * @date 2026-04-07
 */
@Data
public class StandardsDataCategoryRespDTO {

    private static final long serialVersionUID = 1L;

    /** ID */
    private Long id;

    /** 类目id */
    private Long catId;

    /** 类目编码 */
    private String catCode;

    /** 分类名称 */
    private String name;

    /** 分类名称缩写名 */
    private String shortName;

    /** 数据分级 */
    private Long dataLevelId;

    /** 任务优先级;HIGHEST,HIGH,MEDIUM,LOW,LOWEST */
    private String priority;

    /** 描述 */
    private String description;

    /** 是否有效;0：无效，1：有效 */
    private Boolean validFlag;

    /** 删除标志;1：已删除，0：未删除 */
    private Boolean delFlag;


}
