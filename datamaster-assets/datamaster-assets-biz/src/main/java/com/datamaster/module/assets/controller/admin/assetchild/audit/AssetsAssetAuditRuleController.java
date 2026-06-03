package com.datamaster.module.assets.controller.admin.assetchild.audit;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.util.Arrays;

import cn.hutool.core.date.DateUtil;

import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.datamaster.common.core.domain.AjaxResult;
import com.datamaster.common.core.page.PageParam;
import com.datamaster.common.annotation.Log;
import com.datamaster.common.core.controller.BaseController;
import com.datamaster.common.core.domain.CommonResult;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.common.enums.BusinessType;
import com.datamaster.common.utils.object.BeanUtils;
import com.datamaster.common.utils.poi.ExcelUtil;
import com.datamaster.common.exception.enums.GlobalErrorCodeConstants;
import com.datamaster.module.assets.controller.admin.assetchild.audit.vo.AssetsAssetAuditRulePageReqVO;
import com.datamaster.module.assets.controller.admin.assetchild.audit.vo.AssetsAssetAuditRuleRespVO;
import com.datamaster.module.assets.controller.admin.assetchild.audit.vo.AssetsAssetAuditRuleSaveReqVO;
import com.datamaster.module.assets.convert.assetchild.audit.AssetsAssetAuditRuleConvert;
import com.datamaster.module.assets.dal.dataobject.assetchild.audit.AssetsAssetAuditRuleDO;
import com.datamaster.module.assets.service.assetchild.audit.IAssetsAssetAuditRuleService;

/**
 * 数据资产质量结果记录Controller * * @author DATAMASTER * @date 2025-05-09
 */
@Tag(name = "数据资产质量结果记录")
@RestController
@RequestMapping("/ast/assetAuditRule")
@Validated
public class AssetsAssetAuditRuleController extends BaseController {
    @Resource
    private IAssetsAssetAuditRuleService AssetsAssetAuditRuleService;

    @Operation(summary = "查询数据资产质量结果记录列表")
    @PreAuthorize("@ss.hasPermi('da:assetAuditRule:list')")
    @GetMapping("/list")
    public CommonResult<PageResult<AssetsAssetAuditRuleRespVO>> list(AssetsAssetAuditRulePageReqVO AssetsAssetAuditRule) {
        PageResult<AssetsAssetAuditRuleDO> page = AssetsAssetAuditRuleService.getAssetAuditRulePage(AssetsAssetAuditRule);
        return CommonResult.success(BeanUtils.toBean(page, AssetsAssetAuditRuleRespVO.class));
    }

    @Operation(summary = "导出数据资产质量结果记录列表")
    @PreAuthorize("@ss.hasPermi('da:assetAuditRule:export')")
    @Log(title = "数据资产质量结果记录", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, AssetsAssetAuditRulePageReqVO exportReqVO) {
        exportReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<AssetsAssetAuditRuleDO> list = (List<AssetsAssetAuditRuleDO>) AssetsAssetAuditRuleService.getAssetAuditRulePage(exportReqVO).getRows();
        ExcelUtil<AssetsAssetAuditRuleRespVO> util = new ExcelUtil<>(AssetsAssetAuditRuleRespVO.class);
        util.exportExcel(response, AssetsAssetAuditRuleConvert.INSTANCE.convertToRespVOList(list), "应用管理数据");
    }

    @Operation(summary = "导入数据资产质量结果记录列表")
    @PreAuthorize("@ss.hasPermi('da:assetAuditRule:import')")
    @Log(title = "数据资产质量结果记录", businessType = BusinessType.IMPORT)
    @PostMapping("/importData")
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception {
        ExcelUtil<AssetsAssetAuditRuleRespVO> util = new ExcelUtil<>(AssetsAssetAuditRuleRespVO.class);
        List<AssetsAssetAuditRuleRespVO> importExcelList = util.importExcel(file.getInputStream());
        String operName = getUsername();
        String message = AssetsAssetAuditRuleService.importAssetAuditRule(importExcelList, updateSupport, operName);
        return success(message);
    }

    @Operation(summary = "获取数据资产质量结果记录详细信息")
    @PreAuthorize("@ss.hasPermi('da:assetAuditRule:query')")
    @GetMapping(value = "/{id}")
    public CommonResult<AssetsAssetAuditRuleRespVO> getInfo(@PathVariable("id") Long id) {
        AssetsAssetAuditRuleDO AssetsAssetAuditRuleDO = AssetsAssetAuditRuleService.getAssetAuditRuleById(id);
        return CommonResult.success(BeanUtils.toBean(AssetsAssetAuditRuleDO, AssetsAssetAuditRuleRespVO.class));
    }

    @Operation(summary = "新增数据资产质量结果记录")
    @PreAuthorize("@ss.hasPermi('da:assetAuditRule:add')")
    @Log(title = "数据资产质量结果记录", businessType = BusinessType.INSERT)
    @PostMapping
    public CommonResult<Long> add(@Valid @RequestBody AssetsAssetAuditRuleSaveReqVO AssetsAssetAuditRule) {
        AssetsAssetAuditRule.setCreatorId(getUserId());
        AssetsAssetAuditRule.setCreateBy(getNickName());
        AssetsAssetAuditRule.setCreateTime(DateUtil.date());
        return CommonResult.toAjax(AssetsAssetAuditRuleService.createAssetAuditRule(AssetsAssetAuditRule));
    }

    @Operation(summary = "修改数据资产质量结果记录")
    @PreAuthorize("@ss.hasPermi('da:assetAuditRule:edit')")
    @Log(title = "数据资产质量结果记录", businessType = BusinessType.UPDATE)
    @PutMapping
    public CommonResult<Integer> edit(@Valid @RequestBody AssetsAssetAuditRuleSaveReqVO AssetsAssetAuditRule) {
        AssetsAssetAuditRule.setUpdatorId(getUserId());
        AssetsAssetAuditRule.setUpdateBy(getNickName());
        AssetsAssetAuditRule.setUpdateTime(DateUtil.date());
        return CommonResult.toAjax(AssetsAssetAuditRuleService.updateAssetAuditRule(AssetsAssetAuditRule));
    }

    @Operation(summary = "删除数据资产质量结果记录")
    @PreAuthorize("@ss.hasPermi('da:assetAuditRule:remove')")
    @Log(title = "数据资产质量结果记录", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public CommonResult<Integer> remove(@PathVariable Long[] ids) {
        return CommonResult.toAjax(AssetsAssetAuditRuleService.removeAssetAuditRule(Arrays.asList(ids)));
    }
}