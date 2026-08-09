

package com.datamaster.api.ds.api.etl;

import com.alibaba.fastjson2.annotation.JSONField;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.datamaster.common.enums.ExecuteType;

/**
 * <P>
 * 用途:
 * </p>
 *
 * @author: FXB
 * @create: 2025-03-27 14:31
 **/
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
public class DSExecuteDTO {

    /**
     * 流程id
     */
    @JSONField(name = "workflowInstanceId", alternateNames = {"processInstanceId"})
    private Long processInstanceId;

    /**
     * 执行类型
     */
    private ExecuteType executeType;
}
