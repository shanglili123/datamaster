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
import com.datamaster.module.standards.controller.admin.dataElem.vo.StandardsDataElemAssetRelPageReqVO;
import com.datamaster.module.standards.controller.admin.dataElem.vo.StandardsDataElemAssetRelRespVO;
import com.datamaster.module.standards.controller.admin.dataElem.vo.StandardsDataElemAssetRelSaveReqVO;
import com.datamaster.module.standards.convert.dataElem.StandardsDataElemAssetRelConvert;
import com.datamaster.module.standards.dal.dataobject.dataElem.StandardsDataElemAssetRelDO;
import com.datamaster.module.standards.service.dataElem.IStandardsDataElemAssetRelService;

/**
 * 数据元数据资产关联信息Controller
 *
 * @author DATAMASTER
 * @date 2025-01-21
 */
@Tag(name = "数据元数据资产关联信息")
@RestController
@RequestMapping("/std/dataElemAssetRel")
@Validated
public class StandardsDataElemAssetRelController extends BaseController {
    @Resource
    private IStandardsDataElemAssetRelService StandardsDataElemAssetRelService;

    @Operation(summary = "查询数据元数据资产关联信息列表")
//    @PreAuthorize("@ss.hasPermi('dp:dataElemAssetRel:list')")
    @GetMapping("/list")
    public CommonResult<PageResult<StandardsDataElemAssetRelRespVO>> list(StandardsDataElemAssetRelPageReqVO StandardsDataElemAssetRel) {
        PageResult<StandardsDataElemAssetRelDO> page = StandardsDataElemAssetRelService.getDpDataElemAssetRelPage(StandardsDataElemAssetRel);
        return CommonResult.success(BeanUtils.toBean(page, StandardsDataElemAssetRelRespVO.class));
    }

    @Operation(summary = "导出数据元数据资产关联信息列表")
//    @PreAuthorize("@ss.hasPermi('dp:dataElemAssetRel:export')")
    @Log(title = "数据元数据资产关联信息", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, StandardsDataElemAssetRelPageReqVO exportReqVO) {
        exportReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<StandardsDataElemAssetRelDO> list = (List<StandardsDataElemAssetRelDO>) StandardsDataElemAssetRelService.getDpDataElemAssetRelPage(exportReqVO).getRows();
        ExcelUtil<StandardsDataElemAssetRelRespVO> util = new ExcelUtil<>(StandardsDataElemAssetRelRespVO.class);
        util.exportExcel(response, StandardsDataElemAssetRelConvert.INSTANCE.convertToRespVOList(list), "应用管理数据");
    }

    @Operation(summary = "导入数据元数据资产关联信息列表")
//    @PreAuthorize("@ss.hasPermi('dp:dataElemAssetRel:import')")
    @Log(title = "数据元数据资产关联信息", businessType = BusinessType.IMPORT)
    @PostMapping("/importData")
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception {
        ExcelUtil<StandardsDataElemAssetRelRespVO> util = new ExcelUtil<>(StandardsDataElemAssetRelRespVO.class);
        List<StandardsDataElemAssetRelRespVO> importExcelList = util.importExcel(file.getInputStream());
        String operName = getUsername();
        String message = StandardsDataElemAssetRelService.importDpDataElemAssetRel(importExcelList, updateSupport, operName);
        return success(message);
    }

    @Operation(summary = "获取数据元数据资产关联信息详细信息")
//    @PreAuthorize("@ss.hasPermi('dp:dataElemAssetRel:query')")
    @GetMapping(value = "/{id}")
    public CommonResult<StandardsDataElemAssetRelRespVO> getInfo(@PathVariable("id") Long id) {
        StandardsDataElemAssetRelDO StandardsDataElemAssetRelDO = StandardsDataElemAssetRelService.getDpDataElemAssetRelById(id);
        return CommonResult.success(BeanUtils.toBean(StandardsDataElemAssetRelDO, StandardsDataElemAssetRelRespVO.class));
    }

    @Operation(summary = "新增数据元数据资产关联信息")
//    @PreAuthorize("@ss.hasPermi('dp:dataElemAssetRel:add')")
    @Log(title = "数据元数据资产关联信息", businessType = BusinessType.INSERT)
    @PostMapping
    public CommonResult<Long> add(@Valid @RequestBody StandardsDataElemAssetRelSaveReqVO StandardsDataElemAssetRel) {
        StandardsDataElemAssetRel.setCreatorId(getUserId());
        StandardsDataElemAssetRel.setCreateBy(getNickName());
        StandardsDataElemAssetRel.setCreateTime(DateUtil.date());
        return CommonResult.toAjax(StandardsDataElemAssetRelService.createDpDataElemAssetRel(StandardsDataElemAssetRel));
    }

    @Operation(summary = "修改数据元数据资产关联信息")
//    @PreAuthorize("@ss.hasPermi('dp:dataElemAssetRel:edit')")
    @Log(title = "数据元数据资产关联信息", businessType = BusinessType.UPDATE)
    @PutMapping
    public CommonResult<Integer> edit(@Valid @RequestBody StandardsDataElemAssetRelSaveReqVO StandardsDataElemAssetRel) {
        StandardsDataElemAssetRel.setUpdatorId(getUserId());
        StandardsDataElemAssetRel.setUpdateBy(getNickName());
        StandardsDataElemAssetRel.setUpdateTime(DateUtil.date());
        return CommonResult.toAjax(StandardsDataElemAssetRelService.updateDpDataElemAssetRel(StandardsDataElemAssetRel));
    }

    @Operation(summary = "删除数据元数据资产关联信息")
//    @PreAuthorize("@ss.hasPermi('dp:dataElemAssetRel:remove')")
    @Log(title = "数据元数据资产关联信息", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public CommonResult<Integer> remove(@PathVariable Long[] ids) {
        return CommonResult.toAjax(StandardsDataElemAssetRelService.removeDpDataElemAssetRel(Arrays.asList(ids)));
    }

}
