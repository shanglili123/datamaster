package com.datamaster.module.catalog.controller.admin.tableLog;

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
import com.datamaster.module.catalog.controller.admin.tableLog.vo.CatalogTableLogPageReqVO;
import com.datamaster.module.catalog.controller.admin.tableLog.vo.CatalogTableLogRespVO;
import com.datamaster.module.catalog.controller.admin.tableLog.vo.CatalogTableLogSaveReqVO;
import com.datamaster.module.catalog.convert.tableLog.CatalogTableLogConvert;
import com.datamaster.module.catalog.dal.dataobject.tableLog.CatalogTableLogDO;
import com.datamaster.module.catalog.service.tableLog.ICatalogTableLogService;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.util.Arrays;
import java.util.List;

/**
 * 元数据信息 - 日志Controller
 *
 * @author DATAMASTER
 * @date 2026-03-10
 */
@Tag(name = "元数据信息 - 日志")
@RestController
@RequestMapping("/mc/catalogTableLog")
@Validated
public class CatalogTableLogController extends BaseController {
    @Resource
    private ICatalogTableLogService CatalogTableLogService;

    @Operation(summary = "查询元数据信息 - 日志列表")
    @GetMapping("/list")
    public CommonResult<PageResult<CatalogTableLogRespVO>> list(CatalogTableLogPageReqVO CatalogTableLog) {
        PageResult<CatalogTableLogDO> page = CatalogTableLogService.getCatalogTableLogPage(CatalogTableLog);
        return CommonResult.success(BeanUtils.toBean(page, CatalogTableLogRespVO.class));
    }

    @Operation(summary = "导出元数据信息 - 日志列表")
    @Log(title = "元数据信息 - 日志", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, CatalogTableLogPageReqVO exportReqVO) {
        exportReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<CatalogTableLogDO> list = (List<CatalogTableLogDO>) CatalogTableLogService.getCatalogTableLogPage(exportReqVO).getRows();
        ExcelUtil<CatalogTableLogRespVO> util = new ExcelUtil<>(CatalogTableLogRespVO.class);
        util.exportExcel(response, CatalogTableLogConvert.INSTANCE.convertToRespVOList(list), "应用管理数据");
    }

    @Operation(summary = "获取元数据信息 - 日志详细信息")
    @GetMapping(value = "/{id}")
    public CommonResult<CatalogTableLogRespVO> getInfo(@PathVariable("id") Long id) {
        CatalogTableLogDO CatalogTableLogDO = CatalogTableLogService.getCatalogTableLogById(id);
        return CommonResult.success(BeanUtils.toBean(CatalogTableLogDO, CatalogTableLogRespVO.class));
    }

    @Operation(summary = "新增元数据信息 - 日志")
    @Log(title = "元数据信息 - 日志", businessType = BusinessType.INSERT)
    @PostMapping
    public CommonResult<Long> add(@Valid @RequestBody CatalogTableLogSaveReqVO CatalogTableLog) {
        CatalogTableLog.setCreatorId(getUserId());
        CatalogTableLog.setCreateBy(getNickName());
        CatalogTableLog.setCreateTime(DateUtil.date());
        return CommonResult.toAjax(CatalogTableLogService.createCatalogTableLog(CatalogTableLog));
    }

    @Operation(summary = "修改元数据信息 - 日志")
    @Log(title = "元数据信息 - 日志", businessType = BusinessType.UPDATE)
    @PutMapping
    public CommonResult<Integer> edit(@Valid @RequestBody CatalogTableLogSaveReqVO CatalogTableLog) {
        CatalogTableLog.setUpdatorId(getUserId());
        CatalogTableLog.setUpdateBy(getNickName());
        CatalogTableLog.setUpdateTime(DateUtil.date());
        return CommonResult.toAjax(CatalogTableLogService.updateCatalogTableLog(CatalogTableLog));
    }

    @Operation(summary = "删除元数据信息 - 日志")
    @Log(title = "元数据信息 - 日志", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public CommonResult<Integer> remove(@PathVariable Long[] ids) {
        return CommonResult.toAjax(CatalogTableLogService.removeCatalogTableLog(Arrays.asList(ids)));
    }

}
