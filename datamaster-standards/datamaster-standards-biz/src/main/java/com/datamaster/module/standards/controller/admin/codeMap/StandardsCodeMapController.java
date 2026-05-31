package com.datamaster.module.standards.controller.admin.codeMap;

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
import com.datamaster.module.standards.controller.admin.codeMap.vo.StandardsCodeMapPageReqVO;
import com.datamaster.module.standards.controller.admin.codeMap.vo.StandardsCodeMapRespVO;
import com.datamaster.module.standards.controller.admin.codeMap.vo.StandardsCodeMapSaveReqVO;
import com.datamaster.module.standards.convert.codeMap.StandardsCodeMapConvert;
import com.datamaster.module.standards.dal.dataobject.codeMap.StandardsCodeMapDO;
import com.datamaster.module.standards.service.codeMap.IStandardsCodeMapService;

/**
 * 数据元代码映射Controller
 *
 * @author DATAMASTER
 * @date 2025-01-21
 */
@Tag(name = "数据元代码映射")
@RestController
@RequestMapping("/dp/codeMap")
@Validated
public class StandardsCodeMapController extends BaseController {
    @Resource
    private IStandardsCodeMapService StandardsCodeMapService;

    @Operation(summary = "查询数据元代码映射列表")
    @PreAuthorize("@ss.hasPermi('dp:codeMap:list')")
    @GetMapping("/list")
    public CommonResult<PageResult<StandardsCodeMapRespVO>> list(StandardsCodeMapPageReqVO StandardsCodeMap) {
        PageResult<StandardsCodeMapDO> page = StandardsCodeMapService.getDpCodeMapPage(StandardsCodeMap);
        return CommonResult.success(BeanUtils.toBean(page, StandardsCodeMapRespVO.class));
    }

    @Operation(summary = "导出数据元代码映射列表")
    @PreAuthorize("@ss.hasPermi('dp:codeMap:export')")
    @Log(title = "数据元代码映射", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, StandardsCodeMapPageReqVO exportReqVO) {
        exportReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<StandardsCodeMapDO> list = (List<StandardsCodeMapDO>) StandardsCodeMapService.getDpCodeMapPage(exportReqVO).getRows();
        ExcelUtil<StandardsCodeMapRespVO> util = new ExcelUtil<>(StandardsCodeMapRespVO.class);
        util.exportExcel(response, StandardsCodeMapConvert.INSTANCE.convertToRespVOList(list), "应用管理数据");
    }

    @Operation(summary = "导入数据元代码映射列表")
    @PreAuthorize("@ss.hasPermi('dp:codeMap:import')")
    @Log(title = "数据元代码映射", businessType = BusinessType.IMPORT)
    @PostMapping("/importData")
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception {
        ExcelUtil<StandardsCodeMapRespVO> util = new ExcelUtil<>(StandardsCodeMapRespVO.class);
        List<StandardsCodeMapRespVO> importExcelList = util.importExcel(file.getInputStream());
        String operName = getUsername();
        String message = StandardsCodeMapService.importDpCodeMap(importExcelList, updateSupport, operName);
        return success(message);
    }

    @Operation(summary = "获取数据元代码映射详细信息")
    @PreAuthorize("@ss.hasPermi('dp:codeMap:query')")
    @GetMapping(value = "/{id}")
    public CommonResult<StandardsCodeMapRespVO> getInfo(@PathVariable("id") Long id) {
        StandardsCodeMapDO StandardsCodeMapDO = StandardsCodeMapService.getDpCodeMapById(id);
        return CommonResult.success(BeanUtils.toBean(StandardsCodeMapDO, StandardsCodeMapRespVO.class));
    }

    @Operation(summary = "新增数据元代码映射")
    @PreAuthorize("@ss.hasPermi('dp:codeMap:add')")
    @Log(title = "数据元代码映射", businessType = BusinessType.INSERT)
    @PostMapping
    public CommonResult<Long> add(@Valid @RequestBody StandardsCodeMapSaveReqVO StandardsCodeMap) {
        StandardsCodeMap.setCreatorId(getUserId());
        StandardsCodeMap.setCreateBy(getNickName());
        StandardsCodeMap.setCreateTime(DateUtil.date());
        return CommonResult.toAjax(StandardsCodeMapService.createDpCodeMap(StandardsCodeMap));
    }

    @Operation(summary = "修改数据元代码映射")
    @PreAuthorize("@ss.hasPermi('dp:codeMap:edit')")
    @Log(title = "数据元代码映射", businessType = BusinessType.UPDATE)
    @PutMapping
    public CommonResult<Integer> edit(@Valid @RequestBody StandardsCodeMapSaveReqVO StandardsCodeMap) {
        StandardsCodeMap.setUpdatorId(getUserId());
        StandardsCodeMap.setUpdateBy(getNickName());
        StandardsCodeMap.setUpdateTime(DateUtil.date());
        return CommonResult.toAjax(StandardsCodeMapService.updateDpCodeMap(StandardsCodeMap));
    }

    @Operation(summary = "删除数据元代码映射")
    @PreAuthorize("@ss.hasPermi('dp:codeMap:remove')")
    @Log(title = "数据元代码映射", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public CommonResult<Integer> remove(@PathVariable Long[] ids) {
        return CommonResult.toAjax(StandardsCodeMapService.removeDpCodeMap(Arrays.asList(ids)));
    }

}
