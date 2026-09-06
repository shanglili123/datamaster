package com.datamaster.neo4j.node;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;
import com.datamaster.neo4j.rel.ObjectDecisionRel;

import java.util.Date;
import java.util.List;

/**
 * <P>
 * 用途：本体动作执行节点（动作血缘）。
 * </p>
 * <p>
 * 记录一次动作执行（ONT_ACTION_EXECUTION）在 Neo4j 中的操作痕迹，
 * 用于动作级血缘追溯。由 LineageDataService.saveActionExecution 写入，
 * 受 LINEAGE_ENABLED 开关控制（可插拔）。
 *
 * @author: FXB
 * @create: 2025-08-29 10:30
 **/
@Node("ActionExecution")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActionExecutionNode {

    @Id
    @GeneratedValue
    private Long id;

    /**
     * 执行记录ID（对应 ONT_ACTION_EXECUTION.ID）
     */
    private Long executionId;

    /**
     * 动作ID（对应 ONT_ACTION.ID）
     */
    private Long actionId;

    /**
     * 动作名称
     */
    private String actionName;

    /**
     * 动作类型：SELECT / CREATE / UPDATE / DELETE
     */
    private String actionType;

    /**
     * 涉及物理表名
     */
    private String tableName;

    /**
     * 数据源ip:port
     */
    private String datasourceHostPort;

    /**
     * 执行状态：EXECUTED / FAILED
     */
    private String status;

    /**
     * 执行时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date executeTime;

    /** ActionExecution -> Object（决策维度：本次动作执行作用于哪个语义对象） */
    @Relationship(type = "DECISION_ACTION", direction = Relationship.Direction.OUTGOING)
    private List<ObjectDecisionRel> objectRels;
}