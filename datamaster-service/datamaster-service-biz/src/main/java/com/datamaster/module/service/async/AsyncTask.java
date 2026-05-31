

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
            if (ServiceApiById != null) {
                if (HANDLER_RELEASE.equals(type)) {
                    mappingHandlerMapping.registerMapping(ServiceApiById);
                } else if (HANDLER_CANCEL.equals(type)) {
                    mappingHandlerMapping.unregisterMapping(ServiceApiById);
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
