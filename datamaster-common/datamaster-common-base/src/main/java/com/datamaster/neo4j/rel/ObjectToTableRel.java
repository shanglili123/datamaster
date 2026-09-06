

package com.datamaster.neo4j.rel;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.RelationshipProperties;
import org.springframework.data.neo4j.core.schema.TargetNode;
import com.datamaster.neo4j.node.TableNode;

/**
 * <P>
 * 用途：对象血缘「数据维度」关系 —— Object -[MATERIALIZES]-&gt; Table。
 * </p>
 * <p>
 * 表示语义对象由某张物理表支撑（对象集与表绑定的落图），
 * 配合 Table 的上/下游血缘可追溯对象的数据来源。
 *
 * @author: Sisyphus
 * @create: 2026-09-01
 **/
@RelationshipProperties
@Data
@Builder
public class ObjectToTableRel {

    @Id
    @GeneratedValue
    private Long id;

    /**
     * 对象业务ID（=本体概念 ID）
     */
    private Long objectId;

    /** 关联的目标物理表节点 */
    @TargetNode
    private TableNode table;
}
