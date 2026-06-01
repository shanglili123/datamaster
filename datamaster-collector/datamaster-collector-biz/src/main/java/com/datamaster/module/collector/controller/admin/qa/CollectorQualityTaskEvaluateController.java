

package com.datamaster.module.collector.controller.admin.qa;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.io.IOException;
import java.util.*;

import cn.hutool.core.date.DateUtil;

import com.alibaba.fastjson2.JSONObject;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
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
import com.datamaster.common.httpClient.HeaderEntity;
import com.datamaster.common.httpClient.HttpUtils;
import com.datamaster.common.utils.JSONUtils;
import com.datamaster.common.utils.object.BeanUtils;
import com.datamaster.common.utils.poi.ExcelUtil;
import com.datamaster.module.collector.controller.admin.qa.vo.CollectorQualityTaskEvaluatePageReqVO;
import com.datamaster.module.collector.controller.admin.qa.vo.CollectorQualityTaskEvaluateRespVO;
import com.datamaster.module.collector.controller.admin.qa.vo.CollectorQualityTaskEvaluateSaveReqVO;
import com.datamaster.module.collector.convert.qa.CollectorQualityTaskEvaluateConvert;
import com.datamaster.module.collector.dal.dataobject.qa.CollectorQualityTaskEvaluateDO;
import com.datamaster.module.collector.service.qa.ICollectorQualityTaskEvaluateService;
import com.datamaster.module.collector.service.qa.ICollectorQualityTaskService;

/**
 * 数据质量任务-评测规则Controller
 *
 * @author Chaos
 * @date 2025-07-21
 */
@Tag(name = "数据质量任务-评测规则")
@RestController
@RequestMapping("/col/qualityTaskEvaluate")
@Validated
public class CollectorQualityTaskEvaluateController extends BaseController {
    @Resource
    private ICollectorQualityTaskEvaluateService CollectorQualityTaskEvaluateService;
    @Resource
    private ICollectorQualityTaskService CollectorQualityTaskService;

    @Operation(summary = "查询数据质量任务-评测规则列表")
    @GetMapping("/list")
    public CommonResult<PageResult<CollectorQualityTaskEvaluateRespVO>> list(CollectorQualityTaskEvaluatePageReqVO CollectorQualityTaskEvaluate) {
        PageResult<CollectorQualityTaskEvaluateDO> page = CollectorQualityTaskEvaluateService.getCollectorQualityTaskEvaluatePage(CollectorQualityTaskEvaluate);
        return CommonResult.success(BeanUtils.toBean(page, CollectorQualityTaskEvaluateRespVO.class));
    }

    @Operation(summary = "导出数据质量任务-评测规则列表")
    @Log(title = "数据质量任务-评测规则", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, CollectorQualityTaskEvaluatePageReqVO exportReqVO) {
        exportReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<CollectorQualityTaskEvaluateDO> list = (List<CollectorQualityTaskEvaluateDO>) CollectorQualityTaskEvaluateService.getCollectorQualityTaskEvaluatePage(exportReqVO).getRows();
        ExcelUtil<CollectorQualityTaskEvaluateRespVO> util = new ExcelUtil<>(CollectorQualityTaskEvaluateRespVO.class);
        util.exportExcel(response, CollectorQualityTaskEvaluateConvert.INSTANCE.convertToRespVOList(list), "应用管理数据");
    }

    @Operation(summary = "导入数据质量任务-评测规则列表")
    @Log(title = "数据质量任务-评测规则", businessType = BusinessType.IMPORT)
    @PostMapping("/importData")
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception {
        ExcelUtil<CollectorQualityTaskEvaluateRespVO> util = new ExcelUtil<>(CollectorQualityTaskEvaluateRespVO.class);
        List<CollectorQualityTaskEvaluateRespVO> importExcelList = util.importExcel(file.getInputStream());
        String operName = getUsername();
        String message = CollectorQualityTaskEvaluateService.importCollectorQualityTaskEvaluate(importExcelList, updateSupport, operName);
        return success(message);
    }

    @Operation(summary = "获取数据质量任务-评测规则详细信息")
    @GetMapping(value = "/{id}")
    public CommonResult<CollectorQualityTaskEvaluateRespVO> getInfo(@PathVariable("id") Long id) {
        CollectorQualityTaskEvaluateDO CollectorQualityTaskEvaluateDO = CollectorQualityTaskEvaluateService.getCollectorQualityTaskEvaluateById(id);
        return CommonResult.success(BeanUtils.toBean(CollectorQualityTaskEvaluateDO, CollectorQualityTaskEvaluateRespVO.class));
    }

    @Operation(summary = "新增数据质量任务-评测规则")
    @Log(title = "数据质量任务-评测规则", businessType = BusinessType.INSERT)
    @PostMapping
    public CommonResult<Long> add(@Valid @RequestBody CollectorQualityTaskEvaluateSaveReqVO CollectorQualityTaskEvaluate) {
        CollectorQualityTaskEvaluate.setCreatorId(getUserId());
        CollectorQualityTaskEvaluate.setCreateBy(getNickName());
        CollectorQualityTaskEvaluate.setCreateTime(DateUtil.date());
        return CommonResult.toAjax(CollectorQualityTaskEvaluateService.createCollectorQualityTaskEvaluate(CollectorQualityTaskEvaluate));
    }

    @Operation(summary = "修改数据质量任务-评测规则")
    @Log(title = "数据质量任务-评测规则", businessType = BusinessType.UPDATE)
    @PutMapping
    public CommonResult<Integer> edit(@Valid @RequestBody CollectorQualityTaskEvaluateSaveReqVO CollectorQualityTaskEvaluate) {
        CollectorQualityTaskEvaluate.setUpdatorId(getUserId());
        CollectorQualityTaskEvaluate.setUpdateBy(getNickName());
        CollectorQualityTaskEvaluate.setUpdateTime(DateUtil.date());
        return CommonResult.toAjax(CollectorQualityTaskEvaluateService.updateCollectorQualityTaskEvaluate(CollectorQualityTaskEvaluate));
    }

    @Operation(summary = "删除数据质量任务-评测规则")
    @Log(title = "数据质量任务-评测规则", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public CommonResult<Integer> remove(@PathVariable Long[] ids) {
        return CommonResult.toAjax(CollectorQualityTaskEvaluateService.removeCollectorQualityTaskEvaluate(Arrays.asList(ids)));
    }

    @Operation(summary = "删除数据质量任务-检验功能")
    @Log(title = "数据质量任务-检验功能", businessType = BusinessType.DELETE)
    @GetMapping("/verifyInterfaceValue")
    public CommonResult<String> verifyInterfaceValue(CollectorQualityTaskEvaluateSaveReqVO CollectorQualityTaskEvaluate) {

        String meg = CollectorQualityTaskService.verifyInterfaceValue(CollectorQualityTaskEvaluate);
        return CommonResult.success(meg);
    }

    @Operation(summary = "删除数据质量任务-错误抽查功能")
    @PostMapping("/validationErrorDataSql")
    public AjaxResult validationErrorDataSql(@Valid @RequestBody CollectorQualityTaskEvaluateSaveReqVO CollectorQualityTaskEvaluate) {
        return AjaxResult.success(CollectorQualityTaskService.validationErrorDataSql(CollectorQualityTaskEvaluate));
    }

    @Operation(summary = "删除数据质量任务-成功抽查功能")
    @PostMapping("/validationValidDataSql")
    public AjaxResult validationValidDataSql(@Valid @RequestBody CollectorQualityTaskEvaluateSaveReqVO CollectorQualityTaskEvaluate) {
        return AjaxResult.success(CollectorQualityTaskService.validationValidDataSql(CollectorQualityTaskEvaluate));
    }
}
