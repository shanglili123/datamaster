package com.datamaster.module.assets.controller.admin.assetchild.api;

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
import com.datamaster.common.exception.enums.GlobalErrorCodeConstants;
import com.datamaster.module.assets.controller.admin.assetchild.api.vo.AssetsAssetApiPageReqVO;
import com.datamaster.module.assets.controller.admin.assetchild.api.vo.AssetsAssetApiReqVO;
import com.datamaster.module.assets.controller.admin.assetchild.api.vo.AssetsAssetApiRespVO;
import com.datamaster.module.assets.controller.admin.assetchild.api.vo.AssetsAssetApiSaveReqVO;
import com.datamaster.module.assets.convert.assetchild.api.AssetsAssetApiConvert;
import com.datamaster.module.assets.dal.dataobject.assetchild.api.AssetsAssetApiDO;
import com.datamaster.module.assets.service.assetchild.api.IAssetsAssetApiService;

/**
 * 数据资产-外部APIController * * @author DATAMASTER * @date 2025-04-14
 */
@Tag(name = "数据资产-外部API")
@RestController
@RequestMapping("/ast/api")
@Validated
public class AssetsAssetApiController extends BaseController {
    @Resource
    private IAssetsAssetApiService AssetsAssetApiService;

    @Operation(summary = "查询数据资产-外部API列表")
    @PreAuthorize("@ss.hasPermi('da:api:list')")
    @GetMapping("/list")
    public CommonResult<PageResult<AssetsAssetApiRespVO>> list(AssetsAssetApiPageReqVO AssetsAssetApi) {
        PageResult<AssetsAssetApiDO> page = AssetsAssetApiService.getAssetApiPage(AssetsAssetApi);
        return CommonResult.success(BeanUtils.toBean(page, AssetsAssetApiRespVO.class));
    }

    @Operation(summary = "导出数据资产-外部API列表")
    @PreAuthorize("@ss.hasPermi('da:api:export')")
    @Log(title = "数据资产-外部API", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, AssetsAssetApiPageReqVO exportReqVO) {
        exportReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<AssetsAssetApiDO> list = (List<AssetsAssetApiDO>) AssetsAssetApiService.getAssetApiPage(exportReqVO).getRows();
        ExcelUtil<AssetsAssetApiRespVO> util = new ExcelUtil<>(AssetsAssetApiRespVO.class);
        util.exportExcel(response, AssetsAssetApiConvert.INSTANCE.convertToRespVOList(list), "应用管理数据");
    }

    @Operation(summary = "导入数据资产-外部API列表")
    @PreAuthorize("@ss.hasPermi('da:api:import')")
    @Log(title = "数据资产-外部API", businessType = BusinessType.IMPORT)
    @PostMapping("/importData")
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception {
        ExcelUtil<AssetsAssetApiRespVO> util = new ExcelUtil<>(AssetsAssetApiRespVO.class);
        List<AssetsAssetApiRespVO> importExcelList = util.importExcel(file.getInputStream());
        String operName = getUsername();
        String message = AssetsAssetApiService.importAssetApi(importExcelList, updateSupport, operName);
        return success(message);
    }

    @Operation(summary = "获取数据资产-外部API详细信息")
    @PreAuthorize("@ss.hasPermi('da:api:query')")
    @GetMapping(value = "/{id}")
    public CommonResult<AssetsAssetApiRespVO> getInfo(@PathVariable("id") Long id) {
        AssetsAssetApiDO AssetsAssetApiDO = AssetsAssetApiService.getAssetApiById(id);
        return CommonResult.success(BeanUtils.toBean(AssetsAssetApiDO, AssetsAssetApiRespVO.class));
    }

    @Operation(summary = "新增数据资产-外部API")
    @PreAuthorize("@ss.hasPermi('da:api:add')")
    @Log(title = "数据资产-外部API", businessType = BusinessType.INSERT)
    @PostMapping
    public CommonResult<Long> add(@Valid @RequestBody AssetsAssetApiSaveReqVO AssetsAssetApi) {
        AssetsAssetApi.setCreatorId(getUserId());
        AssetsAssetApi.setCreateBy(getNickName());
        AssetsAssetApi.setCreateTime(DateUtil.date());
        return CommonResult.toAjax(AssetsAssetApiService.createAssetApi(AssetsAssetApi));
    }

    @Operation(summary = "修改数据资产-外部API")
    @PreAuthorize("@ss.hasPermi('da:api:edit')")
    @Log(title = "数据资产-外部API", businessType = BusinessType.UPDATE)
    @PutMapping
    public CommonResult<Integer> edit(@Valid @RequestBody AssetsAssetApiSaveReqVO AssetsAssetApi) {
        AssetsAssetApi.setUpdatorId(getUserId());
        AssetsAssetApi.setUpdateBy(getNickName());
        AssetsAssetApi.setUpdateTime(DateUtil.date());
        return CommonResult.toAjax(AssetsAssetApiService.updateAssetApi(AssetsAssetApi));
    }

    @Operation(summary = "删除数据资产-外部API")
    @PreAuthorize("@ss.hasPermi('da:api:remove')")
    @Log(title = "数据资产-外部API", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public CommonResult<Integer> remove(@PathVariable Long[] ids) {
        return CommonResult.toAjax(AssetsAssetApiService.removeAssetApi(Arrays.asList(ids)));
    }

    @Operation(summary = "删除数据资产-外部API")
    @PreAuthorize("@ss.hasPermi('da:asset:edit')")
    @PostMapping("/queryServiceForwarding")
    public void queryServiceForwarding(HttpServletResponse response, @Valid @RequestBody AssetsAssetApiReqVO AssetsAssetApi) {
        response.setContentType(MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8");
        AssetsAssetApiService.queryServiceForwarding(response, AssetsAssetApi);
    }
}