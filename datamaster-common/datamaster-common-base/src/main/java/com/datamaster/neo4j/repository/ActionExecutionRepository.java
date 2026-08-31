package com.datamaster.neo4j.repository;

import com.datamaster.neo4j.node.ActionExecutionNode;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.stereotype.Repository;

/**
 * <P>
 * 用途: 动作执行节点仓库（动作血缘，可插拔）
 * </p>
 * <p>
 * 与 TableRepository/TaskRepository 一样，由 Neo4jLineageConfig 在
 * LINEAGE_ENABLED=true 时通过 @EnableNeo4jRepositories 装配；
 * 开关关闭时不创建任何 Bean，不影响动作执行。
 *
 * @author: FXB
 * @create: 2025-08-29 10:30
 **/
@Repository
public interface ActionExecutionRepository extends Neo4jRepository<ActionExecutionNode, Long> {
}