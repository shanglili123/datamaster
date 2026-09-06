package com.datamaster.ingestion.controller;

import com.datamaster.common.core.domain.CommonResult;
import com.datamaster.ingestion.config.IngestionProperties;
import com.datamaster.ingestion.model.IngestionResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * 数据接收工程管理 Controller
 */
@Tag(name = "数据接收管理")
@RestController
@RequestMapping("/ingestion")
public class IngestionController {

    @Resource
    private IngestionProperties properties;

    @Operation(summary = "获取接收配置")
    @GetMapping("/config")
    public CommonResult<IngestionProperties> config() {
        return CommonResult.success(properties);
    }

    @Operation(summary = "接收工程启停状态")
    @GetMapping("/status")
    public CommonResult<Boolean> status() {
        return CommonResult.success(properties.isEnabled());
    }

    @Operation(summary = "冲突策略清单")
    @GetMapping("/conflict-strategies")
    public CommonResult<String[]> conflictStrategies() {
        return CommonResult.success(
                new String[]{IngestionResult.Action.INSERT.name(),
                        IngestionResult.Action.UPSERT.name(),
                        IngestionResult.Action.SKIP.name(),
                        IngestionResult.Action.ERROR.name()});
    }
}