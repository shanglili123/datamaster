

package com.datamaster.module.taxonomy.controller.admin.cat;

import cn.hutool.core.date.DateUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.datamaster.common.annotation.Log;
import com.datamaster.common.core.controller.BaseController;
import com.datamaster.common.core.domain.AjaxResult;
import com.datamaster.common.core.domain.CommonResult;
import com.datamaster.common.core.page.PageParam;
import com.datamaster.common.enums.BusinessType;
import com.datamaster.common.utils.object.BeanUtils;
import com.datamaster.common.utils.poi.ExcelUtil;
import com.datamaster.module.taxonomy.controller.admin.cat.vo.TaxonomyModelCatPageReqVO;
import com.datamaster.module.taxonomy.controller.admin.cat.vo.TaxonomyModelCatRespVO;
import com.datamaster.module.taxonomy.controller.admin.cat.vo.TaxonomyModelCatSaveReqVO;
import com.datamaster.module.taxonomy.convert.cat.TaxonomyModelCatConvert;
import com.datamaster.module.taxonomy.dal.dataobject.cat.TaxonomyModelCatDO;
import com.datamaster.module.taxonomy.service.cat.ITaxonomyModelCatService;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.util.List;

/**
 * 逻辑模型类目管理Controller
 *
 * @author DATAMASTER
 * @date 2025-01-20
 */
@Tag(name = "逻辑模型类目管理")
@RestController
@RequestMapping("/att/modelCat")
@Validated
public class TaxonomyModelCatController extends BaseController {
    @Resource
    private ITaxonomyModelCatService TaxonomyModelCatService;

    @Operation(summary = "查询逻辑模型类目管理列表")
    @PreAuthorize("@ss.hasPermi('att:modelCat:list')")
    @GetMapping("/list")
    public CommonResult<List<TaxonomyModelCatRespVO>> list(TaxonomyModelCatPageReqVO TaxonomyModelCat) {
        List<TaxonomyModelCatDO> TaxonomyModelCatDOList = TaxonomyModelCatService.getAttModelCatList(TaxonomyModelCat);
        return CommonResult.success(BeanUtils.toBean(TaxonomyModelCatDOList, TaxonomyModelCatRespVO.class));
    }

    @Operation(summary = "导出逻辑模型类目管理列表")
    @PreAuthorize("@ss.hasPermi('att:modelCat:export')")
    @Log(title = "逻辑模型类目管理", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, TaxonomyModelCatPageReqVO exportReqVO) {
        exportReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<TaxonomyModelCatDO> list = (List<TaxonomyModelCatDO>) TaxonomyModelCatService.getAttModelCatPage(exportReqVO).getRows();
        ExcelUtil<TaxonomyModelCatRespVO> util = new ExcelUtil<>(TaxonomyModelCatRespVO.class);
        util.exportExcel(response, TaxonomyModelCatConvert.INSTANCE.convertToRespVOList(list), "应用管理数据");
    }

    @Operation(summary = "导入逻辑模型类目管理列表")
    @PreAuthorize("@ss.hasPermi('att:modelCat:import')")
    @Log(title = "逻辑模型类目管理", businessType = BusinessType.IMPORT)
    @PostMapping("/importData")
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception {
        ExcelUtil<TaxonomyModelCatRespVO> util = new ExcelUtil<>(TaxonomyModelCatRespVO.class);
        List<TaxonomyModelCatRespVO> importExcelList = util.importExcel(file.getInputStream());
        String operName = getUsername();
        String message = TaxonomyModelCatService.importAttModelCat(importExcelList, updateSupport, operName);
        return success(message);
    }

    @Operation(summary = "获取逻辑模型类目管理详细信息")
    @PreAuthorize("@ss.hasPermi('att:modelCat:query')")
    @GetMapping(value = "/{ID}")
    public CommonResult<TaxonomyModelCatRespVO> getInfo(@PathVariable("ID") Long ID) {
        TaxonomyModelCatDO TaxonomyModelCatDO = TaxonomyModelCatService.getAttModelCatById(ID);
        return CommonResult.success(BeanUtils.toBean(TaxonomyModelCatDO, TaxonomyModelCatRespVO.class));
    }

    @Operation(summary = "新增逻辑模型类目管理")
    @PreAuthorize("@ss.hasPermi('att:modelCat:add')")
    @Log(title = "逻辑模型类目管理", businessType = BusinessType.INSERT)
    @PostMapping
    public CommonResult<Long> add(@Valid @RequestBody TaxonomyModelCatSaveReqVO TaxonomyModelCat) {
        TaxonomyModelCat.setCreatorId(getUserId());
        TaxonomyModelCat.setCreateBy(getNickName());
        TaxonomyModelCat.setCreateTime(DateUtil.date());
        return CommonResult.toAjax(TaxonomyModelCatService.createAttModelCat(TaxonomyModelCat));
    }

    @Operation(summary = "修改逻辑模型类目管理")
    @PreAuthorize("@ss.hasPermi('att:modelCat:edit')")
    @Log(title = "逻辑模型类目管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public CommonResult<Integer> edit(@Valid @RequestBody TaxonomyModelCatSaveReqVO TaxonomyModelCat) {
        TaxonomyModelCat.setUpdatorId(getUserId());
        TaxonomyModelCat.setUpdateBy(getNickName());
        TaxonomyModelCat.setUpdateTime(DateUtil.date());
        return CommonResult.toAjax(TaxonomyModelCatService.updateAttModelCat(TaxonomyModelCat));
    }

    //    @Operation(summary = "删除逻辑模型类目管理")
//    @PreAuthorize("@ss.hasPermi('att:modelCat:remove')")
//    @Log(title = "逻辑模型类目管理", businessType = BusinessType.DELETE)
//    @DeleteMapping("/{IDs}")
//    public CommonResult<Integer> remove(@PathVariable Long[] ids) {
//        return CommonResult.toAjax(TaxonomyModelCatService.removeAttModelCat(Arrays.asList(ids)));
//    }
    //删除
    @Operation(summary = "删除逻辑模型类目管理")
    @PreAuthorize("@ss.hasPermi('att:modelCat:remove')")
    @Log(title = "逻辑模型类目管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ID}")
    public CommonResult<Integer> remove(@PathVariable Long ID) {
        return CommonResult.toAjax(TaxonomyModelCatService.removeAttModelCat(ID));
    }

}
