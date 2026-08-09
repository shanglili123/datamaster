package com.datamaster.metadata.controller.discovery;

import cn.hutool.core.date.DateUtil;
import cn.hutool.json.JSONObject;
import com.datamaster.common.annotation.Log;
import com.datamaster.common.core.controller.BaseController;
import com.datamaster.common.core.domain.AjaxResult;
import com.datamaster.common.core.domain.CommonResult;
import com.datamaster.common.core.page.PageParam;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.common.enums.BusinessType;
import com.datamaster.common.utils.object.BeanUtils;
import com.datamaster.common.utils.poi.ExcelUtil;
import com.datamaster.metadata.controller.discovery.vo.AssetsDiscoveryTablePageReqVO;
import com.datamaster.metadata.controller.discovery.vo.AssetsDiscoveryTableRespVO;
import com.datamaster.metadata.controller.discovery.vo.AssetsDiscoveryTableSaveReqVO;
import com.datamaster.metadata.convert.discovery.AssetsDiscoveryTableConvert;
import com.datamaster.metadata.dal.dataobject.discovery.AssetsDiscoveryTableDO;
import com.datamaster.metadata.service.discovery.IAssetsDiscoveryTableService;
import com.datamaster.module.assets.api.service.asset.IAssetsAssetApiOutService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * 数据发现库信息Controller
 *
 * @author DATAMASTER
 * @date 2025-02-11
 */
@Tag(name = "数据发现库信息")
@RestController
@RequestMapping("/ast/discoveryTable")
@Validated
public class AssetsDiscoveryTableController extends BaseController {

    @Resource
    private IAssetsDiscoveryTableService AssetsDiscoveryTableService;

    @Resource
    private IAssetsAssetApiOutService AssetsAssetService;

    @Operation(summary = "查询数据发现库信息列表")
    @PreAuthorize("@ss.hasPermi('ast:discoveryTable:list')")
    @GetMapping("/list")
    public CommonResult<PageResult<AssetsDiscoveryTableRespVO>> list(AssetsDiscoveryTablePageReqVO AssetsDiscoveryTable) {
        PageResult<AssetsDiscoveryTableDO> page = AssetsDiscoveryTableService.getDaDiscoveryTablePage(AssetsDiscoveryTable);
        return CommonResult.success(BeanUtils.toBean(page, AssetsDiscoveryTableRespVO.class));
    }

    @Operation(summary = "查询数据发现库信息列表")
    @PreAuthorize("@ss.hasPermi('ast:discoveryTable:list')")
    @GetMapping("/getDaDiscoveryTableList")
    public CommonResult<List<AssetsDiscoveryTableRespVO>> getDaDiscoveryTableList(AssetsDiscoveryTablePageReqVO AssetsDiscoveryTable) {
        List<AssetsDiscoveryTableDO> page = AssetsDiscoveryTableService.getDaDiscoveryTableList(AssetsDiscoveryTable);
        return CommonResult.success(BeanUtils.toBean(page, AssetsDiscoveryTableRespVO.class));
    }

    @Operation(summary = "导出数据发现库信息列表")
    @PreAuthorize("@ss.hasPermi('ast:discoveryTable:export')")
    @Log(title = "数据发现库信息", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, AssetsDiscoveryTablePageReqVO exportReqVO) {
        exportReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<AssetsDiscoveryTableDO> list = (List<AssetsDiscoveryTableDO>) AssetsDiscoveryTableService.getDaDiscoveryTablePage(exportReqVO).getRows();
        ExcelUtil<AssetsDiscoveryTableRespVO> util = new ExcelUtil<>(AssetsDiscoveryTableRespVO.class);
        util.exportExcel(response, AssetsDiscoveryTableConvert.INSTANCE.convertToRespVOList(list), "应用管理数据");
    }

    @Operation(summary = "导入数据发现库信息列表")
    @PreAuthorize("@ss.hasPermi('ast:discoveryTable:import')")
    @Log(title = "数据发现库信息", businessType = BusinessType.IMPORT)
    @PostMapping("/importData")
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception {
        ExcelUtil<AssetsDiscoveryTableRespVO> util = new ExcelUtil<>(AssetsDiscoveryTableRespVO.class);
        List<AssetsDiscoveryTableRespVO> importExcelList = util.importExcel(file.getInputStream());
        String operName = getUsername();
        String message = AssetsDiscoveryTableService.importDaDiscoveryTable(importExcelList, updateSupport, operName);
        return success(message);
    }

    @Operation(summary = "获取数据发现库信息详细信息")
    @PreAuthorize("@ss.hasPermi('ast:discoveryTable:query')")
    @GetMapping(value = "/{id}")
    public CommonResult<AssetsDiscoveryTableRespVO> getInfo(@PathVariable("id") Long id) {
        AssetsDiscoveryTableDO AssetsDiscoveryTableDO = AssetsDiscoveryTableService.getDaDiscoveryTableById(id);
        return CommonResult.success(BeanUtils.toBean(AssetsDiscoveryTableDO, AssetsDiscoveryTableRespVO.class));
    }

    @Operation(summary = "新增数据发现库信息")
    @PreAuthorize("@ss.hasPermi('ast:discoveryTable:add')")
    @Log(title = "数据发现库信息", businessType = BusinessType.INSERT)
    @PostMapping
    public CommonResult<Long> add(@Valid @RequestBody AssetsDiscoveryTableSaveReqVO AssetsDiscoveryTable) {
        AssetsDiscoveryTable.setCreatorId(getUserId());
        AssetsDiscoveryTable.setCreateBy(getNickName());
        AssetsDiscoveryTable.setCreateTime(DateUtil.date());
        return CommonResult.toAjax(AssetsDiscoveryTableService.createDaDiscoveryTable(AssetsDiscoveryTable));
    }

    @Operation(summary = "修改数据发现库信息")
    @PreAuthorize("@ss.hasPermi('ast:discoveryTable:edit')")
    @Log(title = "数据发现库信息", businessType = BusinessType.UPDATE)
    @PutMapping
    public CommonResult<Integer> edit(@Valid @RequestBody AssetsDiscoveryTableSaveReqVO AssetsDiscoveryTable) {
        AssetsDiscoveryTable.setUpdatorId(getUserId());
        AssetsDiscoveryTable.setUpdateBy(getNickName());
        AssetsDiscoveryTable.setUpdateTime(DateUtil.date());
        return CommonResult.toAjax(AssetsDiscoveryTableService.updateDaDiscoveryTable(AssetsDiscoveryTable));
    }

    @Operation(summary = "删除数据发现库信息")
    @PreAuthorize("@ss.hasPermi('ast:discoveryTable:remove')")
    @Log(title = "数据发现库信息", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public CommonResult<Integer> remove(@PathVariable Long[] ids) {
        return CommonResult.toAjax(AssetsDiscoveryTableService.removeDaDiscoveryTable(Arrays.asList(ids)));
    }

    @Operation(summary = "获取数据发现的数据预览")
    @PreAuthorize("@ss.hasPermi('ast:discoveryTable:query')")
    @PostMapping(value = "/preview")
    public AjaxResult getPreview(@RequestBody JSONObject jsonObject) {
        if (jsonObject.getStr("taskId") == null) {
            return error("请携带数据发现任务id");
        }
        if (jsonObject.getStr("tableName") == null) {
            return error("请携带数据库表");
        }
        Map<String, Object> columnData = AssetsAssetService.getColumnData(jsonObject);
        return success(columnData);
    }

    @Operation(summary = "数据发现库信息进行提交撤回")
    @PreAuthorize("@ss.hasPermi('ast:discoveryTable:edit')")
    @PostMapping(value = "/commitOrRevokeDiscoveryInfo")
    public CommonResult<Integer> commitOrRevokeDiscoveryInfo(@RequestBody AssetsDiscoveryTableSaveReqVO AssetsDiscoveryTable) {
        return CommonResult.toAjax(AssetsDiscoveryTableService.commitOrRevokeDiscoveryInfo(AssetsDiscoveryTable));
    }
}
