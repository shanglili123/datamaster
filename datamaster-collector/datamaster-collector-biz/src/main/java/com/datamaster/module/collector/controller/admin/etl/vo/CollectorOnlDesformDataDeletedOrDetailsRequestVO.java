

package com.datamaster.module.collector.controller.admin.etl.vo;

import com.alibaba.fastjson.JSONArray;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

/**
 * <P>
 * 用途:
 * </p>
 *
 * @author: FXB
 * @create: 2024-05-24 16:01
 **/
@Data
public class CollectorOnlDesformDataDeletedOrDetailsRequestVO extends CollectorOnlDesformDataBaseRequestVO implements Serializable {

//    @ApiModelProperty(value = "数据ID")
    private String dataId;

    @Builder(toBuilder = true)
    public CollectorOnlDesformDataDeletedOrDetailsRequestVO(Integer datasourceId, String databaseName, String tableNames, JSONArray fieldName, String uniFieldName, String dataId) {
        super(datasourceId, databaseName, tableNames, fieldName, uniFieldName);
        this.dataId = dataId;
    }
}
