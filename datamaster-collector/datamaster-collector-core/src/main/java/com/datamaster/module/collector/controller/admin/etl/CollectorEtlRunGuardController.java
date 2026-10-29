package com.datamaster.module.collector.controller.admin.etl;

import com.datamaster.common.exception.ServiceException;
import com.datamaster.module.collector.dal.dataobject.etl.CollectorEtlTaskDO;
import com.datamaster.module.collector.dal.mapper.etl.CollectorEtlTaskMapper;
import com.datamaster.module.collector.service.etl.impl.CollectorEtlRunGuardService;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import javax.annotation.Resource;

/** 独立任务Token认证；不要求调度器持有用户登录凭据。 */
@RestController
@RequestMapping("/col/etlTask/run-guard")
public class CollectorEtlRunGuardController {
    @Resource private CollectorEtlTaskMapper taskMapper;
    @Resource private CollectorEtlRunGuardService runGuardService;
    @Resource private com.datamaster.module.collector.service.etl.impl.CollectorEtlCallbackTokenService callbackTokens;
    @Resource private com.datamaster.module.collector.dal.mapper.etl.CollectorEtlRunMapper runMapper;
    @Value("${ds.run_guard.enabled:false}") private boolean enabled;

    @PostMapping("/prepare/{taskId}")
    @com.datamaster.common.annotation.Anonymous
    @org.springframework.transaction.annotation.Transactional(rollbackFor = Exception.class)
    public String prepare(@RequestHeader("X-DataMaster-Task-Token") String token, @PathVariable Long taskId,
                          @RequestBody PrepareBody body) {
        if (!enabled) throw new ServiceException("运行保护未启用");
        if (body.getResourcesJson() == null || body.getResourcesJson().length() > 262144) {
            throw new ServiceException("运行准入请求体无效");
        }
        CollectorEtlTaskDO task = taskMapper.selectById(taskId);
        if (task == null) throw new ServiceException("任务不存在");
        if (runMapper.lockTask(taskId) == null) throw new ServiceException("任务不存在");
        callbackTokens.requireToken(taskId, token);
        com.datamaster.module.collector.dal.dataobject.etl.CollectorEtlRunDO run = runGuardService.prepareLatestComponent(taskId, body.getResourcesJson());
        return String.valueOf(run.getId());
    }
    @ExceptionHandler(Exception.class)
    @ResponseStatus(org.springframework.http.HttpStatus.CONFLICT)
    public String denied(Exception exception) {
        return "运行准入未通过";
    }

    @Data public static class PrepareBody { private String resourcesJson; }
}
