

package com.datamaster.module.modeling.controller.admin.dm;

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
import com.datamaster.module.modeling.controller.admin.dm.vo.ModelingDataLayerPageReqVO;
import com.datamaster.module.modeling.controller.admin.dm.vo.ModelingDataLayerRespVO;
import com.datamaster.module.modeling.controller.admin.dm.vo.ModelingDataLayerSaveReqVO;
import com.datamaster.module.modeling.controller.admin.dm.vo.ModelingDataLayerTreeRespVO;
import com.datamaster.module.modeling.convert.dm.ModelingDataLayerConvert;
import com.datamaster.module.modeling.dal.dataobject.dm.ModelingDataLayerDO;
import com.datamaster.module.modeling.service.dm.IModelingDataLayerService;

/**
 * 数仓分层管理Controller
 *
 * @author FXB
 * @date 2026-03-24
 */
@Tag(name = "数仓分层管理")
@RestController
@RequestMapping("/mdl/dataLayer")
@Validated
public class ModelingDataLayerController extends BaseController {
    @Resource
    private IModelingDataLayerService ModelingDataLayerService;

    @Operation(summary = "查询数仓分层管理列表")
    @PreAuthorize("@ss.hasPermi('dm:dataLayer:list')")
    @GetMapping("/list")
    public CommonResult<PageResult<ModelingDataLayerRespVO>> list(ModelingDataLayerPageReqVO ModelingDataLayer) {
        PageResult<ModelingDataLayerDO> page = ModelingDataLayerService.getModelingDataLayerPage(ModelingDataLayer);
        return CommonResult.success(BeanUtils.toBean(page, ModelingDataLayerRespVO.class));
    }

    @Operation(summary = "查询数仓分层管理树")
    @PreAuthorize("@ss.hasPermi('dm:dataLayer:list')")
    @GetMapping("/tree")
    public CommonResult<List<ModelingDataLayerTreeRespVO>> tree() {
        return CommonResult.success(ModelingDataLayerService.tree());
    }

    @Operation(summary = "导出数仓分层管理列表")
    @PreAuthorize("@ss.hasPermi('dm:dataLayer:export')")
    @Log(title = "数仓分层管理", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, ModelingDataLayerPageReqVO exportReqVO) {
        exportReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<ModelingDataLayerDO> list = (List<ModelingDataLayerDO>) ModelingDataLayerService.getModelingDataLayerPage(exportReqVO).getRows();
        ExcelUtil<ModelingDataLayerRespVO> util = new ExcelUtil<>(ModelingDataLayerRespVO.class);
        util.exportExcel(response, ModelingDataLayerConvert.INSTANCE.convertToRespVOList(list), "应用管理数据");
    }

    @Operation(summary = "导入数仓分层管理列表")
    @PreAuthorize("@ss.hasPermi('dm:dataLayer:import')")
    @Log(title = "数仓分层管理", businessType = BusinessType.IMPORT)
    @PostMapping("/importData")
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception {
        ExcelUtil<ModelingDataLayerRespVO> util = new ExcelUtil<>(ModelingDataLayerRespVO.class);
        List<ModelingDataLayerRespVO> importExcelList = util.importExcel(file.getInputStream());
        String operName = getUsername();
        String message = ModelingDataLayerService.importModelingDataLayer(importExcelList, updateSupport, operName);
        return success(message);
    }

    @Operation(summary = "获取数仓分层管理详细信息")
    @PreAuthorize("@ss.hasPermi('dm:dataLayer:query')")
    @GetMapping(value = "/{id}")
    public CommonResult<ModelingDataLayerRespVO> getInfo(@PathVariable("id") Long id) {
        ModelingDataLayerDO ModelingDataLayerDO = ModelingDataLayerService.getModelingDataLayerById(id);
        return CommonResult.success(BeanUtils.toBean(ModelingDataLayerDO, ModelingDataLayerRespVO.class));
    }

    @Operation(summary = "新增数仓分层管理")
    @PreAuthorize("@ss.hasPermi('dm:dataLayer:add')")
    @Log(title = "数仓分层管理", businessType = BusinessType.INSERT)
    @PostMapping
    public CommonResult<Long> add(@Valid @RequestBody ModelingDataLayerSaveReqVO ModelingDataLayer) {
        ModelingDataLayer.setCreatorId(getUserId());
        ModelingDataLayer.setCreateBy(getNickName());
        ModelingDataLayer.setCreateTime(DateUtil.date());
        return CommonResult.toAjax(ModelingDataLayerService.createModelingDataLayer(ModelingDataLayer));
    }

    @Operation(summary = "修改数仓分层管理")
    @PreAuthorize("@ss.hasPermi('dm:dataLayer:edit')")
    @Log(title = "数仓分层管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public CommonResult<Integer> edit(@Valid @RequestBody ModelingDataLayerSaveReqVO ModelingDataLayer) {
        ModelingDataLayer.setUpdatorId(getUserId());
        ModelingDataLayer.setUpdateBy(getNickName());
        ModelingDataLayer.setUpdateTime(DateUtil.date());
        return CommonResult.toAjax(ModelingDataLayerService.updateModelingDataLayer(ModelingDataLayer));
    }

    @Operation(summary = "删除数仓分层管理")
    @PreAuthorize("@ss.hasPermi('dm:dataLayer:remove')")
    @Log(title = "数仓分层管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public CommonResult<Integer> remove(@PathVariable Long[] ids) {
        return CommonResult.toAjax(ModelingDataLayerService.removeModelingDataLayer(Arrays.asList(ids)));
    }

}
