package com.datamaster.module.catalog.controller.admin.task;

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
import com.datamaster.module.catalog.controller.admin.task.vo.CatalogTaskInstanceLogPageReqVO;
import com.datamaster.module.catalog.controller.admin.task.vo.CatalogTaskInstanceLogRespVO;
import com.datamaster.module.catalog.controller.admin.task.vo.CatalogTaskInstanceLogSaveReqVO;
import com.datamaster.module.catalog.convert.task.CatalogTaskInstanceLogConvert;
import com.datamaster.module.catalog.dal.dataobject.task.CatalogTaskInstanceLogDO;
import com.datamaster.module.catalog.service.task.ICatalogTaskInstanceLogService;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.util.Arrays;
import java.util.List;

/**
 * 采集任务实例-日志Controller
 *
 * @author DATAMASTER
 * @date 2025-12-16
 */
@Tag(name = "采集任务实例-日志")
@RestController
@RequestMapping("/cat/taskInstanceLog")
@Validated
public class CatalogTaskInstanceLogController extends BaseController {
    @Resource
    private ICatalogTaskInstanceLogService CatalogTaskInstanceLogService;

    @Operation(summary = "查询采集任务实例-日志列表")
    @GetMapping("/list")
    public CommonResult<PageResult<CatalogTaskInstanceLogRespVO>> list(CatalogTaskInstanceLogPageReqVO CatalogTaskInstanceLog) {
        PageResult<CatalogTaskInstanceLogDO> page = CatalogTaskInstanceLogService.getCatalogTaskInstanceLogPage(CatalogTaskInstanceLog);
        return CommonResult.success(BeanUtils.toBean(page, CatalogTaskInstanceLogRespVO.class));
    }

    @Operation(summary = "导出采集任务实例-日志列表")
    @Log(title = "采集任务实例-日志", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, CatalogTaskInstanceLogPageReqVO exportReqVO) {
        exportReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<CatalogTaskInstanceLogDO> list = (List<CatalogTaskInstanceLogDO>) CatalogTaskInstanceLogService.getCatalogTaskInstanceLogPage(exportReqVO).getRows();
        ExcelUtil<CatalogTaskInstanceLogRespVO> util = new ExcelUtil<>(CatalogTaskInstanceLogRespVO.class);
        util.exportExcel(response, CatalogTaskInstanceLogConvert.INSTANCE.convertToRespVOList(list), "应用管理数据");
    }

    @Operation(summary = "获取采集任务实例-日志详细信息")
    @GetMapping(value = "/{id}")
    public CommonResult<CatalogTaskInstanceLogRespVO> getInfo(@PathVariable("id") Long id) {
        CatalogTaskInstanceLogDO CatalogTaskInstanceLogDO = CatalogTaskInstanceLogService.getCatalogTaskInstanceLogById(id);
        return CommonResult.success(BeanUtils.toBean(CatalogTaskInstanceLogDO, CatalogTaskInstanceLogRespVO.class));
    }

    @Operation(summary = "新增采集任务实例-日志")
    @Log(title = "采集任务实例-日志", businessType = BusinessType.INSERT)
    @PostMapping
    public CommonResult<Long> add(@Valid @RequestBody CatalogTaskInstanceLogSaveReqVO CatalogTaskInstanceLog) {
        CatalogTaskInstanceLog.setCreatorId(getUserId());
        CatalogTaskInstanceLog.setCreateBy(getNickName());
        CatalogTaskInstanceLog.setCreateTime(DateUtil.date());
        return CommonResult.toAjax(CatalogTaskInstanceLogService.createCatalogTaskInstanceLog(CatalogTaskInstanceLog));
    }

    @Operation(summary = "修改采集任务实例-日志")
    @Log(title = "采集任务实例-日志", businessType = BusinessType.UPDATE)
    @PutMapping
    public CommonResult<Integer> edit(@Valid @RequestBody CatalogTaskInstanceLogSaveReqVO CatalogTaskInstanceLog) {
        CatalogTaskInstanceLog.setUpdatorId(getUserId());
        CatalogTaskInstanceLog.setUpdateBy(getNickName());
        CatalogTaskInstanceLog.setUpdateTime(DateUtil.date());
        return CommonResult.toAjax(CatalogTaskInstanceLogService.updateCatalogTaskInstanceLog(CatalogTaskInstanceLog));
    }

    @Operation(summary = "删除采集任务实例-日志")
    @Log(title = "采集任务实例-日志", businessType = BusinessType.DELETE)
    @DeleteMapping("/{taskInstanceIds}")
    public CommonResult<Integer> remove(@PathVariable Long[] ids) {
        return CommonResult.toAjax(CatalogTaskInstanceLogService.removeCatalogTaskInstanceLog(Arrays.asList(ids)));
    }

}
