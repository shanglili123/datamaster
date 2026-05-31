

package com.datamaster.module.standards.controller.admin.dataCategory;

import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import com.datamaster.common.annotation.Log;
import com.datamaster.common.core.controller.BaseController;
import com.datamaster.common.core.domain.CommonResult;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.common.enums.BusinessType;
import com.datamaster.common.utils.StringUtils;
import com.datamaster.common.utils.object.BeanUtils;
import com.datamaster.module.standards.controller.admin.dataCategory.vo.*;
import com.datamaster.module.standards.dal.dataobject.dataCategory.StandardsDataCategoryDO;
import com.datamaster.module.standards.service.dataCategory.IStandardsDataCategoryService;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.Arrays;
import java.util.List;

/**
 * 数据分类Controller
 *
 * @author DATAMASTER
 * @date 2026-04-07
 */
@Tag(name = "数据分类")
@RestController
@RequestMapping("/dg/dataCategory")
@Validated
public class StandardsDataCategoryController extends BaseController {
    @Resource
    private IStandardsDataCategoryService StandardsDataCategoryService;

    @Operation(summary = "查询数据分类树列表")
    @GetMapping("/selectTree")
    public CommonResult<List<StandardsDataCategoryTreeRespVO>> selectTree(@RequestParam(required = false) String type) {
        if (StringUtils.isBlank(type)) {
            type = "1";
        }
        return CommonResult.success(StandardsDataCategoryService.selectTree(type));
    }


    @Operation(summary = "查询数据分类列表")
    //@PreAuthorize("@ss.hasPermi('dg:dataCategory:list')")
    @GetMapping("/list")
    public CommonResult<PageResult<StandardsDataCategoryRespVO>> list(StandardsDataCategoryPageReqVO StandardsDataCategory) {
        PageResult<StandardsDataCategoryDO> page = StandardsDataCategoryService.getDgDataCategoryPage(StandardsDataCategory);
        return CommonResult.success(BeanUtils.toBean(page, StandardsDataCategoryRespVO.class));
    }

    @Operation(summary = "查询数据分类列表")
    //@PreAuthorize("@ss.hasPermi('dg:dataCategory:list')")
    @GetMapping("/listAll")
    public CommonResult<List<StandardsDataCategoryRespVO>> listAll(StandardsDataCategoryPageReqVO StandardsDataCategory) {
        List<StandardsDataCategoryDO> page = StandardsDataCategoryService.getDgDataCategoryList(StandardsDataCategory);
        return CommonResult.success(BeanUtils.toBean(page, StandardsDataCategoryRespVO.class));
    }


    @Operation(summary = "获取数据分类详细信息")
    //@PreAuthorize("@ss.hasPermi('dg:dataCategory:query')")
    @GetMapping(value = "/{id}")
    public CommonResult<StandardsDataCategoryRespVO> getInfo(@PathVariable("id") Long id) {
        StandardsDataCategoryDO StandardsDataCategoryDO = StandardsDataCategoryService.getDgDataCategoryById(id);
        return CommonResult.success(BeanUtils.toBean(StandardsDataCategoryDO, StandardsDataCategoryRespVO.class));
    }

    @Operation(summary = "新增数据分类")
    //@PreAuthorize("@ss.hasPermi('dg:dataCategory:add')")
    @Log(title = "数据分类", businessType = BusinessType.INSERT)
    @PostMapping
    public CommonResult<Long> add(@Valid @RequestBody StandardsDataCategorySaveReqVO StandardsDataCategory) {
        StandardsDataCategory.setCreatorId(getUserId());
        StandardsDataCategory.setCreateBy(getNickName());
        StandardsDataCategory.setCreateTime(DateUtil.date());
        return CommonResult.toAjax(StandardsDataCategoryService.createDgDataCategory(StandardsDataCategory));
    }

    @Operation(summary = "修改数据分类")
    //@PreAuthorize("@ss.hasPermi('dg:dataCategory:edit')")
    @Log(title = "数据分类", businessType = BusinessType.UPDATE)
    @PutMapping
    public CommonResult<Integer> edit(@Valid @RequestBody StandardsDataCategorySaveReqVO StandardsDataCategory) {
        StandardsDataCategory.setUpdatorId(getUserId());
        StandardsDataCategory.setUpdateBy(getNickName());
        StandardsDataCategory.setUpdateTime(DateUtil.date());
        return CommonResult.toAjax(StandardsDataCategoryService.updateDgDataCategory(StandardsDataCategory));
    }

    @Operation(summary = "批量設置数据分級")
    //@PreAuthorize("@ss.hasPermi('dg:dataCategory:edit')")
    @Log(title = "批量設置数据分級", businessType = BusinessType.UPDATE)
    @PutMapping("/batchDataLevel")
    public CommonResult<Boolean> batchDataLevel(@RequestBody StandardsDataCategoryBatchDataLevelReqVO reqVO) {
        return CommonResult.toAjax(StandardsDataCategoryService.update(Wrappers.lambdaUpdate(StandardsDataCategoryDO.class).set(StandardsDataCategoryDO::getDataLevelId, reqVO.getDataLevelId()).in(StandardsDataCategoryDO::getId, reqVO.getIds())));
    }

    @Operation(summary = "删除数据分类")
    //@PreAuthorize("@ss.hasPermi('dg:dataCategory:remove')")
    @Log(title = "数据分类", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public CommonResult<Integer> remove(@PathVariable Long[] ids) {
        return CommonResult.toAjax(StandardsDataCategoryService.removeDgDataCategory(Arrays.asList(ids)));
    }

}
