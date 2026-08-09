package com.datamaster.common.datasource.mgmt.api.dto;

import com.datamaster.common.database.core.DbColumn;
import lombok.Data;

import java.util.List;

@Data
public class DatasourceCreaTeTableReqDTO {

    private String datasourceType;

    private String datasourceConfig;

    private String ip;

    private Long port;

    private String dbname;

    private String tableName;

    private String tableComment;

    private List<DbColumn> columnsList;
}
