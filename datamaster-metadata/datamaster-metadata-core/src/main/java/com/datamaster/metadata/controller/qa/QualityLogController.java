

package com.datamaster.metadata.controller.qa;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import cn.hutool.core.date.DateUtil;
import java.util.List;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.datamaster.common.core.domain.AjaxResult;
import com.datamaster.common.core.domain.ReturnT;
import com.datamaster.common.core.page.PageParam;
import com.datamaster.common.annotation.Log;
import com.datamaster.common.core.controller.BaseController;
import com.datamaster.common.core.domain.CommonResult;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.common.enums.BusinessType;
import com.datamaster.common.utils.object.BeanUtils;
import com.datamaster.common.utils.poi.ExcelUtil;
import com.datamaster.metadata.controller.qa.vo.QualityLogPageReqVO;
import com.datamaster.metadata.controller.qa.vo.QualityLogRespVO;
import com.datamaster.metadata.controller.qa.vo.QualityLogSaveReqVO;
import com.datamaster.metadata.controller.qa.vo.LogResult;
import com.datamaster.metadata.convert.qa.QualityLogConvert;
import com.datamaster.metadata.dal.dataobject.qa.QualityLogDO;
import com.datamaster.metadata.service.qa.IQualityLogService;
import com.datamaster.metadata.service.qa.IQualityTaskService;

/**
 * 探查任务实例 Controller
 *
 * @author DATAMASTER
 * @date 2025-07-19
 */
@Tag(name = "探查任务实例")
@RestController
@RequestMapping("/metadata/probeTaskInstance")
@Validated
public class QualityLogController extends BaseController {
    private static final long MAX_LOG_BYTES = 5L * 1024L * 1024L;

    @Value("${file.job.log.qualitytask_prefix_url}")
    private String qualityTaskLogDirectory;

    @Resource
    private IQualityLogService QualityLogService;
    @Resource
    private IQualityTaskService qualityTaskService;

    @Operation(summary = "查询探查任务实例列表")
    @GetMapping("/list")
    public CommonResult<PageResult<QualityLogRespVO>> list(QualityLogPageReqVO QualityLog) {
        PageResult<QualityLogDO> page = QualityLogService.getQualityLogPage(QualityLog);
        return CommonResult.success(BeanUtils.toBean(page, QualityLogRespVO.class));
    }

    @Operation(summary = "按表查询探查历史记录(多次探查结果)")
    @GetMapping("/listByTable")
    public CommonResult<List<QualityLogDO>> listByTable(@RequestParam("datasourceId") Long datasourceId,
                                                        @RequestParam("tableName") String tableName) {
        return CommonResult.success(qualityTaskService.getQualityLogListByTable(datasourceId, tableName));
    }

    @Operation(summary = "导出探查任务实例列表")
    @Log(title = "探查任务实例", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, QualityLogPageReqVO exportReqVO) {
        exportReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<QualityLogDO> list = (List<QualityLogDO>) QualityLogService.getQualityLogPage(exportReqVO).getRows();
        ExcelUtil<QualityLogRespVO> util = new ExcelUtil<>(QualityLogRespVO.class);
        util.exportExcel(response, QualityLogConvert.INSTANCE.convertToRespVOList(list), "应用管理数据");
    }

    @Operation(summary = "导入探查任务实例列表")
    @Log(title = "探查任务实例", businessType = BusinessType.IMPORT)
    @PostMapping("/importData")
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception {
        ExcelUtil<QualityLogRespVO> util = new ExcelUtil<>(QualityLogRespVO.class);
        List<QualityLogRespVO> importExcelList = util.importExcel(file.getInputStream());
        String operName = getUsername();
        String message = QualityLogService.importQualityLog(importExcelList, updateSupport, operName);
        return success(message);
    }

    @Operation(summary = "获取探查任务实例详细信息")
    @GetMapping(value = "/{id}")
    public CommonResult<QualityLogRespVO> getInfo(@PathVariable("id") Long id) {
        QualityLogDO QualityLogDO = QualityLogService.getQualityLogById(id);
        return CommonResult.success(BeanUtils.toBean(QualityLogDO, QualityLogRespVO.class));
    }

    @Operation(summary = "新增探查任务实例")
    @Log(title = "探查任务实例", businessType = BusinessType.INSERT)
    @PostMapping
    public CommonResult<Long> add(@Valid @RequestBody QualityLogSaveReqVO QualityLog) {
        QualityLog.setCreatorId(getUserId());
        QualityLog.setCreateBy(getNickName());
        QualityLog.setCreateTime(DateUtil.date());
        return CommonResult.toAjax(QualityLogService.createQualityLog(QualityLog));
    }

    @Operation(summary = "修改探查任务实例")
    @Log(title = "探查任务实例", businessType = BusinessType.UPDATE)
    @PutMapping
    public CommonResult<Integer> edit(@Valid @RequestBody QualityLogSaveReqVO QualityLog) {
        QualityLog.setUpdatorId(getUserId());
        QualityLog.setUpdateBy(getNickName());
        QualityLog.setUpdateTime(DateUtil.date());
        return CommonResult.toAjax(QualityLogService.updateQualityLog(QualityLog));
    }

    @Operation(summary = "删除探查任务实例")
    @Log(title = "探查任务实例", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public CommonResult<Integer> remove(@PathVariable Long[] ids) {
        return CommonResult.toAjax(QualityLogService.removeQualityLog(Arrays.asList(ids)));
    }


    @RequestMapping(value = "/logDetailCat", method = RequestMethod.GET)
    @Operation(summary = "运行日志详情")
    @PreAuthorize("@ss.hasPermi('ast:qualityTask:query')")
    public ReturnT<LogResult> logDetailCat(String handleMsg) {
        try {
            if (handleMsg == null || handleMsg.trim().isEmpty()
                    || qualityTaskLogDirectory == null || qualityTaskLogDirectory.trim().isEmpty()) {
                return new ReturnT<>(ReturnT.FAIL_CODE, "日志路径不能为空");
            }
            Path root = Paths.get(qualityTaskLogDirectory).toAbsolutePath().normalize();
            Path requested = Paths.get(handleMsg).toAbsolutePath().normalize();
            if (!requested.startsWith(root)
                    || !requested.getFileName().toString().startsWith("taskLog-")) {
                logger.warn("拒绝读取越界质量日志路径：{}", handleMsg);
                return new ReturnT<>(ReturnT.FAIL_CODE, "非法日志路径");
            }
            if (!Files.isRegularFile(requested) || Files.size(requested) > MAX_LOG_BYTES) {
                return new ReturnT<>(ReturnT.FAIL_CODE, "日志文件不存在或超过大小限制");
            }
            String logContent = new String(Files.readAllBytes(requested), StandardCharsets.UTF_8);
            ReturnT<LogResult> returnT = new ReturnT<>(ReturnT.SUCCESS_CODE, "查询日志成功");
            LogResult logResult = new LogResult(0, logContent.length(), logContent, true);
            returnT.setContent(logResult);
            return returnT;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return new ReturnT<>(ReturnT.FAIL_CODE, "暂未找到日志文件信息");
        }
    }

}
