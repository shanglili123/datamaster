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
import com.datamaster.common.enums.BusinessType;
import com.datamaster.common.utils.object.BeanUtils;
import com.datamaster.common.utils.poi.ExcelUtil;
import com.datamaster.common.exception.enums.GlobalErrorCodeConstants;
import com.datamaster.module.standards.controller.admin.model.vo.StandardsMaterializedMethodReqVO;
import com.datamaster.module.standards.controller.admin.model.vo.StandardsModelMaterializedPageReqVO;
import com.datamaster.module.standards.controller.admin.model.vo.StandardsModelMaterializedRespVO;
import com.datamaster.module.standards.controller.admin.model.vo.StandardsModelMaterializedSaveReqVO;
import com.datamaster.module.standards.convert.model.StandardsModelMaterializedConvert;
import com.datamaster.module.standards.dal.dataobject.model.StandardsModelMaterializedDO;
import com.datamaster.module.standards.service.model.IStandardsModelMaterializedService;

/**
 * 物化模型记录Controller
 *
 * @author DATAMASTER
 * @date 2025-01-21
 */
@Tag(name = "物化模型记录")
@RestController
@RequestMapping("/dp/modelMaterialized")
@Validated
public class StandardsModelMaterializedController extends BaseController {
    @Resource
    private IStandardsModelMaterializedService StandardsModelMaterializedService;

    @Operation(summary = "查询物化模型记录列表")
//    @PreAuthorize("@ss.hasPermi('dp:modelMaterialized:list')")
    @GetMapping("/list")
    public CommonResult<PageResult<StandardsModelMaterializedRespVO>> list(StandardsModelMaterializedPageReqVO StandardsModelMaterialized) {
        PageResult<StandardsModelMaterializedDO> page = StandardsModelMaterializedService.getDpModelMaterializedPage(StandardsModelMaterialized);
        return CommonResult.success(BeanUtils.toBean(page, StandardsModelMaterializedRespVO.class));
    }

    @Operation(summary = "导出物化模型记录列表")
//    @PreAuthorize("@ss.hasPermi('dp:modelMaterialized:export')")
    @Log(title = "物化模型记录", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, StandardsModelMaterializedPageReqVO exportReqVO) {
        exportReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<StandardsModelMaterializedDO> list = (List<StandardsModelMaterializedDO>) StandardsModelMaterializedService.getDpModelMaterializedPage(exportReqVO).getRows();
        ExcelUtil<StandardsModelMaterializedRespVO> util = new ExcelUtil<>(StandardsModelMaterializedRespVO.class);
        util.exportExcel(response, StandardsModelMaterializedConvert.INSTANCE.convertToRespVOList(list), "应用管理数据");
    }

    @Operation(summary = "导入物化模型记录列表")
//    @PreAuthorize("@ss.hasPermi('dp:modelMaterialized:import')")
    @Log(title = "物化模型记录", businessType = BusinessType.IMPORT)
    @PostMapping("/importData")
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception {
        ExcelUtil<StandardsModelMaterializedRespVO> util = new ExcelUtil<>(StandardsModelMaterializedRespVO.class);
        List<StandardsModelMaterializedRespVO> importExcelList = util.importExcel(file.getInputStream());
        String operName = getUsername();
        String message = StandardsModelMaterializedService.importDpModelMaterialized(importExcelList, updateSupport, operName);
        return success(message);
    }

    @Operation(summary = "获取物化模型记录详细信息")
//    @PreAuthorize("@ss.hasPermi('dp:modelMaterialized:query')")
    @GetMapping(value = "/{id}")
    public CommonResult<StandardsModelMaterializedRespVO> getInfo(@PathVariable("id") Long id) {
        StandardsModelMaterializedDO StandardsModelMaterializedDO = StandardsModelMaterializedService.getDpModelMaterializedById(id);
        return CommonResult.success(BeanUtils.toBean(StandardsModelMaterializedDO, StandardsModelMaterializedRespVO.class));
    }

    @Operation(summary = "新增物化模型记录")
//    @PreAuthorize("@ss.hasPermi('dp:modelMaterialized:add')")
    @Log(title = "物化模型记录", businessType = BusinessType.INSERT)
    @PostMapping
    public CommonResult<Long> add(@Valid @RequestBody StandardsModelMaterializedSaveReqVO StandardsModelMaterialized) {
        StandardsModelMaterialized.setCreatorId(getUserId());
        StandardsModelMaterialized.setCreateBy(getNickName());
        StandardsModelMaterialized.setCreateTime(DateUtil.date());
        return CommonResult.toAjax(StandardsModelMaterializedService.createDpModelMaterialized(StandardsModelMaterialized));
    }

    @Operation(summary = "新增物化模型记录")
//    @PreAuthorize("@ss.hasPermi('dp:modelMaterialized:add')")
    @Log(title = "物化模型记录", businessType = BusinessType.INSERT)
    @PostMapping("/createMaterializedTable")
    public CommonResult<Long> createMaterializedTable(@Valid @RequestBody StandardsMaterializedMethodReqVO StandardsModelMaterialized) {
        StandardsModelMaterialized.setCreatorId(getUserId());
        StandardsModelMaterialized.setCreateBy(getNickName());
        StandardsModelMaterialized.setCreateTime(DateUtil.date());
        return CommonResult.success(StandardsModelMaterializedService.createMaterializedTable(StandardsModelMaterialized));
    }

    @Operation(summary = "修改物化模型记录")
//    @PreAuthorize("@ss.hasPermi('dp:modelMaterialized:edit')")
    @Log(title = "物化模型记录", businessType = BusinessType.UPDATE)
    @PutMapping
    public CommonResult<Integer> edit(@Valid @RequestBody StandardsModelMaterializedSaveReqVO StandardsModelMaterialized) {
        StandardsModelMaterialized.setUpdatorId(getUserId());
        StandardsModelMaterialized.setUpdateBy(getNickName());
        StandardsModelMaterialized.setUpdateTime(DateUtil.date());
        return CommonResult.toAjax(StandardsModelMaterializedService.updateDpModelMaterialized(StandardsModelMaterialized));
    }

    @Operation(summary = "删除物化模型记录")
//    @PreAuthorize("@ss.hasPermi('dp:modelMaterialized:remove')")
    @Log(title = "物化模型记录", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public CommonResult<Integer> remove(@PathVariable Long[] ids) {
        return CommonResult.toAjax(StandardsModelMaterializedService.removeDpModelMaterialized(Arrays.asList(ids)));
    }

}
