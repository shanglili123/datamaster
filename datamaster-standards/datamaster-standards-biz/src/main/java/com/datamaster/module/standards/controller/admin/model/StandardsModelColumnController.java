package com.datamaster.module.standards.controller.admin.model;

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
import com.datamaster.common.core.domain.R;
import com.datamaster.common.core.page.PageParam;
import com.datamaster.common.annotation.Log;
import com.datamaster.common.core.controller.BaseController;
import com.datamaster.common.core.domain.CommonResult;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.common.enums.BusinessType;
import com.datamaster.common.utils.object.BeanUtils;
import com.datamaster.common.utils.poi.ExcelUtil;
import com.datamaster.module.standards.controller.admin.model.vo.StandardsModelColumnPageReqVO;
import com.datamaster.module.standards.controller.admin.model.vo.StandardsModelColumnRespVO;
import com.datamaster.module.standards.controller.admin.model.vo.StandardsModelColumnSaveReqVO;
import com.datamaster.module.standards.convert.model.StandardsModelColumnConvert;
import com.datamaster.module.standards.dal.dataobject.model.StandardsModelColumnDO;
import com.datamaster.module.standards.service.model.IStandardsModelColumnService;

/**
 * 逻辑模型属性信息Controller
 *
 * @author DATAMASTER
 * @date 2025-01-21
 */
@Tag(name = "逻辑模型属性信息")
@RestController
@RequestMapping("/std/modelColumn")
@Validated
public class StandardsModelColumnController extends BaseController {
    @Resource
    private IStandardsModelColumnService StandardsModelColumnService;

    @Operation(summary = "查询逻辑模型属性信息列表")
//    @PreAuthorize("@ss.hasPermi('dp:modelColumn:list')")
    @GetMapping("/list")
    public CommonResult<PageResult<StandardsModelColumnRespVO>> list(StandardsModelColumnPageReqVO StandardsModelColumn) {
        PageResult<StandardsModelColumnDO> page = StandardsModelColumnService.getDpModelColumnPage(StandardsModelColumn);
        return CommonResult.success(BeanUtils.toBean(page, StandardsModelColumnRespVO.class));
    }

    @Operation(summary = "查询逻辑模型属性信息列表")
//    @PreAuthorize("@ss.hasPermi('dp:modelColumn:list')")
    @GetMapping("/getDpModelColumnList")
    public AjaxResult getDpModelColumnList(StandardsModelColumnSaveReqVO StandardsModelColumn) {
        List<StandardsModelColumnDO> StandardsModelColumnList = StandardsModelColumnService.getDpModelColumnList(StandardsModelColumn);
        return AjaxResult.success(StandardsModelColumnList);
    }

    @Operation(summary = "导出逻辑模型属性信息列表")
//    @PreAuthorize("@ss.hasPermi('dp:modelColumn:export')")
    @Log(title = "逻辑模型属性信息", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, StandardsModelColumnPageReqVO exportReqVO) {
        exportReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<StandardsModelColumnDO> list = (List<StandardsModelColumnDO>) StandardsModelColumnService.getDpModelColumnPage(exportReqVO).getRows();
        ExcelUtil<StandardsModelColumnRespVO> util = new ExcelUtil<>(StandardsModelColumnRespVO.class);
        util.exportExcel(response, StandardsModelColumnConvert.INSTANCE.convertToRespVOList(list), "应用管理数据");
    }

    @Operation(summary = "导入逻辑模型属性信息列表")
//    @PreAuthorize("@ss.hasPermi('dp:modelColumn:import')")
    @Log(title = "逻辑模型属性信息", businessType = BusinessType.IMPORT)
    @PostMapping("/importData")
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception {
        ExcelUtil<StandardsModelColumnRespVO> util = new ExcelUtil<>(StandardsModelColumnRespVO.class);
        List<StandardsModelColumnRespVO> importExcelList = util.importExcel(file.getInputStream());
        String operName = getUsername();
        String message = StandardsModelColumnService.importDpModelColumn(importExcelList, updateSupport, operName);
        return success(message);
    }

    @Operation(summary = "获取逻辑模型属性信息详细信息")
//    @PreAuthorize("@ss.hasPermi('dp:modelColumn:query')")
    @GetMapping(value = "/{id}")
    public CommonResult<StandardsModelColumnRespVO> getInfo(@PathVariable("id") Long id) {
        StandardsModelColumnDO StandardsModelColumnDO = StandardsModelColumnService.getDpModelColumnById(id);
        return CommonResult.success(BeanUtils.toBean(StandardsModelColumnDO, StandardsModelColumnRespVO.class));
    }

    @Operation(summary = "新增逻辑模型属性信息")
//    @PreAuthorize("@ss.hasPermi('dp:modelColumn:add')")
    @Log(title = "逻辑模型属性信息", businessType = BusinessType.INSERT)
    @PostMapping
    public CommonResult<Long> add(@Valid @RequestBody StandardsModelColumnSaveReqVO StandardsModelColumn) {
        StandardsModelColumn.setCreatorId(getUserId());
        StandardsModelColumn.setCreateBy(getNickName());
        StandardsModelColumn.setCreateTime(DateUtil.date());
        return CommonResult.toAjax(StandardsModelColumnService.createDpModelColumn(StandardsModelColumn));
    }

    @Operation(summary = "修改逻辑模型属性信息")
//    @PreAuthorize("@ss.hasPermi('dp:modelColumn:edit')")
    @Log(title = "逻辑模型属性信息", businessType = BusinessType.UPDATE)
    @PutMapping
    public CommonResult<Integer> edit(@Valid @RequestBody StandardsModelColumnSaveReqVO StandardsModelColumn) {
        StandardsModelColumn.setUpdatorId(getUserId());
        StandardsModelColumn.setUpdateBy(getNickName());
        StandardsModelColumn.setUpdateTime(DateUtil.date());
        return CommonResult.toAjax(StandardsModelColumnService.updateDpModelColumn(StandardsModelColumn));
    }

    @Operation(summary = "批量新增逻辑模型属性信息")
//    @PreAuthorize("@ss.hasPermi('dp:modelColumn:add')")
    @Log(title = "逻辑模型属性信息", businessType = BusinessType.INSERT)
    @PostMapping(value = "/addList")
    public CommonResult<Boolean> addList(@Valid @RequestBody List<StandardsModelColumnSaveReqVO> StandardsModelColumnList) {
        for (StandardsModelColumnSaveReqVO StandardsModelColumnSaveReqVO : StandardsModelColumnList) {
            StandardsModelColumnSaveReqVO.setCreatorId(getUserId());
            StandardsModelColumnSaveReqVO.setCreateBy(getNickName());
            StandardsModelColumnSaveReqVO.setCreateTime(DateUtil.date());
        }
        return CommonResult.toAjax(StandardsModelColumnService.createDpModelColumnList(StandardsModelColumnList));
    }

    @Operation(summary = "批量修改逻辑模型属性信息")
//    @PreAuthorize("@ss.hasPermi('dp:modelColumn:edit')")
    @Log(title = "逻辑模型属性信息", businessType = BusinessType.UPDATE)
    @PutMapping(value = "/editList")
    public CommonResult<Boolean> editList(@Valid @RequestBody List<StandardsModelColumnSaveReqVO> StandardsModelColumnList) {
        for (StandardsModelColumnSaveReqVO StandardsModelColumnSaveReqVO : StandardsModelColumnList) {
            StandardsModelColumnSaveReqVO.setCreatorId(getUserId());
            StandardsModelColumnSaveReqVO.setCreateBy(getNickName());
            StandardsModelColumnSaveReqVO.setCreateTime(DateUtil.date());
        }
        return CommonResult.toAjax(StandardsModelColumnService.updateDpModelColumnList(StandardsModelColumnList));
    }

    @Operation(summary = "删除逻辑模型属性信息")
//    @PreAuthorize("@ss.hasPermi('dp:modelColumn:remove')")
    @Log(title = "逻辑模型属性信息", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public CommonResult<Integer> remove(@PathVariable Long[] ids) {
        return CommonResult.toAjax(StandardsModelColumnService.removeDpModelColumn(Arrays.asList(ids)));
    }

}
