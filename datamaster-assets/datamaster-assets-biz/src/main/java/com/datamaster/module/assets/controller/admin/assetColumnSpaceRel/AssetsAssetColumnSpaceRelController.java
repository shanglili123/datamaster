package com.datamaster.module.assets.controller.admin.assetColumnSpaceRel;

import cn.hutool.core.date.DateUtil;
import com.datamaster.common.annotation.Log;
import com.datamaster.common.core.controller.BaseController;
import com.datamaster.common.core.domain.CommonResult;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.common.enums.BusinessType;
import com.datamaster.common.utils.object.BeanUtils;
import com.datamaster.module.assets.controller.admin.assetColumnSpaceRel.vo.AssetsAssetColumnSpaceRelPageReqVO;
import com.datamaster.module.assets.controller.admin.assetColumnSpaceRel.vo.AssetsAssetColumnSpaceRelRespVO;
import com.datamaster.module.assets.controller.admin.assetColumnSpaceRel.vo.AssetsAssetColumnSpaceRelSaveReqVO;
import com.datamaster.module.assets.dal.dataobject.assetColumnSpaceRel.AssetsAssetColumnSpaceRelDO;
import com.datamaster.module.assets.service.assetColumnSpaceRel.IAssetsAssetColumnSpaceRelService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.Arrays;

/**
 * 数据资产字段与空间关联关系 Controller
 *
 * @author DATAMASTER
 */
@Tag(name = "数据资产字段与空间关联关系")
@RestController
@RequestMapping("/ast/assetColumnSpaceRel")
@Validated
public class AssetsAssetColumnSpaceRelController extends BaseController {
    @Resource
    private IAssetsAssetColumnSpaceRelService assetsAssetColumnSpaceRelService;

    @Operation(summary = "查询数据资产字段与空间关联关系列表")
    @PreAuthorize("@ss.hasPermi('ast:assetColumnSpaceRel:list')")
    @GetMapping("/list")
    public CommonResult<PageResult<AssetsAssetColumnSpaceRelRespVO>> list(AssetsAssetColumnSpaceRelPageReqVO reqVO) {
        PageResult<AssetsAssetColumnSpaceRelDO> page = assetsAssetColumnSpaceRelService.getAssetColumnSpaceRelPage(reqVO);
        return CommonResult.success(BeanUtils.toBean(page, AssetsAssetColumnSpaceRelRespVO.class));
    }

    @Operation(summary = "获取数据资产字段与空间关联关系详细信息")
    @PreAuthorize("@ss.hasPermi('ast:assetColumnSpaceRel:query')")
    @GetMapping(value = "/{id}")
    public CommonResult<AssetsAssetColumnSpaceRelRespVO> getInfo(@PathVariable("id") Long id) {
        AssetsAssetColumnSpaceRelDO rel = assetsAssetColumnSpaceRelService.getAssetColumnSpaceRelById(id);
        return CommonResult.success(BeanUtils.toBean(rel, AssetsAssetColumnSpaceRelRespVO.class));
    }

    @Operation(summary = "新增数据资产字段与空间关联关系")
    @PreAuthorize("@ss.hasPermi('ast:assetColumnSpaceRel:add')")
    @Log(title = "数据资产字段与空间关联关系", businessType = BusinessType.INSERT)
    @PostMapping
    public CommonResult<Long> add(@Valid @RequestBody AssetsAssetColumnSpaceRelSaveReqVO reqVO) {
        reqVO.setCreatorId(getUserId());
        reqVO.setCreateBy(getNickName());
        reqVO.setCreateTime(DateUtil.date());
        return CommonResult.toAjax(assetsAssetColumnSpaceRelService.createAssetColumnSpaceRel(reqVO));
    }

    @Operation(summary = "修改数据资产字段与空间关联关系")
    @PreAuthorize("@ss.hasPermi('ast:assetColumnSpaceRel:edit')")
    @Log(title = "数据资产字段与空间关联关系", businessType = BusinessType.UPDATE)
    @PutMapping
    public CommonResult<Integer> edit(@Valid @RequestBody AssetsAssetColumnSpaceRelSaveReqVO reqVO) {
        reqVO.setUpdatorId(getUserId());
        reqVO.setUpdateBy(getNickName());
        reqVO.setUpdateTime(DateUtil.date());
        return CommonResult.toAjax(assetsAssetColumnSpaceRelService.updateAssetColumnSpaceRel(reqVO));
    }

    @Operation(summary = "删除数据资产字段与空间关联关系")
    @PreAuthorize("@ss.hasPermi('ast:assetColumnSpaceRel:remove')")
    @Log(title = "数据资产字段与空间关联关系", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public CommonResult<Integer> remove(@PathVariable Long[] ids) {
        return CommonResult.toAjax(assetsAssetColumnSpaceRelService.removeAssetColumnSpaceRel(Arrays.asList(ids)));
    }
}
