package com.datamaster.module.assets.controller.admin.assetchild.audit;

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
import com.datamaster.module.assets.controller.admin.assetchild.audit.vo.AssetsAssetAuditSchedulePageReqVO;
import com.datamaster.module.assets.controller.admin.assetchild.audit.vo.AssetsAssetAuditScheduleRespVO;
import com.datamaster.module.assets.controller.admin.assetchild.audit.vo.AssetsAssetAuditScheduleSaveReqVO;
import com.datamaster.module.assets.convert.assetchild.audit.AssetsAssetAuditScheduleConvert;
import com.datamaster.module.assets.dal.dataobject.assetchild.audit.AssetsAssetAuditScheduleDO;
import com.datamaster.module.assets.service.assetchild.audit.IAssetsAssetAuditScheduleService;

/**
 * 资产稽查调度Controller * * @author DATAMASTER * @date 2025-05-09
 */
@Tag(name = "资产稽查调度")
@RestController
@RequestMapping("/ast/assetAuditSchedule")
@Validated
public class AssetsAssetAuditScheduleController extends BaseController {
    @Resource
    private IAssetsAssetAuditScheduleService AssetsAssetAuditScheduleService;

    @Operation(summary = "查询资产稽查调度列表")
    @PreAuthorize("@ss.hasPermi('da:assetAuditSchedule:list')")
    @GetMapping("/list")
    public CommonResult<PageResult<AssetsAssetAuditScheduleRespVO>> list(AssetsAssetAuditSchedulePageReqVO AssetsAssetAuditSchedule) {
        PageResult<AssetsAssetAuditScheduleDO> page = AssetsAssetAuditScheduleService.getAssetAuditSchedulePage(AssetsAssetAuditSchedule);
        return CommonResult.success(BeanUtils.toBean(page, AssetsAssetAuditScheduleRespVO.class));
    }

    @Operation(summary = "导出资产稽查调度列表")
    @PreAuthorize("@ss.hasPermi('da:assetAuditSchedule:export')")
    @Log(title = "资产稽查调度", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, AssetsAssetAuditSchedulePageReqVO exportReqVO) {
        exportReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<AssetsAssetAuditScheduleDO> list = (List<AssetsAssetAuditScheduleDO>) AssetsAssetAuditScheduleService.getAssetAuditSchedulePage(exportReqVO).getRows();
        ExcelUtil<AssetsAssetAuditScheduleRespVO> util = new ExcelUtil<>(AssetsAssetAuditScheduleRespVO.class);
        util.exportExcel(response, AssetsAssetAuditScheduleConvert.INSTANCE.convertToRespVOList(list), "应用管理数据");
    }

    @Operation(summary = "导入资产稽查调度列表")
    @PreAuthorize("@ss.hasPermi('da:assetAuditSchedule:import')")
    @Log(title = "资产稽查调度", businessType = BusinessType.IMPORT)
    @PostMapping("/importData")
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception {
        ExcelUtil<AssetsAssetAuditScheduleRespVO> util = new ExcelUtil<>(AssetsAssetAuditScheduleRespVO.class);
        List<AssetsAssetAuditScheduleRespVO> importExcelList = util.importExcel(file.getInputStream());
        String operName = getUsername();
        String message = AssetsAssetAuditScheduleService.importAssetAuditSchedule(importExcelList, updateSupport, operName);
        return success(message);
    }

    @Operation(summary = "获取资产稽查调度详细信息")
    @PreAuthorize("@ss.hasPermi('da:assetAuditSchedule:query')")
    @GetMapping(value = "/{id}")
    public CommonResult<AssetsAssetAuditScheduleRespVO> getInfo(@PathVariable("id") Long id) {
        AssetsAssetAuditScheduleDO AssetsAssetAuditScheduleDO = AssetsAssetAuditScheduleService.getAssetAuditScheduleById(id);
        return CommonResult.success(BeanUtils.toBean(AssetsAssetAuditScheduleDO, AssetsAssetAuditScheduleRespVO.class));
    }

    @Operation(summary = "新增资产稽查调度")
    @PreAuthorize("@ss.hasPermi('da:assetAuditSchedule:add')")
    @Log(title = "资产稽查调度", businessType = BusinessType.INSERT)
    @PostMapping
    public CommonResult<Long> add(@Valid @RequestBody AssetsAssetAuditScheduleSaveReqVO AssetsAssetAuditSchedule) {
        AssetsAssetAuditSchedule.setCreatorId(getUserId());
        AssetsAssetAuditSchedule.setCreateBy(getNickName());
        AssetsAssetAuditSchedule.setCreateTime(DateUtil.date());
        return CommonResult.toAjax(AssetsAssetAuditScheduleService.createAssetAuditSchedule(AssetsAssetAuditSchedule));
    }

    @Operation(summary = "修改资产稽查调度")
    @PreAuthorize("@ss.hasPermi('da:assetAuditSchedule:edit')")
    @Log(title = "资产稽查调度", businessType = BusinessType.UPDATE)
    @PutMapping
    public CommonResult<Integer> edit(@Valid @RequestBody AssetsAssetAuditScheduleSaveReqVO AssetsAssetAuditSchedule) {
        AssetsAssetAuditSchedule.setUpdatorId(getUserId());
        AssetsAssetAuditSchedule.setUpdateBy(getNickName());
        AssetsAssetAuditSchedule.setUpdateTime(DateUtil.date());
        return CommonResult.toAjax(AssetsAssetAuditScheduleService.updateAssetAuditSchedule(AssetsAssetAuditSchedule));
    }

    @Operation(summary = "删除资产稽查调度")
    @PreAuthorize("@ss.hasPermi('da:assetAuditSchedule:remove')")
    @Log(title = "资产稽查调度", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public CommonResult<Integer> remove(@PathVariable Long[] ids) {
        return CommonResult.toAjax(AssetsAssetAuditScheduleService.removeAssetAuditSchedule(Arrays.asList(ids)));
    }
}