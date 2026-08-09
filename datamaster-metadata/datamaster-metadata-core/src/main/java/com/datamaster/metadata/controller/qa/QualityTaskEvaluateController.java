

package com.datamaster.metadata.controller.qa;

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
import com.datamaster.metadata.controller.qa.vo.QualityTaskEvaluatePageReqVO;
import com.datamaster.metadata.controller.qa.vo.QualityTaskEvaluateRespVO;
import com.datamaster.metadata.controller.qa.vo.QualityTaskEvaluateSaveReqVO;
import com.datamaster.metadata.convert.qa.QualityTaskEvaluateConvert;
import com.datamaster.metadata.dal.dataobject.qa.QualityTaskEvaluateDO;
import com.datamaster.metadata.service.qa.IQualityTaskEvaluateService;
import com.datamaster.metadata.service.qa.IQualityTaskService;

/**
 * 质量探查任务-评测规则Controller
 *
 * @author Chaos
 * @date 2025-07-21
 */
@Tag(name = "质量探查任务-评测规则")
@RestController
@RequestMapping("/metadata/qualityTaskEvaluate")
@Validated
public class QualityTaskEvaluateController extends BaseController {
    @Resource
    private IQualityTaskEvaluateService QualityTaskEvaluateService;
    @Resource
    private IQualityTaskService QualityTaskService;

    @Operation(summary = "查询质量探查任务-评测规则列表")
    @GetMapping("/list")
    public CommonResult<PageResult<QualityTaskEvaluateRespVO>> list(QualityTaskEvaluatePageReqVO QualityTaskEvaluate) {
        PageResult<QualityTaskEvaluateDO> page = QualityTaskEvaluateService.getQualityTaskEvaluatePage(QualityTaskEvaluate);
        return CommonResult.success(BeanUtils.toBean(page, QualityTaskEvaluateRespVO.class));
    }

    @Operation(summary = "导出质量探查任务-评测规则列表")
    @Log(title = "质量探查任务-评测规则", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, QualityTaskEvaluatePageReqVO exportReqVO) {
        exportReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<QualityTaskEvaluateDO> list = (List<QualityTaskEvaluateDO>) QualityTaskEvaluateService.getQualityTaskEvaluatePage(exportReqVO).getRows();
        ExcelUtil<QualityTaskEvaluateRespVO> util = new ExcelUtil<>(QualityTaskEvaluateRespVO.class);
        util.exportExcel(response, QualityTaskEvaluateConvert.INSTANCE.convertToRespVOList(list), "应用管理数据");
    }

    @Operation(summary = "导入质量探查任务-评测规则列表")
    @Log(title = "质量探查任务-评测规则", businessType = BusinessType.IMPORT)
    @PostMapping("/importData")
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception {
        ExcelUtil<QualityTaskEvaluateRespVO> util = new ExcelUtil<>(QualityTaskEvaluateRespVO.class);
        List<QualityTaskEvaluateRespVO> importExcelList = util.importExcel(file.getInputStream());
        String operName = getUsername();
        String message = QualityTaskEvaluateService.importQualityTaskEvaluate(importExcelList, updateSupport, operName);
        return success(message);
    }

    @Operation(summary = "获取质量探查任务-评测规则详细信息")
    @GetMapping(value = "/{id}")
    public CommonResult<QualityTaskEvaluateRespVO> getInfo(@PathVariable("id") Long id) {
        QualityTaskEvaluateDO QualityTaskEvaluateDO = QualityTaskEvaluateService.getQualityTaskEvaluateById(id);
        return CommonResult.success(BeanUtils.toBean(QualityTaskEvaluateDO, QualityTaskEvaluateRespVO.class));
    }

    @Operation(summary = "新增质量探查任务-评测规则")
    @Log(title = "质量探查任务-评测规则", businessType = BusinessType.INSERT)
    @PostMapping
    public CommonResult<Long> add(@Valid @RequestBody QualityTaskEvaluateSaveReqVO QualityTaskEvaluate) {
        QualityTaskEvaluate.setCreatorId(getUserId());
        QualityTaskEvaluate.setCreateBy(getNickName());
        QualityTaskEvaluate.setCreateTime(DateUtil.date());
        return CommonResult.toAjax(QualityTaskEvaluateService.createQualityTaskEvaluate(QualityTaskEvaluate));
    }

    @Operation(summary = "修改质量探查任务-评测规则")
    @Log(title = "质量探查任务-评测规则", businessType = BusinessType.UPDATE)
    @PutMapping
    public CommonResult<Integer> edit(@Valid @RequestBody QualityTaskEvaluateSaveReqVO QualityTaskEvaluate) {
        QualityTaskEvaluate.setUpdatorId(getUserId());
        QualityTaskEvaluate.setUpdateBy(getNickName());
        QualityTaskEvaluate.setUpdateTime(DateUtil.date());
        return CommonResult.toAjax(QualityTaskEvaluateService.updateQualityTaskEvaluate(QualityTaskEvaluate));
    }

    @Operation(summary = "删除质量探查任务-评测规则")
    @Log(title = "质量探查任务-评测规则", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public CommonResult<Integer> remove(@PathVariable Long[] ids) {
        return CommonResult.toAjax(QualityTaskEvaluateService.removeQualityTaskEvaluate(Arrays.asList(ids)));
    }

    @Operation(summary = "删除质量探查任务-检验功能")
    @Log(title = "质量探查任务-检验功能", businessType = BusinessType.DELETE)
    @GetMapping("/verifyInterfaceValue")
    public CommonResult<String> verifyInterfaceValue(QualityTaskEvaluateSaveReqVO QualityTaskEvaluate) {

        String meg = QualityTaskService.verifyInterfaceValue(QualityTaskEvaluate);
        return CommonResult.success(meg);
    }

    @Operation(summary = "删除质量探查任务-错误抽查功能")
    @PostMapping("/validationErrorDataSql")
    public AjaxResult validationErrorDataSql(@Valid @RequestBody QualityTaskEvaluateSaveReqVO QualityTaskEvaluate) {
        return AjaxResult.success(QualityTaskService.validationErrorDataSql(QualityTaskEvaluate));
    }

    @Operation(summary = "删除质量探查任务-成功抽查功能")
    @PostMapping("/validationValidDataSql")
    public AjaxResult validationValidDataSql(@Valid @RequestBody QualityTaskEvaluateSaveReqVO QualityTaskEvaluate) {
        return AjaxResult.success(QualityTaskService.validationValidDataSql(QualityTaskEvaluate));
    }
}
