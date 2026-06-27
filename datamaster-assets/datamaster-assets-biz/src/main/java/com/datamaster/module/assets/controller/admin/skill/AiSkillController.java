package com.datamaster.module.assets.controller.admin.skill;

import com.datamaster.common.core.controller.BaseController;
import com.datamaster.common.core.domain.CommonResult;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.assets.controller.admin.skill.vo.AiSkillPageReqVO;
import com.datamaster.module.assets.controller.admin.skill.vo.AiSkillRespVO;
import com.datamaster.module.assets.controller.admin.skill.vo.AiSkillSaveReqVO;
import com.datamaster.module.assets.controller.admin.skill.vo.AiSkillVersionRespVO;
import com.datamaster.module.assets.controller.admin.skill.vo.AiTableSkillGenerateReqVO;
import com.datamaster.module.assets.service.dbgpt.IDbGptSkillSyncService;
import com.datamaster.module.assets.service.skill.IAiSkillService;
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
import javax.validation.Valid;
import java.util.List;

/**
 * AI Skill controller.
 */
@Tag(name = "AI Skill管理")
@RestController
@RequestMapping("/ai/skill")
@Validated
public class AiSkillController extends BaseController {

    @Resource
    private IAiSkillService aiSkillService;
    @Resource
    private IDbGptSkillSyncService dbGptSkillSyncService;

    @Operation(summary = "分页查询Skill")
    @PreAuthorize("@ss.hasPermi('ai:skill:list')")
    @GetMapping("/page")
    public CommonResult<PageResult<AiSkillRespVO>> page(AiSkillPageReqVO reqVO) {
        return CommonResult.success(aiSkillService.getSkillPage(reqVO));
    }

    @Operation(summary = "获取Skill详情")
    @PreAuthorize("@ss.hasPermi('ai:skill:query')")
    @GetMapping("/{id}")
    public CommonResult<AiSkillRespVO> get(@PathVariable Long id) {
        return CommonResult.success(aiSkillService.getSkill(id));
    }

    @Operation(summary = "新增Skill")
    @PreAuthorize("@ss.hasPermi('ai:skill:add')")
    @PostMapping
    public CommonResult<Long> add(@Valid @RequestBody AiSkillSaveReqVO reqVO) {
        reqVO.setStatus(defaultStatus(reqVO.getStatus()));
        return CommonResult.toAjax(aiSkillService.createSkill(reqVO));
    }

    @Operation(summary = "修改Skill")
    @PreAuthorize("@ss.hasPermi('ai:skill:edit')")
    @PutMapping
    public CommonResult<Integer> edit(@Valid @RequestBody AiSkillSaveReqVO reqVO) {
        return CommonResult.toAjax(aiSkillService.updateSkill(reqVO));
    }

    @Operation(summary = "归档Skill")
    @PreAuthorize("@ss.hasPermi('ai:skill:remove')")
    @DeleteMapping("/{id}")
    public CommonResult<Integer> remove(@PathVariable Long id) {
        return CommonResult.toAjax(aiSkillService.removeSkill(id));
    }

    @Operation(summary = "发布Skill")
    @PreAuthorize("@ss.hasPermi('ai:skill:publish')")
    @PostMapping("/{id}/publish")
    public CommonResult<Integer> publish(@PathVariable Long id) {
        return CommonResult.toAjax(aiSkillService.publishSkill(id));
    }

    @Operation(summary = "回滚Skill版本")
    @PreAuthorize("@ss.hasPermi('ai:skill:edit')")
    @PostMapping("/{id}/rollback/{version}")
    public CommonResult<Integer> rollback(@PathVariable Long id, @PathVariable Integer version) {
        return CommonResult.toAjax(aiSkillService.rollbackSkill(id, version));
    }

    @Operation(summary = "查询Skill版本")
    @PreAuthorize("@ss.hasPermi('ai:skill:query')")
    @GetMapping("/{id}/versions")
    public CommonResult<List<AiSkillVersionRespVO>> versions(@PathVariable Long id) {
        return CommonResult.success(aiSkillService.getSkillVersions(id));
    }

    @Operation(summary = "生成元数据平台Skill")
    @PreAuthorize("@ss.hasPermi('ai:skill:generate')")
    @PostMapping("/generate/metadata")
    public CommonResult<AiSkillRespVO> generateMetadata(@RequestParam(required = false) Boolean publish) {
        return CommonResult.success(aiSkillService.generateMetadataSkill(publish));
    }

    @Operation(summary = "生成质量平台Skill")
    @PreAuthorize("@ss.hasPermi('ai:skill:generate')")
    @PostMapping("/generate/quality")
    public CommonResult<AiSkillRespVO> generateQuality(@RequestParam(required = false) Boolean publish) {
        return CommonResult.success(aiSkillService.generateQualitySkill(publish));
    }

    @Operation(summary = "生成表级问数Skill")
    @PreAuthorize("@ss.hasPermi('ai:skill:generate')")
    @PostMapping("/generate/table")
    public CommonResult<AiSkillRespVO> generateTable(@RequestBody AiTableSkillGenerateReqVO reqVO) {
        return CommonResult.success(aiSkillService.generateTableSkill(reqVO));
    }

    @Operation(summary = "生成表级问数Skill")
    @PreAuthorize("@ss.hasPermi('ai:skill:generate')")
    @PostMapping("/generate/table/{assetId}")
    public CommonResult<AiSkillRespVO> generateTableByAssetId(@PathVariable Long assetId,
                                                              @RequestBody(required = false) AiTableSkillGenerateReqVO reqVO) {
        if (reqVO == null) {
            reqVO = new AiTableSkillGenerateReqVO();
        }
        reqVO.setAssetId(assetId);
        return CommonResult.success(aiSkillService.generateTableSkill(reqVO));
    }

    @Operation(summary = "同步全部已发布Skill到AI问数")
    @PreAuthorize("@ss.hasPermi('ai:skill:sync')")
    @PostMapping("/sync/dbgpt")
    public com.datamaster.common.core.domain.AjaxResult syncAllToDbGpt() {
        return dbGptSkillSyncService.syncAllSkills();
    }

    @Operation(summary = "同步指定Skill到AI问数")
    @PreAuthorize("@ss.hasPermi('ai:skill:sync')")
    @PostMapping("/{id}/sync/dbgpt")
    public com.datamaster.common.core.domain.AjaxResult syncOneToDbGpt(@PathVariable Long id) {
        return dbGptSkillSyncService.syncSkillById(id);
    }

    private String defaultStatus(String status) {
        return status == null || status.trim().length() == 0 ? "DRAFT" : status;
    }
}
