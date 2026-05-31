package com.datamaster.module.assets.controller.admin.daAssetApply;

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
import com.datamaster.module.assets.controller.admin.daAssetApply.vo.AssetsAssetApplyPageReqVO;
import com.datamaster.module.assets.controller.admin.daAssetApply.vo.AssetsAssetApplyRespVO;
import com.datamaster.module.assets.controller.admin.daAssetApply.vo.AssetsAssetApplySaveReqVO;
import com.datamaster.module.assets.convert.daAssetApply.AssetsAssetApplyConvert;
import com.datamaster.module.assets.dal.dataobject.daAssetApply.AssetsAssetApplyDO;
import com.datamaster.module.assets.service.daAssetApply.IAssetsAssetApplyService;

/**
 * 数据资产申请Controller * * @author shu * @date 2025-03-19
 */
@Tag(name = "数据资产申请")
@RestController
@RequestMapping("/da/assetApply")
@Validated
public class AssetsAssetApplyController extends BaseController {
    @Resource
    private IAssetsAssetApplyService AssetsAssetApplyService;

    @Operation(summary = "查询数据资产申请列表")
    @PreAuthorize("@ss.hasPermi('da:assetApply:list')")
    @GetMapping("/list")
    public CommonResult<PageResult<AssetsAssetApplyRespVO>> list(AssetsAssetApplyPageReqVO AssetsAssetApply) {
        AssetsAssetApply.setOrderByColumn("status");
        PageResult<AssetsAssetApplyDO> page = AssetsAssetApplyService.getDaAssetApplyPage(AssetsAssetApply);
        return CommonResult.success(BeanUtils.toBean(page, AssetsAssetApplyRespVO.class));
    }

    @Operation(summary = "导出数据资产申请列表")
    @PreAuthorize("@ss.hasPermi('da:assetApply:export')")
    @Log(title = "数据资产申请", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, AssetsAssetApplyPageReqVO exportReqVO) {
        exportReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<AssetsAssetApplyDO> list = (List<AssetsAssetApplyDO>) AssetsAssetApplyService.getDaAssetApplyPage(exportReqVO).getRows();
        ExcelUtil<AssetsAssetApplyRespVO> util = new ExcelUtil<>(AssetsAssetApplyRespVO.class);
        util.exportExcel(response, AssetsAssetApplyConvert.INSTANCE.convertToRespVOList(list), "应用管理数据");
    }

    @Operation(summary = "导入数据资产申请列表")
    @PreAuthorize("@ss.hasPermi('da:assetApply:import')")
    @Log(title = "数据资产申请", businessType = BusinessType.IMPORT)
    @PostMapping("/importData")
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception {
        ExcelUtil<AssetsAssetApplyRespVO> util = new ExcelUtil<>(AssetsAssetApplyRespVO.class);
        List<AssetsAssetApplyRespVO> importExcelList = util.importExcel(file.getInputStream());
        String operName = getUsername();
        String message = AssetsAssetApplyService.importDaAssetApply(importExcelList, updateSupport, operName);
        return success(message);
    }

    @Operation(summary = "获取数据资产申请详细信息")
    @PreAuthorize("@ss.hasPermi('da:assetApply:query')")
    @GetMapping(value = "/{id}")
    public CommonResult<AssetsAssetApplyRespVO> getInfo(@PathVariable("id") Long id) {
        AssetsAssetApplyDO AssetsAssetApplyDO = AssetsAssetApplyService.getDaAssetApplyById(id);
        return CommonResult.success(BeanUtils.toBean(AssetsAssetApplyDO, AssetsAssetApplyRespVO.class));
    }

    @Operation(summary = "新增数据资产申请")
    @PreAuthorize("@ss.hasAnyPermi('da:assetApply:add,da:asset:asset:add')")
    @Log(title = "数据资产申请", businessType = BusinessType.INSERT)
    @PostMapping
    public CommonResult<Long> add(@Valid @RequestBody AssetsAssetApplySaveReqVO AssetsAssetApply) {
        AssetsAssetApply.setCreatorId(getUserId());
        AssetsAssetApply.setCreateBy(getNickName());
        AssetsAssetApply.setCreateTime(DateUtil.date());
        return CommonResult.toAjax(AssetsAssetApplyService.createDaAssetApply(AssetsAssetApply));
    }

    @Operation(summary = "修改数据资产申请")
    @PreAuthorize("@ss.hasPermi('da:assetApply:edit')")
    @Log(title = "数据资产申请", businessType = BusinessType.UPDATE)
    @PutMapping
    public CommonResult<Integer> edit(@Valid @RequestBody AssetsAssetApplySaveReqVO AssetsAssetApply) {
        AssetsAssetApply.setUpdatorId(getUserId());
        AssetsAssetApply.setUpdateBy(getNickName());
        AssetsAssetApply.setUpdateTime(DateUtil.date());
        return CommonResult.toAjax(AssetsAssetApplyService.updateDaAssetApply(AssetsAssetApply));
    }

    @Operation(summary = "删除数据资产申请")
    @PreAuthorize("@ss.hasPermi('da:assetApply:remove')")
    @Log(title = "数据资产申请", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public CommonResult<Integer> remove(@PathVariable Long[] ids) {
        return CommonResult.toAjax(AssetsAssetApplyService.removeDaAssetApply(Arrays.asList(ids)));
    }
}