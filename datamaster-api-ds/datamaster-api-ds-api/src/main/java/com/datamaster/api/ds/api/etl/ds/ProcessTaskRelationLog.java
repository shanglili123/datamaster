

package com.datamaster.api.ds.api.etl.ds;

/**
 * process task relation log
 */
public class ProcessTaskRelationLog extends ProcessTaskRelation {


    public ProcessTaskRelationLog() {
        super();
    }

    public ProcessTaskRelationLog(ProcessTaskRelation processTaskRelation) {
        super();
        this.setName(processTaskRelation.getName());
        this.setProcessDefinitionCode(processTaskRelation.getProcessDefinitionCode());
        this.setProcessDefinitionVersion(processTaskRelation.getProcessDefinitionVersion());
        this.setProjectCode(processTaskRelation.getProjectCode());
        this.setPreTaskCode(processTaskRelation.getPreTaskCode());
        this.setPreTaskVersion(processTaskRelation.getPreTaskVersion());
        this.setPostTaskCode(processTaskRelation.getPostTaskCode());
        this.setPostTaskVersion(processTaskRelation.getPostTaskVersion());
        this.setConditionType(processTaskRelation.getConditionType());
        this.setCreateTime(processTaskRelation.getCreateTime());
        this.setUpdateTime(processTaskRelation.getUpdateTime());
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
