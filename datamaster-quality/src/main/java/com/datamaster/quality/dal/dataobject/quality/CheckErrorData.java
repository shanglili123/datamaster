

package com.datamaster.quality.dal.dataobject.quality;

import com.alibaba.fastjson2.JSONObject;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
public class CheckErrorData implements Serializable {
    private String id;

    /**
     * 报告id
     */
    private String reportId;


    /**
     * 错误数据数量
     */
    private Integer count;


    /**
     * 错误数据数量
     */
    private Integer errorCount;


    /**
     * 错误数据json列表
     */
    private String dataJsonStr;

    /**
     * 核查时间
     */
    private Date time;

    /**
     * 错误数据json列表
     */
    private JSONObject jsonData;

    /**
     * 错误数据json列表
     */
    private JSONObject jsonDataOld;
    /**
     * 错误数据json列表
     */
    private String dataJsonStrOLd;

    /**
     * 是否已修复 0:否 1:是 2:忽略
     */
    private Integer repair;


    /**
     * 备注
     */
    private String remark;

}
