

package com.datamaster.api.ds.api.base;

import com.alibaba.fastjson2.annotation.JSONField;
import lombok.Data;

/**
 * <P>
 * 用途:状态相关接口响应DTO
 * </p>
 *
 * @author: FXB
 * @create: 2025-02-18 14:20
 **/
@Data
public class DsStatusRespDTO extends DsResultDTO {
    /**
     * 是否成功
     */
    @JSONField(deserialize = false)
    private Boolean data;

    @JSONField(name = "data")
    public void setData(Object data) {
        if (data instanceof Boolean) {
            this.data = (Boolean) data;
        } else if (data != null) {
            this.data = Boolean.TRUE;
        } else {
            this.data = null;
        }
    }
}
