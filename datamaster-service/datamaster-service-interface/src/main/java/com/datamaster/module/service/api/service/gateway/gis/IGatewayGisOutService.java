package com.datamaster.module.service.api.service.gateway.gis;

import javax.servlet.http.HttpServletResponse;
import java.util.Map;

/**
 * 数据服务-GIS网关对外服务
 *
 * @author DATAMASTER
 */
public interface IGatewayGisOutService {

    void executeServiceForwarding(HttpServletResponse response, Long gisId, Map<String, Object> params);
}
