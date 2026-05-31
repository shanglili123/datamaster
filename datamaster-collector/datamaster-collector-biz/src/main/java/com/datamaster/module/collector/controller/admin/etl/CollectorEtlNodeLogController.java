

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
import com.datamaster.module.collector.controller.admin.etl.vo.CollectorEtlNodeLogPageReqVO;
import com.datamaster.module.collector.controller.admin.etl.vo.CollectorEtlNodeLogRespVO;
import com.datamaster.module.collector.controller.admin.etl.vo.CollectorEtlNodeLogSaveReqVO;
import com.datamaster.module.collector.convert.etl.CollectorEtlNodeLogConvert;
import com.datamaster.module.collector.dal.dataobject.etl.CollectorEtlNodeLogDO;
import com.datamaster.module.collector.service.etl.ICollectorEtlNodeLogService;

/**
 * 数据集成节点-日志Controller
 *
 * @author DATAMASTER
 * @date 2025-02-13
 */
@Tag(name = "数据集成节点-日志")
@RestController
@RequestMapping("/dpp/etlNodeLog")
@Validated
public class CollectorEtlNodeLogController extends BaseController {
    @Resource
    private ICollectorEtlNodeLogService CollectorEtlNodeLogService;

    @Operation(summary = "查询数据集成节点-日志列表")
//    @PreAuthorize("@ss.hasPermi('dpp:etlNodeLog:list')")
    @GetMapping("/list")
    public CommonResult<PageResult<CollectorEtlNodeLogRespVO>> list(CollectorEtlNodeLogPageReqVO CollectorEtlNodeLog) {
        PageResult<CollectorEtlNodeLogDO> page = CollectorEtlNodeLogService.getCollectorEtlNodeLogPage(CollectorEtlNodeLog);
        return CommonResult.success(BeanUtils.toBean(page, CollectorEtlNodeLogRespVO.class));
    }

    @Operation(summary = "导出数据集成节点-日志列表")
//    @PreAuthorize("@ss.hasPermi('dpp:etlNodeLog:export')")
    @Log(title = "数据集成节点-日志", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, CollectorEtlNodeLogPageReqVO exportReqVO) {
        exportReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<CollectorEtlNodeLogDO> list = (List<CollectorEtlNodeLogDO>) CollectorEtlNodeLogService.getCollectorEtlNodeLogPage(exportReqVO).getRows();
        ExcelUtil<CollectorEtlNodeLogRespVO> util = new ExcelUtil<>(CollectorEtlNodeLogRespVO.class);
        util.exportExcel(response, CollectorEtlNodeLogConvert.INSTANCE.convertToRespVOList(list), "应用管理数据");
    }

    @Operation(summary = "导入数据集成节点-日志列表")
//    @PreAuthorize("@ss.hasPermi('dpp:etlNodeLog:import')")
    @Log(title = "数据集成节点-日志", businessType = BusinessType.IMPORT)
    @PostMapping("/importData")
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception {
        ExcelUtil<CollectorEtlNodeLogRespVO> util = new ExcelUtil<>(CollectorEtlNodeLogRespVO.class);
        List<CollectorEtlNodeLogRespVO> importExcelList = util.importExcel(file.getInputStream());
        String operName = getUsername();
        String message = CollectorEtlNodeLogService.importCollectorEtlNodeLog(importExcelList, updateSupport, operName);
        return success(message);
    }

    @Operation(summary = "获取数据集成节点-日志详细信息")
//    @PreAuthorize("@ss.hasPermi('dpp:etlNodeLog:query')")
    @GetMapping(value = "/{id}")
    public CommonResult<CollectorEtlNodeLogRespVO> getInfo(@PathVariable("id") Long id) {
        CollectorEtlNodeLogDO CollectorEtlNodeLogDO = CollectorEtlNodeLogService.getCollectorEtlNodeLogById(id);
        return CommonResult.success(BeanUtils.toBean(CollectorEtlNodeLogDO, CollectorEtlNodeLogRespVO.class));
    }

    @Operation(summary = "新增数据集成节点-日志")
//    @PreAuthorize("@ss.hasPermi('dpp:etlNodeLog:add')")
    @Log(title = "数据集成节点-日志", businessType = BusinessType.INSERT)
    @PostMapping
    public CommonResult<Long> add(@Valid @RequestBody CollectorEtlNodeLogSaveReqVO CollectorEtlNodeLog) {
        CollectorEtlNodeLog.setCreatorId(getUserId());
        CollectorEtlNodeLog.setCreateBy(getNickName());
        CollectorEtlNodeLog.setCreateTime(DateUtil.date());
        return CommonResult.toAjax(CollectorEtlNodeLogService.createCollectorEtlNodeLog(CollectorEtlNodeLog));
    }

    @Operation(summary = "修改数据集成节点-日志")
//    @PreAuthorize("@ss.hasPermi('dpp:etlNodeLog:edit')")
    @Log(title = "数据集成节点-日志", businessType = BusinessType.UPDATE)
    @PutMapping
    public CommonResult<Integer> edit(@Valid @RequestBody CollectorEtlNodeLogSaveReqVO CollectorEtlNodeLog) {
        CollectorEtlNodeLog.setUpdatorId(getUserId());
        CollectorEtlNodeLog.setUpdateBy(getNickName());
        CollectorEtlNodeLog.setUpdateTime(DateUtil.date());
        return CommonResult.toAjax(CollectorEtlNodeLogService.updateCollectorEtlNodeLog(CollectorEtlNodeLog));
    }

    @Operation(summary = "删除数据集成节点-日志")
//    @PreAuthorize("@ss.hasPermi('dpp:etlNodeLog:remove')")
    @Log(title = "数据集成节点-日志", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public CommonResult<Integer> remove(@PathVariable Long[] ids) {
        return CommonResult.toAjax(CollectorEtlNodeLogService.removeCollectorEtlNodeLog(Arrays.asList(ids)));
    }

}
