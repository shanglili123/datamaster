

package com.datamaster.module.collector.controller.admin.etl.vo;

import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.annotation.TableField;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import com.datamaster.common.annotation.Excel;
import com.datamaster.common.utils.JSONUtils;
import com.datamaster.module.collector.dal.dataobject.etl.CollectorEtlTaskDO;
import com.datamaster.module.collector.dal.dataobject.etl.CollectorEtlTaskInstanceDO;

import javax.validation.constraints.Size;
import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 数据集成任务 Response VO 对象 COL_ETL_TASK
 *
 * @author DATAMASTER
 * @date 2025-02-13
 */
@Schema(description = "数据集成任务 Response VO")
@Data
public class CollectorEtlTaskUpdateQueryRespVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Excel(name = "ID")
    @Schema(description = "ID")
    private Long id;

    @Schema(description = "类目id", example = "")
    private Long catId;

    @Schema(description = "类目编码", example = "")
    @Size(max = 256, message = "类目编码长度不能超过256个字符")
    private String catCode;


    /**
     * @see CollectorEtlTaskDO#type
     */
    @Excel(name = "任务类型")
    @Schema(description = "任务类型", example = "")
    private String type;

    @Excel(name = "任务名称")
    @Schema(description = "任务名称", example = "")
    private String name;

    @Excel(name = "任务编码")
    @Schema(description = "任务编码", example = "")
    private String code;

    @Excel(name = "任务版本")
    @Schema(description = "任务版本", example = "")
    private Long version;

    @Excel(name = "项目id")
    @Schema(description = "项目id", example = "")
    private Long projectId;

    @Excel(name = "项目编码")
    @Schema(description = "项目编码", example = "")
    private String projectCode;

    @Excel(name = "责任人")
    @Schema(description = "责任人", example = "")
    private String personCharge;

    @Excel(name = "责任人名称")
    @Schema(description = "责任人名称", example = "")
    private String personChargeName;

    @Excel(name = "联系电话")
    @Schema(description = "联系电话", example = "")
    private String contactNumber;

    @Excel(name = "节点坐标信息")
    @Schema(description = "节点坐标信息", example = "")
    private List<Map<String, Object>> locations;

    @Excel(name = "描述")
    @Schema(description = "描述", example = "")
    private String description;

    @Schema(description = "任务的执行策略", example = "")
    private String executionType;

    @Excel(name = "超时时间")
    @Schema(description = "超时时间", example = "")
    private Long timeout;

    @Excel(name = "抽取量")
    @Schema(description = "抽取量", example = "")
    private Long extractionCount;

    @Excel(name = "写入量")
    @Schema(description = "写入量", example = "")
    private Long writeCount;

    @Excel(name = "任务状态")
    @Schema(description = "任务状态", example = "")
    private String status;

    @Excel(name = "DolphinScheduler的id")
    @Schema(description = "DolphinScheduler的id", example = "")
    private Long dsId;

    @Excel(name = "是否有效")
    @Schema(description = "是否有效", example = "")
    private Boolean validFlag;

    @Excel(name = "删除标志")
    @Schema(description = "删除标志", example = "")
    private Boolean delFlag;

    @Excel(name = "创建人")
    @Schema(description = "创建人", example = "")
    private String createBy;

    @Excel(name = "创建人id")
    @Schema(description = "创建人id", example = "")
    private Long creatorId;

    @Excel(name = "创建时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间", example = "")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    @Excel(name = "更新人")
    @Schema(description = "更新人", example = "")
    private String updateBy;

    @Excel(name = "更新人id")
    @Schema(description = "更新人id", example = "")
    private Long updaterId;

    @Excel(name = "更新时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "更新时间", example = "")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;

    @Excel(name = "备注")
    @Schema(description = "备注", example = "")
    private String remark;

    /**
     * cron表达式
     */
    @TableField(exist = false)
    private String crontab;


    @TableField(exist = false)
    private Map<String, Object> taskConfig;

    @TableField(exist = false)
    List<Map<String, Object>> taskDefinitionList;

    @TableField(exist = false)
    List<Map<String, Object>> taskRelationJson;

    @Schema(description = "草稿任务配置信息", example = "")
    private String draftJson;

    /** 最后执行时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date lastExecuteTime;

    /** 最后执行状态 */
    private String lastExecuteStatus;

    /** 调度上下线状态 */
    private String schedulerState;

    /**
     * 任务实例
     */
    private CollectorEtlTaskInstanceDO taskInstance;

    public CollectorEtlTaskUpdateQueryRespVO(CollectorEtlTaskDO CollectorEtlTaskDO) {
        this.id = CollectorEtlTaskDO.getId();
        this.catId = CollectorEtlTaskDO.getCatId();
        this.catCode = CollectorEtlTaskDO.getCatCode();
        this.type = CollectorEtlTaskDO.getType();
        this.name = CollectorEtlTaskDO.getName();
        this.code = CollectorEtlTaskDO.getCode();
        this.version = CollectorEtlTaskDO.getVersion();
        this.projectId = CollectorEtlTaskDO.getProjectId();
        this.projectCode = CollectorEtlTaskDO.getProjectCode();
        this.personCharge = CollectorEtlTaskDO.getPersonCharge();
        this.personChargeName = CollectorEtlTaskDO.getPersonChargeName();
        this.contactNumber = CollectorEtlTaskDO.getContactNumber();
        this.locations = parseList(CollectorEtlTaskDO.getLocations());
        this.description = CollectorEtlTaskDO.getDescription();
        this.executionType = CollectorEtlTaskDO.getExecutionType();
        this.timeout = CollectorEtlTaskDO.getTimeout();
        this.extractionCount = CollectorEtlTaskDO.getExtractionCount();
        this.writeCount = CollectorEtlTaskDO.getWriteCount();
        this.status = CollectorEtlTaskDO.getStatus();
        this.dsId = CollectorEtlTaskDO.getDsId();
        this.validFlag = CollectorEtlTaskDO.getValidFlag();
        this.delFlag = CollectorEtlTaskDO.getDelFlag();
        this.draftJson = CollectorEtlTaskDO.getDraftJson();
        this.createBy = CollectorEtlTaskDO.getCreateBy();
        this.creatorId = CollectorEtlTaskDO.getCreatorId();
        this.createTime = CollectorEtlTaskDO.getCreateTime();
        this.updateBy = CollectorEtlTaskDO.getUpdateBy();
        this.updaterId = CollectorEtlTaskDO.getUpdatorId();
        this.updateTime = CollectorEtlTaskDO.getUpdateTime();
    }

    private List<Map<String, Object>> parseList(String string) {
        List<Map<String, Object>> list = JSONUtils.convertTaskDefinitionJson(string);
        return list;
    }

    private Map<String, Object> parseMap(String string) {
        Map<String, Object> stringObjectMap = JSONUtils.convertTaskDefinitionJsonMap(string);
        return stringObjectMap;
    }

    public void setTaskRelationJsonFromNodeRelList(List<CollectorEtlTaskNodeRelRespVO> CollectorEtlTaskNodeRelRespVOList) {
        // 将 List<CollectorEtlTaskNodeRelRespVO> 转换为 List<Map<String, Object>>
        List<Map<String, Object>> taskRelationJsonList = new ArrayList<>();
        if (CollectorEtlTaskNodeRelRespVOList == null || CollectorEtlTaskNodeRelRespVOList.isEmpty()) {
            this.taskRelationJson = taskRelationJsonList;
            return;
        }

        for (CollectorEtlTaskNodeRelRespVO nodeRel : CollectorEtlTaskNodeRelRespVOList) {
            Map<String, Object> nodeRelMap = new HashMap<>();
            nodeRelMap.put("id", nodeRel.getId());
            nodeRelMap.put("projectId", nodeRel.getProjectId());
            nodeRelMap.put("projectCode", nodeRel.getProjectCode());
            nodeRelMap.put("taskId", nodeRel.getTaskId());
            nodeRelMap.put("taskCode", nodeRel.getTaskCode());
            nodeRelMap.put("taskVersion", nodeRel.getTaskVersion());
            nodeRelMap.put("preNodeId", nodeRel.getPreNodeId());
            nodeRelMap.put("preNodeCode", nodeRel.getPreNodeCode());
            nodeRelMap.put("preNodeVersion", nodeRel.getPreNodeVersion());
            nodeRelMap.put("postNodeId", nodeRel.getPostNodeId());
            nodeRelMap.put("postNodeCode", nodeRel.getPostNodeCode());
            nodeRelMap.put("postNodeVersion", nodeRel.getPostNodeVersion());
            nodeRelMap.put("validFlag", nodeRel.getValidFlag());
            nodeRelMap.put("delFlag", nodeRel.getDelFlag());
            nodeRelMap.put("createBy", nodeRel.getCreateBy());
            nodeRelMap.put("creatorId", nodeRel.getCreatorId());
            nodeRelMap.put("createTime", nodeRel.getCreateTime());
            nodeRelMap.put("updateBy", nodeRel.getUpdateBy());
            nodeRelMap.put("updaterId", nodeRel.getUpdaterId());
            nodeRelMap.put("updateTime", nodeRel.getUpdateTime());
            nodeRelMap.put("remark", nodeRel.getRemark());

            taskRelationJsonList.add(nodeRelMap);
        }

        // 存入 taskRelationJson 字段
        this.taskRelationJson = taskRelationJsonList;
    }


    public void setTaskDefinitionList(List<CollectorEtlNodeRespVO> CollectorEtlNodeRespVOList) {
        if (CollectorEtlNodeRespVOList == null || CollectorEtlNodeRespVOList.isEmpty()) {
            this.taskDefinitionList = new ArrayList<>();
            return;
        }
        // 将 CollectorEtlNodeRespVO 列表转换为 List<Map<String, Object>>
        List<Map<String, Object>> taskDefinitionList = CollectorEtlNodeRespVOList.stream()
                .map(node -> {
                    Map<String, Object> stringObjectMap = parseMap(node.getParameters());
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", node.getId());
                    map.put("taskType", node.getType());
                    map.put("name", node.getName());
                    map.put("code", node.getCode());
                    map.put("releaseState", node.getReleaseState());
                    map.put("version", node.getVersion());
                    map.put("projectId", node.getProjectId());
                    map.put("projectCode", node.getProjectCode());
                    map.put("taskParams", stringObjectMap);
                    map.put("priority", node.getPriority());
                    map.put("failRetryTimes", node.getFailRetryTimes());
                    map.put("failRetryInterval", node.getFailRetryInterval());
                    map.put("timeout", node.getTimeout());
                    map.put("delayTime", node.getDelayTime());
                    map.put("cpuQuota", node.getCpuQuota());
                    map.put("memoryMax", node.getMemoryMax());
                    map.put("description", node.getDescription());
                    map.put("componentType", node.getComponentType());
                    map.put("dsId", node.getDsId());
                    map.put("validFlag", node.getValidFlag());
                    map.put("delFlag", node.getDelFlag());
                    map.put("createBy", node.getCreateBy());
                    map.put("creatorId", node.getCreatorId());
                    map.put("createTime", DateUtil.format(node.getCreateTime(),"yyyy-MM-dd HH:mm:ss"));
                    map.put("updateBy", node.getUpdateBy());
                    map.put("updaterId", node.getUpdaterId());
                    map.put("updateTime", DateUtil.format(node.getUpdateTime(),"yyyy-MM-dd HH:mm:ss"));
                    map.put("remark", node.getRemark());
                    return map;
                })
                .collect(Collectors.toList());

        // 将转换后的列表赋值给 taskDefinitionList
        this.taskDefinitionList = taskDefinitionList;
    }


    public void createTaskConfig() {
        Map<String, Object> taskConfig = JSONUtils.checkJsonValid(this.draftJson, false)
                ? JSONUtils.convertTaskDefinitionJsonMap(this.draftJson) : new HashMap<>();
        if (taskConfig == null) {
            taskConfig = new HashMap<>();
        }
        taskConfig.put("type", this.type);
        taskConfig.put("releaseState", this.status);
        taskConfig.put("description", this.description); // 从本身获取描述
        taskConfig.put("name", this.name); // 从本身获取任务名称
        taskConfig.put("executionType", this.executionType); // 从本身获取执行策略
        taskConfig.put("crontab", this.crontab); // 固定 crontab 表达式
        taskConfig.put("personCharge", this.personCharge); // 责任人
        taskConfig.put("contactNumber", this.contactNumber); // 联系电话
        taskConfig.put("catCode", this.catCode); // 责任人
        taskConfig.put("draftJson", this.draftJson);

        // 设置 taskConfig
        this.setTaskConfig(taskConfig);
    }
}
