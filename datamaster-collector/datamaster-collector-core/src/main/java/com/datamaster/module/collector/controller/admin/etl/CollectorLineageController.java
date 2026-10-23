package com.datamaster.module.collector.controller.admin.etl;

import com.datamaster.common.api.space.ISpaceApi;
import com.datamaster.common.core.domain.CommonResult;
import com.datamaster.common.exception.ServiceException;
import com.datamaster.common.utils.SecurityUtils;
import com.datamaster.module.collector.dal.dataobject.etl.CollectorEtlTaskDO;
import com.datamaster.module.collector.dal.dataobject.etl.CollectorLineageOutboxDO;
import com.datamaster.module.collector.dal.mapper.etl.CollectorEtlTaskMapper;
import com.datamaster.module.collector.dal.mapper.etl.CollectorLineageOutboxMapper;
import com.datamaster.module.collector.service.etl.impl.CollectorLineageSyncService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import javax.annotation.Resource;
import java.util.LinkedHashMap;
import java.util.Map;

/** Explicit, version-checked replay. Does not publish tasks, execute SQL or import Neo4j. */
@RestController
@RequestMapping("/col/etlTask/lineage")
public class CollectorLineageController {
    @Resource private CollectorLineageSyncService syncService;
    @Resource private CollectorEtlTaskMapper tasks;
    @Resource private CollectorLineageOutboxMapper outbox;
    @Resource private ISpaceApi spaces;

    @PreAuthorize("@ss.hasPermi('col:etlTask:edit')")
    @PostMapping("/rebuild/{taskId}")
    public CommonResult<Map<String, Object>> rebuild(@PathVariable Long taskId,
            @RequestParam(defaultValue = "true") boolean dryRun,
            @RequestParam(required = false) Long expectedSequence) {
        if (!syncService.isEnabled()) throw new ServiceException("JDBC血缘未启用，请先审核迁移及配置");
        CollectorEtlTaskDO task = tasks.selectById(taskId);
        if (task == null || !spaces.hasUserSpace(SecurityUtils.getUserId(), task.getSpaceId(), task.getSpaceCode())) {
            throw new ServiceException("任务不存在或不属于当前用户可访问空间");
        }
        CollectorLineageOutboxDO snapshot = outbox.latestSnapshot(taskId);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("taskId", taskId);
        result.put("dryRun", dryRun);
        result.put("sequence", snapshot == null ? null : snapshot.getSequence());
        if (snapshot == null) {
            result.put("status", "SKIPPED");
            result.put("reason", "NO_PUBLISHED_SNAPSHOT");
            return CommonResult.success(result);
        }
        if (!dryRun) {
            if (expectedSequence == null) throw new ServiceException("请先预检，再提交预检返回的sequence");
            syncService.replay(taskId, expectedSequence);
        }
        result.put("status", dryRun ? "READY" : "QUEUED");
        return CommonResult.success(result);
    }
}
