

package com.datamaster.module.taxonomy.controller.admin.rule;

import cn.hutool.core.date.DateUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.datamaster.common.annotation.Log;
import com.datamaster.common.core.controller.BaseController;
import com.datamaster.common.core.domain.AjaxResult;
import com.datamaster.common.core.domain.CommonResult;
import com.datamaster.common.core.page.PageParam;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.common.enums.BusinessType;
import com.datamaster.common.utils.object.BeanUtils;
import com.datamaster.common.utils.poi.ExcelUtil;
import com.datamaster.module.taxonomy.controller.admin.rule.vo.TaxonomyAuditRulePageReqVO;
import com.datamaster.module.taxonomy.controller.admin.rule.vo.TaxonomyAuditRuleRespVO;
import com.datamaster.module.taxonomy.controller.admin.rule.vo.TaxonomyAuditRuleSaveReqVO;
import com.datamaster.module.taxonomy.convert.rule.TaxonomyAuditRuleConvert;
import com.datamaster.module.taxonomy.dal.dataobject.rule.TaxonomyAuditRuleDO;
import com.datamaster.module.taxonomy.service.rule.ITaxonomyAuditRuleService;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.util.Arrays;
import java.util.List;

/**
 * 稽查规则Controller
 *
 * @author DATAMASTER
 * @date 2025-01-20
 */
@Tag(name = "稽查规则")
@RestController
@RequestMapping("/att/auditRule")
@Validated
public class TaxonomyAuditRuleController extends BaseController {
    @Resource
    private ITaxonomyAuditRuleService TaxonomyAuditRuleService;

    @Operation(summary = "查询稽查规则列表")
    @PreAuthorize("@ss.hasPermi('att:auditRule:list')")
    @GetMapping("/list")
    public CommonResult<PageResult<TaxonomyAuditRuleRespVO>> list(TaxonomyAuditRulePageReqVO TaxonomyAuditRule) {
        PageResult<TaxonomyAuditRuleDO> page = TaxonomyAuditRuleService.getAttAuditRulePage(TaxonomyAuditRule);
        return CommonResult.success(BeanUtils.toBean(page, TaxonomyAuditRuleRespVO.class));
    }

    @Operation(summary = "导出稽查规则列表")
    @PreAuthorize("@ss.hasPermi('att:auditRule:export')")
    @Log(title = "稽查规则", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, TaxonomyAuditRulePageReqVO exportReqVO) {
        exportReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<TaxonomyAuditRuleDO> list = (List<TaxonomyAuditRuleDO>) TaxonomyAuditRuleService.getAttAuditRulePage(exportReqVO)
                .getRows();
        ExcelUtil<TaxonomyAuditRuleRespVO> util = new ExcelUtil<>(TaxonomyAuditRuleRespVO.class);
        util.exportExcel(response, TaxonomyAuditRuleConvert.INSTANCE.convertToRespVOList(list), "应用管理数据");
    }

    @Operation(summary = "导入稽查规则列表")
    @PreAuthorize("@ss.hasPermi('att:auditRule:import')")
    @Log(title = "稽查规则", businessType = BusinessType.IMPORT)
    @PostMapping("/importData")
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception {
        ExcelUtil<TaxonomyAuditRuleRespVO> util = new ExcelUtil<>(TaxonomyAuditRuleRespVO.class);
        List<TaxonomyAuditRuleRespVO> importExcelList = util.importExcel(file.getInputStream());
        String operName = getUsername();
        String message = TaxonomyAuditRuleService.importAttAuditRule(importExcelList, updateSupport, operName);
        return success(message);
    }

    @Operation(summary = "获取稽查规则详细信息")
    @PreAuthorize("@ss.hasPermi('att:auditRule:query')")
    @GetMapping(value = "/{ID}")
    public CommonResult<TaxonomyAuditRuleRespVO> getInfo(@PathVariable("ID") Long ID) {
        TaxonomyAuditRuleDO TaxonomyAuditRuleDO = TaxonomyAuditRuleService.getAttAuditRuleById(ID);
        return CommonResult.success(BeanUtils.toBean(TaxonomyAuditRuleDO, TaxonomyAuditRuleRespVO.class));
    }

    @Operation(summary = "新增稽查规则")
    @PreAuthorize("@ss.hasPermi('att:auditRule:add')")
    @Log(title = "稽查规则", businessType = BusinessType.INSERT)
    @PostMapping
    public CommonResult<Long> add(@Valid @RequestBody TaxonomyAuditRuleSaveReqVO TaxonomyAuditRule) {
        TaxonomyAuditRule.setCreatorId(getUserId());
        TaxonomyAuditRule.setCreateBy(getNickName());
        TaxonomyAuditRule.setCreateTime(DateUtil.date());
        return CommonResult.toAjax(TaxonomyAuditRuleService.createAttAuditRule(TaxonomyAuditRule));
    }

    @Operation(summary = "修改稽查规则")
    @PreAuthorize("@ss.hasPermi('att:auditRule:edit')")
    @Log(title = "稽查规则", businessType = BusinessType.UPDATE)
    @PutMapping
    public CommonResult<Integer> edit(@Valid @RequestBody TaxonomyAuditRuleSaveReqVO TaxonomyAuditRule) {
        TaxonomyAuditRule.setUpdatorId(getUserId());
        TaxonomyAuditRule.setUpdateBy(getNickName());
        TaxonomyAuditRule.setUpdateTime(DateUtil.date());
        return CommonResult.toAjax(TaxonomyAuditRuleService.updateAttAuditRule(TaxonomyAuditRule));
    }

    @Operation(summary = "删除稽查规则")
    @PreAuthorize("@ss.hasPermi('att:auditRule:remove')")
    @Log(title = "稽查规则", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public CommonResult<Integer> remove(@PathVariable Long[] ids) {
        return CommonResult.toAjax(TaxonomyAuditRuleService.removeAttAuditRule(Arrays.asList(ids)));
    }

    @Operation(summary = "获取稽查规则树形结构")
    @GetMapping("/tree")
    public CommonResult<List<TaxonomyAuditRuleRespVO>> tree(@RequestParam Long dataElemId) {
        List<TaxonomyAuditRuleRespVO> tree = TaxonomyAuditRuleService.getAttAuditRuleTree(dataElemId);
        return CommonResult.success(tree);
    }

}
