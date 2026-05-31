package com.datamaster.module.standards.controller.admin.dataElem;

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
import com.datamaster.module.standards.controller.admin.dataElem.vo.StandardsDataElemRuleRelPageReqVO;
import com.datamaster.module.standards.controller.admin.dataElem.vo.StandardsDataElemRuleRelRespVO;
import com.datamaster.module.standards.controller.admin.dataElem.vo.StandardsDataElemRuleRelSaveReqVO;
import com.datamaster.module.standards.convert.dataElem.StandardsDataElemRuleRelConvert;
import com.datamaster.module.standards.dal.dataobject.dataElem.StandardsDataElemRuleRelDO;
import com.datamaster.module.standards.service.dataElem.IStandardsDataElemRuleRelService;

/**
 * 数据元数据规则关联信息Controller
 *
 * @author DATAMASTER
 * @date 2025-01-21
 */
@Tag(name = "数据元数据规则关联信息")
@RestController
@RequestMapping("/dp/dataElemRuleRel")
@Validated
public class StandardsDataElemRuleRelController extends BaseController {
    @Resource
    private IStandardsDataElemRuleRelService StandardsDataElemRuleRelService;

    @Operation(summary = "查询数据元数据规则关联信息列表")
//    @PreAuthorize("@ss.hasPermi('dp:dataElemRuleRel:list')")
    @GetMapping("/list")
    public CommonResult<PageResult<StandardsDataElemRuleRelDO>> list(StandardsDataElemRuleRelPageReqVO StandardsDataElemRuleRel) {
        PageResult<StandardsDataElemRuleRelDO> page = StandardsDataElemRuleRelService.getDpDataElemRuleRelPage(StandardsDataElemRuleRel);
        return CommonResult.success(page);
    }

    @Operation(summary = "导出数据元数据规则关联信息列表")
//    @PreAuthorize("@ss.hasPermi('dp:dataElemRuleRel:export')")
    @Log(title = "数据元数据规则关联信息", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, StandardsDataElemRuleRelPageReqVO exportReqVO) {
        exportReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<StandardsDataElemRuleRelDO> list = (List<StandardsDataElemRuleRelDO>) StandardsDataElemRuleRelService.getDpDataElemRuleRelPage(exportReqVO).getRows();
        ExcelUtil<StandardsDataElemRuleRelRespVO> util = new ExcelUtil<>(StandardsDataElemRuleRelRespVO.class);
        util.exportExcel(response, StandardsDataElemRuleRelConvert.INSTANCE.convertToRespVOList(list), "应用管理数据");
    }

    @Operation(summary = "导入数据元数据规则关联信息列表")
//    @PreAuthorize("@ss.hasPermi('dp:dataElemRuleRel:import')")
    @Log(title = "数据元数据规则关联信息", businessType = BusinessType.IMPORT)
    @PostMapping("/importData")
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception {
        ExcelUtil<StandardsDataElemRuleRelRespVO> util = new ExcelUtil<>(StandardsDataElemRuleRelRespVO.class);
        List<StandardsDataElemRuleRelRespVO> importExcelList = util.importExcel(file.getInputStream());
        String operName = getUsername();
        String message = StandardsDataElemRuleRelService.importDpDataElemRuleRel(importExcelList, updateSupport, operName);
        return success(message);
    }

    @Operation(summary = "获取数据元数据规则关联信息详细信息")
//    @PreAuthorize("@ss.hasPermi('dp:dataElemRuleRel:query')")
    @GetMapping(value = "/{id}")
    public CommonResult<StandardsDataElemRuleRelRespVO> getInfo(@PathVariable("id") Long id) {
        StandardsDataElemRuleRelDO StandardsDataElemRuleRelDO = StandardsDataElemRuleRelService.getDpDataElemRuleRelById(id);
        return CommonResult.success(BeanUtils.toBean(StandardsDataElemRuleRelDO, StandardsDataElemRuleRelRespVO.class));
    }

    @Operation(summary = "新增数据元数据规则关联信息")
//    @PreAuthorize("@ss.hasPermi('dp:dataElemRuleRel:add')")
    @Log(title = "数据元数据规则关联信息", businessType = BusinessType.INSERT)
    @PostMapping
    public CommonResult<Long> add(@Valid @RequestBody StandardsDataElemRuleRelSaveReqVO StandardsDataElemRuleRel) {
        StandardsDataElemRuleRel.setCreatorId(getUserId());
        StandardsDataElemRuleRel.setCreateBy(getNickName());
        StandardsDataElemRuleRel.setCreateTime(DateUtil.date());
        return CommonResult.success(StandardsDataElemRuleRelService.createDpDataElemRuleRel(StandardsDataElemRuleRel));
    }

    @Operation(summary = "修改数据元数据规则关联信息")
//    @PreAuthorize("@ss.hasPermi('dp:dataElemRuleRel:edit')")
    @Log(title = "数据元数据规则关联信息", businessType = BusinessType.UPDATE)
    @PutMapping
    public CommonResult<Integer> edit(@Valid @RequestBody StandardsDataElemRuleRelSaveReqVO StandardsDataElemRuleRel) {
        StandardsDataElemRuleRel.setUpdatorId(getUserId());
        StandardsDataElemRuleRel.setUpdateBy(getNickName());
        StandardsDataElemRuleRel.setUpdateTime(DateUtil.date());
        return CommonResult.success(StandardsDataElemRuleRelService.updateDpDataElemRuleRel(StandardsDataElemRuleRel));
    }

    @Operation(summary = "删除数据元数据规则关联信息")
//    @PreAuthorize("@ss.hasPermi('dp:dataElemRuleRel:remove')")
    @Log(title = "数据元数据规则关联信息", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public CommonResult<Integer> remove(@PathVariable Long[] ids) {
        return CommonResult.toAjax(StandardsDataElemRuleRelService.removeDpDataElemRuleRel(Arrays.asList(ids)));
    }

}
