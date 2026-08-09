package com.datamaster.module.service.controller.admin.gateway.api;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.util.Arrays;

import cn.hutool.core.date.DateUtil;
import org.springframework.http.MediaType;

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
import com.datamaster.module.service.controller.admin.gateway.api.vo.GatewayApiPageReqVO;
import com.datamaster.module.service.controller.admin.gateway.api.vo.GatewayApiReqVO;
import com.datamaster.module.service.controller.admin.gateway.api.vo.GatewayApiRespVO;
import com.datamaster.module.service.controller.admin.gateway.api.vo.GatewayApiSaveReqVO;
import com.datamaster.module.service.convert.gateway.api.GatewayApiConvert;
import com.datamaster.module.service.dal.dataobject.gateway.api.GatewayApiDO;
import com.datamaster.module.service.service.gateway.api.IGatewayApiService;

/**
 * 数据服务-API网关Controller
 *
 * @author DATAMASTER
 * @date 2025-04-14
 */
@Tag(name = "数据服务-API网关")
@RestController
@RequestMapping("/svc/gateway/api")
@Validated
public class GatewayApiController extends BaseController {
    @Resource
    private IGatewayApiService gatewayApiService;

    @Operation(summary = "查询API网关列表")
    @PreAuthorize("@ss.hasPermi('svc:gateway:api:list')")
    @GetMapping("/list")
    public CommonResult<PageResult<GatewayApiRespVO>> list(GatewayApiPageReqVO gatewayApi) {
        PageResult<GatewayApiDO> page = gatewayApiService.getGatewayApiPage(gatewayApi);
        return CommonResult.success(BeanUtils.toBean(page, GatewayApiRespVO.class));
    }

    @Operation(summary = "导出API网关列表")
    @PreAuthorize("@ss.hasPermi('svc:gateway:api:export')")
    @Log(title = "API网关", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, GatewayApiPageReqVO exportReqVO) {
        exportReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<GatewayApiDO> list = (List<GatewayApiDO>) gatewayApiService.getGatewayApiPage(exportReqVO).getRows();
        ExcelUtil<GatewayApiRespVO> util = new ExcelUtil<>(GatewayApiRespVO.class);
        util.exportExcel(response, GatewayApiConvert.INSTANCE.convertToRespVOList(list), "应用管理数据");
    }

    @Operation(summary = "导入API网关列表")
    @PreAuthorize("@ss.hasPermi('svc:gateway:api:import')")
    @Log(title = "API网关", businessType = BusinessType.IMPORT)
    @PostMapping("/importData")
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception {
        ExcelUtil<GatewayApiRespVO> util = new ExcelUtil<>(GatewayApiRespVO.class);
        List<GatewayApiRespVO> importExcelList = util.importExcel(file.getInputStream());
        String operName = getUsername();
        String message = gatewayApiService.importGatewayApi(importExcelList, updateSupport, operName);
        return success(message);
    }

    @Operation(summary = "获取API网关详细信息")
    @PreAuthorize("@ss.hasPermi('svc:gateway:api:query')")
    @GetMapping(value = "/{id}")
    public CommonResult<GatewayApiRespVO> getInfo(@PathVariable("id") Long id) {
        GatewayApiDO gatewayApiDO = gatewayApiService.getGatewayApiById(id);
        return CommonResult.success(BeanUtils.toBean(gatewayApiDO, GatewayApiRespVO.class));
    }

    @Operation(summary = "新增API网关")
    @PreAuthorize("@ss.hasPermi('svc:gateway:api:add')")
    @Log(title = "API网关", businessType = BusinessType.INSERT)
    @PostMapping
    public CommonResult<Long> add(@Valid @RequestBody GatewayApiSaveReqVO gatewayApi) {
        gatewayApi.setCreatorId(getUserId());
        gatewayApi.setCreateBy(getNickName());
        gatewayApi.setCreateTime(DateUtil.date());
        return CommonResult.toAjax(gatewayApiService.createGatewayApi(gatewayApi));
    }

    @Operation(summary = "修改API网关")
    @PreAuthorize("@ss.hasPermi('svc:gateway:api:edit')")
    @Log(title = "API网关", businessType = BusinessType.UPDATE)
    @PutMapping
    public CommonResult<Integer> edit(@Valid @RequestBody GatewayApiSaveReqVO gatewayApi) {
        gatewayApi.setUpdatorId(getUserId());
        gatewayApi.setUpdateBy(getNickName());
        gatewayApi.setUpdateTime(DateUtil.date());
        return CommonResult.toAjax(gatewayApiService.updateGatewayApi(gatewayApi));
    }

    @Operation(summary = "删除API网关")
    @PreAuthorize("@ss.hasPermi('svc:gateway:api:remove')")
    @Log(title = "API网关", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public CommonResult<Integer> remove(@PathVariable Long[] ids) {
        return CommonResult.toAjax(gatewayApiService.removeGatewayApi(Arrays.asList(ids)));
    }

    @Operation(summary = "API网关转发调用")
    @PreAuthorize("@ss.hasPermi('svc:gateway:api:queryServiceForwarding')")
    @PostMapping("/queryServiceForwarding")
    public void queryServiceForwarding(HttpServletResponse response, @Valid @RequestBody GatewayApiReqVO gatewayApi) {
        response.setContentType(MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8");
        gatewayApiService.queryServiceForwarding(response, gatewayApi);
    }
}
