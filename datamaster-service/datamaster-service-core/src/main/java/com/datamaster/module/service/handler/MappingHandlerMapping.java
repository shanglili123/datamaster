

package com.datamaster.module.service.handler;


import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import com.datamaster.common.enums.RequestMethodEnum;
import com.datamaster.module.service.dal.dataobject.api.ServiceApiDO;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class MappingHandlerMapping {

    private static Map<String, ServiceApiDO> mappings = new ConcurrentHashMap<>();
    private RequestMappingHandlerMapping requestMappingHandlerMapping;
    private RequestHandler handler;
    private Method method;

    {
        try {
            method = RequestHandler.class.getDeclaredMethod("invoke", HttpServletRequest.class, HttpServletResponse.class, Map.class, Map.class, Map.class);
        } catch (NoSuchMethodException e) {
        }
    }

    private String ignore = "services";
    private String prefix = "v1.0.0";
    private String separator = "/";

    public MappingHandlerMapping() {}

    public void setRequestMappingHandlerMapping(RequestMappingHandlerMapping requestMappingHandlerMapping) {
        this.requestMappingHandlerMapping = requestMappingHandlerMapping;
    }

    public void setHandler(RequestHandler handler) {
        this.handler = handler;
    }

    public static ServiceApiDO getMappingApiInfo(HttpServletRequest request) {
        NativeWebRequest webRequest = new ServletWebRequest(request);
        String requestMapping = (String) webRequest.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE, RequestAttributes.SCOPE_REQUEST);
        return getMappingApiInfo(buildMappingKey(request.getMethod(), requestMapping));
    }

    public static ServiceApiDO getMappingApiInfo(String key) {
        return mappings.get(key);
    }

    public static String buildMappingKey(String requestMethod, String requestMapping) {
        String valByKey = null;
        //如果requestMethod是数字判断
        if("1".equals(requestMethod)||"2".equals(requestMethod)){
            valByKey = RequestMethodEnum.getValByKey(requestMethod.toUpperCase());
        }else{
            valByKey = requestMethod.toLowerCase();
        }
        return valByKey + ":" + requestMapping;
    }

    /**
     * 注册请求映射
     *
     * @param api
     */
    public synchronized void registerMapping(ServiceApiDO api) {
        String mappingKey = getMappingKey(api);
        ServiceApiDO previous = mappings.get(mappingKey);
        if (previous != null && !java.util.Objects.equals(previous.getId(), api.getId())) throw new com.datamaster.common.exception.ServiceException("API路由已被其他服务占用，不能覆盖");
        RequestMappingInfo requestMapping = getRequestMapping(api);
        if (previous != null) {
            requestMappingHandlerMapping.unregisterMapping(getRequestMapping(previous));
            mappings.remove(mappingKey);
        }
        requestMappingHandlerMapping.registerMapping(requestMapping, handler, method);
        mappings.put(mappingKey, api);
    }

    /**
     * 取消注册请求映射
     *
     * @param api
     */
    public synchronized void unregisterMapping(ServiceApiDO api) {
        String mappingKey = getMappingKey(api);
        ServiceApiDO registered = mappings.get(mappingKey);
        if (registered != null && java.util.Objects.equals(registered.getId(), api.getId())) {
            requestMappingHandlerMapping.unregisterMapping(getRequestMapping(registered));
            mappings.remove(mappingKey);
        }
    }

    private String getMappingKey(ServiceApiDO api) {
        if (api == null || api.getId() == null || api.getReqMethod() == null) throw new com.datamaster.common.exception.ServiceException("API映射身份或请求方式缺失");
        return buildMappingKey(api.getReqMethod().toUpperCase(), getRequestPath(api.getApiVersion(), api.getApiUrl()));
    }

    private RequestMappingInfo getRequestMapping(ServiceApiDO api) {
        String verb = RequestMethodEnum.getValByKey(api.getReqMethod());
        if (verb == null) throw new com.datamaster.common.exception.ServiceException("API仅支持已声明的GET/POST请求方式");
        return RequestMappingInfo.paths(getRequestPath(api.getApiVersion(), api.getApiUrl()))
                .methods(RequestMethod.valueOf(verb.toUpperCase()))
                .options(requestMappingHandlerMapping.getBuilderConfiguration())
                .build();
    }

    /**
     * 调用接口 /services/v1.0.0/user/1
     * @param version
     * @param path
     * @return
     */
    private String getRequestPath(String version, String path) {
        if (path == null || path.isBlank()) throw new com.datamaster.common.exception.ServiceException("API路径不能为空");
        String effectiveVersion = version == null || version.isBlank() ? prefix : version;
        return separator + ignore + separator + effectiveVersion + (path.startsWith(separator) ? path : (separator + path));
    }
}
