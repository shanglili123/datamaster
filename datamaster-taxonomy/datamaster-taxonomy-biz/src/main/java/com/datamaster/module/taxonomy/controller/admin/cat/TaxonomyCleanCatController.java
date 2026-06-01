

package com.datamaster.module.taxonomy.controller.admin.cat;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
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
import com.datamaster.module.taxonomy.controller.admin.cat.vo.TaxonomyCleanCatPageReqVO;
import com.datamaster.module.taxonomy.controller.admin.cat.vo.TaxonomyCleanCatRespVO;
import com.datamaster.module.taxonomy.controller.admin.cat.vo.TaxonomyCleanCatSaveReqVO;
import com.datamaster.module.taxonomy.convert.cat.TaxonomyCleanCatConvert;
import com.datamaster.module.taxonomy.dal.dataobject.cat.TaxonomyCleanCatDO;
import com.datamaster.module.taxonomy.service.cat.ITaxonomyCleanCatService;

/**
 * 清洗规则类目Controller
 *
 * @author DATAMASTER
 * @date 2025-08-11
 */
@Tag(name = "清洗规则类目")
@RestController
@RequestMapping("/tax/cleanCat")
@Validated
public class TaxonomyCleanCatController extends BaseController {
    @Resource
    private ITaxonomyCleanCatService TaxonomyCleanCatService;

    @Operation(summary = "查询清洗规则类目列表")
    @GetMapping("/listPage")
    public CommonResult<PageResult<TaxonomyCleanCatRespVO>> listPage(TaxonomyCleanCatPageReqVO TaxonomyCleanCat) {
        PageResult<TaxonomyCleanCatDO> page = TaxonomyCleanCatService.getAttCleanCatPage(TaxonomyCleanCat);
        return CommonResult.success(BeanUtils.toBean(page, TaxonomyCleanCatRespVO.class));
    }
    @Operation(summary = "查询清洗规则类目列表")
    @GetMapping("/list")
    public CommonResult<List<TaxonomyCleanCatRespVO>> list(TaxonomyCleanCatPageReqVO TaxonomyCleanCat) {
        List<TaxonomyCleanCatDO> page = TaxonomyCleanCatService.getAttCleanCatList(TaxonomyCleanCat);
        return CommonResult.success(BeanUtils.toBean(page, TaxonomyCleanCatRespVO.class));
    }

    @Operation(summary = "导出清洗规则类目列表")
    @PreAuthorize("@ss.hasPermi('att:cleanCat:export')")
    @Log(title = "清洗规则类目", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, TaxonomyCleanCatPageReqVO exportReqVO) {
        exportReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<TaxonomyCleanCatDO> list = (List<TaxonomyCleanCatDO>) TaxonomyCleanCatService.getAttCleanCatPage(exportReqVO).getRows();
        ExcelUtil<TaxonomyCleanCatRespVO> util = new ExcelUtil<>(TaxonomyCleanCatRespVO.class);
        util.exportExcel(response, TaxonomyCleanCatConvert.INSTANCE.convertToRespVOList(list), "应用管理数据");
    }

    @Operation(summary = "导入清洗规则类目列表")
    @PreAuthorize("@ss.hasPermi('att:cleanCat:import')")
    @Log(title = "清洗规则类目", businessType = BusinessType.IMPORT)
    @PostMapping("/importData")
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception {
        ExcelUtil<TaxonomyCleanCatRespVO> util = new ExcelUtil<>(TaxonomyCleanCatRespVO.class);
        List<TaxonomyCleanCatRespVO> importExcelList = util.importExcel(file.getInputStream());
        String operName = getUsername();
        String message = TaxonomyCleanCatService.importAttCleanCat(importExcelList, updateSupport, operName);
        return success(message);
    }

    @Operation(summary = "获取清洗规则类目详细信息")
    @GetMapping(value = "/{id}")
    public CommonResult<TaxonomyCleanCatRespVO> getInfo(@PathVariable("id") Long id) {
        TaxonomyCleanCatDO TaxonomyCleanCatDO = TaxonomyCleanCatService.getAttCleanCatById(id);
        return CommonResult.success(BeanUtils.toBean(TaxonomyCleanCatDO, TaxonomyCleanCatRespVO.class));
    }

    @Operation(summary = "新增清洗规则类目")
    @PreAuthorize("@ss.hasPermi('att:cleanCat:add')")
    @Log(title = "清洗规则类目", businessType = BusinessType.INSERT)
    @PostMapping
    public CommonResult<Long> add(@Valid @RequestBody TaxonomyCleanCatSaveReqVO TaxonomyCleanCat) {
        TaxonomyCleanCat.setCreatorId(getUserId());
        TaxonomyCleanCat.setCreateBy(getNickName());
        TaxonomyCleanCat.setCreateTime(DateUtil.date());
        return CommonResult.toAjax(TaxonomyCleanCatService.createAttCleanCat(TaxonomyCleanCat));
    }

    @Operation(summary = "修改清洗规则类目")
    @PreAuthorize("@ss.hasPermi('att:cleanCat:edit')")
    @Log(title = "清洗规则类目", businessType = BusinessType.UPDATE)
    @PutMapping
    public CommonResult<Integer> edit(@Valid @RequestBody TaxonomyCleanCatSaveReqVO TaxonomyCleanCat) {
        TaxonomyCleanCat.setUpdatorId(getUserId());
        TaxonomyCleanCat.setUpdateBy(getNickName());
        TaxonomyCleanCat.setUpdateTime(DateUtil.date());
        return CommonResult.toAjax(TaxonomyCleanCatService.updateAttCleanCat(TaxonomyCleanCat));
    }

    @Operation(summary = "删除清洗规则类目")
    @PreAuthorize("@ss.hasPermi('att:cleanCat:remove')")
    @Log(title = "清洗规则类目", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public CommonResult<Integer> remove(@PathVariable Long ids) {
        return CommonResult.toAjax(TaxonomyCleanCatService.removeAttCleanCat(ids));
    }

}
