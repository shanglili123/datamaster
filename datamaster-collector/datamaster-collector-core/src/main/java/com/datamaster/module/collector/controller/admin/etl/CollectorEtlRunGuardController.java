package com.datamaster.module.collector.controller.admin.etl;

import com.datamaster.common.exception.ServiceException;
import com.datamaster.module.collector.dal.dataobject.etl.CollectorEtlTaskDO;
import com.datamaster.module.collector.dal.mapper.etl.CollectorEtlTaskMapper;
import com.datamaster.module.collector.service.etl.impl.CollectorEtlRunGuardService;
import com.datamaster.module.collector.utils.RunAdmissionDiagnostics;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import javax.annotation.Resource;

/** 独立任务Token认证；不要求调度器持有用户登录凭据。 */
@RestController
@RequestMapping("/col/etlTask/run-guard")
public class CollectorEtlRunGuardController {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(CollectorEtlRunGuardController.class);
    @Resource private CollectorEtlTaskMapper taskMapper;
    @Resource private CollectorEtlRunGuardService runGuardService;
    @Resource private com.datamaster.module.collector.service.etl.impl.CollectorEtlCallbackTokenService callbackTokens;
    @Resource private com.datamaster.module.collector.dal.mapper.etl.CollectorEtlRunMapper runMapper;
    @Value("${ds.run_guard.enabled:false}") private boolean enabled;

    @PostMapping("/prepare/{taskId}")
    @com.datamaster.common.annotation.Anonymous
    @org.springframework.transaction.annotation.Transactional(rollbackFor = Exception.class)
    public String prepare(@RequestHeader("X-DataMaster-Task-Token") String token, @PathVariable Long taskId) {
        RunAdmissionDiagnostics diagnostics = new RunAdmissionDiagnostics(taskId);
        return diagnostics.execute(() -> {
            diagnostics.step("FEATURE_CHECK");
            if (!enabled) throw new ServiceException("运行保护未启用");
            diagnostics.step("TASK_LOOKUP");
            CollectorEtlTaskDO task = taskMapper.selectById(taskId);
            if (task == null) throw new ServiceException("任务不存在");
            diagnostics.step("TASK_LOCK");
            if (runMapper.lockTask(taskId) == null) throw new ServiceException("任务不存在");
            diagnostics.step("TOKEN_CHECK");
            callbackTokens.requireToken(taskId, token);
            com.datamaster.module.collector.dal.dataobject.etl.CollectorEtlRunDO run =
                    runGuardService.prepareLatestComponent(taskId, "[]", diagnostics);
            return String.valueOf(run.getId());
        });
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> denied(Exception exception) {
        RunAdmissionDiagnostics.Failure failure = RunAdmissionDiagnostics.failure(exception);
        if (failure.isBusinessRejection()) {
            log.warn("运行准入拒绝 diagnosticId={} taskId={} schedulerInstanceId={} executionId={} step={} reason={} observed={}",
                    failure.getDiagnosticId(), failure.getTaskId(), failure.getSchedulerInstanceId(),
                    failure.getExecutionId(), failure.getStep(), failure.getReason(), failure.getObservation());
        } else {
            log.error("运行准入内部异常 diagnosticId={} taskId={} schedulerInstanceId={} executionId={} step={} reason={} observed={}",
                    failure.getDiagnosticId(), failure.getTaskId(), failure.getSchedulerInstanceId(),
                    failure.getExecutionId(), failure.getStep(), failure.getReason(), failure.getObservation(), failure);
        }
        return ResponseEntity.status(failure.isRetryable() ? 503 : failure.isBusinessRejection() ? 409 : 500)
                .header("Cache-Control", "no-store, private")
                .header("X-DataMaster-Admission-Retryable", Boolean.toString(failure.isRetryable()))
                .header("X-DataMaster-Diagnostic-Id", failure.getDiagnosticId())
                .body("运行准入未通过，诊断编号=" + failure.getDiagnosticId());
    }

}
