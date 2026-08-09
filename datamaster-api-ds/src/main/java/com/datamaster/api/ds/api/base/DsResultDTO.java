

package com.datamaster.api.ds.api.base;

import lombok.Data;

/**
 * <P>
 * 用途:DS结果VO
 * </p>
 *
 * @author: FXB
 * @create: 2025-02-18 15:58
 **/
@Data
public class DsResultDTO {

    /**
     * 状态码
     */
    private Integer code;

    /**
     * 信息
     */
    private String msg;

    /**
     * 是否失败
     */
    private Boolean failed;

    /**
     * 是否成功
     */
    private Boolean success;

    /**
     * DS 响应数据（可能包含 id 等字段）
     */
    private Object data;

    /**
     * 判断是否调用成功，兼容 DS 返回 success 字段缺失的情况
     */
    public boolean isOk() {
        return Boolean.TRUE.equals(success) || (code != null && code == 0);
    }
}
