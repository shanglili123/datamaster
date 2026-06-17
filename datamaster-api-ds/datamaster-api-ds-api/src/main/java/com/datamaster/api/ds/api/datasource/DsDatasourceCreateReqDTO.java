
package com.datamaster.api.ds.api.datasource;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DsDatasourceCreateReqDTO {

    private String name;

    private String type;

    private String host;

    private Integer port;

    private String userName;

    private String password;

    private String database;

    /**
     * DS 2.0+ 要求指定执行节点，传 "default" 使用默认
     */
    private String node;

    /**
     * 连接方式：PUBLIC（默认）或 PRIVATE
     */
    private String connectType;

    private Map<String, Object> other;

}
