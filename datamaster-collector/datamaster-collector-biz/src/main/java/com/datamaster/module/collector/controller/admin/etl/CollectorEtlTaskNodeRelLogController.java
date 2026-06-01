

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
import com.datamaster.module.collector.controller.admin.etl.vo.CollectorEtlTaskNodeRelLogPageReqVO;
import com.datamaster.module.collector.controller.admin.etl.vo.CollectorEtlTaskNodeRelLogRespVO;
import com.datamaster.module.collector.controller.admin.etl.vo.CollectorEtlTaskNodeRelLogSaveReqVO;
import com.datamaster.module.collector.convert.etl.CollectorEtlTaskNodeRelLogConvert;
import com.datamaster.module.collector.dal.dataobject.etl.CollectorEtlTaskNodeRelLogDO;
import com.datamaster.module.collector.service.etl.ICollectorEtlTaskNodeRelLogService;

/**
 * 数据集成任务节点关系-日志Controller
 *
 * @author DATAMASTER
 * @date 2025-02-13
 */
@Tag(name = "数据集成任务节点关系-日志")
@RestController
@RequestMapping("/col/etlTaskNodeRelLog")
@Validated
public class CollectorEtlTaskNodeRelLogController extends BaseController {
    @Resource
    private ICollectorEtlTaskNodeRelLogService CollectorEtlTaskNodeRelLogService;

    @Operation(summary = "查询数据集成任务节点关系-日志列表")
//    @PreAuthorize("@ss.hasPermi('dpp:etlTaskNodeRelLog:list')")
    @GetMapping("/list")
    public CommonResult<PageResult<CollectorEtlTaskNodeRelLogRespVO>> list(CollectorEtlTaskNodeRelLogPageReqVO CollectorEtlTaskNodeRelLog) {
        PageResult<CollectorEtlTaskNodeRelLogDO> page = CollectorEtlTaskNodeRelLogService.getCollectorEtlTaskNodeRelLogPage(CollectorEtlTaskNodeRelLog);
        return CommonResult.success(BeanUtils.toBean(page, CollectorEtlTaskNodeRelLogRespVO.class));
    }

    @Operation(summary = "导出数据集成任务节点关系-日志列表")
//    @PreAuthorize("@ss.hasPermi('dpp:etlTaskNodeRelLog:export')")
    @Log(title = "数据集成任务节点关系-日志", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, CollectorEtlTaskNodeRelLogPageReqVO exportReqVO) {
        exportReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<CollectorEtlTaskNodeRelLogDO> list = (List<CollectorEtlTaskNodeRelLogDO>) CollectorEtlTaskNodeRelLogService.getCollectorEtlTaskNodeRelLogPage(exportReqVO).getRows();
        ExcelUtil<CollectorEtlTaskNodeRelLogRespVO> util = new ExcelUtil<>(CollectorEtlTaskNodeRelLogRespVO.class);
        util.exportExcel(response, CollectorEtlTaskNodeRelLogConvert.INSTANCE.convertToRespVOList(list), "应用管理数据");
    }

    @Operation(summary = "导入数据集成任务节点关系-日志列表")
//    @PreAuthorize("@ss.hasPermi('dpp:etlTaskNodeRelLog:import')")
    @Log(title = "数据集成任务节点关系-日志", businessType = BusinessType.IMPORT)
    @PostMapping("/importData")
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception {
        ExcelUtil<CollectorEtlTaskNodeRelLogRespVO> util = new ExcelUtil<>(CollectorEtlTaskNodeRelLogRespVO.class);
        List<CollectorEtlTaskNodeRelLogRespVO> importExcelList = util.importExcel(file.getInputStream());
        String operName = getUsername();
        String message = CollectorEtlTaskNodeRelLogService.importCollectorEtlTaskNodeRelLog(importExcelList, updateSupport, operName);
        return success(message);
    }

    @Operation(summary = "获取数据集成任务节点关系-日志详细信息")
//    @PreAuthorize("@ss.hasPermi('dpp:etlTaskNodeRelLog:query')")
    @GetMapping(value = "/{id}")
    public CommonResult<CollectorEtlTaskNodeRelLogRespVO> getInfo(@PathVariable("id") Long id) {
        CollectorEtlTaskNodeRelLogDO CollectorEtlTaskNodeRelLogDO = CollectorEtlTaskNodeRelLogService.getCollectorEtlTaskNodeRelLogById(id);
        return CommonResult.success(BeanUtils.toBean(CollectorEtlTaskNodeRelLogDO, CollectorEtlTaskNodeRelLogRespVO.class));
    }

    @Operation(summary = "新增数据集成任务节点关系-日志")
//    @PreAuthorize("@ss.hasPermi('dpp:etlTaskNodeRelLog:add')")
    @Log(title = "数据集成任务节点关系-日志", businessType = BusinessType.INSERT)
    @PostMapping
    public CommonResult<Long> add(@Valid @RequestBody CollectorEtlTaskNodeRelLogSaveReqVO CollectorEtlTaskNodeRelLog) {
        CollectorEtlTaskNodeRelLog.setCreatorId(getUserId());
        CollectorEtlTaskNodeRelLog.setCreateBy(getNickName());
        CollectorEtlTaskNodeRelLog.setCreateTime(DateUtil.date());
        return CommonResult.toAjax(CollectorEtlTaskNodeRelLogService.createCollectorEtlTaskNodeRelLog(CollectorEtlTaskNodeRelLog));
    }

    @Operation(summary = "修改数据集成任务节点关系-日志")
//    @PreAuthorize("@ss.hasPermi('dpp:etlTaskNodeRelLog:edit')")
    @Log(title = "数据集成任务节点关系-日志", businessType = BusinessType.UPDATE)
    @PutMapping
    public CommonResult<Integer> edit(@Valid @RequestBody CollectorEtlTaskNodeRelLogSaveReqVO CollectorEtlTaskNodeRelLog) {
        CollectorEtlTaskNodeRelLog.setUpdatorId(getUserId());
        CollectorEtlTaskNodeRelLog.setUpdateBy(getNickName());
        CollectorEtlTaskNodeRelLog.setUpdateTime(DateUtil.date());
        return CommonResult.toAjax(CollectorEtlTaskNodeRelLogService.updateCollectorEtlTaskNodeRelLog(CollectorEtlTaskNodeRelLog));
    }

    @Operation(summary = "删除数据集成任务节点关系-日志")
//    @PreAuthorize("@ss.hasPermi('dpp:etlTaskNodeRelLog:remove')")
    @Log(title = "数据集成任务节点关系-日志", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public CommonResult<Integer> remove(@PathVariable Long[] ids) {
        return CommonResult.toAjax(CollectorEtlTaskNodeRelLogService.removeCollectorEtlTaskNodeRelLog(Arrays.asList(ids)));
    }

}
