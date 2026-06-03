package com.datamaster.module.assets.controller.admin.assetchild.operate;

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
import com.datamaster.common.exception.enums.GlobalErrorCodeConstants;
import com.datamaster.module.assets.controller.admin.assetchild.operate.vo.AssetsAssetOperateLogPageReqVO;
import com.datamaster.module.assets.controller.admin.assetchild.operate.vo.AssetsAssetOperateLogRespVO;
import com.datamaster.module.assets.controller.admin.assetchild.operate.vo.AssetsAssetOperateLogSaveReqVO;
import com.datamaster.module.assets.convert.assetchild.operate.AssetsAssetOperateLogConvert;
import com.datamaster.module.assets.dal.dataobject.assetchild.operate.AssetsAssetOperateLogDO;
import com.datamaster.module.assets.service.assetchild.operate.IAssetsAssetOperateLogService;

/**
 * 数据资产操作记录Controller * * @author DATAMASTER * @date 2025-05-09
 */
@Tag(name = "数据资产操作记录")
@RestController
@RequestMapping("/ast/assetOperateLog")
@Validated
public class AssetsAssetOperateLogController extends BaseController {
    @Resource
    private IAssetsAssetOperateLogService AssetsAssetOperateLogService;

    @Operation(summary = "查询数据资产操作记录列表")
    @PreAuthorize("@ss.hasPermi('da:assetOperateLog:list')")
    @GetMapping("/list")
    public CommonResult<PageResult<AssetsAssetOperateLogRespVO>> list(AssetsAssetOperateLogPageReqVO AssetsAssetOperateLog) {
        PageResult<AssetsAssetOperateLogDO> page = AssetsAssetOperateLogService.getAssetOperateLogPage(AssetsAssetOperateLog);
        return CommonResult.success(BeanUtils.toBean(page, AssetsAssetOperateLogRespVO.class));
    }

    @Operation(summary = "导出数据资产操作记录列表")
    @PreAuthorize("@ss.hasPermi('da:assetOperateLog:export')")
    @Log(title = "数据资产操作记录", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, AssetsAssetOperateLogPageReqVO exportReqVO) {
        exportReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<AssetsAssetOperateLogDO> list = (List<AssetsAssetOperateLogDO>) AssetsAssetOperateLogService.getAssetOperateLogPage(exportReqVO).getRows();
        ExcelUtil<AssetsAssetOperateLogRespVO> util = new ExcelUtil<>(AssetsAssetOperateLogRespVO.class);
        util.exportExcel(response, AssetsAssetOperateLogConvert.INSTANCE.convertToRespVOList(list), "应用管理数据");
    }

    @Operation(summary = "导入数据资产操作记录列表")
    @PreAuthorize("@ss.hasPermi('da:assetOperateLog:import')")
    @Log(title = "数据资产操作记录", businessType = BusinessType.IMPORT)
    @PostMapping("/importData")
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception {
        ExcelUtil<AssetsAssetOperateLogRespVO> util = new ExcelUtil<>(AssetsAssetOperateLogRespVO.class);
        List<AssetsAssetOperateLogRespVO> importExcelList = util.importExcel(file.getInputStream());
        String operName = getUsername();
        String message = AssetsAssetOperateLogService.importAssetOperateLog(importExcelList, updateSupport, operName);
        return success(message);
    }

    @Operation(summary = "获取数据资产操作记录详细信息")
    @PreAuthorize("@ss.hasPermi('da:assetOperateLog:query')")
    @GetMapping(value = "/{id}")
    public CommonResult<AssetsAssetOperateLogRespVO> getInfo(@PathVariable("id") Long id) {
        AssetsAssetOperateLogDO AssetsAssetOperateLogDO = AssetsAssetOperateLogService.getAssetOperateLogById(id);
        return CommonResult.success(BeanUtils.toBean(AssetsAssetOperateLogDO, AssetsAssetOperateLogRespVO.class));
    }

    @Operation(summary = "新增数据资产操作记录")
    @PreAuthorize("@ss.hasPermi('da:assetOperateLog:add')")
    @Log(title = "数据资产操作记录", businessType = BusinessType.INSERT)
    @PostMapping
    public CommonResult<Long> add(@Valid @RequestBody AssetsAssetOperateLogSaveReqVO AssetsAssetOperateLog) {
        AssetsAssetOperateLog.setCreatorId(getUserId());
        AssetsAssetOperateLog.setCreateBy(getNickName());
        AssetsAssetOperateLog.setCreateTime(DateUtil.date());
        return CommonResult.toAjax(AssetsAssetOperateLogService.createAssetOperateLog(AssetsAssetOperateLog));
    }

    @Operation(summary = "修改数据资产操作记录")
    @PreAuthorize("@ss.hasPermi('da:assetOperateLog:edit')")
    @Log(title = "数据资产操作记录", businessType = BusinessType.UPDATE)
    @PutMapping
    public CommonResult<Integer> edit(@Valid @RequestBody AssetsAssetOperateLogSaveReqVO AssetsAssetOperateLog) {
        AssetsAssetOperateLog.setUpdatorId(getUserId());
        AssetsAssetOperateLog.setUpdateBy(getNickName());
        AssetsAssetOperateLog.setUpdateTime(DateUtil.date());
        return CommonResult.toAjax(AssetsAssetOperateLogService.updateAssetOperateLog(AssetsAssetOperateLog));
    }

    @Operation(summary = "删除数据资产操作记录")
    @PreAuthorize("@ss.hasPermi('da:assetOperateLog:remove')")
    @Log(title = "数据资产操作记录", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public CommonResult<Integer> remove(@PathVariable Long[] ids) {
        return CommonResult.toAjax(AssetsAssetOperateLogService.removeAssetOperateLog(Arrays.asList(ids)));
    }

    @Operation(summary = "回滚")
    @PreAuthorize("@ss.hasPermi('da:assetOperateLog:add')")
    @PostMapping("/rollBack/{id}")
    public R rollBack(@PathVariable Long id) {
        AssetsAssetOperateLogService.rollBack(id);
        return R.ok();
    }

    @Operation(summary = "查询数据资产操作记录列表")
    @PreAuthorize("@ss.hasPermi('da:assetOperateLog:list')")
    @GetMapping("/queryAssetOperateLogPage")
    public CommonResult<PageResult<AssetsAssetOperateLogRespVO>> queryAssetOperateLogPage(AssetsAssetOperateLogPageReqVO AssetsAssetOperateLog) {
        PageResult<AssetsAssetOperateLogDO> page = AssetsAssetOperateLogService.queryAssetOperateLogPage(AssetsAssetOperateLog);
        return CommonResult.success(BeanUtils.toBean(page, AssetsAssetOperateLogRespVO.class));
    }
}