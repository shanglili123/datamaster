

package com.datamaster.quality;

import org.dromara.x.file.storage.spring.EnableFileStorage;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.FilterType;
import com.datamaster.common.httpClient.DsRequestUtils;

/**
 * <P>
 * 用途:数据质量启动类
 * </p>
 *
 * @author: FXB
 * @create: 2025-07-15 15:02
 **/
@EnableFileStorage
@ComponentScan(basePackages = {"com.datamaster"}, excludeFilters = {
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {DsRequestUtils.class})
})
@ServletComponentScan(basePackages = {"com.datamaster"})
@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class, SecurityAutoConfiguration.class,
        MongoAutoConfiguration.class, MongoDataAutoConfiguration.class})
@EnableAspectJAutoProxy(proxyTargetClass = true)
public class QualityApplication {
    public static void main(String[] args) {
        SpringApplication.run(QualityApplication.class, args);
        System.out.println("(♥◠‿◠)ﾉﾞ  数据质量启动成功   ლ(´ڡ`ლ)ﾞ  \n" +
                "    _            _         _        \n" +
                "   / \\    _ __  (_)__   __(_)  __ _ \n" +
                "  / _ \\  | '_ \\ | |\\ \\ / /| | / _` |\n" +
                " / ___ \\ | | | || | \\ V / | || (_| |\n" +
                "/_/   \\_\\|_| |_||_|  \\_/  |_| \\__,_|");
    }
}
