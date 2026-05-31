package com.datamaster.module.service.dal.dataobject.apiLog;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;
import lombok.experimental.SuperBuilder;
import com.datamaster.common.core.domain.BaseEntity;

import java.time.LocalDateTime;

/**
 * API服务调用日志 DO 对象 Service_API_LOG
 *
 * @author lhs
 * @date 2025-02-12
 */
@Data
@TableName(value = "SVC_API_LOG")
// 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
// @KeySequence("Service_API_LOG_seq")
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ServiceApiLogDO extends BaseEntity {
    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

/** 类目ID */
    private Long catId;

    /** 类目编码 */
    private String catCode;

    /** 类目名称 */
    @TableField(exist = false)
    private String catName;

    /** 调用API服务Id */
    private Long apiId;

    @TableField(exist = false)
    private String apiName;

    @TableField(exist = false)
    private String reqMethod;

    /** 调用者id */
    private String callerId;

    /** 调用者 */
    private String callerBy;

    /** 调用者ip */
    private String callerIp;

    /** 调用url */
    private String callerUrl;

    /** 调用参数 */
    private String callerParams;

    /** 调用开始时间 */
    private LocalDateTime callerStartDate;

    /** 调用结束时间 */
    private LocalDateTime callerEndDate;

    /** 调用数据量 */
    private int callerSize;

    /** 调用耗时(毫秒) */
    private Long callerTime;

    /** 信息记录 */
    private String msg;

    /** 状态 */
    private Integer status;

    /** 是否有效 */
    private Boolean validFlag;

    /**
     * 返回参数
     */
    private String fieldParameters;

    /** 删除标志 */
    @TableLogic
    private Boolean delFlag;



}
