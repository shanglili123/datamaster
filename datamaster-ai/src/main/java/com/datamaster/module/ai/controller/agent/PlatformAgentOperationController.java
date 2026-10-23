package com.datamaster.module.ai.controller.agent;

import com.datamaster.common.core.domain.CommonResult;
import com.datamaster.common.core.controller.BaseController;
import com.datamaster.common.exception.ServiceException;
import com.datamaster.common.api.space.ISpaceApi;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import javax.annotation.Resource;
import java.util.List;

/** 数据智能体平台运营流程入口。 */
@Tag(name = "数据智能体运营流程")
@RestController
@RequestMapping("/ai/agent/operation")
@PreAuthorize("@ss.hasPermi('ai:ask-data:query')")
public class PlatformAgentOperationController extends BaseController {

    @Resource
    private PlatformAgentOperationService operationService;
    @Resource
    private ISpaceApi spaceApi;

    @Operation(summary = "启动数据智能体自动运营流程")
    @PostMapping("/start")
    public CommonResult<PlatformAgentOperationService.OperationState> start(@RequestBody PlatformAgentOperationReqVO request) {
        return CommonResult.success(operationService.start(request, getUserId(), getNickName()));
    }

    @Operation(summary = "补充连接信息并继续自动运营流程")
    @PostMapping("/{id}/resume")
    public CommonResult<PlatformAgentOperationService.OperationState> resume(
            @PathVariable String id, @RequestBody PlatformAgentOperationReqVO request) {
        assertOwner(id);
        return CommonResult.success(operationService.resume(id, request, getUserId(), getNickName()));
    }

    @Operation(summary = "查询数据智能体运营流程状态")
    @GetMapping("/{id}")
    public CommonResult<PlatformAgentOperationService.OperationState> get(@PathVariable String id) {
        assertOwner(id);
        PlatformAgentOperationService.OperationState state = operationService.get(id);
        if (state == null) return CommonResult.error(404, "流程实例不存在");
        return CommonResult.success(state);
    }

    @Operation(summary = "查询当前空间的数据智能体流程任务")
    @GetMapping("/page")
    public CommonResult<List<PlatformAgentOperationService.OperationState>> page(
            @RequestParam(required = false) Long spaceId,
            @RequestParam(required = false) String spaceCode,
            @RequestParam(defaultValue = "20") Integer limit) {
        return CommonResult.success(operationService.listLatest(spaceId, spaceCode, limit == null ? 20 : limit, getUserId()));
    }

    @Operation(summary = "从失败步骤重试数据智能体运营流程")
    @PostMapping("/{id}/retry")
    public CommonResult<PlatformAgentOperationService.OperationState> retry(@PathVariable String id) {
        assertOwner(id);
        return CommonResult.success(operationService.retry(id, getUserId(), getNickName()));
    }

    @Operation(summary = "确认发布并执行元数据探查任务")
    @PostMapping("/{id}/confirm-metadata")
    public CommonResult<PlatformAgentOperationService.OperationState> confirmMetadata(@PathVariable String id) {
        assertOwner(id);
        return CommonResult.success(operationService.confirmMetadataTask(id, getUserId(), getNickName()));
    }

    @Operation(summary = "取消数据智能体运营流程")
    @PostMapping("/{id}/cancel")
    public CommonResult<PlatformAgentOperationService.OperationState> cancel(@PathVariable String id) {
        assertOwner(id);
        return CommonResult.success(operationService.cancel(id, getUserId()));
    }

    private void assertOwner(String id) {
        if (!operationService.belongsTo(id, getUserId(), null, null)) {
            throw new ServiceException("流程不属于当前用户");
        }
        PlatformAgentOperationService.OperationState state = operationService.get(id);
        if (state != null && (state.getSpaceId() != null || state.getSpaceCode() != null)
                && (spaceApi == null || !spaceApi.hasUserSpace(getUserId(), state.getSpaceId(), state.getSpaceCode()))) {
            throw new ServiceException("当前用户无权访问该空间");
        }
    }
}
