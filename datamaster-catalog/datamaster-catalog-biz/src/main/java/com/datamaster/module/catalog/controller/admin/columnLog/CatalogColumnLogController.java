package com.datamaster.module.catalog.controller.admin.columnLog;

import cn.hutool.core.date.DateUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import com.datamaster.common.annotation.Log;
import com.datamaster.common.core.controller.BaseController;
import com.datamaster.common.core.domain.CommonResult;
import com.datamaster.common.core.page.PageParam;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.common.enums.BusinessType;
import com.datamaster.common.utils.object.BeanUtils;
import com.datamaster.common.utils.poi.ExcelUtil;
import com.datamaster.module.catalog.controller.admin.columnLog.vo.CatalogColumnLogPageReqVO;
import com.datamaster.module.catalog.controller.admin.columnLog.vo.CatalogColumnLogRespVO;
import com.datamaster.module.catalog.controller.admin.columnLog.vo.CatalogColumnLogSaveReqVO;
import com.datamaster.module.catalog.convert.columnLog.CatalogColumnLogConvert;
import com.datamaster.module.catalog.dal.dataobject.columnLog.CatalogColumnLogDO;
import com.datamaster.module.catalog.service.columnLog.ICatalogColumnLogService;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.util.Arrays;
import java.util.List;

/**
 * 元数据字段信息 - 日志Controller
 *
 * @author DATAMASTER
 * @date 2026-03-10
 */
@Tag(name = "元数据字段信息 - 日志")
@RestController
@RequestMapping("/mc/catalogColumnLog")
@Validated
public class CatalogColumnLogController extends BaseController {
    @Resource
    private ICatalogColumnLogService CatalogColumnLogService;

    @Operation(summary = "查询元数据字段信息 - 日志列表")
    @GetMapping("/list")
    public CommonResult<PageResult<CatalogColumnLogRespVO>> list(CatalogColumnLogPageReqVO CatalogColumnLog) {
        PageResult<CatalogColumnLogDO> page = CatalogColumnLogService.getCatalogColumnLogPage(CatalogColumnLog);
        return CommonResult.success(BeanUtils.toBean(page, CatalogColumnLogRespVO.class));
    }

    @Operation(summary = "导出元数据字段信息 - 日志列表")
    @Log(title = "元数据字段信息 - 日志", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, CatalogColumnLogPageReqVO exportReqVO) {
        exportReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<CatalogColumnLogDO> list = (List<CatalogColumnLogDO>) CatalogColumnLogService.getCatalogColumnLogPage(exportReqVO).getRows();
        ExcelUtil<CatalogColumnLogRespVO> util = new ExcelUtil<>(CatalogColumnLogRespVO.class);
        util.exportExcel(response, CatalogColumnLogConvert.INSTANCE.convertToRespVOList(list), "应用管理数据");
    }

    @Operation(summary = "获取元数据字段信息 - 日志详细信息")
    @GetMapping(value = "/{id}")
    public CommonResult<CatalogColumnLogRespVO> getInfo(@PathVariable("id") Long id) {
        CatalogColumnLogDO CatalogColumnLogDO = CatalogColumnLogService.getCatalogColumnLogById(id);
        return CommonResult.success(BeanUtils.toBean(CatalogColumnLogDO, CatalogColumnLogRespVO.class));
    }

    @Operation(summary = "新增元数据字段信息 - 日志")
    @Log(title = "元数据字段信息 - 日志", businessType = BusinessType.INSERT)
    @PostMapping
    public CommonResult<Long> add(@Valid @RequestBody CatalogColumnLogSaveReqVO CatalogColumnLog) {
        CatalogColumnLog.setCreatorId(getUserId());
        CatalogColumnLog.setCreateBy(getNickName());
        CatalogColumnLog.setCreateTime(DateUtil.date());
        return CommonResult.toAjax(CatalogColumnLogService.createCatalogColumnLog(CatalogColumnLog));
    }

    @Operation(summary = "修改元数据字段信息 - 日志")
    @Log(title = "元数据字段信息 - 日志", businessType = BusinessType.UPDATE)
    @PutMapping
    public CommonResult<Integer> edit(@Valid @RequestBody CatalogColumnLogSaveReqVO CatalogColumnLog) {
        CatalogColumnLog.setUpdatorId(getUserId());
        CatalogColumnLog.setUpdateBy(getNickName());
        CatalogColumnLog.setUpdateTime(DateUtil.date());
        return CommonResult.toAjax(CatalogColumnLogService.updateCatalogColumnLog(CatalogColumnLog));
    }

    @Operation(summary = "删除元数据字段信息 - 日志")
    @Log(title = "元数据字段信息 - 日志", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public CommonResult<Integer> remove(@PathVariable Long[] ids) {
        return CommonResult.toAjax(CatalogColumnLogService.removeCatalogColumnLog(Arrays.asList(ids)));
    }

}
