

package com.datamaster.module.collector.controller.admin.etl;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.util.Arrays;
import cn.hutool.core.date.DateUtil;

import java.util.Date;
import java.util.List;

import com.alibaba.fastjson2.JSONObject;
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
import com.datamaster.module.collector.controller.admin.etl.vo.*;
import com.datamaster.module.collector.controller.admin.qa.vo.CheckErrorDataReqDTO;
import com.datamaster.module.collector.convert.etl.CollectorEvaluateLogConvert;
import com.datamaster.module.collector.dal.dataobject.etl.CollectorEvaluateLogDO;
import com.datamaster.module.collector.service.etl.ICollectorEvaluateLogService;

/**
 * 评测规则结果Controller
 *
 * @author DATAMASTER
 * @date 2025-07-21
 */
@Tag(name = "评测规则结果")
@RestController
@RequestMapping("/col/evaluateLog")
@Validated
public class CollectorEvaluateLogController extends BaseController {
    @Resource
    private ICollectorEvaluateLogService CollectorEvaluateLogService;

    @Operation(summary = "查询评测规则结果列表")
//    @PreAuthorize("@ss.hasPermi('dpp:evaluateLog:list')")
    @GetMapping("/list")
    public CommonResult<PageResult<CollectorEvaluateLogRespVO>> list(CollectorEvaluateLogPageReqVO CollectorEvaluateLog) {
        PageResult<CollectorEvaluateLogDO> page = CollectorEvaluateLogService.getCollectorEvaluateLogPage(CollectorEvaluateLog);
        return CommonResult.success(BeanUtils.toBean(page, CollectorEvaluateLogRespVO.class));
    }

    @Operation(summary = "导出评测规则结果列表")
//    @PreAuthorize("@ss.hasPermi('dpp:evaluateLog:export')")
    @Log(title = "评测规则结果", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, CollectorEvaluateLogPageReqVO exportReqVO) {
        exportReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<CollectorEvaluateLogDO> list = (List<CollectorEvaluateLogDO>) CollectorEvaluateLogService.getCollectorEvaluateLogPage(exportReqVO).getRows();
        ExcelUtil<CollectorEvaluateLogRespVO> util = new ExcelUtil<>(CollectorEvaluateLogRespVO.class);
        util.exportExcel(response, CollectorEvaluateLogConvert.INSTANCE.convertToRespVOList(list), "应用管理数据");
    }

    @Operation(summary = "导入评测规则结果列表")
//    @PreAuthorize("@ss.hasPermi('dpp:evaluateLog:import')")
    @Log(title = "评测规则结果", businessType = BusinessType.IMPORT)
    @PostMapping("/importData")
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception {
        ExcelUtil<CollectorEvaluateLogRespVO> util = new ExcelUtil<>(CollectorEvaluateLogRespVO.class);
        List<CollectorEvaluateLogRespVO> importExcelList = util.importExcel(file.getInputStream());
        String operName = getUsername();
        String message = CollectorEvaluateLogService.importCollectorEvaluateLog(importExcelList, updateSupport, operName);
        return success(message);
    }

    @Operation(summary = "获取评测规则结果详细信息")
//    @PreAuthorize("@ss.hasPermi('dpp:evaluateLog:query')")
    @GetMapping(value = "/{id}")
    public CommonResult<CollectorEvaluateLogRespVO> getInfo(@PathVariable("id") Long id) {
        CollectorEvaluateLogDO CollectorEvaluateLogDO = CollectorEvaluateLogService.getCollectorEvaluateLogById(id);
        return CommonResult.success(BeanUtils.toBean(CollectorEvaluateLogDO, CollectorEvaluateLogRespVO.class));
    }

    @Operation(summary = "新增评测规则结果")
//    @PreAuthorize("@ss.hasPermi('dpp:evaluateLog:add')")
    @Log(title = "评测规则结果", businessType = BusinessType.INSERT)
    @PostMapping
    public CommonResult<Long> add(@Valid @RequestBody CollectorEvaluateLogSaveReqVO CollectorEvaluateLog) {
        CollectorEvaluateLog.setCreatorId(getUserId());
        CollectorEvaluateLog.setCreateBy(getNickName());
        CollectorEvaluateLog.setCreateTime(DateUtil.date());
        return CommonResult.toAjax(CollectorEvaluateLogService.createCollectorEvaluateLog(CollectorEvaluateLog));
    }

    @Operation(summary = "修改评测规则结果")
//    @PreAuthorize("@ss.hasPermi('dpp:evaluateLog:edit')")
    @Log(title = "评测规则结果", businessType = BusinessType.UPDATE)
    @PutMapping
    public CommonResult<Integer> edit(@Valid @RequestBody CollectorEvaluateLogSaveReqVO CollectorEvaluateLog) {
        CollectorEvaluateLog.setUpdatorId(getUserId());
        CollectorEvaluateLog.setUpdateBy(getNickName());
        CollectorEvaluateLog.setUpdateTime(DateUtil.date());
        return CommonResult.toAjax(CollectorEvaluateLogService.updateCollectorEvaluateLog(CollectorEvaluateLog));
    }

    @Operation(summary = "删除评测规则结果")
//    @PreAuthorize("@ss.hasPermi('dpp:evaluateLog:remove')")
    @Log(title = "评测规则结果", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public CommonResult<Integer> remove(@PathVariable Long[] ids) {
        return CommonResult.toAjax(CollectorEvaluateLogService.removeCollectorEvaluateLog(Arrays.asList(ids)));
    }

    @Operation(summary = "统计评测规则结果")
    @GetMapping("/statisticsEvaluateOne/{id}")
    public CommonResult<List<CollectorEvaluateLogStatisticsVO>> statisticsEvaluateOne(@PathVariable Long id) {
        return CommonResult.success(CollectorEvaluateLogService.statisticsEvaluateOne(id));
    }


    @Operation(summary = "统计评测规则结果")
    @GetMapping("/statisticsEvaluateTow")
    public CommonResult<JSONObject> statisticsEvaluateTow( Long id , Date deDate , Date oldDate , int type) {
        return CommonResult.success(CollectorEvaluateLogService.statisticsEvaluateTow(id , deDate , oldDate , type));
    }

    @Operation(summary = "统计评测规则结果")
    @GetMapping("/statisticsEvaluateTable/{id}")
    public CommonResult<List<CollectorEvaluateLogRespVO>> statisticsEvaluateTable(@PathVariable Long id) {
        return CommonResult.success(CollectorEvaluateLogService.statisticsEvaluateTable(id));
    }

    @Operation(summary = "统计评测规则结果")
    @GetMapping("/pageErrorData")
    public CommonResult<JSONObject> pageErrorData(CheckErrorDataReqDTO checkErrorDataReqDTO) {
        return CommonResult.success(CollectorEvaluateLogService.pageErrorData(checkErrorDataReqDTO));
    }
    @Operation(summary = "修改错误数据")
    @PostMapping("/updateErrorData")
    public CommonResult<Boolean> updateErrorData(@RequestBody CheckErrorDataReqDTO checkErrorDataReqDTO) {
        boolean success = CollectorEvaluateLogService.updateErrorData(checkErrorDataReqDTO);
        return CommonResult.success(success);
    }
}
