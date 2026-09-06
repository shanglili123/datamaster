

package com.datamaster.neo4j.node;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;
import com.datamaster.neo4j.rel.ObjectToTableRel;

import java.util.List;

/**
 * <P>
 * 用途：本体语义对象节点（对象血缘 / Object Lineage）。
 * </p>
 * <p>
 * 一个 Object 节点对应「本体概念 × 命中的物理表」这一语义对象，是对象血缘四维度
 * （数据 / 决策 / 版本 / 权限）的统一载体：
 * <ul>
 *   <li>数据维度：Object -[MATERIALIZES]-&gt; Table（对象由哪张物理表支撑，并沿用表级上/下游血缘）</li>
 *   <li>决策维度：ActionExecution -[DECISION_ACTION]-&gt; Object（哪些动作/执行消费或产出该对象）</li>
 *   <li>版本维度：由 ONT_ACTION_EXECUTION 的 beforeData/afterData 快照派生，不落 Neo4j</li>
 *   <li>权限维度：由 assets 统一权限入口实时派生，不落 Neo4j</li>
 * </ul>
 * 与 TableNode/TaskNode/ActionExecutionNode 同由 Neo4jLineageConfig 装配，受 LINEAGE_ENABLED 开关控制。
 *
 * @author: Sisyphus
 * @create: 2026-09-01
 **/
@Node("Object")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ObjectNode {

    @Id
    @GeneratedValue
    private Long id;

    /**
     * 对象业务ID（=本体概念 ID ONT_CONCEPT.ID）
     */
    private Long objectId;

    /**
     * 本体 ID（ONT_ONTOLOGY.ID）
     */
    private Long ontologyId;

    /**
     * 概念 ID（ONT_CONCEPT.ID）
     */
    private Long conceptId;

    /**
     * 概念编码
     */
    private String conceptCode;

    /**
     * 概念名称
     */
    private String conceptName;

    /**
     * 命中的物理表名
     */
    private String tableName;

    /**
     * 数据源ip:port
     */
    private String datasourceHostPort;

    /**
     * 数据库名，冗余字段可空
     */
    private String dbName;

    /**
     * 模式名，冗余字段可空
     */
    private String sid;

    /** Object -> Table（数据维度：对象由物理表支撑） */
    @Relationship(type = "MATERIALIZES", direction = Relationship.Direction.OUTGOING)
    private List<ObjectToTableRel> materializesRels;
}
