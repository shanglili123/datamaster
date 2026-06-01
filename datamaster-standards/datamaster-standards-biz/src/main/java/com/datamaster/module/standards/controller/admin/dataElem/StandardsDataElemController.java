package com.datamaster.module.standards.controller.admin.dataElem;

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
import com.datamaster.module.standards.controller.admin.dataElem.vo.StandardsDataElemPageReqVO;
import com.datamaster.module.standards.controller.admin.dataElem.vo.StandardsDataElemRespVO;
import com.datamaster.module.standards.controller.admin.dataElem.vo.StandardsDataElemSaveReqVO;
import com.datamaster.module.standards.convert.dataElem.StandardsDataElemConvert;
import com.datamaster.module.standards.dal.dataobject.dataElem.StandardsDataElemDO;
import com.datamaster.module.standards.service.dataElem.IStandardsDataElemService;

/**
 * 数据元Controller
 *
 * @author DATAMASTER
 * @date 2025-01-21
 */
@Tag(name = "数据元")
@RestController
@RequestMapping("/std/dataElem")
@Validated
public class StandardsDataElemController extends BaseController {
    @Resource
    private IStandardsDataElemService StandardsDataElemService;

    @Operation(summary = "查询数据元列表")
    @PreAuthorize("@ss.hasPermi('dp:dataElem:list')")
    @GetMapping("/list")
    public CommonResult<PageResult<StandardsDataElemRespVO>> list(StandardsDataElemPageReqVO StandardsDataElem) {
        PageResult<StandardsDataElemDO> page = StandardsDataElemService.getDpDataElemPage(StandardsDataElem);
        return CommonResult.success(BeanUtils.toBean(page, StandardsDataElemRespVO.class));
    }

    @Operation(summary = "查询数据元列表")
    @PreAuthorize("@ss.hasPermi('dp:dataElem:list')")
    @GetMapping("/getDpDataElemList")
    public CommonResult<List<StandardsDataElemRespVO>> getDpDataElemList(StandardsDataElemPageReqVO StandardsDataElem) {
        List<StandardsDataElemDO> list = StandardsDataElemService.getDpDataElemList(StandardsDataElem);
        return CommonResult.success(BeanUtils.toBean(list, StandardsDataElemRespVO.class));
    }

    @Operation(summary = "导出数据元列表")
    @PreAuthorize("@ss.hasPermi('dp:dataElem:export')")
    @Log(title = "数据元", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, StandardsDataElemPageReqVO exportReqVO) {
        exportReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<StandardsDataElemDO> list = (List<StandardsDataElemDO>) StandardsDataElemService.getDpDataElemPage(exportReqVO).getRows();
        ExcelUtil<StandardsDataElemRespVO> util = new ExcelUtil<>(StandardsDataElemRespVO.class);
        util.exportExcel(response, StandardsDataElemConvert.INSTANCE.convertToRespVOList(list), "应用管理数据");
    }

    @Operation(summary = "导入数据元列表")
    @PreAuthorize("@ss.hasPermi('dp:dataElem:import')")
    @Log(title = "数据元", businessType = BusinessType.IMPORT)
    @PostMapping("/importData")
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception {
        ExcelUtil<StandardsDataElemRespVO> util = new ExcelUtil<>(StandardsDataElemRespVO.class);
        List<StandardsDataElemRespVO> importExcelList = util.importExcel(file.getInputStream());
        String operName = getUsername();
        String message = StandardsDataElemService.importDpDataElem(importExcelList, updateSupport, operName);
        return success(message);
    }

    @Operation(summary = "获取数据元详细信息")
    @PreAuthorize("@ss.hasPermi('dp:dataElem:query')")
    @GetMapping(value = "/{id}")
    public CommonResult<StandardsDataElemRespVO> getInfo(@PathVariable("id") Long id) {
        StandardsDataElemDO StandardsDataElemDO = StandardsDataElemService.getDpDataElemById(id);
        return CommonResult.success(BeanUtils.toBean(StandardsDataElemDO, StandardsDataElemRespVO.class));
    }

    @Operation(summary = "新增数据元")
    @PreAuthorize("@ss.hasPermi('dp:dataElem:add')")
    @Log(title = "数据元", businessType = BusinessType.INSERT)
    @PostMapping
    public CommonResult<Long> add(@Valid @RequestBody StandardsDataElemSaveReqVO StandardsDataElem) {
        StandardsDataElem.setCreatorId(getUserId());
        StandardsDataElem.setCreateBy(getNickName());
        StandardsDataElem.setCreateTime(DateUtil.date());
        return CommonResult.toAjax(StandardsDataElemService.createDpDataElem(StandardsDataElem));
    }

    @Operation(summary = "修改数据元")
    @PreAuthorize("@ss.hasPermi('dp:dataElem:edit')")
    @Log(title = "数据元", businessType = BusinessType.UPDATE)
    @PutMapping
    public CommonResult<Integer> edit(@Valid @RequestBody StandardsDataElemSaveReqVO StandardsDataElem) {
        StandardsDataElem.setUpdatorId(getUserId());
        StandardsDataElem.setUpdateBy(getNickName());
        StandardsDataElem.setUpdateTime(DateUtil.date());
        return CommonResult.toAjax(StandardsDataElemService.updateDpDataElem(StandardsDataElem));
    }

    @Operation(summary = "删除数据元")
    @PreAuthorize("@ss.hasPermi('dp:dataElem:remove')")
    @Log(title = "数据元", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public CommonResult<Integer> remove(@PathVariable Long[] ids) {
        return CommonResult.toAjax(StandardsDataElemService.removeDpDataElem(Arrays.asList(ids)));
    }

    @Operation(summary = "更改数据元状态")
    @PreAuthorize("@ss.hasPermi('dp:dataElem:edit')")
    @Log(title = "更改数据元状态", businessType = BusinessType.UPDATE)
    @PostMapping("/updateStatus/{id}/{status}")
    public CommonResult<Boolean> updateStatus(@PathVariable Long id,@PathVariable Long status) {
        return CommonResult.toAjax(StandardsDataElemService.updateStatus(id,status));
    }


}
