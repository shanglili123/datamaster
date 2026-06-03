package com.datamaster.module.collector.controller.admin.etl;

import com.datamaster.common.core.domain.AjaxResult;
import com.datamaster.module.collector.service.etl.ICollectorEtlIncrementalService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * Internal callback used by the DS HTTP node before a FLINKX incremental run.
 */
@Slf4j
@RestController
@RequestMapping("/col/etlTask/incremental")
public class CollectorEtlIncrementalController {

    @Resource
    private ICollectorEtlIncrementalService collectorEtlIncrementalService;

    @PutMapping("/prepare/{taskId}")
    public ResponseEntity<?> prepare(@PathVariable Long taskId,
                                     @RequestParam Long processInstanceId) {
        try {
            String runtimeJobJson = collectorEtlIncrementalService.prepareIncrementalTask(taskId, processInstanceId);
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(runtimeJobJson);
        } catch (Exception e) {
            log.error("增量同步边界准备失败，taskId={}", taskId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(AjaxResult.error("增量同步边界准备失败: " + e.getMessage()));
        }
    }

    @PutMapping("/complete/{taskId}")
    public ResponseEntity<AjaxResult> complete(@PathVariable Long taskId,
                                               @RequestParam Long processInstanceId) {
        try {
            collectorEtlIncrementalService.completeIncrementalTask(taskId, processInstanceId);
            return ResponseEntity.ok(AjaxResult.success("FLINKX任务状态回写完成"));
        } catch (Exception e) {
            log.error("FLINKX任务状态回写失败，taskId={}，processInstanceId={}", taskId, processInstanceId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(AjaxResult.error("FLINKX任务状态回写失败: " + e.getMessage()));
        }
    }
}
