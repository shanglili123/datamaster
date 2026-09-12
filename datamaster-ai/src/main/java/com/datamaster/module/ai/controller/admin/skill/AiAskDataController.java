package com.datamaster.module.ai.controller.admin.skill;

import com.datamaster.common.core.domain.CommonResult;
import com.datamaster.module.ai.controller.admin.skill.vo.AiAskDataReportReqVO;
import com.datamaster.module.ai.controller.admin.skill.vo.AiAskDataReportRespVO;
import com.datamaster.module.ai.controller.admin.skill.vo.AiAskDataSqlReqVO;
import com.datamaster.module.ai.controller.admin.skill.vo.AiAskDataSqlRespVO;
import com.datamaster.module.ai.service.skill.IAiAskDataService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import javax.annotation.Resource;
import javax.validation.Valid;

/**
 * AI ask-data controller.
 */
@Tag(name = "决策智能体")
@RestController
@RequestMapping("/ai/ask-data")
@Validated
public class AiAskDataController {

    @Resource
    private IAiAskDataService aiAskDataService;

    @Operation(summary = "决策智能体对话")
    @PreAuthorize("@ss.hasPermi('ai:ask-data:query')")
    @PostMapping("/dbgpt/chat")
    public CommonResult<AiAskDataSqlRespVO> chatWithDbGpt(@Valid @RequestBody AiAskDataSqlReqVO reqVO) {
        return CommonResult.success(aiAskDataService.chatWithDbGpt(reqVO));
    }

    @Operation(summary = "决策智能体流式对话")
    @PreAuthorize("@ss.hasPermi('ai:ask-data:query')")
    @PostMapping(value = "/dbgpt/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chatWithDbGptStream(@Valid @RequestBody AiAskDataSqlReqVO reqVO) {
        return aiAskDataService.chatWithDbGptStream(reqVO);
    }

    @Operation(summary = "决策智能体报告生成")
    @PreAuthorize("@ss.hasPermi('ai:ask-data:query')")
    @PostMapping("/dbgpt/report")
    public CommonResult<AiAskDataReportRespVO> generateReport(@Valid @RequestBody AiAskDataReportReqVO reqVO) {
        return CommonResult.success(aiAskDataService.generateReport(reqVO));
    }
}

