

package com.datamaster.api.ds.api.etl.ds;

import com.baomidou.mybatisplus.annotation.TableField;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessDefinition {

    /**
     * id
     */
    private Long id;

    /**
     * 编码
     */
    private String code;

    /**
     * 名称
     */
    private String name;

    /**
     * 版本号
     */
    private int version;

    /**
     * 发布状态 : online/offline
     */
    private String releaseState;

    /**
     * 项目编码
     */
    private String projectCode;

    /**
     * description
     */
    private String description;

    /**
     * create time
     */
    private Date createTime;

    /**
     * update time
     */
    private Date updateTime;

    /**
     * locations array for web
     */
    private String locations;

    /**
     * schedule release state : online/offline
     */
//    private ReleaseState scheduleReleaseState;

    /**
     * 流程定义日志列表
     */
    ProcessDefinitionLog processDefinitionLog;

    /**
     * 任务定义日志列表
     */
    List<TaskDefinitionLog> taskDefinitionLogList;

    /**
     * 任务关系日志列表
     */
    @TableField(exist = false)
    List<ProcessTaskRelationLog> taskRelationLogList;


    /**
     * 任务定义列表
     */
    @TableField(exist = false)
    List<TaskDefinition> taskDefinitionList;

    /**
     * 任务关系日志列表
     */
    @TableField(exist = false)
    List<ProcessTaskRelation> taskRelationList;

    /**
     * 执行策略
     */
    private String executionType;
}
