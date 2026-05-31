

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
import com.datamaster.module.taxonomy.controller.admin.rule.vo.TaxonomyCleanRulePageReqVO;
import com.datamaster.module.taxonomy.controller.admin.rule.vo.TaxonomyCleanRuleRespVO;
import com.datamaster.module.taxonomy.controller.admin.rule.vo.TaxonomyCleanRuleSaveReqVO;
import com.datamaster.module.taxonomy.convert.rule.TaxonomyCleanRuleConvert;
import com.datamaster.module.taxonomy.dal.dataobject.rule.TaxonomyCleanRuleDO;
import com.datamaster.module.taxonomy.service.rule.ITaxonomyCleanRuleService;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.util.Arrays;
import java.util.List;

/**
 * 清洗规则Controller
 *
 * @author DATAMASTER
 * @date 2025-01-20
 */
@Tag(name = "清洗规则")
@RestController
@RequestMapping("/att/cleanRule")
@Validated
public class TaxonomyCleanRuleController extends BaseController {
    @Resource
    private ITaxonomyCleanRuleService TaxonomyCleanRuleService;

    @Operation(summary = "查询清洗规则列表")
    @PreAuthorize("@ss.hasPermi('att:cleanRule:list')")
    @GetMapping("/list")
    public CommonResult<PageResult<TaxonomyCleanRuleRespVO>> list(TaxonomyCleanRulePageReqVO TaxonomyCleanRule) {
        PageResult<TaxonomyCleanRuleDO> page = TaxonomyCleanRuleService.getAttCleanRulePage(TaxonomyCleanRule);
        return CommonResult.success(BeanUtils.toBean(page, TaxonomyCleanRuleRespVO.class));
    }

    @Operation(summary = "查询清洗规则列表")
    @GetMapping("/listAll")
    public CommonResult<List<TaxonomyCleanRuleRespVO>> listAll(TaxonomyCleanRulePageReqVO TaxonomyCleanRule) {
        List<TaxonomyCleanRuleRespVO> page = TaxonomyCleanRuleService.getAttCleanRuleList(TaxonomyCleanRule);
        return CommonResult.success(page);
    }

    @Operation(summary = "导出清洗规则列表")
    @PreAuthorize("@ss.hasPermi('att:cleanRule:export')")
    @Log(title = "清洗规则", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, TaxonomyCleanRulePageReqVO exportReqVO) {
        exportReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<TaxonomyCleanRuleDO> list = (List<TaxonomyCleanRuleDO>) TaxonomyCleanRuleService.getAttCleanRulePage(exportReqVO)
                .getRows();
        ExcelUtil<TaxonomyCleanRuleRespVO> util = new ExcelUtil<>(TaxonomyCleanRuleRespVO.class);
        util.exportExcel(response, TaxonomyCleanRuleConvert.INSTANCE.convertToRespVOList(list), "应用管理数据");
    }

    @Operation(summary = "导入清洗规则列表")
    @PreAuthorize("@ss.hasPermi('att:cleanRule:import')")
    @Log(title = "清洗规则", businessType = BusinessType.IMPORT)
    @PostMapping("/importData")
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception {
        ExcelUtil<TaxonomyCleanRuleRespVO> util = new ExcelUtil<>(TaxonomyCleanRuleRespVO.class);
        List<TaxonomyCleanRuleRespVO> importExcelList = util.importExcel(file.getInputStream());
        String operName = getUsername();
        String message = TaxonomyCleanRuleService.importAttCleanRule(importExcelList, updateSupport, operName);
        return success(message);
    }

    @Operation(summary = "获取清洗规则详细信息")
    @PreAuthorize("@ss.hasPermi('att:cleanRule:query')")
    @GetMapping(value = "/{ID}")
    public CommonResult<TaxonomyCleanRuleRespVO> getInfo(@PathVariable("ID") Long ID) {
        TaxonomyCleanRuleDO TaxonomyCleanRuleDO = TaxonomyCleanRuleService.getAttCleanRuleById(ID);
        return CommonResult.success(BeanUtils.toBean(TaxonomyCleanRuleDO, TaxonomyCleanRuleRespVO.class));
    }

    @Operation(summary = "新增清洗规则")
    @PreAuthorize("@ss.hasPermi('att:cleanRule:add')")
    @Log(title = "清洗规则", businessType = BusinessType.INSERT)
    @PostMapping
    public CommonResult<Long> add(@Valid @RequestBody TaxonomyCleanRuleSaveReqVO TaxonomyCleanRule) {
        TaxonomyCleanRule.setCreatorId(getUserId());
        TaxonomyCleanRule.setCreateBy(getNickName());
        TaxonomyCleanRule.setCreateTime(DateUtil.date());
        return CommonResult.toAjax(TaxonomyCleanRuleService.createAttCleanRule(TaxonomyCleanRule));
    }

    @Operation(summary = "修改清洗规则")
    @PreAuthorize("@ss.hasPermi('att:cleanRule:edit')")
    @Log(title = "清洗规则", businessType = BusinessType.UPDATE)
    @PutMapping
    public CommonResult<Integer> edit(@Valid @RequestBody TaxonomyCleanRuleSaveReqVO TaxonomyCleanRule) {
        TaxonomyCleanRule.setUpdatorId(getUserId());
        TaxonomyCleanRule.setUpdateBy(getNickName());
        TaxonomyCleanRule.setUpdateTime(DateUtil.date());
        return CommonResult.toAjax(TaxonomyCleanRuleService.updateAttCleanRule(TaxonomyCleanRule));
    }

    @Operation(summary = "删除清洗规则")
    @PreAuthorize("@ss.hasPermi('att:cleanRule:remove')")
    @Log(title = "清洗规则", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public CommonResult<Integer> remove(@PathVariable Long[] ids) {
        return CommonResult.toAjax(TaxonomyCleanRuleService.removeAttCleanRule(Arrays.asList(ids)));
    }

    @Operation(summary = "获取清洗规则树形结构")
    @GetMapping("/tree")
    public CommonResult<List<TaxonomyCleanRuleRespVO>> tree(@RequestParam Long dataElemId) {
        List<TaxonomyCleanRuleRespVO> tree = TaxonomyCleanRuleService.getAttCleanRuleTree(dataElemId);
        return CommonResult.success(tree);
    }

    @Operation(summary = "获取清洗规则树形结构")
    @GetMapping("/getCleaningRuleTree")
    public CommonResult<List<TaxonomyCleanRuleRespVO>> getCleaningRuleTree(@RequestParam(name = "ids", required = false) Long[] ids) {
        List<TaxonomyCleanRuleRespVO> tree = TaxonomyCleanRuleService.getCleaningRuleTree(ids);
        return CommonResult.success(tree);
    }

}
