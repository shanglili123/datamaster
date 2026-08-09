package com.datamaster.module.service.api.service.gateway.api;

import javax.servlet.http.HttpServletResponse;
import java.util.Map;

/**
 * 数据服务-API网关对外服务
 *
 * @author DATAMASTER
 */
public interface IGatewayApiOutService {

    void executeServiceForwarding(HttpServletResponse response, Long apiId, Map<String, Object> params);
}
