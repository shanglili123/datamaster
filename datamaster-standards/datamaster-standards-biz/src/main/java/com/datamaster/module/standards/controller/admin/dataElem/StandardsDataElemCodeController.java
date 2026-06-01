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
import com.datamaster.module.standards.controller.admin.dataElem.vo.StandardsDataElemCodePageReqVO;
import com.datamaster.module.standards.controller.admin.dataElem.vo.StandardsDataElemCodeRespVO;
import com.datamaster.module.standards.controller.admin.dataElem.vo.StandardsDataElemCodeSaveReqVO;
import com.datamaster.module.standards.convert.dataElem.StandardsDataElemCodeConvert;
import com.datamaster.module.standards.dal.dataobject.dataElem.StandardsDataElemCodeDO;
import com.datamaster.module.standards.service.dataElem.IStandardsDataElemCodeService;

/**
 * 数据元代码Controller
 *
 * @author DATAMASTER
 * @date 2025-01-21
 */
@Tag(name = "数据元代码")
@RestController
@RequestMapping("/std/dataElemCode")
@Validated
public class StandardsDataElemCodeController extends BaseController {
    @Resource
    private IStandardsDataElemCodeService StandardsDataElemCodeService;

    @Operation(summary = "查询数据元代码列表")
    @PreAuthorize("@ss.hasPermi('dp:dataElemCode:list')")
    @GetMapping("/list")
    public CommonResult<PageResult<StandardsDataElemCodeRespVO>> list(StandardsDataElemCodePageReqVO StandardsDataElemCode) {
        PageResult<StandardsDataElemCodeDO> page = StandardsDataElemCodeService.getDpDataElemCodePage(StandardsDataElemCode);
        return CommonResult.success(BeanUtils.toBean(page, StandardsDataElemCodeRespVO.class));
    }

    @Operation(summary = "导出数据元代码列表")
    @PreAuthorize("@ss.hasPermi('dp:dataElemCode:export')")
    @Log(title = "数据元代码", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, StandardsDataElemCodePageReqVO exportReqVO) {
        exportReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<StandardsDataElemCodeDO> list = (List<StandardsDataElemCodeDO>) StandardsDataElemCodeService.getDpDataElemCodePage(exportReqVO).getRows();
        ExcelUtil<StandardsDataElemCodeRespVO> util = new ExcelUtil<>(StandardsDataElemCodeRespVO.class);
        util.exportExcel(response, StandardsDataElemCodeConvert.INSTANCE.convertToRespVOList(list), "应用管理数据");
    }

    @Operation(summary = "导入数据元代码列表")
    @PreAuthorize("@ss.hasPermi('dp:dataElemCode:import')")
    @Log(title = "数据元代码", businessType = BusinessType.IMPORT)
    @PostMapping("/importData")
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception {
        ExcelUtil<StandardsDataElemCodeRespVO> util = new ExcelUtil<>(StandardsDataElemCodeRespVO.class);
        List<StandardsDataElemCodeRespVO> importExcelList = util.importExcel(file.getInputStream());
        String operName = getUsername();
        String message = StandardsDataElemCodeService.importDpDataElemCode(importExcelList, updateSupport, operName);
        return success(message);
    }

    @Operation(summary = "获取数据元代码详细信息")
    @PreAuthorize("@ss.hasPermi('dp:dataElemCode:query')")
    @GetMapping(value = "/{id}")
    public CommonResult<StandardsDataElemCodeRespVO> getInfo(@PathVariable("id") Long id) {
        StandardsDataElemCodeDO StandardsDataElemCodeDO = StandardsDataElemCodeService.getDpDataElemCodeById(id);
        return CommonResult.success(BeanUtils.toBean(StandardsDataElemCodeDO, StandardsDataElemCodeRespVO.class));
    }

    @Operation(summary = "新增数据元代码")
    @PreAuthorize("@ss.hasPermi('dp:dataElemCode:add')")
    @Log(title = "数据元代码", businessType = BusinessType.INSERT)
    @PostMapping
    public CommonResult<Long> add(@Valid @RequestBody StandardsDataElemCodeSaveReqVO StandardsDataElemCode) {
        StandardsDataElemCode.setCreatorId(getUserId());
        StandardsDataElemCode.setCreateBy(getNickName());
        StandardsDataElemCode.setCreateTime(DateUtil.date());
        return CommonResult.toAjax(StandardsDataElemCodeService.createDpDataElemCode(StandardsDataElemCode));
    }

    @Operation(summary = "修改数据元代码")
    @PreAuthorize("@ss.hasPermi('dp:dataElemCode:edit')")
    @Log(title = "数据元代码", businessType = BusinessType.UPDATE)
    @PutMapping
    public CommonResult<Integer> edit(@Valid @RequestBody StandardsDataElemCodeSaveReqVO StandardsDataElemCode) {
        StandardsDataElemCode.setUpdatorId(getUserId());
        StandardsDataElemCode.setUpdateBy(getNickName());
        StandardsDataElemCode.setUpdateTime(DateUtil.date());
        return CommonResult.toAjax(StandardsDataElemCodeService.updateDpDataElemCode(StandardsDataElemCode));
    }

    @Operation(summary = "删除数据元代码")
    @PreAuthorize("@ss.hasPermi('dp:dataElemCode:remove')")
    @Log(title = "数据元代码", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public CommonResult<Integer> remove(@PathVariable Long[] ids) {
        return CommonResult.toAjax(StandardsDataElemCodeService.removeDpDataElemCode(Arrays.asList(ids)));
    }

    @Operation(summary = "校验源代码值")
    @GetMapping("/validateCodeValue")
    public CommonResult<Integer> validateCodeValue(@RequestParam String dataElemId, @RequestParam String codeValue
            , @RequestParam(required = false) String id) {
        return CommonResult.success(StandardsDataElemCodeService.validateCodeValue(dataElemId, codeValue, id));
    }

}
