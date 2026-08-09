package com.datamaster.module.service.controller.admin.gateway.api;

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
import com.datamaster.module.service.controller.admin.gateway.api.vo.GatewayApiParamPageReqVO;
import com.datamaster.module.service.controller.admin.gateway.api.vo.GatewayApiParamRespVO;
import com.datamaster.module.service.controller.admin.gateway.api.vo.GatewayApiParamSaveReqVO;
import com.datamaster.module.service.convert.gateway.api.GatewayApiParamConvert;
import com.datamaster.module.service.dal.dataobject.gateway.api.GatewayApiParamDO;
import com.datamaster.module.service.service.gateway.api.IGatewayApiParamService;

/**
 * 数据服务-API网关-参数Controller
 *
 * @author DATAMASTER
 * @date 2025-04-14
 */
@Tag(name = "数据服务-API网关-参数")
@RestController
@RequestMapping("/svc/gateway/apiParam")
@Validated
public class GatewayApiParamController extends BaseController {
    @Resource
    private IGatewayApiParamService gatewayApiParamService;

    @Operation(summary = "查询API网关-参数列表")
    @PreAuthorize("@ss.hasPermi('svc:gateway:apiParam:list')")
    @GetMapping("/list")
    public CommonResult<PageResult<GatewayApiParamRespVO>> list(GatewayApiParamPageReqVO gatewayApiParam) {
        PageResult<GatewayApiParamDO> page = gatewayApiParamService.getGatewayApiParamPage(gatewayApiParam);
        return CommonResult.success(BeanUtils.toBean(page, GatewayApiParamRespVO.class));
    }

    @Operation(summary = "查询API网关-参数树列表")
    @PreAuthorize("@ss.hasPermi('svc:gateway:apiParam:list')")
    @GetMapping("/getGatewayApiParamList")
    public CommonResult<List<GatewayApiParamRespVO>> getGatewayApiParamList(GatewayApiParamPageReqVO gatewayApiParam) {
        return CommonResult.success(BeanUtils.toBean(gatewayApiParamService.getGatewayApiParamList(gatewayApiParam.getApiId()), GatewayApiParamRespVO.class));
    }

    @Operation(summary = "导出API网关-参数列表")
    @PreAuthorize("@ss.hasPermi('svc:gateway:apiParam:export')")
    @Log(title = "API网关-参数", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, GatewayApiParamPageReqVO exportReqVO) {
        exportReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<GatewayApiParamDO> list = (List<GatewayApiParamDO>) gatewayApiParamService.getGatewayApiParamPage(exportReqVO).getRows();
        ExcelUtil<GatewayApiParamRespVO> util = new ExcelUtil<>(GatewayApiParamRespVO.class);
        util.exportExcel(response, GatewayApiParamConvert.INSTANCE.convertToRespVOList(list), "应用管理数据");
    }

    @Operation(summary = "导入API网关-参数列表")
    @PreAuthorize("@ss.hasPermi('svc:gateway:apiParam:import')")
    @Log(title = "API网关-参数", businessType = BusinessType.IMPORT)
    @PostMapping("/importData")
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception {
        ExcelUtil<GatewayApiParamRespVO> util = new ExcelUtil<>(GatewayApiParamRespVO.class);
        List<GatewayApiParamRespVO> importExcelList = util.importExcel(file.getInputStream());
        String operName = getUsername();
        String message = gatewayApiParamService.importGatewayApiParam(importExcelList, updateSupport, operName);
        return success(message);
    }

    @Operation(summary = "获取API网关-参数详细信息")
    @PreAuthorize("@ss.hasPermi('svc:gateway:apiParam:query')")
    @GetMapping(value = "/{id}")
    public CommonResult<GatewayApiParamRespVO> getInfo(@PathVariable("id") Long id) {
        GatewayApiParamDO gatewayApiParamDO = gatewayApiParamService.getGatewayApiParamById(id);
        return CommonResult.success(BeanUtils.toBean(gatewayApiParamDO, GatewayApiParamRespVO.class));
    }

    @Operation(summary = "新增API网关-参数")
    @PreAuthorize("@ss.hasPermi('svc:gateway:apiParam:add')")
    @Log(title = "API网关-参数", businessType = BusinessType.INSERT)
    @PostMapping
    public CommonResult<Long> add(@Valid @RequestBody GatewayApiParamSaveReqVO gatewayApiParam) {
        gatewayApiParam.setCreatorId(getUserId());
        gatewayApiParam.setCreateBy(getNickName());
        gatewayApiParam.setCreateTime(DateUtil.date());
        return CommonResult.toAjax(gatewayApiParamService.createGatewayApiParam(gatewayApiParam));
    }

    @Operation(summary = "修改API网关-参数")
    @PreAuthorize("@ss.hasPermi('svc:gateway:apiParam:edit')")
    @Log(title = "API网关-参数", businessType = BusinessType.UPDATE)
    @PutMapping
    public CommonResult<Integer> edit(@Valid @RequestBody GatewayApiParamSaveReqVO gatewayApiParam) {
        gatewayApiParam.setUpdatorId(getUserId());
        gatewayApiParam.setUpdateBy(getNickName());
        gatewayApiParam.setUpdateTime(DateUtil.date());
        return CommonResult.toAjax(gatewayApiParamService.updateGatewayApiParam(gatewayApiParam));
    }

    @Operation(summary = "删除API网关-参数")
    @PreAuthorize("@ss.hasPermi('svc:gateway:apiParam:remove')")
    @Log(title = "API网关-参数", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public CommonResult<Integer> remove(@PathVariable Long[] ids) {
        return CommonResult.toAjax(gatewayApiParamService.removeGatewayApiParam(Arrays.asList(ids)));
    }
}
