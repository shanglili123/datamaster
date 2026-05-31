package com.datamaster.module.standards.controller.admin.standard;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import com.datamaster.common.annotation.Log;
import com.datamaster.common.core.controller.BaseController;
import com.datamaster.common.core.domain.BatchDeleteCheck;
import com.datamaster.common.core.domain.CommonResult;
import com.datamaster.common.enums.BusinessType;
import com.datamaster.common.utils.object.BeanUtils;
import com.datamaster.module.standards.controller.admin.standard.vo.StandardsDataElemCatPageReqVO;
import com.datamaster.module.standards.controller.admin.standard.vo.StandardsDataElemCatRespVO;
import com.datamaster.module.standards.controller.admin.standard.vo.StandardsDataElemCatSaveReqVO;
import com.datamaster.module.standards.dal.dataobject.standard.StandardsDataElemCatDO;
import com.datamaster.module.standards.service.standard.IStandardsDataElemCatService;


import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.Arrays;
import java.util.List;

/**
 * 数据元类目管理Controller
 *
 * @author DATAMASTER
 * @date 2025-01-20
 */
@Tag(name = "数据元类目管理")
@RestController
@RequestMapping("/dg/dataElemCat")
@Validated
public class StandardsDataElemCatController extends BaseController {
    @Resource
    private IStandardsDataElemCatService service;

    @Operation(summary = "查询数据元类目管理列表")
    @PreAuthorize("@ss.hasPermi('dg:dataElemCat:list')")
    @GetMapping("/list")
    public CommonResult<List<StandardsDataElemCatRespVO>> list(StandardsDataElemCatPageReqVO StandardsDataElemCat) {
        List<StandardsDataElemCatDO> StandardsDataElemCatList = service.getDgDataElemCatList(StandardsDataElemCat);
        return CommonResult.success(BeanUtils.toBean(StandardsDataElemCatList, StandardsDataElemCatRespVO.class));
    }

    @Operation(summary = "获取数据元类目管理详细信息")
    @PreAuthorize("@ss.hasPermi('dg:dataElemCat:query')")
    @GetMapping(value = "/{id}")
    public CommonResult<StandardsDataElemCatRespVO> getInfo(@PathVariable("id") Long id) {
        StandardsDataElemCatDO StandardsDataElemCatDO = service.getDgDataElemCatById(id);
        return CommonResult.success(BeanUtils.toBean(StandardsDataElemCatDO, StandardsDataElemCatRespVO.class));
    }

    @Operation(summary = "新增数据元类目管理")
    @PreAuthorize("@ss.hasPermi('dg:dataElemCat:add')")
    @Log(title = "数据元类目管理", businessType = BusinessType.INSERT)
    @PostMapping
    public CommonResult<Long> add(@Valid @RequestBody StandardsDataElemCatSaveReqVO StandardsDataElemCat) {
        return CommonResult.toAjax(service.createDgDataElemCat(StandardsDataElemCat));
    }

    @Operation(summary = "修改数据元类目管理")
    @PreAuthorize("@ss.hasPermi('dg:dataElemCat:edit')")
    @Log(title = "数据元类目管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public CommonResult<Integer> edit(@Valid @RequestBody StandardsDataElemCatSaveReqVO StandardsDataElemCat) {
        return CommonResult.toAjax(service.updateDgDataElemCat(StandardsDataElemCat));
    }

    @Operation(summary = "删除数据元类目管理")
    @PreAuthorize("@ss.hasPermi('dg:dataElemCat:remove')")
    @Log(title = "数据元类目管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public CommonResult<Integer> remove(@PathVariable Long[] ids) {
        return CommonResult.toAjax(service.removeDgDataElemCat(Arrays.asList(ids)));
    }

    /**
     * 批量删除检查,查询可删除数和不可删除数
     */
    @Operation(summary = "批量删除检查数据元类目管理")
    @PreAuthorize("@ss.hasPermi('dg:dataElemCat:remove')")
    @GetMapping("/batchDeleteCheck/{ids}")
    public CommonResult<BatchDeleteCheck<Long>> batchDeleteCheck(@PathVariable Long[] ids) {
        BatchDeleteCheck<Long> result = service.batchDeleteCheck(Arrays.asList(ids));
        return CommonResult.success(result);
    }

}
