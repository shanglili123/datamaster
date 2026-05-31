

package com.datamaster.module.standards.controller.admin.desensitizeRules;

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
import com.datamaster.module.standards.controller.admin.desensitizeRules.vo.StandardsDesensitizeRulePageReqVO;
import com.datamaster.module.standards.controller.admin.desensitizeRules.vo.StandardsDesensitizeRuleRespVO;
import com.datamaster.module.standards.controller.admin.desensitizeRules.vo.StandardsDesensitizeRuleSaveReqVO;
import com.datamaster.module.standards.convert.desensitizeRules.StandardsDesensitizeRuleConvert;
import com.datamaster.module.standards.dal.dataobject.desensitizeRules.StandardsDesensitizeRuleDO;
import com.datamaster.module.standards.service.desensitizeRules.IStandardsDesensitizeRuleService;

/**
 * 脱敏规则Controller
 *
 * @author DATAMASTER
 * @date 2026-04-10
 */
@Tag(name = "脱敏规则")
@RestController
@RequestMapping("/dg/desensitizeRules")
@Validated
public class StandardsDesensitizeRuleController extends BaseController {
    @Resource
    private IStandardsDesensitizeRuleService StandardsDesensitizeRuleService;

    @Operation(summary = "查询脱敏规则列表")
    @PreAuthorize("@ss.hasPermi('dg:desensitizerules:list')")
    @GetMapping("/list")
    public CommonResult<PageResult<StandardsDesensitizeRuleRespVO>> list(StandardsDesensitizeRulePageReqVO StandardsDesensitizeRule) {
        PageResult<StandardsDesensitizeRuleDO> page = StandardsDesensitizeRuleService.getDgDesensitizeRulePage(StandardsDesensitizeRule);
        return CommonResult.success(BeanUtils.toBean(page, StandardsDesensitizeRuleRespVO.class));
    }

    @Operation(summary = "导出脱敏规则列表")
    @PreAuthorize("@ss.hasPermi('dg:desensitizerules:export')")
    @Log(title = "脱敏规则", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, StandardsDesensitizeRulePageReqVO exportReqVO) {
        exportReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<StandardsDesensitizeRuleDO> list = (List<StandardsDesensitizeRuleDO>) StandardsDesensitizeRuleService.getDgDesensitizeRulePage(exportReqVO).getRows();
        ExcelUtil<StandardsDesensitizeRuleRespVO> util = new ExcelUtil<>(StandardsDesensitizeRuleRespVO.class);
        util.exportExcel(response, StandardsDesensitizeRuleConvert.INSTANCE.convertToRespVOList(list), "应用管理数据");
    }

    @Operation(summary = "导入脱敏规则列表")
    @PreAuthorize("@ss.hasPermi('dg:desensitizerules:import')")
    @Log(title = "脱敏规则", businessType = BusinessType.IMPORT)
    @PostMapping("/importData")
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception {
        ExcelUtil<StandardsDesensitizeRuleRespVO> util = new ExcelUtil<>(StandardsDesensitizeRuleRespVO.class);
        List<StandardsDesensitizeRuleRespVO> importExcelList = util.importExcel(file.getInputStream());
        String operName = getUsername();
        String message = StandardsDesensitizeRuleService.importDgDesensitizeRule(importExcelList, updateSupport, operName);
        return success(message);
    }

    @Operation(summary = "获取脱敏规则详细信息")
    @PreAuthorize("@ss.hasPermi('dg:desensitizerules:query')")
    @GetMapping(value = "/{id}")
    public CommonResult<StandardsDesensitizeRuleRespVO> getInfo(@PathVariable("id") Long id) {
        StandardsDesensitizeRuleDO StandardsDesensitizeRuleDO = StandardsDesensitizeRuleService.getDgDesensitizeRuleById(id);
        return CommonResult.success(BeanUtils.toBean(StandardsDesensitizeRuleDO, StandardsDesensitizeRuleRespVO.class));
    }

    @Operation(summary = "新增脱敏规则")
    @PreAuthorize("@ss.hasPermi('dg:desensitizerules:add')")
    @Log(title = "脱敏规则", businessType = BusinessType.INSERT)
    @PostMapping
    public CommonResult<Long> add(@Valid @RequestBody StandardsDesensitizeRuleSaveReqVO StandardsDesensitizeRule) {
        StandardsDesensitizeRule.setCreatorId(getUserId());
        StandardsDesensitizeRule.setCreateBy(getNickName());
        StandardsDesensitizeRule.setCreateTime(DateUtil.date());
        return CommonResult.toAjax(StandardsDesensitizeRuleService.createDgDesensitizeRule(StandardsDesensitizeRule));
    }

    @Operation(summary = "修改脱敏规则")
    @PreAuthorize("@ss.hasPermi('dg:desensitizerules:edit')")
    @Log(title = "脱敏规则", businessType = BusinessType.UPDATE)
    @PutMapping
    public CommonResult<Integer> edit(@Valid @RequestBody StandardsDesensitizeRuleSaveReqVO StandardsDesensitizeRule) {
        StandardsDesensitizeRule.setUpdatorId(getUserId());
        StandardsDesensitizeRule.setUpdateBy(getNickName());
        StandardsDesensitizeRule.setUpdateTime(DateUtil.date());
        return CommonResult.toAjax(StandardsDesensitizeRuleService.updateDgDesensitizeRule(StandardsDesensitizeRule));
    }

    @Operation(summary = "删除脱敏规则")
    @PreAuthorize("@ss.hasPermi('dg:desensitizerules:remove')")
    @Log(title = "脱敏规则", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public CommonResult<Integer> remove(@PathVariable Long[] ids) {
        return CommonResult.toAjax(StandardsDesensitizeRuleService.removeDgDesensitizeRule(Arrays.asList(ids)));
    }

}
