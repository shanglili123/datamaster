

package com.datamaster.module.standards.controller.admin.dataCategoryCat;

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
import com.datamaster.common.core.page.PageParam;
import com.datamaster.common.annotation.Log;
import com.datamaster.common.core.controller.BaseController;
import com.datamaster.common.core.domain.CommonResult;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.common.enums.BusinessType;
import com.datamaster.common.utils.object.BeanUtils;
import com.datamaster.common.utils.poi.ExcelUtil;
import com.datamaster.common.exception.enums.GlobalErrorCodeConstants;
import com.datamaster.module.standards.controller.admin.dataCategoryCat.vo.StandardsDataCategoryCatPageReqVO;
import com.datamaster.module.standards.controller.admin.dataCategoryCat.vo.StandardsDataCategoryCatRespVO;
import com.datamaster.module.standards.controller.admin.dataCategoryCat.vo.StandardsDataCategoryCatSaveReqVO;
import com.datamaster.module.standards.convert.dataCategoryCat.StandardsDataCategoryCatConvert;
import com.datamaster.module.standards.dal.dataobject.dataCategoryCat.StandardsDataCategoryCatDO;
import com.datamaster.module.standards.service.dataCategoryCat.IStandardsDataCategoryCatService;

/**
 * 数据分类-类目Controller
 *
 * @author FXB
 * @date 2026-04-07
 */
@Tag(name = "数据分类-类目")
@RestController
@RequestMapping("/cat/dataCategoryCat")
@Validated
public class StandardsDataCategoryCatController extends BaseController {
    @Resource
    private IStandardsDataCategoryCatService StandardsDataCategoryCatService;

    @Operation(summary = "查询数据分类-类目列表")
    @GetMapping("/list")
    public CommonResult<List<StandardsDataCategoryCatRespVO>> list() {
        List<StandardsDataCategoryCatDO> StandardsDataCategoryCatDOList = StandardsDataCategoryCatService.getDgDataCategoryCatList();
        return CommonResult.success(BeanUtils.toBean(StandardsDataCategoryCatDOList, StandardsDataCategoryCatRespVO.class));
    }

    @Operation(summary = "获取数据分类-类目详细信息")
    @GetMapping(value = "/{id}")
    public CommonResult<StandardsDataCategoryCatRespVO> getInfo(@PathVariable("id") Long id) {
        StandardsDataCategoryCatDO StandardsDataCategoryCatDO = StandardsDataCategoryCatService.getDgDataCategoryCatById(id);
        return CommonResult.success(BeanUtils.toBean(StandardsDataCategoryCatDO, StandardsDataCategoryCatRespVO.class));
    }

    @Operation(summary = "新增数据分类-类目")
//    @PreAuthorize("@ss.hasPermi('dg:dataCategoryCat:add')")
    @Log(title = "数据分类-类目", businessType = BusinessType.INSERT)
    @PostMapping
    public CommonResult<Long> add(@Valid @RequestBody StandardsDataCategoryCatSaveReqVO StandardsDataCategoryCat) {
        StandardsDataCategoryCat.setCreatorId(getUserId());
        StandardsDataCategoryCat.setCreateBy(getNickName());
        StandardsDataCategoryCat.setCreateTime(DateUtil.date());
        return CommonResult.toAjax(StandardsDataCategoryCatService.createDgDataCategoryCat(StandardsDataCategoryCat));
    }

    @Operation(summary = "修改数据分类-类目")
//    @PreAuthorize("@ss.hasPermi('dg:dataCategoryCat:edit')")
    @Log(title = "数据分类-类目", businessType = BusinessType.UPDATE)
    @PutMapping
    public CommonResult<Integer> edit(@Valid @RequestBody StandardsDataCategoryCatSaveReqVO StandardsDataCategoryCat) {
        StandardsDataCategoryCat.setUpdatorId(getUserId());
        StandardsDataCategoryCat.setUpdateBy(getNickName());
        StandardsDataCategoryCat.setUpdateTime(DateUtil.date());
        return CommonResult.toAjax(StandardsDataCategoryCatService.updateDgDataCategoryCat(StandardsDataCategoryCat));
    }

    @Operation(summary = "删除数据分类-类目")
//    @PreAuthorize("@ss.hasPermi('dg:dataCategoryCat:remove')")
    @Log(title = "数据分类-类目", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public CommonResult<Integer> remove(@PathVariable Long[] ids) {
        return CommonResult.toAjax(StandardsDataCategoryCatService.removeDgDataCategoryCat(Arrays.asList(ids)));
    }

}
