

package com.datamaster.neo4j.rel;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.RelationshipProperties;
import org.springframework.data.neo4j.core.schema.TargetNode;
import com.datamaster.neo4j.node.ObjectNode;

/**
 * <P>
 * 用途：对象血缘「决策维度」关系 —— ActionExecution -[DECISION_ACTION]-&gt; Object。
 * </p>
 * <p>
 * 挂在 ActionExecutionNode 上（OUTGOING），表示一次动作执行指向其作用的语义对象。
 * 决策维度：哪些决策/动作消费或产出该对象。
 *
 * @author: Sisyphus
 * @create: 2026-09-01
 **/
@RelationshipProperties
@Data
@Builder
public class ObjectDecisionRel {

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

    /** 关联的目标语义对象节点 */
    @TargetNode
    private ObjectNode object;
}
