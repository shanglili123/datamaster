package com.datamaster.module.service.config.auth;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import com.datamaster.module.service.annotation.handler.ServiceCheckClientTokenInterceptor;

import javax.annotation.Resource;

/**
 * 注册拦截器
 * <p>
 * 移除原 Sa-Token 的 SaInterceptor，仅保留自定义的 ClientToken 校验拦截器。
 *
 * @author Ming
 */
@Configuration
public class ServiceTokenConfigure implements WebMvcConfigurer {

    @Resource
    private ServiceCheckClientTokenInterceptor checkClientTokenInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(checkClientTokenInterceptor)
                .addPathPatterns("/**");
    }
}
