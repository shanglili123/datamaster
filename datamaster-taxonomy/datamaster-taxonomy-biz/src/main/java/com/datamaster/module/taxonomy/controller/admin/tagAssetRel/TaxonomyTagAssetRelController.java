

package com.datamaster.module.taxonomy.controller.admin.tagAssetRel;

import cn.hutool.core.date.DateUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.datamaster.common.annotation.Log;
import com.datamaster.common.core.controller.BaseController;
import com.datamaster.common.core.domain.AjaxResult;
import com.datamaster.common.core.domain.CommonResult;
import com.datamaster.common.core.page.PageParam;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.common.enums.BusinessType;
import com.datamaster.common.utils.object.BeanUtils;
import com.datamaster.common.utils.poi.ExcelUtil;
import com.datamaster.module.taxonomy.controller.admin.tagAssetRel.vo.TaxonomyTagAssetRelPageReqVO;
import com.datamaster.module.taxonomy.controller.admin.tagAssetRel.vo.TaxonomyTagAssetRelRespVO;
import com.datamaster.module.taxonomy.controller.admin.tagAssetRel.vo.TaxonomyTagAssetRelSaveReqVO;
import com.datamaster.module.taxonomy.convert.Rel.TaxonomyTagAssetRelConvert;
import com.datamaster.module.taxonomy.dal.dataobject.Rel.TaxonomyTagAssetRelDO;
import com.datamaster.module.taxonomy.service.Rel.ITaxonomyTagAssetRelService;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.util.Arrays;
import java.util.List;

/**
 * 标签与资产关联关系Controller
 *
 * @author DATAMASTER
 * @date 2025-07-11
 */
@Tag(name = "标签与资产关联关系")
@RestController
@RequestMapping("/att/tagAssetRel")
@Validated
public class TaxonomyTagAssetRelController extends BaseController {
    @Resource
    private ITaxonomyTagAssetRelService TaxonomyTagAssetRelService;

    @Operation(summary = "查询标签与资产关联关系列表")
//    @PreAuthorize("@ss.hasPermi('att:tagAssetRel:list')")
    @GetMapping("/list")
    public CommonResult<PageResult<TaxonomyTagAssetRelRespVO>> list(TaxonomyTagAssetRelPageReqVO TaxonomyTagAssetRel) {
        PageResult<TaxonomyTagAssetRelDO> page = TaxonomyTagAssetRelService.getAttTagAssetRelPage(TaxonomyTagAssetRel);
        return CommonResult.success(BeanUtils.toBean(page, TaxonomyTagAssetRelRespVO.class));
    }



    @Operation(summary = "导出标签与资产关联关系列表")
//    @PreAuthorize("@ss.hasPermi('att:tagAssetRel:export')")
    @Log(title = "标签与资产关联关系", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, TaxonomyTagAssetRelPageReqVO exportReqVO) {
        exportReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<TaxonomyTagAssetRelDO> list = (List<TaxonomyTagAssetRelDO>) TaxonomyTagAssetRelService.getAttTagAssetRelPage(exportReqVO).getRows();
        ExcelUtil<TaxonomyTagAssetRelRespVO> util = new ExcelUtil<>(TaxonomyTagAssetRelRespVO.class);
        util.exportExcel(response, TaxonomyTagAssetRelConvert.INSTANCE.convertToRespVOList(list), "应用管理数据");
    }

    @Operation(summary = "导入标签与资产关联关系列表")
//    @PreAuthorize("@ss.hasPermi('att:tagAssetRel:import')")
    @Log(title = "标签与资产关联关系", businessType = BusinessType.IMPORT)
    @PostMapping("/importData")
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception {
        ExcelUtil<TaxonomyTagAssetRelRespVO> util = new ExcelUtil<>(TaxonomyTagAssetRelRespVO.class);
        List<TaxonomyTagAssetRelRespVO> importExcelList = util.importExcel(file.getInputStream());
        String operName = getUsername();
        String message = TaxonomyTagAssetRelService.importAttTagAssetRel(importExcelList, updateSupport, operName);
        return success(message);
    }

    @Operation(summary = "获取标签与资产关联关系详细信息")
//    @PreAuthorize("@ss.hasPermi('att:tagAssetRel:query')")
    @GetMapping(value = "/{ID}")
    public CommonResult<TaxonomyTagAssetRelRespVO> getInfo(@PathVariable("ID") Long ID) {
        TaxonomyTagAssetRelDO TaxonomyTagAssetRelDO = TaxonomyTagAssetRelService.getAttTagAssetRelById(ID);
        return CommonResult.success(BeanUtils.toBean(TaxonomyTagAssetRelDO, TaxonomyTagAssetRelRespVO.class));
    }

    @Operation(summary = "新增标签与资产关联关系")
//    @PreAuthorize("@ss.hasPermi('att:tagAssetRel:add')")
    @Log(title = "标签与资产关联关系", businessType = BusinessType.INSERT)
    @PostMapping
    public CommonResult<Long> add(@Valid @RequestBody TaxonomyTagAssetRelSaveReqVO TaxonomyTagAssetRel) {
        TaxonomyTagAssetRel.setCreatorId(getUserId());
        TaxonomyTagAssetRel.setCreateBy(getNickName());
        TaxonomyTagAssetRel.setCreateTime(DateUtil.date());
        return CommonResult.toAjax(TaxonomyTagAssetRelService.createAttTagAssetRel(TaxonomyTagAssetRel));
    }

    @Operation(summary = "修改标签与资产关联关系")
//    @PreAuthorize("@ss.hasPermi('att:tagAssetRel:edit')")
    @Log(title = "标签与资产关联关系", businessType = BusinessType.UPDATE)
    @PutMapping
    public CommonResult<Integer> edit(@Valid @RequestBody TaxonomyTagAssetRelSaveReqVO TaxonomyTagAssetRel) {
        TaxonomyTagAssetRel.setUpdatorId(getUserId());
        TaxonomyTagAssetRel.setUpdateBy(getNickName());
        TaxonomyTagAssetRel.setUpdateTime(DateUtil.date());
        return CommonResult.toAjax(TaxonomyTagAssetRelService.updateAttTagAssetRel(TaxonomyTagAssetRel));
    }

    @Operation(summary = "删除标签与资产关联关系")
//    @PreAuthorize("@ss.hasPermi('att:tagAssetRel:remove')")
    @Log(title = "标签与资产关联关系", businessType = BusinessType.DELETE)
    @DeleteMapping("/{IDs}")
    public CommonResult<Integer> remove(@PathVariable Long[] ids) {
        return CommonResult.toAjax(TaxonomyTagAssetRelService.removeAttTagAssetRel(Arrays.asList(ids)));
    }

    @Operation(summary = "删除标签与资产关联关系")
//    @PreAuthorize("@ss.hasPermi('att:tagAssetRel:remove')")
    @DeleteMapping("/delByTagIdAndAesstId")
    public CommonResult<Integer> delByTagIdAndAesstId(TaxonomyTagAssetRelPageReqVO TaxonomyTagAssetRel) {
        PageResult<TaxonomyTagAssetRelDO> page = TaxonomyTagAssetRelService.getAttTagAssetRelPage(TaxonomyTagAssetRel);
        List<TaxonomyTagAssetRelDO> rows = (List<TaxonomyTagAssetRelDO>) page.getRows();
        return CommonResult.toAjax(TaxonomyTagAssetRelService.removeAttTagAssetRel(rows.get(0).getId() , TaxonomyTagAssetRel));
    }

}
