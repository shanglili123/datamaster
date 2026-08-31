

package com.datamaster.server;

import org.dromara.x.file.storage.spring.EnableFileStorage;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 启动程序
 *
 * @author dataMaster
 */
@EnableFileStorage
@EnableScheduling
@ComponentScan(basePackages = {"com.datamaster"})
@ServletComponentScan(basePackages = {"com.datamaster"})
@SpringBootApplication(exclude = { DataSourceAutoConfiguration.class,
        MongoAutoConfiguration.class, MongoDataAutoConfiguration.class })
@EnableAspectJAutoProxy(proxyTargetClass = true)
public class DataMasterApplication
{
    public static final String RESET = "\u001B[0m";

    public static void main(String[] args)
    {
        // System.setProperty("spring.devtools.restart.enabled", "false");
        SpringApplication.run(DataMasterApplication.class, args);

        System.out.println("==========服务已启动===========");
    }
}
