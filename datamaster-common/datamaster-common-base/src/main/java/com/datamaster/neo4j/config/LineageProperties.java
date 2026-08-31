package com.datamaster.neo4j.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 血缘记录开关配置（可插拔）。
 * <p>
 * 绑定前缀 {@code datamaster.lineage}，对应环境变量 {@code LINEAGE_ENABLED}。
 * <p>
 * 设计目标：
 * <ul>
 *   <li>未开启（默认 false）时，不装配 Neo4j 血缘链路（见 {@code Neo4jLineageConfig}），
 *       ETL 任务发布/删除、动作执行等主流程完全不受影响，也不要求安装 Neo4j；</li>
 *   <li>开启时需要同时配置 {@code spring.data.neo4j.uri/username/password}（由
 *       {@link Neo4jProperties} 承载）。</li>
 * </ul>
 *
 * @author dataMaster
 */
@Data
@Component
@ConfigurationProperties(prefix = "datamaster.lineage")
public class LineageProperties {

    /**
     * 是否开启血缘记录，默认 false。
     * 环境变量：LINEAGE_ENABLED（true/false）。
     */
    private boolean enabled = false;
}