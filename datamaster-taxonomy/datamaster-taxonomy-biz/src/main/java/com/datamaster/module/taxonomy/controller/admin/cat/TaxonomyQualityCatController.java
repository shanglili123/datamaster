

package com.datamaster.module.taxonomy.controller.admin.cat;

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
import com.datamaster.common.enums.BusinessType;
import com.datamaster.common.utils.object.BeanUtils;
import com.datamaster.common.utils.poi.ExcelUtil;
import com.datamaster.module.taxonomy.controller.admin.cat.vo.TaxonomyQualityCatPageReqVO;
import com.datamaster.module.taxonomy.controller.admin.cat.vo.TaxonomyQualityCatRespVO;
import com.datamaster.module.taxonomy.controller.admin.cat.vo.TaxonomyQualityCatSaveReqVO;
import com.datamaster.module.taxonomy.convert.cat.TaxonomyQualityCatConvert;
import com.datamaster.module.taxonomy.dal.dataobject.cat.TaxonomyQualityCatDO;
import com.datamaster.module.taxonomy.service.cat.ITaxonomyQualityCatService;

/**
 * 数据质量类目Controller
 *
 * @author DATAMASTER
 * @date 2025-07-19
 */
@Tag(name = "数据质量类目")
@RestController
@RequestMapping("/att/qualityCat")
@Validated
public class TaxonomyQualityCatController extends BaseController {
    @Resource
    private ITaxonomyQualityCatService TaxonomyQualityCatService;

    @Operation(summary = "查询数据质量类目列表")
    @GetMapping("/list")
    public CommonResult<List<TaxonomyQualityCatRespVO>> list(TaxonomyQualityCatPageReqVO TaxonomyQualityCat) {
        List<TaxonomyQualityCatDO> page = TaxonomyQualityCatService.getAttQualityCatList(TaxonomyQualityCat);
        return CommonResult.success(BeanUtils.toBean(page, TaxonomyQualityCatRespVO.class));
    }

    @Operation(summary = "导出数据质量类目列表")
    @Log(title = "数据质量类目", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, TaxonomyQualityCatPageReqVO exportReqVO) {
        exportReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<TaxonomyQualityCatDO> list = (List<TaxonomyQualityCatDO>) TaxonomyQualityCatService.getAttQualityCatPage(exportReqVO).getRows();
        ExcelUtil<TaxonomyQualityCatRespVO> util = new ExcelUtil<>(TaxonomyQualityCatRespVO.class);
        util.exportExcel(response, TaxonomyQualityCatConvert.INSTANCE.convertToRespVOList(list), "应用管理数据");
    }

    @Operation(summary = "导入数据质量类目列表")
    @PreAuthorize("@ss.hasPermi('att:qualityCat:import')")
    @Log(title = "数据质量类目", businessType = BusinessType.IMPORT)
    @PostMapping("/importData")
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception {
        ExcelUtil<TaxonomyQualityCatRespVO> util = new ExcelUtil<>(TaxonomyQualityCatRespVO.class);
        List<TaxonomyQualityCatRespVO> importExcelList = util.importExcel(file.getInputStream());
        String operName = getUsername();
        String message = TaxonomyQualityCatService.importAttQualityCat(importExcelList, updateSupport, operName);
        return success(message);
    }

    @Operation(summary = "获取数据质量类目详细信息")
    @GetMapping(value = "/{id}")
    public CommonResult<TaxonomyQualityCatRespVO> getInfo(@PathVariable("id") Long id) {
        TaxonomyQualityCatDO TaxonomyQualityCatDO = TaxonomyQualityCatService.getAttQualityCatById(id);
        return CommonResult.success(BeanUtils.toBean(TaxonomyQualityCatDO, TaxonomyQualityCatRespVO.class));
    }

    @Operation(summary = "新增数据质量类目")
    @PreAuthorize("@ss.hasPermi('att:qualityCat:add')")
    @Log(title = "数据质量类目", businessType = BusinessType.INSERT)
    @PostMapping
    public CommonResult<Long> add(@Valid @RequestBody TaxonomyQualityCatSaveReqVO TaxonomyQualityCat) {
        TaxonomyQualityCat.setCreatorId(getUserId());
        TaxonomyQualityCat.setCreateBy(getNickName());
        TaxonomyQualityCat.setCreateTime(DateUtil.date());
        return CommonResult.toAjax(TaxonomyQualityCatService.createAttQualityCat(TaxonomyQualityCat));
    }

    @Operation(summary = "修改数据质量类目")
    @PreAuthorize("@ss.hasPermi('att:qualityCat:edit')")
    @Log(title = "数据质量类目", businessType = BusinessType.UPDATE)
    @PutMapping
    public CommonResult<Integer> edit(@Valid @RequestBody TaxonomyQualityCatSaveReqVO TaxonomyQualityCat) {
        TaxonomyQualityCat.setUpdatorId(getUserId());
        TaxonomyQualityCat.setUpdateBy(getNickName());
        TaxonomyQualityCat.setUpdateTime(DateUtil.date());
        return CommonResult.toAjax(TaxonomyQualityCatService.updateAttQualityCat(TaxonomyQualityCat));
    }

    @Operation(summary = "删除数据质量类目")
    @PreAuthorize("@ss.hasPermi('att:qualityCat:remove')")
    @Log(title = "数据质量类目", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public CommonResult<Integer> remove(@PathVariable Long[] ids) {
        return CommonResult.toAjax(TaxonomyQualityCatService.removeAttQualityCat(Arrays.asList(ids)));
    }

}
