

package com.datamaster.module.service.config.api;


import cn.hutool.core.collection.CollUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import com.datamaster.common.enums.DataConstant;
import com.datamaster.module.service.dal.dataobject.api.ServiceApiDO;
import com.datamaster.module.service.handler.MappingHandlerMapping;
import com.datamaster.module.service.service.api.IServiceApiService;

import javax.annotation.Resource;
import java.util.List;

@Component
@RequiredArgsConstructor
public class StartedUpRunner implements ApplicationRunner {

    private final ConfigurableApplicationContext context;
    private final Environment environment;

    @Resource
    private IServiceApiService ServiceApiService;

    @Resource
    private MappingHandlerMapping mappingHandlerMapping;

    @Override
    public void run(ApplicationArguments args) {
        if (context.isActive()) {
            // 项目启动时，初始化已发布的接口
            List<ServiceApiDO> list = ServiceApiService.lambdaQuery()
                    .eq(ServiceApiDO::getStatus, DataConstant.ApiState.WAIT.getKey())
                    .list();
            if (CollUtil.isNotEmpty(list)) {
                list.forEach(api -> mappingHandlerMapping.registerMapping(api));
            }
        }
    }
}
