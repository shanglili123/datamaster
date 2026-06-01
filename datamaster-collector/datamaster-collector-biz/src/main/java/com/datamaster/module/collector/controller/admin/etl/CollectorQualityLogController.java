

package com.datamaster.module.collector.controller.admin.etl;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.InputStream;
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
import com.datamaster.common.core.domain.ReturnT;
import com.datamaster.common.core.page.PageParam;
import com.datamaster.common.annotation.Log;
import com.datamaster.common.core.controller.BaseController;
import com.datamaster.common.core.domain.CommonResult;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.common.enums.BusinessType;
import com.datamaster.common.utils.object.BeanUtils;
import com.datamaster.common.utils.poi.ExcelUtil;
import com.datamaster.module.collector.controller.admin.etl.vo.CollectorQualityLogPageReqVO;
import com.datamaster.module.collector.controller.admin.etl.vo.CollectorQualityLogRespVO;
import com.datamaster.module.collector.controller.admin.etl.vo.CollectorQualityLogSaveReqVO;
import com.datamaster.module.collector.controller.admin.etl.vo.LogResult;
import com.datamaster.module.collector.convert.etl.CollectorQualityLogConvert;
import com.datamaster.module.collector.dal.dataobject.etl.CollectorQualityLogDO;
import com.datamaster.module.collector.service.etl.ICollectorQualityLogService;

/**
 * 数据质量日志Controller
 *
 * @author DATAMASTER
 * @date 2025-07-19
 */
@Tag(name = "数据质量日志")
@RestController
@RequestMapping("/col/qualityLog")
@Validated
public class CollectorQualityLogController extends BaseController {
    @Resource
    private ICollectorQualityLogService CollectorQualityLogService;

    @Operation(summary = "查询数据质量日志列表")
    @GetMapping("/list")
    public CommonResult<PageResult<CollectorQualityLogRespVO>> list(CollectorQualityLogPageReqVO CollectorQualityLog) {
        PageResult<CollectorQualityLogDO> page = CollectorQualityLogService.getCollectorQualityLogPage(CollectorQualityLog);
        return CommonResult.success(BeanUtils.toBean(page, CollectorQualityLogRespVO.class));
    }

    @Operation(summary = "导出数据质量日志列表")
    @Log(title = "数据质量日志", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, CollectorQualityLogPageReqVO exportReqVO) {
        exportReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<CollectorQualityLogDO> list = (List<CollectorQualityLogDO>) CollectorQualityLogService.getCollectorQualityLogPage(exportReqVO).getRows();
        ExcelUtil<CollectorQualityLogRespVO> util = new ExcelUtil<>(CollectorQualityLogRespVO.class);
        util.exportExcel(response, CollectorQualityLogConvert.INSTANCE.convertToRespVOList(list), "应用管理数据");
    }

    @Operation(summary = "导入数据质量日志列表")
    @Log(title = "数据质量日志", businessType = BusinessType.IMPORT)
    @PostMapping("/importData")
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception {
        ExcelUtil<CollectorQualityLogRespVO> util = new ExcelUtil<>(CollectorQualityLogRespVO.class);
        List<CollectorQualityLogRespVO> importExcelList = util.importExcel(file.getInputStream());
        String operName = getUsername();
        String message = CollectorQualityLogService.importCollectorQualityLog(importExcelList, updateSupport, operName);
        return success(message);
    }

    @Operation(summary = "获取数据质量日志详细信息")
    @GetMapping(value = "/{id}")
    public CommonResult<CollectorQualityLogRespVO> getInfo(@PathVariable("id") Long id) {
        CollectorQualityLogDO CollectorQualityLogDO = CollectorQualityLogService.getCollectorQualityLogById(id);
        return CommonResult.success(BeanUtils.toBean(CollectorQualityLogDO, CollectorQualityLogRespVO.class));
    }

    @Operation(summary = "新增数据质量日志")
    @Log(title = "数据质量日志", businessType = BusinessType.INSERT)
    @PostMapping
    public CommonResult<Long> add(@Valid @RequestBody CollectorQualityLogSaveReqVO CollectorQualityLog) {
        CollectorQualityLog.setCreatorId(getUserId());
        CollectorQualityLog.setCreateBy(getNickName());
        CollectorQualityLog.setCreateTime(DateUtil.date());
        return CommonResult.toAjax(CollectorQualityLogService.createCollectorQualityLog(CollectorQualityLog));
    }

    @Operation(summary = "修改数据质量日志")
    @Log(title = "数据质量日志", businessType = BusinessType.UPDATE)
    @PutMapping
    public CommonResult<Integer> edit(@Valid @RequestBody CollectorQualityLogSaveReqVO CollectorQualityLog) {
        CollectorQualityLog.setUpdatorId(getUserId());
        CollectorQualityLog.setUpdateBy(getNickName());
        CollectorQualityLog.setUpdateTime(DateUtil.date());
        return CommonResult.toAjax(CollectorQualityLogService.updateCollectorQualityLog(CollectorQualityLog));
    }

    @Operation(summary = "删除数据质量日志")
    @Log(title = "数据质量日志", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public CommonResult<Integer> remove(@PathVariable Long[] ids) {
        return CommonResult.toAjax(CollectorQualityLogService.removeCollectorQualityLog(Arrays.asList(ids)));
    }


    @RequestMapping(value = "/logDetailCat", method = RequestMethod.GET)
    @Operation(summary = "运行日志详情")
    public ReturnT<LogResult> logDetailCat(String handleMsg) {
        // 添加日志审计功能
        try {
            InputStream in = new FileInputStream(handleMsg);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) != -1) {
                bos.write(buf, 0, len);
            }
            String logContent = new String(bos.toByteArray(), "UTF-8");
            if (bos != null) {
                bos.close();
            }
            if (in != null) {
                in.close();
            }
            // @TODO 查看日志
            ReturnT<LogResult> returnT = new ReturnT<>(ReturnT.SUCCESS_CODE, "查询日志成功");
            LogResult logResult = new LogResult(0, 0, logContent, true);
            returnT.setContent(logResult);
            return returnT;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return new ReturnT<>(ReturnT.FAIL_CODE, "暂未找到日志文件信息");
        }
    }

}
