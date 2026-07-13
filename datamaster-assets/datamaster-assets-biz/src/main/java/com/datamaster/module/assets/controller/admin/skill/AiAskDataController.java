package com.datamaster.module.assets.controller.admin.skill;

import com.datamaster.common.core.domain.CommonResult;
import com.datamaster.module.assets.controller.admin.skill.vo.AiAskDataPrepareReqVO;
import com.datamaster.module.assets.controller.admin.skill.vo.AiAskDataPrepareRespVO;
import com.datamaster.module.assets.controller.admin.skill.vo.AiAskDataReportReqVO;
import com.datamaster.module.assets.controller.admin.skill.vo.AiAskDataReportRespVO;
import com.datamaster.module.assets.controller.admin.skill.vo.AiAskDataSqlReqVO;
import com.datamaster.module.assets.controller.admin.skill.vo.AiAskDataSqlRespVO;
import com.datamaster.module.assets.service.skill.IAiAskDataService;
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
@Tag(name = "AI问数")
@RestController
@RequestMapping("/ai/ask-data")
@Validated
public class AiAskDataController {

    @Resource
    private IAiAskDataService aiAskDataService;

    @Operation(summary = "准备问数上下文")
    @PreAuthorize("@ss.hasPermi('ai:ask-data:query')")
    @PostMapping("/prepare")
    public CommonResult<AiAskDataPrepareRespVO> prepare(@RequestBody AiAskDataPrepareReqVO reqVO) {
        return CommonResult.success(aiAskDataService.prepare(reqVO));
    }

    @Operation(summary = "生成SQL")
    @PreAuthorize("@ss.hasPermi('ai:ask-data:query')")
    @PostMapping("/sql")
    public CommonResult<AiAskDataSqlRespVO> generateSql(@Valid @RequestBody AiAskDataSqlReqVO reqVO) {
        return CommonResult.success(aiAskDataService.generateSql(reqVO));
    }

    @Operation(summary = "一站式问数对话")
    @PreAuthorize("@ss.hasPermi('ai:ask-data:query')")
    @PostMapping("/chat")
    public CommonResult<AiAskDataSqlRespVO> chat(@Valid @RequestBody AiAskDataSqlReqVO reqVO) {
        return CommonResult.success(aiAskDataService.chat(reqVO));
    }

    @Operation(summary = "AI问数对话")
    @PreAuthorize("@ss.hasPermi('ai:ask-data:query')")
    @PostMapping("/dbgpt/chat")
    public CommonResult<AiAskDataSqlRespVO> chatWithDbGpt(@Valid @RequestBody AiAskDataSqlReqVO reqVO) {
        return CommonResult.success(aiAskDataService.chatWithDbGpt(reqVO));
    }

    @Operation(summary = "AI问数流式对话")
    @PreAuthorize("@ss.hasPermi('ai:ask-data:query')")
    @PostMapping(value = "/dbgpt/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chatWithDbGptStream(@Valid @RequestBody AiAskDataSqlReqVO reqVO) {
        return aiAskDataService.chatWithDbGptStream(reqVO);
    }

    @Operation(summary = "AI问数报告生成")
    @PreAuthorize("@ss.hasPermi('ai:ask-data:query')")
    @PostMapping("/dbgpt/report")
    public CommonResult<AiAskDataReportRespVO> generateReport(@Valid @RequestBody AiAskDataReportReqVO reqVO) {
        return CommonResult.success(aiAskDataService.generateReport(reqVO));
    }
}
