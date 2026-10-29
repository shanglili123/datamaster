package com.datamaster.module.collector.controller.admin.etl;

import com.datamaster.module.collector.service.etl.impl.CollectorEtlStageRuntimeService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import javax.annotation.Resource;

/** 作业JSON含连接配置；仅接受该任务当前发布版本的随机Token。 */
@RestController
@RequestMapping("/col/etlTask/stage-incremental")
public class CollectorEtlStageRuntimeController {
    @Resource private CollectorEtlStageRuntimeService stageService;
    @Resource private com.datamaster.module.collector.service.etl.impl.CollectorEtlCallbackTokenService callbackTokens;
    @Resource private com.datamaster.module.collector.dal.mapper.etl.CollectorEtlRunMapper runMapper;

    @GetMapping(value = "/prepare/{taskId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @com.datamaster.common.annotation.Anonymous
    @org.springframework.transaction.annotation.Transactional(rollbackFor = Exception.class)
    public String prepare(@RequestHeader("X-DataMaster-Task-Token") String token, @PathVariable Long taskId, @RequestParam String stageKey, @RequestParam Long executionId,
                          javax.servlet.http.HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store, private");
        response.setHeader("Pragma", "no-cache");
        response.setHeader("X-Content-Type-Options", "nosniff");
        if (runMapper.lockTask(taskId) == null) throw new com.datamaster.common.exception.ServiceException("任务不存在");
        callbackTokens.requireToken(taskId, token);
        return stageService.prepareExecution(taskId, stageKey, executionId);
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public String denied(Exception exception) {
        return "{\"message\":\"阶段增量准备失败，请核对游标、运行资格和已发布配置\"}";
    }
}
