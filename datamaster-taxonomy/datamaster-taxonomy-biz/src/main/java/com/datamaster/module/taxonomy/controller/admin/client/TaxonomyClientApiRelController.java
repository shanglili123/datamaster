

package com.datamaster.module.taxonomy.controller.admin.client;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.Arrays;

import cn.hutool.core.date.DateUtil;


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
import com.datamaster.module.taxonomy.controller.admin.client.vo.TaxonomyClientApiRelPageReqVO;
import com.datamaster.module.taxonomy.controller.admin.client.vo.TaxonomyClientApiRelRespVO;
import com.datamaster.module.taxonomy.controller.admin.client.vo.TaxonomyClientApiRelSaveReqVO;
import com.datamaster.module.taxonomy.dal.dataobject.client.TaxonomyClientApiRelDO;
import com.datamaster.module.taxonomy.service.client.ITaxonomyClientApiRelService;

/**
 * 应用API服务关联Controller
 *
 * @author FXB
 * @date 2025-08-21
 */
@Tag(name = "应用API服务关联")
@RestController
@RequestMapping("/att/clientApiRel")
@Validated
public class TaxonomyClientApiRelController extends BaseController {
    @Resource
    private ITaxonomyClientApiRelService TaxonomyClientApiRelService;

    @Operation(summary = "查询应用API服务关联列表")
    @GetMapping("/list")
    public CommonResult<PageResult<TaxonomyClientApiRelRespVO>> list(TaxonomyClientApiRelPageReqVO TaxonomyClientApiRel) {
        PageResult<TaxonomyClientApiRelDO> page = TaxonomyClientApiRelService.getAttClientApiRelPage(TaxonomyClientApiRel);
        return CommonResult.success(BeanUtils.toBean(page, TaxonomyClientApiRelRespVO.class));
    }

    @Operation(summary = "新增应用API服务关联")
//    @PreAuthorize("@ss.hasPermi('att:clientApiRel:add')")
    @Log(title = "应用API服务关联", businessType = BusinessType.INSERT)
    @PostMapping
    public CommonResult<Long> add(@Valid @RequestBody TaxonomyClientApiRelSaveReqVO TaxonomyClientApiRel) {
        TaxonomyClientApiRel.setCreatorId(getUserId());
        TaxonomyClientApiRel.setCreateBy(getNickName());
        TaxonomyClientApiRel.setCreateTime(DateUtil.date());
        return CommonResult.toAjax(TaxonomyClientApiRelService.createAttClientApiRel(TaxonomyClientApiRel));
    }

    @Operation(summary = "修改应用API服务关联")
//    @PreAuthorize("@ss.hasPermi('att:clientApiRel:edit')")
    @Log(title = "应用API服务关联", businessType = BusinessType.UPDATE)
    @PutMapping
    public CommonResult<Integer> edit(@Valid @RequestBody TaxonomyClientApiRelSaveReqVO TaxonomyClientApiRel) {
        TaxonomyClientApiRel.setUpdatorId(getUserId());
        TaxonomyClientApiRel.setUpdateBy(getNickName());
        TaxonomyClientApiRel.setUpdateTime(DateUtil.date());
        return CommonResult.toAjax(TaxonomyClientApiRelService.updateAttClientApiRel(TaxonomyClientApiRel));
    }

    @Operation(summary = "删除应用API服务关联")
//    @PreAuthorize("@ss.hasPermi('att:clientApiRel:remove')")
    @Log(title = "应用API服务关联", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public CommonResult<Integer> remove(@PathVariable Long[] ids) {
        return CommonResult.toAjax(TaxonomyClientApiRelService.removeAttClientApiRel(Arrays.asList(ids)));
    }

}
