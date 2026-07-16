package com.datamaster.module.assets.controller.admin.assetColumnProjectRel;

import cn.hutool.core.date.DateUtil;
import com.datamaster.common.annotation.Log;
import com.datamaster.common.core.controller.BaseController;
import com.datamaster.common.core.domain.CommonResult;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.common.enums.BusinessType;
import com.datamaster.common.utils.object.BeanUtils;
import com.datamaster.module.assets.controller.admin.assetColumnProjectRel.vo.AssetsAssetColumnProjectRelPageReqVO;
import com.datamaster.module.assets.controller.admin.assetColumnProjectRel.vo.AssetsAssetColumnProjectRelRespVO;
import com.datamaster.module.assets.controller.admin.assetColumnProjectRel.vo.AssetsAssetColumnProjectRelSaveReqVO;
import com.datamaster.module.assets.dal.dataobject.assetColumnProjectRel.AssetsAssetColumnProjectRelDO;
import com.datamaster.module.assets.service.assetColumnProjectRel.IAssetsAssetColumnProjectRelService;
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
 * 数据资产字段与项目关联关系 Controller
 *
 * @author DATAMASTER
 */
@Tag(name = "数据资产字段与项目关联关系")
@RestController
@RequestMapping("/ast/assetColumnProjectRel")
@Validated
public class AssetsAssetColumnProjectRelController extends BaseController {
    @Resource
    private IAssetsAssetColumnProjectRelService assetsAssetColumnProjectRelService;

    @Operation(summary = "查询数据资产字段与项目关联关系列表")
    @PreAuthorize("@ss.hasPermi('da:assetColumnProjectRel:list')")
    @GetMapping("/list")
    public CommonResult<PageResult<AssetsAssetColumnProjectRelRespVO>> list(AssetsAssetColumnProjectRelPageReqVO reqVO) {
        PageResult<AssetsAssetColumnProjectRelDO> page = assetsAssetColumnProjectRelService.getAssetColumnProjectRelPage(reqVO);
        return CommonResult.success(BeanUtils.toBean(page, AssetsAssetColumnProjectRelRespVO.class));
    }

    @Operation(summary = "获取数据资产字段与项目关联关系详细信息")
    @PreAuthorize("@ss.hasPermi('da:assetColumnProjectRel:query')")
    @GetMapping(value = "/{id}")
    public CommonResult<AssetsAssetColumnProjectRelRespVO> getInfo(@PathVariable("id") Long id) {
        AssetsAssetColumnProjectRelDO rel = assetsAssetColumnProjectRelService.getAssetColumnProjectRelById(id);
        return CommonResult.success(BeanUtils.toBean(rel, AssetsAssetColumnProjectRelRespVO.class));
    }

    @Operation(summary = "新增数据资产字段与项目关联关系")
    @PreAuthorize("@ss.hasPermi('da:assetColumnProjectRel:add')")
    @Log(title = "数据资产字段与项目关联关系", businessType = BusinessType.INSERT)
    @PostMapping
    public CommonResult<Long> add(@Valid @RequestBody AssetsAssetColumnProjectRelSaveReqVO reqVO) {
        reqVO.setCreatorId(getUserId());
        reqVO.setCreateBy(getNickName());
        reqVO.setCreateTime(DateUtil.date());
        return CommonResult.toAjax(assetsAssetColumnProjectRelService.createAssetColumnProjectRel(reqVO));
    }

    @Operation(summary = "修改数据资产字段与项目关联关系")
    @PreAuthorize("@ss.hasPermi('da:assetColumnProjectRel:edit')")
    @Log(title = "数据资产字段与项目关联关系", businessType = BusinessType.UPDATE)
    @PutMapping
    public CommonResult<Integer> edit(@Valid @RequestBody AssetsAssetColumnProjectRelSaveReqVO reqVO) {
        reqVO.setUpdatorId(getUserId());
        reqVO.setUpdateBy(getNickName());
        reqVO.setUpdateTime(DateUtil.date());
        return CommonResult.toAjax(assetsAssetColumnProjectRelService.updateAssetColumnProjectRel(reqVO));
    }

    @Operation(summary = "删除数据资产字段与项目关联关系")
    @PreAuthorize("@ss.hasPermi('da:assetColumnProjectRel:remove')")
    @Log(title = "数据资产字段与项目关联关系", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public CommonResult<Integer> remove(@PathVariable Long[] ids) {
        return CommonResult.toAjax(assetsAssetColumnProjectRelService.removeAssetColumnProjectRel(Arrays.asList(ids)));
    }
}
