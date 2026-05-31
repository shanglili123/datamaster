package com.datamaster.module.assets.utils.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import com.datamaster.common.core.domain.BaseEntity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
public class TaskSaveReqInput extends BaseEntity {

    @Schema(description = "", example = "")
    private String name;
    private Long id;
    /** id */
    @Schema(description = "id", example = "")
    private Long nodeId;

    /**  */
    @Schema(description = "", example = "")
    private String nodeCode;

    /** id */
    @Schema(description = "id", example = "")
    private Long taskId;

    /**  */
    @Schema(description = "", example = "")
    private String taskCode;

    /**
     * {
     *   "prop": "id",
     *   "httpParametersType": "PARAMETER",
     *   "value": "111111"
     * }
     *
     * 	1.	PARAMETER URL
     * 	2.	BODY POST
     * 	3.	HEADER HTTP
     */
    private List<Map<String, Object>> httpParams;

    // 构造器
    public TaskSaveReqInput() {
        this.httpParams = new ArrayList<>(); // 初始化 httpParams
    }

    // 方法：动态添加 httpParams
    public void addHttpParam(String prop, String httpParametersType, Object value) {
        Map<String, Object> param = new HashMap<>();
        param.put("prop", prop);
        param.put("httpParametersType", httpParametersType);
        param.put("value", value);
        this.httpParams.add(param); // 将新参数添加到 httpParams 列表中
    }
}
