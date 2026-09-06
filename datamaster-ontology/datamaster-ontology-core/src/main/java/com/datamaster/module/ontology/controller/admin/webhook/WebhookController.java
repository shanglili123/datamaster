package com.datamaster.module.ontology.controller.admin.webhook;

import com.datamaster.common.core.domain.CommonResult;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.ontology.controller.admin.webhook.vo.*;
import com.datamaster.module.ontology.service.IWebhookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.List;

/**
 * 本体 Webhook 回调管理 Controller
 *
 * <p>Webhook 基于「动作/决策执行记录」回调源系统，是回写源系统的关键外部通道。</p>
 */
@Tag(name = "本体 Webhook 回调管理")
@RestController
@RequestMapping("/ont/webhook")
@Validated
public class WebhookController {

    @Resource
    private IWebhookService webhookService;

    @Operation(summary = "分页查询 Webhook 配置")
    @PreAuthorize("@ss.hasPermi('ont:webhook:list')")
    @GetMapping("/page")
    public CommonResult<PageResult<WebhookRespVO>> page(WebhookPageReqVO pageReqVO) {
        return CommonResult.success(webhookService.getWebhookPage(pageReqVO));
    }

    @Operation(summary = "按本体查询 Webhook 列表")
    @PreAuthorize("@ss.hasPermi('ont:webhook:list')")
    @GetMapping("/list")
    public CommonResult<List<WebhookRespVO>> list(@RequestParam Long ontologyId) {
        return CommonResult.success(webhookService.getWebhooksByOntology(ontologyId));
    }

    @Operation(summary = "获取 Webhook 详情")
    @PreAuthorize("@ss.hasPermi('ont:webhook:query')")
    @GetMapping("/{id}")
    public CommonResult<WebhookRespVO> get(@PathVariable Long id) {
        return CommonResult.success(webhookService.getWebhookById(id));
    }

    @Operation(summary = "新增 Webhook")
    @PreAuthorize("@ss.hasPermi('ont:webhook:add')")
    @PostMapping
    public CommonResult<Long> add(@Valid @RequestBody WebhookSaveReqVO saveReqVO) {
        return CommonResult.success(webhookService.createWebhook(saveReqVO));
    }

    @Operation(summary = "修改 Webhook")
    @PreAuthorize("@ss.hasPermi('ont:webhook:edit')")
    @PutMapping
    public CommonResult<Integer> edit(@Valid @RequestBody WebhookSaveReqVO saveReqVO) {
        return CommonResult.success(webhookService.updateWebhook(saveReqVO));
    }

    @Operation(summary = "删除 Webhook")
    @PreAuthorize("@ss.hasPermi('ont:webhook:remove')")
    @DeleteMapping("/{id}")
    public CommonResult<Integer> delete(@PathVariable Long id) {
        return CommonResult.success(webhookService.deleteWebhook(id));
    }

    @Operation(summary = "分页查询回调日志")
    @PreAuthorize("@ss.hasPermi('ont:webhook:list')")
    @GetMapping("/log/page")
    public CommonResult<PageResult<WebhookLogRespVO>> logPage(WebhookLogPageReqVO pageReqVO) {
        return CommonResult.success(webhookService.getWebhookLogPage(pageReqVO));
    }

    @Operation(summary = "清除回调日志")
    @PreAuthorize("@ss.hasPermi('ont:webhook:remove')")
    @DeleteMapping("/log")
    public CommonResult<Integer> clearLogs(@RequestParam(required = false) Long webhookId) {
        return CommonResult.success(webhookService.clearWebhookLogs(webhookId));
    }
}