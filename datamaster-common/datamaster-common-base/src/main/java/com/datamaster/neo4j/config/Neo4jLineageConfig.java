package com.datamaster.neo4j.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;

/**
 * <P>
 * Neo4j 血缘链路条件装配（可插拔）。
 * </p>
 * <p>
 * 仅在 {@code datamaster.lineage.enabled=true}（即环境变量 {@code LINEAGE_ENABLED=true}）
 * 时才启用 Neo4j 数据访问能力：
 * <ul>
 *   <li>{@code @EnableNeo4jRepositories}：启用 {@code com.datamaster.neo4j.repository} 下的
 *       Table/Task 等 Neo4j Repository；</li>
 *   <li>{@code @EntityScan}：扫描 {@code com.datamaster.neo4j.node} 下的节点/关系实体。</li>
 * </ul>
 * 开关未开启时，本配置类不生效，不会创建任何 Neo4j 相关 Bean，
 * ETL 任务发布/删除、动作执行等主流程不受影响，也无需安装 Neo4j。
 * </p>
 *
 * @author dataMaster
 */
@Configuration
@ConditionalOnProperty(prefix = "datamaster.lineage", name = "enabled", havingValue = "true")
@EnableNeo4jRepositories(basePackages = "com.datamaster.neo4j.repository")
@EntityScan(basePackages = "com.datamaster.neo4j.node")
public class Neo4jLineageConfig {
}