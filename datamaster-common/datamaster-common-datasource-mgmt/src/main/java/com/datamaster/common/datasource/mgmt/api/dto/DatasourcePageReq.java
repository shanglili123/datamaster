package com.datamaster.common.datasource.mgmt.api.dto;

import com.datamaster.common.core.page.PageParam;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class DatasourcePageReq extends PageParam {

    private static final long serialVersionUID = 1L;

    private String datasourceName;

    private String datasourceType;

    private String datasourceConfig;

    private String ip;

    private Long port;

    private Long listCount;

    private Long syncCount;

    private Long dataSize;

    private String description;

    private List<Long> idList;

}
