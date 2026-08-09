

package com.datamaster.metadata.controller.qa;

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
import com.datamaster.metadata.controller.qa.vo.*;
import com.datamaster.metadata.controller.qa.vo.CheckErrorDataReqDTO;
import com.datamaster.metadata.convert.qa.EvaluateLogConvert;
import com.datamaster.metadata.dal.dataobject.qa.EvaluateLogDO;
import com.datamaster.metadata.service.qa.IEvaluateLogService;

/**
 * 评测规则结果Controller
 *
 * @author DATAMASTER
 * @date 2025-07-21
 */
@Tag(name = "评测规则结果")
@RestController
@RequestMapping("/metadata/evaluateLog")
@Validated
public class EvaluateLogController extends BaseController {
    @Resource
    private IEvaluateLogService EvaluateLogService;

    @Operation(summary = "查询评测规则结果列表")
//    @PreAuthorize("@ss.hasPermi('col:evaluateLog:list')")
    @GetMapping("/list")
    public CommonResult<PageResult<EvaluateLogRespVO>> list(EvaluateLogPageReqVO EvaluateLog) {
        PageResult<EvaluateLogDO> page = EvaluateLogService.getEvaluateLogPage(EvaluateLog);
        return CommonResult.success(BeanUtils.toBean(page, EvaluateLogRespVO.class));
    }

    @Operation(summary = "导出评测规则结果列表")
//    @PreAuthorize("@ss.hasPermi('col:evaluateLog:export')")
    @Log(title = "评测规则结果", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, EvaluateLogPageReqVO exportReqVO) {
        exportReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<EvaluateLogDO> list = (List<EvaluateLogDO>) EvaluateLogService.getEvaluateLogPage(exportReqVO).getRows();
        ExcelUtil<EvaluateLogRespVO> util = new ExcelUtil<>(EvaluateLogRespVO.class);
        util.exportExcel(response, EvaluateLogConvert.INSTANCE.convertToRespVOList(list), "应用管理数据");
    }

    @Operation(summary = "导入评测规则结果列表")
//    @PreAuthorize("@ss.hasPermi('col:evaluateLog:import')")
    @Log(title = "评测规则结果", businessType = BusinessType.IMPORT)
    @PostMapping("/importData")
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception {
        ExcelUtil<EvaluateLogRespVO> util = new ExcelUtil<>(EvaluateLogRespVO.class);
        List<EvaluateLogRespVO> importExcelList = util.importExcel(file.getInputStream());
        String operName = getUsername();
        String message = EvaluateLogService.importEvaluateLog(importExcelList, updateSupport, operName);
        return success(message);
    }

    @Operation(summary = "获取评测规则结果详细信息")
//    @PreAuthorize("@ss.hasPermi('col:evaluateLog:query')")
    @GetMapping(value = "/{id}")
    public CommonResult<EvaluateLogRespVO> getInfo(@PathVariable("id") Long id) {
        EvaluateLogDO EvaluateLogDO = EvaluateLogService.getEvaluateLogById(id);
        return CommonResult.success(BeanUtils.toBean(EvaluateLogDO, EvaluateLogRespVO.class));
    }

    @Operation(summary = "新增评测规则结果")
//    @PreAuthorize("@ss.hasPermi('col:evaluateLog:add')")
    @Log(title = "评测规则结果", businessType = BusinessType.INSERT)
    @PostMapping
    public CommonResult<Long> add(@Valid @RequestBody EvaluateLogSaveReqVO EvaluateLog) {
        EvaluateLog.setCreatorId(getUserId());
        EvaluateLog.setCreateBy(getNickName());
        EvaluateLog.setCreateTime(DateUtil.date());
        return CommonResult.toAjax(EvaluateLogService.createEvaluateLog(EvaluateLog));
    }

    @Operation(summary = "修改评测规则结果")
//    @PreAuthorize("@ss.hasPermi('col:evaluateLog:edit')")
    @Log(title = "评测规则结果", businessType = BusinessType.UPDATE)
    @PutMapping
    public CommonResult<Integer> edit(@Valid @RequestBody EvaluateLogSaveReqVO EvaluateLog) {
        EvaluateLog.setUpdatorId(getUserId());
        EvaluateLog.setUpdateBy(getNickName());
        EvaluateLog.setUpdateTime(DateUtil.date());
        return CommonResult.toAjax(EvaluateLogService.updateEvaluateLog(EvaluateLog));
    }

    @Operation(summary = "删除评测规则结果")
//    @PreAuthorize("@ss.hasPermi('col:evaluateLog:remove')")
    @Log(title = "评测规则结果", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public CommonResult<Integer> remove(@PathVariable Long[] ids) {
        return CommonResult.toAjax(EvaluateLogService.removeEvaluateLog(Arrays.asList(ids)));
    }

    @Operation(summary = "统计评测规则结果")
    @GetMapping("/statisticsEvaluateOne/{id}")
    public CommonResult<List<EvaluateLogStatisticsVO>> statisticsEvaluateOne(@PathVariable Long id) {
        return CommonResult.success(EvaluateLogService.statisticsEvaluateOne(id));
    }


    @Operation(summary = "统计评测规则结果")
    @GetMapping("/statisticsEvaluateTow")
    public CommonResult<JSONObject> statisticsEvaluateTow( Long id , Date deDate , Date oldDate , int type) {
        return CommonResult.success(EvaluateLogService.statisticsEvaluateTow(id , deDate , oldDate , type));
    }

    @Operation(summary = "统计评测规则结果")
    @GetMapping("/statisticsEvaluateTable/{id}")
    public CommonResult<List<EvaluateLogRespVO>> statisticsEvaluateTable(@PathVariable Long id) {
        return CommonResult.success(EvaluateLogService.statisticsEvaluateTable(id));
    }

    @Operation(summary = "统计评测规则结果")
    @GetMapping("/pageErrorData")
    public CommonResult<JSONObject> pageErrorData(CheckErrorDataReqDTO checkErrorDataReqDTO) {
        return CommonResult.success(EvaluateLogService.pageErrorData(checkErrorDataReqDTO));
    }
    @Operation(summary = "修改错误数据")
    @PostMapping("/updateErrorData")
    public CommonResult<Boolean> updateErrorData(@RequestBody CheckErrorDataReqDTO checkErrorDataReqDTO) {
        boolean success = EvaluateLogService.updateErrorData(checkErrorDataReqDTO);
        return CommonResult.success(success);
    }
}
