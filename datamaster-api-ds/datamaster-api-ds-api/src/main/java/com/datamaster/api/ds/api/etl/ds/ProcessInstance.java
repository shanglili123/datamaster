

package com.datamaster.api.ds.api.etl.ds;

import com.baomidou.mybatisplus.annotation.TableField;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.datamaster.common.enums.*;

import java.util.Date;

/**
 * 流程实例
 */
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
public class ProcessInstance {

    /**
     * id
     */
    private Long id;

    /**
     * 流程编码
     */
    private String processDefinitionCode;
    /**
     * 流程版本
     */
    private int processDefinitionVersion;
    /**
     * 项目编码
     */
    private String projectCode;
    /**
     * 状态
     */
    private WorkflowExecutionStatus state;
    /**
     * 状态历史
     */
    private String stateHistory;
    /**
     * 调度时间
     */
    private Date scheduleTime;
    /**
     * 执行开始时间
     */
    private Date commandStartTime;

    /**
     * 开始时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date startTime;
    /**
     * 结束时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date endTime;

    /**
     * 运行次数
     */
    private Integer runTimes;
    /**
     * 任务实例名称
     */
    private String name;
    /**
     * 流程定义
     */
    @TableField(exist = false)
    private ProcessDefinition processDefinition;
    /**
     * 运行类型
     */
    private CommandType commandType;

    private String commandParam;
    /**
     * 最大重试次数
     */
    private int maxTryTimes;
    /**
     * 是否是子流程
     */
    private Flag isSubProcess;
    /**
     * 优先级
     */
    private Priority processInstancePriority;
    /**
     * 失败策略
     */
    private FailureStrategy failureStrategy;


    private String dataSource;
}
