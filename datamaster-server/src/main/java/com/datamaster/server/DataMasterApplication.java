

package com.datamaster.server;

import org.dromara.x.file.storage.spring.EnableFileStorage;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;

/**
 * 启动程序
 *
 * @author dataMaster
 */
@EnableFileStorage
@ComponentScan(basePackages = {"com.datamaster"})
@ServletComponentScan(basePackages = {"com.datamaster"})
@SpringBootApplication(exclude = { DataSourceAutoConfiguration.class,
        MongoAutoConfiguration.class, MongoDataAutoConfiguration.class })
@EnableNeo4jRepositories(basePackages = "com.datamaster.neo4j.repository")
@EntityScan(basePackages = "com.datamaster.neo4j.node")   // 节点/关系实
@EnableAspectJAutoProxy(proxyTargetClass = true)
public class DataMasterApplication
{
    public static final String BRAND_BLUE = "\u001B[38;2;29;80;163m";
    public static final String RESET = "\u001B[0m";

    public static void main(String[] args)
    {
        // System.setProperty("spring.devtools.restart.enabled", "false");
        SpringApplication.run(DataMasterApplication.class, args);

        System.out.println("==========项目已启动===========");
    }
}
