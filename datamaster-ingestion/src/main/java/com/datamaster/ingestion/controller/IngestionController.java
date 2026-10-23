package com.datamaster.ingestion.controller;

import com.datamaster.common.core.domain.CommonResult;
import com.datamaster.common.annotation.Log;
import com.datamaster.common.enums.BusinessType;
import com.datamaster.ingestion.config.IngestionProperties;
import com.datamaster.ingestion.controller.vo.IngestionConfigRespVO;
import com.datamaster.ingestion.controller.vo.IngestionRuntimeStatusRespVO;
import com.datamaster.ingestion.dal.dataobject.IngestionLogDO;
import com.datamaster.ingestion.kafka.IngestionKafkaConsumer;
import com.datamaster.ingestion.model.IngestionResult;
import com.datamaster.ingestion.service.IngestionDeadLetterService;
import com.datamaster.ingestion.service.IngestionMetricsService;
import com.datamaster.ingestion.controller.vo.IngestionDeadLetterReplayReq;
import com.datamaster.ingestion.controller.vo.IngestionDeadLetterBatchReplayReq;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import javax.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.List;

/**
 * 数据接收工程管理 Controller
 */
@Tag(name = "数据接收管理")
@RestController
@RequestMapping("/ingestion")
public class IngestionController {

    @Resource
    private IngestionProperties properties;

    @Resource
    private IngestionDeadLetterService deadLetterService;

    @Resource
    private IngestionMetricsService metricsService;

    /** ingestion 关闭时消费者 Bean 不存在，接口仍应可返回配置状态。 */
    @Autowired(required = false)
    private IngestionKafkaConsumer kafkaConsumer;

    @Operation(summary = "获取接收配置")
    @PreAuthorize("@ss.hasPermi('ingestion:query')")
    @GetMapping("/config")
    public CommonResult<IngestionConfigRespVO> config() {
        return CommonResult.success(IngestionConfigRespVO.from(properties));
    }

    @Operation(summary = "接收工程启停状态")
    @PreAuthorize("@ss.hasPermi('ingestion:query')")
    @GetMapping("/status")
    public CommonResult<Boolean> status() {
        return CommonResult.success(properties.isEnabled());
    }

    @Operation(summary = "获取数据接入消费者运行状态")
    @PreAuthorize("@ss.hasPermi('ingestion:query')")
    @GetMapping("/runtime")
    public CommonResult<IngestionRuntimeStatusRespVO> runtime() {
        if (kafkaConsumer == null) {
            IngestionRuntimeStatusRespVO result = new IngestionRuntimeStatusRespVO();
            result.setState(properties.isEnabled() ? "NOT_STARTED" : "DISABLED");
            result.setConfiguredEnabled(properties.isEnabled());
            result.setActive(false);
            result.setMessage(properties.isEnabled()
                    ? "消费者未装配，通常是 ingestion 条件未满足或应用未完成启动"
                    : "ingestion 未启用");
            return CommonResult.success(result);
        }
        return CommonResult.success(kafkaConsumer.runtimeStatus());
    }

    @Operation(summary = "暂停数据接入消费")
    @PreAuthorize("@ss.hasPermi('ingestion:control')")
    @Log(title = "数据接入消费", businessType = BusinessType.UPDATE)
    @PostMapping("/pause")
    public CommonResult<IngestionRuntimeStatusRespVO> pause() {
        if (kafkaConsumer == null) {
            return CommonResult.error(409, properties.isEnabled() ? "消费者尚未启动" : "ingestion 未启用");
        }
        if (!kafkaConsumer.requestPause()) {
            return CommonResult.error(409, "消费者当前状态不可暂停");
        }
        return CommonResult.success(kafkaConsumer.runtimeStatus());
    }

    @Operation(summary = "恢复数据接入消费")
    @PreAuthorize("@ss.hasPermi('ingestion:control')")
    @Log(title = "数据接入消费", businessType = BusinessType.UPDATE)
    @PostMapping("/resume")
    public CommonResult<IngestionRuntimeStatusRespVO> resume() {
        if (kafkaConsumer == null) {
            return CommonResult.error(409, properties.isEnabled() ? "消费者尚未启动" : "ingestion 未启用");
        }
        if (!kafkaConsumer.requestResume()) {
            return CommonResult.error(409, "消费者当前状态不可恢复");
        }
        return CommonResult.success(kafkaConsumer.runtimeStatus());
    }

    @Operation(summary = "查询数据接入运行指标")
    @PreAuthorize("@ss.hasPermi('ingestion:query')")
    @GetMapping("/metrics")
    public CommonResult<java.util.Map<String, Object>> metrics() {
        return CommonResult.success(metricsService.snapshot());
    }

    @Operation(summary = "冲突策略清单")
    @PreAuthorize("@ss.hasPermi('ingestion:query')")
    @GetMapping("/conflict-strategies")
    public CommonResult<String[]> conflictStrategies() {
        return CommonResult.success(
                new String[]{IngestionResult.Action.INSERT.name(),
                        IngestionResult.Action.UPSERT.name(),
                        IngestionResult.Action.SKIP.name(),
                        IngestionResult.Action.ERROR.name()});
    }

    @Operation(summary = "查询数据接入死信")
    @PreAuthorize("@ss.hasPermi('ingestion:query')")
    @GetMapping("/dead-letters")
    public CommonResult<List<IngestionLogDO>> deadLetters(
            @RequestParam(value = "limit", required = false, defaultValue = "50") Integer limit,
            @RequestParam(value = "topic", required = false) String topic,
            @RequestParam(value = "targetTable", required = false) String targetTable) {
        return CommonResult.success(deadLetterService.list(limit, topic, targetTable));
    }

    @Operation(summary = "人工重放数据接入死信")
    @PreAuthorize("@ss.hasPermi('ingestion:replay')")
    @Log(title = "数据接入死信", businessType = BusinessType.UPDATE)
    @PostMapping("/dead-letters/replay")
    public CommonResult<Boolean> replayDeadLetter(@RequestBody IngestionDeadLetterReplayReq request) {
        return CommonResult.success(deadLetterService.replay(request == null ? null : request.getLogId()));
    }

    @Operation(summary = "批量人工重放数据接入死信")
    @PreAuthorize("@ss.hasPermi('ingestion:replay')")
    @Log(title = "数据接入死信", businessType = BusinessType.UPDATE)
    @PostMapping("/dead-letters/replay-batch")
    public CommonResult<Integer> replayDeadLetters(@RequestBody IngestionDeadLetterBatchReplayReq request) {
        return CommonResult.success(deadLetterService.replayBatch(request == null ? null : request.getLogIds()));
    }
}
