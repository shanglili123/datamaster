

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
public class CollectorOnlDesformDataListRequestVO extends CollectorOnlDesformDataBaseRequestVO implements Serializable {

    @Builder(toBuilder = true)
    public CollectorOnlDesformDataListRequestVO(Integer datasourceId, String databaseName, String tableNames, JSONArray fieldName, String uniFieldName) {
        super(datasourceId, databaseName, tableNames, fieldName, uniFieldName);
    }
}
