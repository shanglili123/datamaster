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
import com.datamaster.common.core.page.PageParam;
import com.datamaster.common.annotation.Log;
import com.datamaster.common.core.controller.BaseController;
import com.datamaster.common.core.domain.CommonResult;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.common.database.core.DbTable;
import com.datamaster.common.enums.BusinessType;
import com.datamaster.common.utils.object.BeanUtils;
import com.datamaster.common.utils.poi.ExcelUtil;
import com.datamaster.common.exception.enums.GlobalErrorCodeConstants;
import com.datamaster.module.standards.controller.admin.model.vo.StandardsModelPageReqVO;
import com.datamaster.module.standards.controller.admin.model.vo.StandardsModelRespVO;
import com.datamaster.module.standards.controller.admin.model.vo.StandardsModelSaveReqVO;
import com.datamaster.module.standards.convert.model.StandardsModelConvert;
import com.datamaster.module.standards.dal.dataobject.model.StandardsModelDO;
import com.datamaster.module.standards.service.model.IStandardsModelService;

/**
 * 逻辑模型Controller
 *
 * @author DATAMASTER
 * @date 2025-01-21
 */
@Tag(name = "逻辑模型")
@RestController
@RequestMapping("/dp/model")
@Validated
public class StandardsModelController extends BaseController {
    @Resource
    private IStandardsModelService StandardsModelService;

    @Operation(summary = "查询逻辑模型列表")
    @PreAuthorize("@ss.hasPermi('dp:model:list')")
    @GetMapping("/list")
    public CommonResult<PageResult<StandardsModelRespVO>> list(StandardsModelPageReqVO StandardsModel) {
        PageResult<StandardsModelDO> page = StandardsModelService.getDpModelPage(StandardsModel);
        return CommonResult.success(BeanUtils.toBean(page, StandardsModelRespVO.class));
    }

    @Operation(summary = "导出逻辑模型列表")
    @PreAuthorize("@ss.hasPermi('dp:model:export')")
    @Log(title = "逻辑模型", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, StandardsModelPageReqVO exportReqVO) {
        exportReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<StandardsModelDO> list = (List<StandardsModelDO>) StandardsModelService.getDpModelPage(exportReqVO).getRows();
        ExcelUtil<StandardsModelRespVO> util = new ExcelUtil<>(StandardsModelRespVO.class);
        util.exportExcel(response, StandardsModelConvert.INSTANCE.convertToRespVOList(list), "应用管理数据");
    }

    @Operation(summary = "导入逻辑模型列表")
    @PreAuthorize("@ss.hasPermi('dp:model:import')")
    @Log(title = "逻辑模型", businessType = BusinessType.IMPORT)
    @PostMapping("/importData")
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception {
        ExcelUtil<StandardsModelRespVO> util = new ExcelUtil<>(StandardsModelRespVO.class);
        List<StandardsModelRespVO> importExcelList = util.importExcel(file.getInputStream());
        String operName = getUsername();
        String message = StandardsModelService.importDpModel(importExcelList, updateSupport, operName);
        return success(message);
    }

    @Operation(summary = "获取逻辑模型详细信息")
    @PreAuthorize("@ss.hasPermi('dp:model:query')")
    @GetMapping(value = "/{ID}")
    public CommonResult<StandardsModelRespVO> getInfo(@PathVariable("ID") Long ID) {
        StandardsModelDO StandardsModelDO = StandardsModelService.getDpModelById(ID);
        return CommonResult.success(BeanUtils.toBean(StandardsModelDO, StandardsModelRespVO.class));
    }

    @Operation(summary = "新增逻辑模型")
    @PreAuthorize("@ss.hasPermi('dp:model:add')")
    @Log(title = "逻辑模型", businessType = BusinessType.INSERT)
    @PostMapping
    public CommonResult<Long> add(@Valid @RequestBody StandardsModelSaveReqVO StandardsModel) {
        StandardsModel.setCreatorId(getUserId());
        StandardsModel.setCreateBy(getNickName());
        StandardsModel.setCreateTime(DateUtil.date());
        return CommonResult.success(StandardsModelService.createDpModel(StandardsModel));
    }

    @Operation(summary = "修改逻辑模型")
    @PreAuthorize("@ss.hasPermi('dp:model:edit')")
    @Log(title = "逻辑模型", businessType = BusinessType.UPDATE)
    @PutMapping
    public CommonResult<Integer> edit(@Valid @RequestBody StandardsModelSaveReqVO StandardsModel) {
        StandardsModel.setUpdatorId(getUserId());
        StandardsModel.setUpdateBy(getNickName());
        StandardsModel.setUpdateTime(DateUtil.date());
        return CommonResult.toAjax(StandardsModelService.updateDpModel(StandardsModel));
    }

    @Operation(summary = "删除逻辑模型")
    @PreAuthorize("@ss.hasPermi('dp:model:remove')")
    @Log(title = "逻辑模型", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public CommonResult<Integer> remove(@PathVariable Long[] ids) {
        return CommonResult.toAjax(StandardsModelService.removeDpModel(Arrays.asList(ids)));
    }

    @Operation(summary = "删除逻辑模型连带字段一起删除")
    @PreAuthorize("@ss.hasPermi('dp:model:remove')")
    @Log(title = "逻辑模型", businessType = BusinessType.DELETE)
    @DeleteMapping("/columnAll/{ids}")
    public CommonResult<Integer> removeAndColumnAll(@PathVariable Long[] ids) {
        return CommonResult.toAjax(StandardsModelService.removeDpModelAndColumnAll(Arrays.asList(ids)));
    }

    @Operation(summary = "更改状态")
    @PreAuthorize("@ss.hasPermi('dp:model:edit')")
    @Log(title = "更改数据元状态", businessType = BusinessType.UPDATE)
    @PostMapping("/updateStatus/{id}/{status}")
    public CommonResult<Boolean> updateStatus(@PathVariable Long id,@PathVariable Long status) {
        return CommonResult.toAjax(StandardsModelService.updateStatus(id,status));
    }
}
