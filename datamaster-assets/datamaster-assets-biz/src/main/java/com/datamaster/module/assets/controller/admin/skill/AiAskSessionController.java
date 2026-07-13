package com.datamaster.module.assets.controller.admin.skill;

import com.datamaster.common.core.controller.BaseController;
import com.datamaster.common.core.domain.CommonResult;
import com.datamaster.module.assets.controller.admin.skill.vo.AiAskMessageRespVO;
import com.datamaster.module.assets.controller.admin.skill.vo.AiAskMessageSaveReqVO;
import com.datamaster.module.assets.controller.admin.skill.vo.AiAskMessageWindowRespVO;
import com.datamaster.module.assets.controller.admin.skill.vo.AiAskSessionRespVO;
import com.datamaster.module.assets.controller.admin.skill.vo.AiAskSessionSaveReqVO;
import com.datamaster.module.assets.service.skill.IAiAskSessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

@Tag(name = "AI问数会话")
@RestController
@RequestMapping("/ai/ask-session")
@Validated
public class AiAskSessionController extends BaseController {

    @Resource
    private IAiAskSessionService aiAskSessionService;

    @Operation(summary = "查询最近AI问数会话")
    @PreAuthorize("@ss.hasPermi('ai:ask-data:query')")
    @GetMapping
    public CommonResult<List<AiAskSessionRespVO>> list(@RequestParam(required = false) Long projectId,
                                                       @RequestParam(defaultValue = "10") Integer limit) {
        return CommonResult.success(aiAskSessionService.listRecent(getUserId(), projectId, limit));
    }

    @Operation(summary = "新增AI问数会话")
    @PreAuthorize("@ss.hasPermi('ai:ask-data:query')")
    @PostMapping
    public CommonResult<AiAskSessionRespVO> create(@RequestBody AiAskSessionSaveReqVO reqVO) {
        return CommonResult.success(aiAskSessionService.create(getUserId(), getUsername(), reqVO));
    }

    @Operation(summary = "修改AI问数会话选择")
    @PreAuthorize("@ss.hasPermi('ai:ask-data:query')")
    @PutMapping("/{sessionId}")
    public CommonResult<AiAskSessionRespVO> update(@PathVariable Long sessionId,
                                                   @RequestBody AiAskSessionSaveReqVO reqVO) {
        return CommonResult.success(aiAskSessionService.update(getUserId(), getUsername(), sessionId, reqVO));
    }

    @Operation(summary = "删除AI问数会话")
    @PreAuthorize("@ss.hasPermi('ai:ask-data:query')")
    @DeleteMapping("/{sessionId}")
    public CommonResult<Integer> delete(@PathVariable Long sessionId,
                                        @RequestParam(required = false) Long projectId) {
        return CommonResult.toAjax(aiAskSessionService.delete(getUserId(), projectId, sessionId));
    }

    @Operation(summary = "游标查询AI问数消息")
    @PreAuthorize("@ss.hasPermi('ai:ask-data:query')")
    @GetMapping("/{sessionId}/messages")
    public CommonResult<AiAskMessageWindowRespVO> messages(@PathVariable Long sessionId,
                                                           @RequestParam(required = false) Long projectId,
                                                           @RequestParam(required = false) Long beforeId,
                                                           @RequestParam(required = false) Long afterId,
                                                           @RequestParam(defaultValue = "10") Integer limit) {
        return CommonResult.success(aiAskSessionService.listMessages(
                getUserId(), projectId, sessionId, beforeId, afterId, limit));
    }

    @Operation(summary = "新增AI问数消息")
    @PreAuthorize("@ss.hasPermi('ai:ask-data:query')")
    @PostMapping("/{sessionId}/messages")
    public CommonResult<AiAskMessageRespVO> appendMessage(@PathVariable Long sessionId,
                                                          @RequestParam(required = false) Long projectId,
                                                          @RequestBody AiAskMessageSaveReqVO reqVO) {
        return CommonResult.success(aiAskSessionService.appendMessage(
                getUserId(), getUsername(), projectId, sessionId, reqVO));
    }

    @Operation(summary = "删除AI问数消息")
    @PreAuthorize("@ss.hasPermi('ai:ask-data:query')")
    @DeleteMapping("/{sessionId}/messages/{messageId}")
    public CommonResult<Integer> deleteMessage(@PathVariable Long sessionId,
                                               @PathVariable Long messageId,
                                               @RequestParam(required = false) Long projectId) {
        return CommonResult.toAjax(aiAskSessionService.deleteMessage(getUserId(), projectId, sessionId, messageId));
    }

    @Operation(summary = "清空AI问数会话消息")
    @PreAuthorize("@ss.hasPermi('ai:ask-data:query')")
    @DeleteMapping("/{sessionId}/messages")
    public CommonResult<Integer> clearMessages(@PathVariable Long sessionId,
                                               @RequestParam(required = false) Long projectId) {
        return CommonResult.success(aiAskSessionService.clearMessages(getUserId(), projectId, sessionId));
    }
}
