

package com.datamaster.neo4j.config;

import org.neo4j.driver.Driver;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.neo4j.core.transaction.Neo4jTransactionManager;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;

/**
 * <P>
 * 用途:
 * </p>
 *
 * @author: FXB
 * @create: 2025-09-09 16:03
 **/
@Configuration
@EnableTransactionManagement
public class MultiTxManagerConfig {

    @Bean("transactionManager")
    @Primary
    public DataSourceTransactionManager jdbcTransactionManager(DataSource ds) {
        return new DataSourceTransactionManager(ds);
    }

    /**
     * Neo4j 事务管理器（可插拔）
     * <p>
     * 仅在血缘开关 {@code datamaster.lineage.enabled=true}（环境变量 LINEAGE_ENABLED=true）
     * 时创建；未开启时不依赖 Neo4j Driver Bean，避免因缺少 spring.data.neo4j 配置导致启动失败。
     *
     * @param dbProvider
     * @return
     */
    @Bean("neo4jTransactionManager")
    @ConditionalOnProperty(prefix = "datamaster.lineage", name = "enabled", havingValue = "true")
    public Neo4jTransactionManager neo4jTransactionManager(Driver dbProvider) {
        return new Neo4jTransactionManager(dbProvider);
    }
}

