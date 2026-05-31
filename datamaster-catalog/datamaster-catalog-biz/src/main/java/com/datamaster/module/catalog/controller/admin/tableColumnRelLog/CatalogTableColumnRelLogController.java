package com.datamaster.module.catalog.controller.admin.tableColumnRelLog;

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
import com.datamaster.module.catalog.controller.admin.tableColumnRelLog.vo.CatalogTableColumnRelLogPageReqVO;
import com.datamaster.module.catalog.controller.admin.tableColumnRelLog.vo.CatalogTableColumnRelLogRespVO;
import com.datamaster.module.catalog.controller.admin.tableColumnRelLog.vo.CatalogTableColumnRelLogSaveReqVO;
import com.datamaster.module.catalog.convert.tableColumnRelLog.CatalogTableColumnRelLogConvert;
import com.datamaster.module.catalog.dal.dataobject.tableColumnRelLog.CatalogTableColumnRelLogDO;
import com.datamaster.module.catalog.service.tableColumnRelLog.ICatalogTableColumnRelLogService;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.util.Arrays;
import java.util.List;

/**
 * 元数据数据库与信息及字段信息关系-日志Controller
 *
 * @author DATAMASTER
 * @date 2026-03-10
 */
@Tag(name = "元数据数据库与信息及字段信息关系-日志")
@RestController
@RequestMapping("/mc/catalogTableColumnRelLog")
@Validated
public class CatalogTableColumnRelLogController extends BaseController {
    @Resource
    private ICatalogTableColumnRelLogService CatalogTableColumnRelLogService;

    @Operation(summary = "查询元数据数据库与信息及字段信息关系-日志列表")
//    @PreAuthorize("@ss.hasPermi('mc:tableColumnRelLog:TableColumnRelLog:list')")
    @GetMapping("/list")
    public CommonResult<PageResult<CatalogTableColumnRelLogRespVO>> list(CatalogTableColumnRelLogPageReqVO CatalogTableColumnRelLog) {
        PageResult<CatalogTableColumnRelLogDO> page = CatalogTableColumnRelLogService.getCatalogTableColumnRelLogPage(CatalogTableColumnRelLog);
        return CommonResult.success(BeanUtils.toBean(page, CatalogTableColumnRelLogRespVO.class));
    }

    @Operation(summary = "导出元数据数据库与信息及字段信息关系-日志列表")
//    @PreAuthorize("@ss.hasPermi('mc:tableColumnRelLog:TableColumnRelLog:export')")
    @Log(title = "元数据数据库与信息及字段信息关系-日志", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, CatalogTableColumnRelLogPageReqVO exportReqVO) {
        exportReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<CatalogTableColumnRelLogDO> list = (List<CatalogTableColumnRelLogDO>) CatalogTableColumnRelLogService.getCatalogTableColumnRelLogPage(exportReqVO).getRows();
        ExcelUtil<CatalogTableColumnRelLogRespVO> util = new ExcelUtil<>(CatalogTableColumnRelLogRespVO.class);
        util.exportExcel(response, CatalogTableColumnRelLogConvert.INSTANCE.convertToRespVOList(list), "应用管理数据");
    }

    @Operation(summary = "获取元数据数据库与信息及字段信息关系-日志详细信息")
//    @PreAuthorize("@ss.hasPermi('mc:tableColumnRelLog:TableColumnRelLog:query')")
    @GetMapping(value = "/{id}")
    public CommonResult<CatalogTableColumnRelLogRespVO> getInfo(@PathVariable("id") Long id) {
        CatalogTableColumnRelLogDO CatalogTableColumnRelLogDO = CatalogTableColumnRelLogService.getCatalogTableColumnRelLogById(id);
        return CommonResult.success(BeanUtils.toBean(CatalogTableColumnRelLogDO, CatalogTableColumnRelLogRespVO.class));
    }

    @Operation(summary = "新增元数据数据库与信息及字段信息关系-日志")
//    @PreAuthorize("@ss.hasPermi('mc:tableColumnRelLog:TableColumnRelLog:add')")
    @Log(title = "元数据数据库与信息及字段信息关系-日志", businessType = BusinessType.INSERT)
    @PostMapping
    public CommonResult<Long> add(@Valid @RequestBody CatalogTableColumnRelLogSaveReqVO CatalogTableColumnRelLog) {
        CatalogTableColumnRelLog.setCreatorId(getUserId());
        CatalogTableColumnRelLog.setCreateBy(getNickName());
        CatalogTableColumnRelLog.setCreateTime(DateUtil.date());
        return CommonResult.toAjax(CatalogTableColumnRelLogService.createCatalogTableColumnRelLog(CatalogTableColumnRelLog));
    }

    @Operation(summary = "修改元数据数据库与信息及字段信息关系-日志")
//    @PreAuthorize("@ss.hasPermi('mc:tableColumnRelLog:TableColumnRelLog:edit')")
    @Log(title = "元数据数据库与信息及字段信息关系-日志", businessType = BusinessType.UPDATE)
    @PutMapping
    public CommonResult<Integer> edit(@Valid @RequestBody CatalogTableColumnRelLogSaveReqVO CatalogTableColumnRelLog) {
        CatalogTableColumnRelLog.setUpdatorId(getUserId());
        CatalogTableColumnRelLog.setUpdateBy(getNickName());
        CatalogTableColumnRelLog.setUpdateTime(DateUtil.date());
        return CommonResult.toAjax(CatalogTableColumnRelLogService.updateCatalogTableColumnRelLog(CatalogTableColumnRelLog));
    }

    @Operation(summary = "删除元数据数据库与信息及字段信息关系-日志")
//    @PreAuthorize("@ss.hasPermi('mc:tableColumnRelLog:TableColumnRelLog:remove')")
    @Log(title = "元数据数据库与信息及字段信息关系-日志", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public CommonResult<Integer> remove(@PathVariable Long[] ids) {
        return CommonResult.toAjax(CatalogTableColumnRelLogService.removeCatalogTableColumnRelLog(Arrays.asList(ids)));
    }

}
