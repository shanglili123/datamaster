

package com.datamaster.module.assets.api.discovery.dto;

import lombok.Data;

/**
 * 数据发现库信息 DTO 对象 DA_DISCOVERY_TABLE
 *
 * @author DATAMASTER
 * @date 2025-02-11
 */
@Data
public class AssetsDiscoveryTableReqDTO {

    private static final long serialVersionUID = 1L;

    /** ID */
    private Long id;

    /** 任务id */
    private Long taskId;

    /** 表名称 */
    private String tableName;

    /** 表描述 */
    private String tableComment;

    /** 数据量 */
    private Long AssetstaCount;

    /** 字段量 */
    private Long fieldCount;

    /** 表结构标识 */
    private String changeFlag;

    /** 状态 */
    private String status;

    /** 是否忽略 */
    private String ignoreFlag;

    /** 是否有效 */
    private Boolean validFlag;

    /** 删除标志 */
    private Boolean delFlag;


}
