package com.datamaster.module.standards.controller.admin.standard;

import cn.hutool.core.date.DateUtil;
import com.datamaster.module.standards.dal.dataobject.standard.StandardsDataMetaDO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import com.datamaster.common.annotation.Log;
import com.datamaster.common.core.controller.BaseController;
import com.datamaster.common.core.domain.CommonResult;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.common.enums.BusinessType;
import com.datamaster.common.utils.object.BeanUtils;
import com.datamaster.module.standards.controller.admin.standard.vo.StandardsDataElemPageReqVO;
import com.datamaster.module.standards.controller.admin.standard.vo.StandardsDataElemRespVO;
import com.datamaster.module.standards.controller.admin.standard.vo.StandardsDataElemSaveReqVO;
import com.datamaster.module.standards.service.standard.IStandardsDataMetaService;


import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.Arrays;
import java.util.List;

/**
 * 数据元Controller
 *
 * @author DATAMASTER
 * @date 2025-01-21
 */
@Tag(name = "数据元")
@RestController
@RequestMapping("/cat/dataElem")
@Validated
public class StandardsDataMetaController extends BaseController {
    @Resource
    private IStandardsDataMetaService service;

    @Operation(summary = "查询数据元列表")
    @PreAuthorize("@ss.hasPermi('dg:dataElem:list')")
    @GetMapping("/list")
    public CommonResult<PageResult<StandardsDataElemRespVO>> list(StandardsDataElemPageReqVO StandardsDataElem) {
        PageResult<StandardsDataMetaDO> page = service.getDgDataElemPage(StandardsDataElem);
        return CommonResult.success(BeanUtils.toBean(page, StandardsDataElemRespVO.class));
    }

    @Operation(summary = "查询数据元列表")
    @PreAuthorize("@ss.hasPermi('dg:dataElem:list')")
    @GetMapping("/getDgDataElemList")
    public CommonResult<List<StandardsDataElemRespVO>> getDgDataElemList(StandardsDataElemPageReqVO StandardsDataElem) {
        List<StandardsDataMetaDO> list = service.getDgDataElemList(StandardsDataElem);
        return CommonResult.success(BeanUtils.toBean(list, StandardsDataElemRespVO.class));
    }

    @Operation(summary = "获取数据元详细信息")
    @PreAuthorize("@ss.hasPermi('dg:dataElem:query')")
    @GetMapping(value = "/{id}")
    public CommonResult<StandardsDataElemRespVO> getInfo(@PathVariable("id") Long id) {
        StandardsDataMetaDO StandardsDataElemDO = service.getDgDataElemById(id);
        return CommonResult.success(BeanUtils.toBean(StandardsDataElemDO, StandardsDataElemRespVO.class));
    }

    @Operation(summary = "新增数据元")
    @PreAuthorize("@ss.hasPermi('dg:dataElem:add')")
    @Log(title = "数据元", businessType = BusinessType.INSERT)
    @PostMapping
    public CommonResult<Long> add(@Valid @RequestBody StandardsDataElemSaveReqVO StandardsDataElem) {
        StandardsDataElem.setCreatorId(getUserId());
        StandardsDataElem.setCreateBy(getNickName());
        StandardsDataElem.setCreateTime(DateUtil.date());
        return CommonResult.toAjax(service.createDgDataElem(StandardsDataElem));
    }

    @Operation(summary = "修改数据元")
    @PreAuthorize("@ss.hasPermi('dg:dataElem:edit')")
    @Log(title = "数据元", businessType = BusinessType.UPDATE)
    @PutMapping
    public CommonResult<Integer> edit(@Valid @RequestBody StandardsDataElemSaveReqVO StandardsDataElem) {
        StandardsDataElem.setUpdatorId(getUserId());
        StandardsDataElem.setUpdateBy(getNickName());
        StandardsDataElem.setUpdateTime(DateUtil.date());
        return CommonResult.toAjax(service.updateDgDataElem(StandardsDataElem));
    }

    @Operation(summary = "删除数据元")
    @PreAuthorize("@ss.hasPermi('dg:dataElem:remove')")
    @Log(title = "数据元", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public CommonResult<Integer> remove(@PathVariable Long[] ids) {
        return CommonResult.toAjax(service.removeDgDataElem(Arrays.asList(ids)));
    }

    @Operation(summary = "更改数据元状态")
    @PreAuthorize("@ss.hasPermi('dg:dataElem:edit')")
    @Log(title = "更改数据元状态", businessType = BusinessType.UPDATE)
    @PostMapping("/updateStatus/{id}/{status}")
    public CommonResult<Boolean> updateStatus(@PathVariable Long id, @PathVariable Long status) {
        return CommonResult.toAjax(service.updateStatus(id, status));
    }


}
