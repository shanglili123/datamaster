package com.datamaster.module.ai.dal.dataobject.agent;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 数据智能体平台运营流程实例。流程状态必须落库，页面刷新或服务重启后才能恢复。
 */
@Data
@TableName("AI_AGENT_OPERATION")
public class AiAgentOperationDO {

    @TableId
    private String id;
    private String goal;
    private Long datasourceId;
    private String datasourceName;
    private String databaseName;
    private Long spaceId;
    private String spaceCode;
    private String requestJson;
    private String status;
    private Integer progress;
    private String currentStep;
    private String message;
    private String outputJson;
    private String stepsJson;
    private String resultJson;
    private String requiredFieldsJson;
    private String assetSyncMessage;
    private Long catalogTaskId;
    private String confirmationType;
    private String confirmationMessage;
    private Long ontologyId;
    private Integer actionCount;
    private Long creatorId;
    private String createBy;
    private Date createTime;
    private Long updaterId;
    private String updateBy;
    private Date updateTime;
    private Boolean delFlag;
}
