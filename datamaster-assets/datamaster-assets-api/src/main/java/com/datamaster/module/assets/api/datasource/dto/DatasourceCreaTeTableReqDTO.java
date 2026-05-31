

package com.datamaster.module.assets.api.datasource.dto;

import lombok.Data;
import com.datamaster.common.database.core.DbColumn;

import java.util.List;

@Data
public class DatasourceCreaTeTableReqDTO {

    /** 数据源类型 */
    private String datasourceType;

    /** 数据源配置(json字符串) */
    private String datasourceConfig;

    /** IP */
    private String ip;

    /** 端口号 */
    private Long port;

    /**
     * 库名
     */
    private String dbname;

    /**
     * 表名
     */
    private String tableName;

    /**
     * 表注释
     */
    private String tableComment;

    /**
     * 表字段
     */
    private List<DbColumn> columnsList;
}
