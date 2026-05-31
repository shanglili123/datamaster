

package com.datamaster.module.collector.controller.admin.etl;

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
import com.datamaster.module.collector.controller.admin.etl.vo.CollectorEtlTaskLogPageReqVO;
import com.datamaster.module.collector.controller.admin.etl.vo.CollectorEtlTaskLogRespVO;
import com.datamaster.module.collector.controller.admin.etl.vo.CollectorEtlTaskLogSaveReqVO;
import com.datamaster.module.collector.convert.etl.CollectorEtlTaskLogConvert;
import com.datamaster.module.collector.dal.dataobject.etl.CollectorEtlTaskLogDO;
import com.datamaster.module.collector.service.etl.ICollectorEtlTaskLogService;

/**
 * 数据集成任务-日志Controller
 *
 * @author DATAMASTER
 * @date 2025-02-13
 */
@Tag(name = "数据集成任务-日志")
@RestController
@RequestMapping("/dpp/etlTaskLog")
@Validated
public class CollectorEtlTaskLogController extends BaseController {
    @Resource
    private ICollectorEtlTaskLogService CollectorEtlTaskLogService;

    @Operation(summary = "查询数据集成任务-日志列表")
//    @PreAuthorize("@ss.hasPermi('dpp:etlTaskLog:list')")
    @GetMapping("/list")
    public CommonResult<PageResult<CollectorEtlTaskLogRespVO>> list(CollectorEtlTaskLogPageReqVO CollectorEtlTaskLog) {
        PageResult<CollectorEtlTaskLogDO> page = CollectorEtlTaskLogService.getCollectorEtlTaskLogPage(CollectorEtlTaskLog);
        return CommonResult.success(BeanUtils.toBean(page, CollectorEtlTaskLogRespVO.class));
    }

    @Operation(summary = "导出数据集成任务-日志列表")
//    @PreAuthorize("@ss.hasPermi('dpp:etlTaskLog:export')")
    @Log(title = "数据集成任务-日志", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, CollectorEtlTaskLogPageReqVO exportReqVO) {
        exportReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<CollectorEtlTaskLogDO> list = (List<CollectorEtlTaskLogDO>) CollectorEtlTaskLogService.getCollectorEtlTaskLogPage(exportReqVO).getRows();
        ExcelUtil<CollectorEtlTaskLogRespVO> util = new ExcelUtil<>(CollectorEtlTaskLogRespVO.class);
        util.exportExcel(response, CollectorEtlTaskLogConvert.INSTANCE.convertToRespVOList(list), "应用管理数据");
    }

    @Operation(summary = "导入数据集成任务-日志列表")
//    @PreAuthorize("@ss.hasPermi('dpp:etlTaskLog:import')")
    @Log(title = "数据集成任务-日志", businessType = BusinessType.IMPORT)
    @PostMapping("/importData")
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception {
        ExcelUtil<CollectorEtlTaskLogRespVO> util = new ExcelUtil<>(CollectorEtlTaskLogRespVO.class);
        List<CollectorEtlTaskLogRespVO> importExcelList = util.importExcel(file.getInputStream());
        String operName = getUsername();
        String message = CollectorEtlTaskLogService.importCollectorEtlTaskLog(importExcelList, updateSupport, operName);
        return success(message);
    }

    @Operation(summary = "获取数据集成任务-日志详细信息")
//    @PreAuthorize("@ss.hasPermi('dpp:etlTaskLog:query')")
    @GetMapping(value = "/{id}")
    public CommonResult<CollectorEtlTaskLogRespVO> getInfo(@PathVariable("id") Long id) {
        CollectorEtlTaskLogDO CollectorEtlTaskLogDO = CollectorEtlTaskLogService.getCollectorEtlTaskLogById(id);
        return CommonResult.success(BeanUtils.toBean(CollectorEtlTaskLogDO, CollectorEtlTaskLogRespVO.class));
    }

    @Operation(summary = "新增数据集成任务-日志")
//    @PreAuthorize("@ss.hasPermi('dpp:etlTaskLog:add')")
    @Log(title = "数据集成任务-日志", businessType = BusinessType.INSERT)
    @PostMapping
    public CommonResult<Long> add(@Valid @RequestBody CollectorEtlTaskLogSaveReqVO CollectorEtlTaskLog) {
        CollectorEtlTaskLog.setCreatorId(getUserId());
        CollectorEtlTaskLog.setCreateBy(getNickName());
        CollectorEtlTaskLog.setCreateTime(DateUtil.date());
        return CommonResult.toAjax(CollectorEtlTaskLogService.createCollectorEtlTaskLog(CollectorEtlTaskLog));
    }

    @Operation(summary = "修改数据集成任务-日志")
//    @PreAuthorize("@ss.hasPermi('dpp:etlTaskLog:edit')")
    @Log(title = "数据集成任务-日志", businessType = BusinessType.UPDATE)
    @PutMapping
    public CommonResult<Integer> edit(@Valid @RequestBody CollectorEtlTaskLogSaveReqVO CollectorEtlTaskLog) {
        CollectorEtlTaskLog.setUpdatorId(getUserId());
        CollectorEtlTaskLog.setUpdateBy(getNickName());
        CollectorEtlTaskLog.setUpdateTime(DateUtil.date());
        return CommonResult.toAjax(CollectorEtlTaskLogService.updateCollectorEtlTaskLog(CollectorEtlTaskLog));
    }

    @Operation(summary = "删除数据集成任务-日志")
//    @PreAuthorize("@ss.hasPermi('dpp:etlTaskLog:remove')")
    @Log(title = "数据集成任务-日志", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public CommonResult<Integer> remove(@PathVariable Long[] ids) {
        return CommonResult.toAjax(CollectorEtlTaskLogService.removeCollectorEtlTaskLog(Arrays.asList(ids)));
    }

}
