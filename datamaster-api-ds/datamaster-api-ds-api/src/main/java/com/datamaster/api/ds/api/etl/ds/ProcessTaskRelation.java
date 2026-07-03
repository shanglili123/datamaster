

package com.datamaster.api.ds.api.etl.ds;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.alibaba.fastjson2.annotation.JSONField;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
public class ProcessTaskRelation {

    /**
     * id
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * name
     */
    private String name;

    /**
     * process version
     */
    @JSONField(name = "workflowDefinitionVersion", alternateNames = {"processDefinitionVersion"})
    private int processDefinitionVersion;

    /**
     * project code
     */
    private String projectCode;

    /**
     * process code
     */
    @JSONField(name = "workflowDefinitionCode", alternateNames = {"processDefinitionCode"})
    private String processDefinitionCode;

    /**
     * pre task code
     */
    private String preTaskCode;

    /**
     * pre node version
     */
    private int preTaskVersion;

    /**
     * post task code
     */
    private String postTaskCode;

    /**
     * post node version
     */
    private int postTaskVersion;

    /**
     * condition type
     */
    private String conditionType;

    /**
     * create time
     */
    private Date createTime;

    /**
     * update time
     */
    private Date updateTime;


    public ProcessTaskRelation(ProcessTaskRelationLog processTaskRelationLog) {
        this.name = processTaskRelationLog.getName();
        this.processDefinitionVersion = processTaskRelationLog.getProcessDefinitionVersion();
        this.projectCode = processTaskRelationLog.getProjectCode();
        this.processDefinitionCode = processTaskRelationLog.getProcessDefinitionCode();
        this.preTaskCode = processTaskRelationLog.getPreTaskCode();
        this.preTaskVersion = processTaskRelationLog.getPreTaskVersion();
        this.postTaskCode = processTaskRelationLog.getPostTaskCode();
        this.postTaskVersion = processTaskRelationLog.getPostTaskVersion();
        this.conditionType = processTaskRelationLog.getConditionType();

        this.createTime = processTaskRelationLog.getCreateTime();
        this.updateTime = new Date();
    }

}
