package com.datamaster.module.service.controller.admin.gateway.gis;

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
import com.datamaster.module.service.controller.admin.gateway.gis.vo.GatewayGisPageReqVO;
import com.datamaster.module.service.controller.admin.gateway.gis.vo.GatewayGisReqVO;
import com.datamaster.module.service.controller.admin.gateway.gis.vo.GatewayGisRespVO;
import com.datamaster.module.service.controller.admin.gateway.gis.vo.GatewayGisSaveReqVO;
import com.datamaster.module.service.convert.gateway.gis.GatewayGisConvert;
import com.datamaster.module.service.dal.dataobject.gateway.gis.GatewayGisDO;
import com.datamaster.module.service.service.gateway.gis.IGatewayGisService;

/**
 * 数据服务-GIS网关Controller
 *
 * @author DATAMASTER
 * @date 2025-04-14
 */
@Tag(name = "数据服务-GIS网关")
@RestController
@RequestMapping("/svc/gateway/gis")
@Validated
public class GatewayGisController extends BaseController {
    @Resource
    private IGatewayGisService gatewayGisService;

    @Operation(summary = "查询GIS网关列表")
    @PreAuthorize("@ss.hasPermi('svc:gateway:gis:list')")
    @GetMapping("/list")
    public CommonResult<PageResult<GatewayGisRespVO>> list(GatewayGisPageReqVO gatewayGis) {
        PageResult<GatewayGisDO> page = gatewayGisService.getGatewayGisPage(gatewayGis);
        return CommonResult.success(BeanUtils.toBean(page, GatewayGisRespVO.class));
    }

    @Operation(summary = "导出GIS网关列表")
    @PreAuthorize("@ss.hasPermi('svc:gateway:gis:export')")
    @Log(title = "GIS网关", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, GatewayGisPageReqVO exportReqVO) {
        exportReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<GatewayGisDO> list = (List<GatewayGisDO>) gatewayGisService.getGatewayGisPage(exportReqVO).getRows();
        ExcelUtil<GatewayGisRespVO> util = new ExcelUtil<>(GatewayGisRespVO.class);
        util.exportExcel(response, GatewayGisConvert.INSTANCE.convertToRespVOList(list), "应用管理数据");
    }

    @Operation(summary = "导入GIS网关列表")
    @PreAuthorize("@ss.hasPermi('svc:gateway:gis:import')")
    @Log(title = "GIS网关", businessType = BusinessType.IMPORT)
    @PostMapping("/importData")
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception {
        ExcelUtil<GatewayGisRespVO> util = new ExcelUtil<>(GatewayGisRespVO.class);
        List<GatewayGisRespVO> importExcelList = util.importExcel(file.getInputStream());
        String operName = getUsername();
        String message = gatewayGisService.importGatewayGis(importExcelList, updateSupport, operName);
        return success(message);
    }

    @Operation(summary = "获取GIS网关详细信息")
    @PreAuthorize("@ss.hasPermi('svc:gateway:gis:query')")
    @GetMapping(value = "/{id}")
    public CommonResult<GatewayGisRespVO> getInfo(@PathVariable("id") Long id) {
        GatewayGisDO gatewayGisDO = gatewayGisService.getGatewayGisById(id);
        return CommonResult.success(BeanUtils.toBean(gatewayGisDO, GatewayGisRespVO.class));
    }

    @Operation(summary = "新增GIS网关")
    @PreAuthorize("@ss.hasPermi('svc:gateway:gis:add')")
    @Log(title = "GIS网关", businessType = BusinessType.INSERT)
    @PostMapping
    public CommonResult<Long> add(@Valid @RequestBody GatewayGisSaveReqVO gatewayGis) {
        gatewayGis.setCreatorId(getUserId());
        gatewayGis.setCreateBy(getNickName());
        gatewayGis.setCreateTime(DateUtil.date());
        return CommonResult.toAjax(gatewayGisService.createGatewayGis(gatewayGis));
    }

    @Operation(summary = "修改GIS网关")
    @PreAuthorize("@ss.hasPermi('svc:gateway:gis:edit')")
    @Log(title = "GIS网关", businessType = BusinessType.UPDATE)
    @PutMapping
    public CommonResult<Integer> edit(@Valid @RequestBody GatewayGisSaveReqVO gatewayGis) {
        gatewayGis.setUpdatorId(getUserId());
        gatewayGis.setUpdateBy(getNickName());
        gatewayGis.setUpdateTime(DateUtil.date());
        return CommonResult.toAjax(gatewayGisService.updateGatewayGis(gatewayGis));
    }

    @Operation(summary = "删除GIS网关")
    @PreAuthorize("@ss.hasPermi('svc:gateway:gis:remove')")
    @Log(title = "GIS网关", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public CommonResult<Integer> remove(@PathVariable Long[] ids) {
        return CommonResult.toAjax(gatewayGisService.removeGatewayGis(Arrays.asList(ids)));
    }

    @Operation(summary = "GIS网关转发调用")
    @PreAuthorize("@ss.hasPermi('svc:gateway:gis:queryServiceForwarding')")
    @PostMapping("/queryServiceForwarding")
    public void queryServiceForwarding(HttpServletResponse response, @Valid @RequestBody GatewayGisReqVO gatewayGis) {
        response.setContentType(MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8");
        gatewayGisService.queryServiceForwarding(response, gatewayGis);
    }
}
