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
import com.datamaster.module.assets.controller.admin.assetchild.audit.vo.AssetsAssetAuditAlertPageReqVO;
import com.datamaster.module.assets.controller.admin.assetchild.audit.vo.AssetsAssetAuditAlertRespVO;
import com.datamaster.module.assets.controller.admin.assetchild.audit.vo.AssetsAssetAuditAlertSaveReqVO;
import com.datamaster.module.assets.convert.assetchild.audit.AssetsAssetAuditAlertConvert;
import com.datamaster.module.assets.dal.dataobject.assetchild.audit.AssetsAssetAuditAlertDO;
import com.datamaster.module.assets.service.assetchild.audit.IAssetsAssetAuditAlertService;

/**
 * 数据资产-质量预警Controller * * @author DATAMASTER * @date 2025-05-09
 */
@Tag(name = "数据资产-质量预警")
@RestController
@RequestMapping("/ast/assetAuditAlert")
@Validated
public class AssetsAssetAuditAlertController extends BaseController {
    @Resource
    private IAssetsAssetAuditAlertService AssetsAssetAuditAlertService;

    @Operation(summary = "查询数据资产-质量预警列表")
    @PreAuthorize("@ss.hasPermi('da:assetAuditAlert:list')")
    @GetMapping("/list")
    public CommonResult<PageResult<AssetsAssetAuditAlertRespVO>> list(AssetsAssetAuditAlertPageReqVO AssetsAssetAuditAlert) {
        PageResult<AssetsAssetAuditAlertDO> page = AssetsAssetAuditAlertService.getAssetAuditAlertPage(AssetsAssetAuditAlert);
        return CommonResult.success(BeanUtils.toBean(page, AssetsAssetAuditAlertRespVO.class));
    }

    @Operation(summary = "导出数据资产-质量预警列表")
    @PreAuthorize("@ss.hasPermi('da:assetAuditAlert:export')")
    @Log(title = "数据资产-质量预警", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, AssetsAssetAuditAlertPageReqVO exportReqVO) {
        exportReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<AssetsAssetAuditAlertDO> list = (List<AssetsAssetAuditAlertDO>) AssetsAssetAuditAlertService.getAssetAuditAlertPage(exportReqVO).getRows();
        ExcelUtil<AssetsAssetAuditAlertRespVO> util = new ExcelUtil<>(AssetsAssetAuditAlertRespVO.class);
        util.exportExcel(response, AssetsAssetAuditAlertConvert.INSTANCE.convertToRespVOList(list), "应用管理数据");
    }

    @Operation(summary = "导入数据资产-质量预警列表")
    @PreAuthorize("@ss.hasPermi('da:assetAuditAlert:import')")
    @Log(title = "数据资产-质量预警", businessType = BusinessType.IMPORT)
    @PostMapping("/importData")
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception {
        ExcelUtil<AssetsAssetAuditAlertRespVO> util = new ExcelUtil<>(AssetsAssetAuditAlertRespVO.class);
        List<AssetsAssetAuditAlertRespVO> importExcelList = util.importExcel(file.getInputStream());
        String operName = getUsername();
        String message = AssetsAssetAuditAlertService.importAssetAuditAlert(importExcelList, updateSupport, operName);
        return success(message);
    }

    @Operation(summary = "获取数据资产-质量预警详细信息")
    @PreAuthorize("@ss.hasPermi('da:assetAuditAlert:query')")
    @GetMapping(value = "/{id}")
    public CommonResult<AssetsAssetAuditAlertRespVO> getInfo(@PathVariable("id") Long id) {
        AssetsAssetAuditAlertDO AssetsAssetAuditAlertDO = AssetsAssetAuditAlertService.getAssetAuditAlertById(id);
        return CommonResult.success(BeanUtils.toBean(AssetsAssetAuditAlertDO, AssetsAssetAuditAlertRespVO.class));
    }

    @Operation(summary = "新增数据资产-质量预警")
    @PreAuthorize("@ss.hasPermi('da:assetAuditAlert:add')")
    @Log(title = "数据资产-质量预警", businessType = BusinessType.INSERT)
    @PostMapping
    public CommonResult<Long> add(@Valid @RequestBody AssetsAssetAuditAlertSaveReqVO AssetsAssetAuditAlert) {
        AssetsAssetAuditAlert.setCreatorId(getUserId());
        AssetsAssetAuditAlert.setCreateBy(getNickName());
        AssetsAssetAuditAlert.setCreateTime(DateUtil.date());
        return CommonResult.toAjax(AssetsAssetAuditAlertService.createAssetAuditAlert(AssetsAssetAuditAlert));
    }

    @Operation(summary = "修改数据资产-质量预警")
    @PreAuthorize("@ss.hasPermi('da:assetAuditAlert:edit')")
    @Log(title = "数据资产-质量预警", businessType = BusinessType.UPDATE)
    @PutMapping
    public CommonResult<Integer> edit(@Valid @RequestBody AssetsAssetAuditAlertSaveReqVO AssetsAssetAuditAlert) {
        AssetsAssetAuditAlert.setUpdatorId(getUserId());
        AssetsAssetAuditAlert.setUpdateBy(getNickName());
        AssetsAssetAuditAlert.setUpdateTime(DateUtil.date());
        return CommonResult.toAjax(AssetsAssetAuditAlertService.updateAssetAuditAlert(AssetsAssetAuditAlert));
    }

    @Operation(summary = "删除数据资产-质量预警")
    @PreAuthorize("@ss.hasPermi('da:assetAuditAlert:remove')")
    @Log(title = "数据资产-质量预警", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public CommonResult<Integer> remove(@PathVariable Long[] ids) {
        return CommonResult.toAjax(AssetsAssetAuditAlertService.removeAssetAuditAlert(Arrays.asList(ids)));
    }
}