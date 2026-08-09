

package com.datamaster.module.assets.api.discovery.dto;

import lombok.Data;

/**
 * 数据发现字段 DTO 对象 DA_DISCOVERY_COLUMN
 *
 * @author DATAMASTER
 * @date 2025-02-11
 */
@Data
public class AssetsDiscoveryColumnRespDTO {

    private static final long serialVersionUID = 1L;

    /** ID */
    private Long id;

    /** 任务id */
    private Long taskId;

    /** 数据发现库表id */
    private Long tableId;

    /** 字段名称/英文名称 */
    private String columnName;

    /** 字段注释/中文名称 */
    private String columnComment;

    /** 数据类型 */
    private String columnType;

    /** 长度 */
    private Long columnLength;

    /** 小数位 */
    private Long columnScale;

    /** 是否必填 */
    private String nullableFlag;

    /** 是否主键 */
    private String pkFlag;

    /** 默认值 */
    private String defaultValue;

    /** 是否有效 */
    private Boolean validFlag;

    /** 删除标志 */
    private Boolean delFlag;


}
