

package com.datamaster.module.collector.api.etl.dto;

import lombok.Data;

import java.util.Date;

/**
 * 数据质量日志 DTO 对象 COL_QUALITY_LOG
 *
 * @author DATAMASTER
 * @date 2025-07-19
 */
@Data
public class CollectorQualityLogReqDTO {

    private static final long serialVersionUID = 1L;

    /** ID */
    private Long id;

    /** 名称 */
    private String name;

    /** 状态 */
    private String successFlag;

    /** 开始时间 */
    private Date startTime;

    /** 结束时间 */
    private Date endTime;

    /** 任务id */
    private String qualityId;

    /** 评分 */
    private Long score;

    /** 问题数据 */
    private Long problemData;

    /** 是否有效 */
    private Boolean validFlag;

    /** 删除标志 */
    private Boolean delFlag;


}
