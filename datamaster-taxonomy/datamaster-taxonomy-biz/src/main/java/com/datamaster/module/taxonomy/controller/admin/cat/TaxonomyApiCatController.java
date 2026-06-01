

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
import com.datamaster.module.taxonomy.controller.admin.cat.vo.TaxonomyApiCatPageReqVO;
import com.datamaster.module.taxonomy.controller.admin.cat.vo.TaxonomyApiCatRespVO;
import com.datamaster.module.taxonomy.controller.admin.cat.vo.TaxonomyApiCatSaveReqVO;
import com.datamaster.module.taxonomy.convert.cat.TaxonomyApiCatConvert;
import com.datamaster.module.taxonomy.dal.dataobject.cat.TaxonomyApiCatDO;
import com.datamaster.module.taxonomy.service.cat.ITaxonomyApiCatService;

/**
 * 数据服务类目管理Controller
 *
 * @author DATAMASTER
 * @date 2025-03-11
 */
@Tag(name = "数据服务类目管理")
@RestController
@RequestMapping("/tax/apiCat")
@Validated
public class TaxonomyApiCatController extends BaseController {
    @Resource
    private ITaxonomyApiCatService TaxonomyApiCatService;

    @Operation(summary = "查询数据服务类目管理列表")
    @PreAuthorize("@ss.hasPermi('att:apiCat:list')")
    @GetMapping("/list")
    public CommonResult<List<TaxonomyApiCatRespVO>> list(TaxonomyApiCatPageReqVO TaxonomyApiCat) {
        List<TaxonomyApiCatDO> TaxonomyApiCatList = TaxonomyApiCatService.getAttApiCatList(TaxonomyApiCat);
        return CommonResult.success(BeanUtils.toBean(TaxonomyApiCatList, TaxonomyApiCatRespVO.class));
    }

    @Operation(summary = "导出数据服务类目管理列表")
    @PreAuthorize("@ss.hasPermi('att:apiCat:export')")
    @Log(title = "数据服务类目管理", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, TaxonomyApiCatPageReqVO exportReqVO) {
        exportReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<TaxonomyApiCatDO> list = (List<TaxonomyApiCatDO>) TaxonomyApiCatService.getAttApiCatPage(exportReqVO).getRows();
        ExcelUtil<TaxonomyApiCatRespVO> util = new ExcelUtil<>(TaxonomyApiCatRespVO.class);
        util.exportExcel(response, TaxonomyApiCatConvert.INSTANCE.convertToRespVOList(list), "应用管理数据");
    }

    @Operation(summary = "导入数据服务类目管理列表")
    @PreAuthorize("@ss.hasPermi('att:apiCat:import')")
    @Log(title = "数据服务类目管理", businessType = BusinessType.IMPORT)
    @PostMapping("/importData")
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception {
        ExcelUtil<TaxonomyApiCatRespVO> util = new ExcelUtil<>(TaxonomyApiCatRespVO.class);
        List<TaxonomyApiCatRespVO> importExcelList = util.importExcel(file.getInputStream());
        String operName = getUsername();
        String message = TaxonomyApiCatService.importAttApiCat(importExcelList, updateSupport, operName);
        return success(message);
    }

    @Operation(summary = "获取数据服务类目管理详细信息")
    @PreAuthorize("@ss.hasPermi('att:apiCat:query')")
    @GetMapping(value = "/{id}")
    public CommonResult<TaxonomyApiCatRespVO> getInfo(@PathVariable("id") Long id) {
        TaxonomyApiCatDO TaxonomyApiCatDO = TaxonomyApiCatService.getAttApiCatById(id);
        return CommonResult.success(BeanUtils.toBean(TaxonomyApiCatDO, TaxonomyApiCatRespVO.class));
    }

    @Operation(summary = "新增数据服务类目管理")
    @PreAuthorize("@ss.hasPermi('att:apiCat:add')")
    @Log(title = "数据服务类目管理", businessType = BusinessType.INSERT)
    @PostMapping
    public CommonResult<Long> add(@Valid @RequestBody TaxonomyApiCatSaveReqVO TaxonomyApiCat) {
        TaxonomyApiCat.setCreatorId(getUserId());
        TaxonomyApiCat.setCreateBy(getNickName());
        TaxonomyApiCat.setCreateTime(DateUtil.date());
        return CommonResult.toAjax(TaxonomyApiCatService.createAttApiCat(TaxonomyApiCat));
    }

    @Operation(summary = "修改数据服务类目管理")
    @PreAuthorize("@ss.hasPermi('att:apiCat:edit')")
    @Log(title = "数据服务类目管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public CommonResult<Integer> edit(@Valid @RequestBody TaxonomyApiCatSaveReqVO TaxonomyApiCat) {
        TaxonomyApiCat.setUpdatorId(getUserId());
        TaxonomyApiCat.setUpdateBy(getNickName());
        TaxonomyApiCat.setUpdateTime(DateUtil.date());
        return CommonResult.toAjax(TaxonomyApiCatService.updateAttApiCat(TaxonomyApiCat));
    }

    @Operation(summary = "删除数据服务类目管理")
    @PreAuthorize("@ss.hasPermi('att:apiCat:remove')")
    @Log(title = "数据服务类目管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public CommonResult<Integer> remove(@PathVariable Long[] ids) {
        return CommonResult.toAjax(TaxonomyApiCatService.removeAttApiCat(Arrays.asList(ids)));
    }

}
