

package com.datamaster.neo4j.repository;

import com.datamaster.neo4j.dto.ObjectLineageDTO;
import com.datamaster.neo4j.node.ObjectNode;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * <P>
 * 用途：语义对象节点仓库（对象血缘，可插拔）。
 * </p>
 * <p>
 * 与 TableRepository/TaskRepository/ActionExecutionRepository 一样由
 * Neo4jLineageConfig 在 LINEAGE_ENABLED=true 时装配；开关关闭时不创建 Bean。
 *
 * @author: Sisyphus
 * @create: 2026-09-01
 **/
@Repository
public interface ObjectRepository extends Neo4jRepository<ObjectNode, Long> {

    /**
     * 按概念ID查询语义对象节点（对象业务ID = 概念ID）。
     */
    Optional<ObjectNode> findByConceptId(Long conceptId);

    /**
     * 对象血缘读取：该对象的物理表（数据维度）+ 作用于该对象的动作执行（决策维度）。
     * <p>
     * 版本/权限维度不落在 Neo4j，由上层基于快照与权限入口实时派生。
     *
     * @param conceptId 本体概念ID
     * @return 对象血缘聚合（对象 + 支撑表 + 决策动作执行）
     */
    @Query(value = "MATCH (obj:Object {conceptId: $conceptId})  " +
            "OPTIONAL MATCH (obj)-[r1:MATERIALIZES]->(table:Table)  " +
            "OPTIONAL MATCH (act:ActionExecution)-[r2:DECISION_ACTION]->(obj)  " +
            "RETURN  " +
            "  obj AS currentObject,  " +
            "  collect(DISTINCT table) AS tables,  " +
            "  collect(DISTINCT act) AS decisions")
    Optional<ObjectLineageDTO> findObjectLineage(@Param("conceptId") Long conceptId);
}
