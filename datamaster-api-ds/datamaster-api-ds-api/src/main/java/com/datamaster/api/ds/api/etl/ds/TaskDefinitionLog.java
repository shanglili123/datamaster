

package com.datamaster.api.ds.api.etl.ds;

import lombok.Data;

/**
 * task definition log
 */
@Data
public class TaskDefinitionLog extends TaskDefinition {


    public TaskDefinitionLog() {
        super();
    }

    public TaskDefinitionLog(TaskDefinition taskDefinition) {
        super();
        this.setCode(taskDefinition.getCode());
        this.setVersion(taskDefinition.getVersion());
        this.setName(taskDefinition.getName());
        this.setDescription(taskDefinition.getDescription());
        this.setUserId(taskDefinition.getUserId());
//        this.setUserName(taskDefinition.getUserName());
        this.setWorkerGroup(taskDefinition.getWorkerGroup());
        this.setEnvironmentCode(taskDefinition.getEnvironmentCode());
        this.setProjectCode(taskDefinition.getProjectCode());
//        this.setProjectName(taskDefinition.getProjectName());
//        this.setResourceIds(taskDefinition.getResourceIds());
        this.setTaskParams(taskDefinition.getTaskParams());
//        this.setTaskParamList(taskDefinition.getTaskParamList());
        this.setTaskParamMap(taskDefinition.getTaskParamMap());
//        this.setTaskPriority(taskDefinition.getTaskPriority());
//        this.setTaskExecuteType(taskDefinition.getTaskExecuteType());
//        this.setTimeoutNotifyStrategy(taskDefinition.getTimeoutNotifyStrategy());
        this.setTaskType(taskDefinition.getTaskType());
        this.setTimeout(taskDefinition.getTimeout());
        this.setDelayTime(taskDefinition.getDelayTime());
//        this.setTimeoutFlag(taskDefinition.getTimeoutFlag());
//        this.setUpdateTime(taskDefinition.getUpdateTime());
//        this.setCreateTime(taskDefinition.getCreateTime());
        this.setFailRetryInterval(taskDefinition.getFailRetryInterval());
        this.setFailRetryTimes(taskDefinition.getFailRetryTimes());
        this.setFlag(taskDefinition.getFlag());
        this.setIsCache(taskDefinition.getIsCache());
//        this.setModifyBy(taskDefinition.getModifyBy());
        this.setCpuQuota(taskDefinition.getCpuQuota());
        this.setMemoryMax(taskDefinition.getMemoryMax());
//        this.setTaskExecuteType(taskDefinition.getTaskExecuteType());
    }


    @Override
    public boolean equals(Object o) {
        return super.equals(o);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public String toString() {
        return super.toString();
    }
}
