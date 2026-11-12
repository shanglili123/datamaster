

package com.datamaster.module.service.async;


import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import com.datamaster.module.service.dal.dataobject.api.ServiceApiDO;
import com.datamaster.module.service.dal.dataobject.apiLog.ServiceApiLogDO;
import com.datamaster.module.service.handler.MappingHandlerMapping;
import com.datamaster.module.service.service.api.IServiceApiService;
import com.datamaster.module.service.service.apiLog.IServiceApiLogService;

import javax.annotation.Resource;
import java.util.Map;

/**
 * 异步保存API服务日志
 */
@Slf4j
@Component
public class AsyncTask {

    @Autowired
    private IServiceApiLogService apiLogService;


    @Autowired
    private MappingHandlerMapping mappingHandlerMapping;

    @Lazy
    @Resource
    private IServiceApiService IServiceApiService;

    private static String HANDLER_RELEASE = "1";
    private static String HANDLER_CANCEL = "2";

    /**
     * 异步保存日志
     * @param apiLogDto
     */
    @Async("threadPoolTaskExecutor")
    public void doTask(ServiceApiLogDO apiLogDto) {
        apiLogService.save(apiLogDto);
    }


    @Async("threadPoolTaskExecutor")
    public void releaseOrCancelDataApi(Map<String, Object> map) {
        try {
            String id =(String) map.get("id");
            String type = (String) map.get("type");//0:取消 1:发布
            ServiceApiDO ServiceApiById = IServiceApiService.getServiceApiById(Long.valueOf(id));
            if (!HANDLER_RELEASE.equals(type) && !HANDLER_CANCEL.equals(type)) return;
            Object original = map.get("api");
            ServiceApiDO route = original instanceof ServiceApiDO ? (ServiceApiDO) original : ServiceApiById;
            // Messages may arrive out of order. Reconcile to current committed state, not stale command intent.
            if (route != null && (ServiceApiById == null || !java.util.Objects.equals(route.getApiUrl(), ServiceApiById.getApiUrl())
                    || !java.util.Objects.equals(route.getApiVersion(), ServiceApiById.getApiVersion()) || !java.util.Objects.equals(route.getReqMethod(), ServiceApiById.getReqMethod()))) mappingHandlerMapping.unregisterMapping(route);
            if (ServiceApiById != null && "1".equals(ServiceApiById.getStatus()) && !Boolean.FALSE.equals(ServiceApiById.getValidFlag())
                    && !Boolean.TRUE.equals(ServiceApiById.getDelFlag())) mappingHandlerMapping.registerMapping(ServiceApiById);
            else if (route != null && ServiceApiById != null) mappingHandlerMapping.unregisterMapping(route);
        } catch (Exception error) {
            log.error("API映射对账失败，apiId={}，请核对当前发布状态", map.get("id"));
        }
    }
}
