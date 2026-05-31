

package com.datamaster.module.service.config.auth;

import cn.dev33.satoken.interceptor.SaInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import com.datamaster.module.service.annotation.handler.ServiceCheckClientTokenInterceptor;

import javax.annotation.Resource;

/**
 * 注册拦截器
 * @author Ming
 */
@Configuration
public class ServiceTokenConfigure implements WebMvcConfigurer {

    @Resource
    private ServiceCheckClientTokenInterceptor checkClientTokenInterceptor;

    /**
     * 注册拦截器，打开注解式鉴权功能
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new SaInterceptor()).addPathPatterns("/**");
        registry.addInterceptor(checkClientTokenInterceptor)
                // 指定要拦截的路径模式
                .addPathPatterns("/**");
    }
}
