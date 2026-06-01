

package com.datamaster.module.service.controller.admin.apiLog;

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
import com.datamaster.module.service.controller.admin.apiLog.vo.ServiceApiLogPageReqVO;
import com.datamaster.module.service.controller.admin.apiLog.vo.ServiceApiLogRespVO;
import com.datamaster.module.service.controller.admin.apiLog.vo.ServiceApiLogSaveReqVO;
import com.datamaster.module.service.convert.apiLog.ServiceApiLogConvert;
import com.datamaster.module.service.dal.dataobject.apiLog.ServiceApiLogDO;
import com.datamaster.module.service.service.apiLog.IServiceApiLogService;

/**
 * API服务调用日志Controller
 *
 * @author lhs
 * @date 2025-02-12
 */
@Tag(name = "API服务调用日志")
@RestController
@RequestMapping("/svc/apiLog")
@Validated
public class ServiceApiLogController extends BaseController {
    @Resource
    private IServiceApiLogService ServiceApiLogService;

    @Operation(summary = "查询API服务调用日志列表")
    @PreAuthorize("@ss.hasPermi('ds:apiLog:list')")
    @GetMapping("/list")
    public CommonResult<PageResult<ServiceApiLogRespVO>> list(ServiceApiLogPageReqVO ServiceApiLog) {
        PageResult<ServiceApiLogDO> page = ServiceApiLogService.getServiceApiLogPage(ServiceApiLog);
        return CommonResult.success(BeanUtils.toBean(page, ServiceApiLogRespVO.class));
    }

    @Operation(summary = "导出API服务调用日志列表")
    @PreAuthorize("@ss.hasPermi('ds:apiLog:export')")
    @Log(title = "API服务调用日志", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, ServiceApiLogPageReqVO exportReqVO) {
        exportReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<ServiceApiLogDO> list = (List<ServiceApiLogDO>) ServiceApiLogService.getServiceApiLogPage(exportReqVO).getRows();
        ExcelUtil<ServiceApiLogRespVO> util = new ExcelUtil<>(ServiceApiLogRespVO.class);
        util.exportExcel(response, ServiceApiLogConvert.INSTANCE.convertToRespVOList(list), "应用管理数据");
    }

    @Operation(summary = "导入API服务调用日志列表")
    @PreAuthorize("@ss.hasPermi('ds:apiLog:import')")
    @Log(title = "API服务调用日志", businessType = BusinessType.IMPORT)
    @PostMapping("/importData")
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception {
        ExcelUtil<ServiceApiLogRespVO> util = new ExcelUtil<>(ServiceApiLogRespVO.class);
        List<ServiceApiLogRespVO> importExcelList = util.importExcel(file.getInputStream());
        String operName = getUsername();
        String message = ServiceApiLogService.importServiceApiLog(importExcelList, updateSupport, operName);
        return success(message);
    }

    @Operation(summary = "获取API服务调用日志详细信息")
    @PreAuthorize("@ss.hasPermi('ds:apiLog:query')")
    @GetMapping(value = "/{ID}")
    public CommonResult<ServiceApiLogRespVO> getInfo(@PathVariable("ID") Long ID) {
        ServiceApiLogDO ServiceApiLogDO = ServiceApiLogService.getServiceApiLogById(ID);
        if(ServiceApiLogDO == null){
            ServiceApiLogDO = new ServiceApiLogDO();
        }
        return CommonResult.success(BeanUtils.toBean(ServiceApiLogDO, ServiceApiLogRespVO.class));
    }

    @Operation(summary = "新增API服务调用日志")
    @PreAuthorize("@ss.hasPermi('ds:apiLog:add')")
    @Log(title = "API服务调用日志", businessType = BusinessType.INSERT)
    @PostMapping
    public CommonResult<Long> add(@Valid @RequestBody ServiceApiLogSaveReqVO ServiceApiLog) {
        ServiceApiLog.setCreatorId(getUserId());
        ServiceApiLog.setCreateBy(getNickName());
        ServiceApiLog.setCreateTime(DateUtil.date());
        return CommonResult.toAjax(ServiceApiLogService.createServiceApiLog(ServiceApiLog));
    }

    @Operation(summary = "修改API服务调用日志")
    @PreAuthorize("@ss.hasPermi('ds:apiLog:edit')")
    @Log(title = "API服务调用日志", businessType = BusinessType.UPDATE)
    @PutMapping
    public CommonResult<Integer> edit(@Valid @RequestBody ServiceApiLogSaveReqVO ServiceApiLog) {
        ServiceApiLog.setUpdatorId(getUserId());
        ServiceApiLog.setUpdateBy(getNickName());
        ServiceApiLog.setUpdateTime(DateUtil.date());
        return CommonResult.toAjax(ServiceApiLogService.updateServiceApiLog(ServiceApiLog));
    }

    @Operation(summary = "删除API服务调用日志")
    @PreAuthorize("@ss.hasPermi('ds:apiLog:remove')")
    @Log(title = "API服务调用日志", businessType = BusinessType.DELETE)
    @DeleteMapping("/{id}")
    public CommonResult<Integer> remove(@PathVariable Long[] id) {
        return CommonResult.toAjax(ServiceApiLogService.removeServiceApiLog(Arrays.asList(id)));
    }

}
