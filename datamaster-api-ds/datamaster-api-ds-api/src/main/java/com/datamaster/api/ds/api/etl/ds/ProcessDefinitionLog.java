

package com.datamaster.api.ds.api.etl.ds;

/**
 * process definition log
 */
public class ProcessDefinitionLog extends ProcessDefinition {

    public ProcessDefinitionLog() {
        super();
    }

    public ProcessDefinitionLog(ProcessDefinition processDefinition) {
        this.setCode(processDefinition.getCode());
        this.setName(processDefinition.getName());
        this.setVersion(processDefinition.getVersion());
        this.setReleaseState(processDefinition.getReleaseState());
        this.setProjectCode(processDefinition.getProjectCode());
        this.setDescription(processDefinition.getDescription());
        this.setCreateTime(processDefinition.getCreateTime());
        this.setUpdateTime(processDefinition.getUpdateTime());
//        this.setFlag(processDefinition.getFlag());
//        this.setUserId(processDefinition.getUserId());
//        this.setUserName(processDefinition.getUserName());
//        this.setProjectName(processDefinition.getProjectName());
        this.setLocations(processDefinition.getLocations());
//        this.setScheduleReleaseState(processDefinition.getScheduleReleaseState());
//        this.setTimeout(processDefinition.getTimeout());
//        this.setModifyBy(processDefinition.getModifyBy());
//        this.setWarningGroupId(processDefinition.getWarningGroupId());
        this.setExecutionType(processDefinition.getExecutionType());
    }

    @Override
    public boolean equals(Object o) {
        return super.equals(o);
    }
}
