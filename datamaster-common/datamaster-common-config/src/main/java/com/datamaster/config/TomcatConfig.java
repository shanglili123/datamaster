package com.datamaster.config;

import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Embedded Tomcat configuration.
 */
@Configuration
public class TomcatConfig {

    @Bean
    public WebServerFactoryCustomizer<TomcatServletWebServerFactory> tomcatJasperInitializerFilter() {
        return factory -> factory.addContextCustomizers(context ->
                context.setContainerSciFilter("org.apache.jasper.servlet.JasperInitializer"));
    }
}
