package com.datamaster.module.service.annotation.handler;

import com.alibaba.fastjson.JSON;
import com.datamaster.module.service.annotation.ServiceCheckClientToken;
import com.datamaster.module.service.config.auth.ServiceTokenService;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 检查 ClientToken 拦截器
 * <p>
 * 替代原 Sa-Token 的 SaOAuth2Util.checkClientToken，从 Redis 校验 Token。
 *
 * @author Ming
 */
@Component
public class ServiceCheckClientTokenInterceptor implements HandlerInterceptor {

    private static final String CLIENT_TOKEN_PARAM = "client_token";

    @Resource
    private ServiceTokenService serviceTokenService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (handler instanceof HandlerMethod) {
            HandlerMethod handlerMethod = (HandlerMethod) handler;
            ServiceCheckClientToken annotation = handlerMethod.getMethodAnnotation(ServiceCheckClientToken.class);
            if (annotation != null) {
                String clientToken = request.getParameter(CLIENT_TOKEN_PARAM);
                String clientId = serviceTokenService.validateToken(clientToken);
                if (clientId == null) {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("application/json;charset=UTF-8");
                    response.getWriter().write(JSON.toJSONString(
                            new com.datamaster.common.core.domain.AjaxResult(HttpServletResponse.SC_UNAUTHORIZED, "client_token 无效或已过期")
                    ));
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
    }
}
