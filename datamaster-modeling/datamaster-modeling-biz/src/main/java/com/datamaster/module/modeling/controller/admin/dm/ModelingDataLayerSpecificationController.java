

package com.datamaster.module.modeling.controller.admin.dm;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.Arrays;

import cn.hutool.core.date.DateUtil;

import java.util.List;

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
import com.datamaster.module.modeling.controller.admin.dm.vo.ModelingDataLayerSpecificationPageReqVO;
import com.datamaster.module.modeling.controller.admin.dm.vo.ModelingDataLayerSpecificationRespVO;
import com.datamaster.module.modeling.controller.admin.dm.vo.ModelingDataLayerSpecificationSaveReqVO;
import com.datamaster.module.modeling.dal.dataobject.dm.ModelingDataLayerSpecificationDO;
import com.datamaster.module.modeling.service.dm.IModelingDataLayerSpecificationService;

/**
 * 数仓分层-规范管理Controller
 *
 * @author FXB
 * @date 2026-03-24
 */
@Tag(name = "数仓分层-规范管理")
@RestController
@RequestMapping("/mdl/dataLayerSpecification")
@Validated
public class ModelingDataLayerSpecificationController extends BaseController {
    @Resource
    private IModelingDataLayerSpecificationService ModelingDataLayerSpecificationService;

    @Operation(summary = "查询数仓分层-规范管理列表")
    @PreAuthorize("@ss.hasPermi('dm:dataLayer:list')")
    @GetMapping("/list")
    public CommonResult<PageResult<ModelingDataLayerSpecificationRespVO>> list(ModelingDataLayerSpecificationPageReqVO ModelingDataLayerSpecification) {
        PageResult<ModelingDataLayerSpecificationDO> page = ModelingDataLayerSpecificationService.getModelingDataLayerSpecificationPage(ModelingDataLayerSpecification);
        return CommonResult.success(BeanUtils.toBean(page, ModelingDataLayerSpecificationRespVO.class));
    }

    @Operation(summary = "获取数仓分层-规范管理详细信息")
    @PreAuthorize("@ss.hasPermi('dm:dataLayer:query')")
    @GetMapping(value = "/{id}")
    public CommonResult<ModelingDataLayerSpecificationRespVO> getInfo(@PathVariable("id") Long id) {
        ModelingDataLayerSpecificationDO ModelingDataLayerSpecificationDO = ModelingDataLayerSpecificationService.getModelingDataLayerSpecificationById(id);
        return CommonResult.success(BeanUtils.toBean(ModelingDataLayerSpecificationDO, ModelingDataLayerSpecificationRespVO.class));
    }

    @Operation(summary = "新增数仓分层-规范管理")
    @PreAuthorize("@ss.hasPermi('dm:dataLayer:add')")
    @Log(title = "数仓分层-规范管理", businessType = BusinessType.INSERT)
    @PostMapping
    public CommonResult<Long> add(@Valid @RequestBody ModelingDataLayerSpecificationSaveReqVO ModelingDataLayerSpecification) {
        ModelingDataLayerSpecification.setCreatorId(getUserId());
        ModelingDataLayerSpecification.setCreateBy(getNickName());
        ModelingDataLayerSpecification.setCreateTime(DateUtil.date());
        return CommonResult.toAjax(ModelingDataLayerSpecificationService.createModelingDataLayerSpecification(ModelingDataLayerSpecification));
    }

    @Operation(summary = "修改数仓分层-规范管理")
    @PreAuthorize("@ss.hasPermi('dm:dataLayer:edit')")
    @Log(title = "数仓分层-规范管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public CommonResult<Integer> edit(@Valid @RequestBody ModelingDataLayerSpecificationSaveReqVO ModelingDataLayerSpecification) {
        ModelingDataLayerSpecification.setUpdatorId(getUserId());
        ModelingDataLayerSpecification.setUpdateBy(getNickName());
        ModelingDataLayerSpecification.setUpdateTime(DateUtil.date());
        return CommonResult.toAjax(ModelingDataLayerSpecificationService.updateModelingDataLayerSpecification(ModelingDataLayerSpecification));
    }

    @Operation(summary = "删除数仓分层-规范管理")
    @PreAuthorize("@ss.hasPermi('dm:dataLayer:remove')")
    @Log(title = "数仓分层-规范管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public CommonResult<Integer> remove(@PathVariable Long[] ids) {
        return CommonResult.toAjax(ModelingDataLayerSpecificationService.removeModelingDataLayerSpecification(Arrays.asList(ids)));
    }

}
