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
    public String prepare(@RequestHeader("X-DataMaster-Task-Token") String token, @PathVariable Long taskId, @RequestParam String stageKey, @RequestParam(required = false) String executionId,
                          @RequestParam(required = false) Long schedulerInstanceId,
                          javax.servlet.http.HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store, private");
        response.setHeader("Pragma", "no-cache");
        response.setHeader("X-Content-Type-Options", "nosniff");
        if (runMapper.lockTask(taskId) == null) throw new com.datamaster.common.exception.ServiceException("任务不存在");
        callbackTokens.requireToken(taskId, token);
        if ((executionId == null) == (schedulerInstanceId == null)) throw new com.datamaster.common.exception.ServiceException("必须明确且仅提供一种运行标识");
        if (schedulerInstanceId != null) {
            com.datamaster.module.collector.dal.dataobject.etl.CollectorEtlRunDO run = runMapper.getByWorkflowInstance(taskId, schedulerInstanceId);
            if (run == null) throw new com.datamaster.common.exception.ServiceException("调度实例尚未通过准入");
            return stageService.prepareExecution(taskId, stageKey, run.getId());
        }
        return stageService.prepareExecution(taskId, stageKey, com.datamaster.module.collector.utils.SchedulerHttpResponse.executionId(executionId));
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public String denied(Exception exception) {
        String reason = exception instanceof com.datamaster.common.exception.ServiceException ? exception.getMessage() : "非业务异常";
        java.util.Set<String> safe = java.util.Set.of("本地执行记录不属于当前任务运行", "工作流尚未通过运行准入或已失去运行资格", "当前阶段的准备节点未在运行，禁止提前查询下游增量边界", "调度实例尚未通过准入", "阶段执行配置已变化，需要显式迁移水位", "阶段固定增量窗口未启用");
        org.slf4j.LoggerFactory.getLogger(getClass()).warn("阶段准备拒绝 type={} reason={}", exception.getClass().getSimpleName(), reason != null && safe.contains(reason) ? reason : "其他校验或内部错误");
        return "{\"message\":\"阶段增量准备失败，请核对游标、运行资格和已发布配置\"}";
    }
}
