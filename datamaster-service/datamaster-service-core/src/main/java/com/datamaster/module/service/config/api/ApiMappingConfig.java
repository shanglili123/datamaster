

package com.datamaster.module.service.config.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import com.datamaster.module.service.handler.MappingHandlerMapping;
import com.datamaster.module.service.handler.RequestHandler;
import com.datamaster.module.service.handler.RequestInterceptor;
import com.datamaster.module.service.service.api.impl.ApiMappingEngine;

@Configuration
public class ApiMappingConfig {

    @Bean
    public MappingHandlerMapping mappingHandlerMapping(RequestMappingHandlerMapping requestMappingHandlerMapping,
                                                       ApiMappingEngine apiMappingEngine,
                                                       RedisTemplate redisTemplate,
                                                       ObjectMapper objectMapper) {
        MappingHandlerMapping mappingHandlerMapping = new MappingHandlerMapping();
        mappingHandlerMapping.setHandler(requestHandler(apiMappingEngine, redisTemplate, objectMapper));
        mappingHandlerMapping.setRequestMappingHandlerMapping(requestMappingHandlerMapping);
        return mappingHandlerMapping;
    }

    @Bean
    public RequestHandler requestHandler(ApiMappingEngine apiMappingEngine, RedisTemplate redisTemplate, ObjectMapper objectMapper) {
        RequestHandler handler = new RequestHandler();
        handler.setApiMappingEngine(apiMappingEngine);
        handler.setObjectMapper(objectMapper);
        handler.setRequestInterceptor(new RequestInterceptor(redisTemplate));
        return handler;
    }
}
