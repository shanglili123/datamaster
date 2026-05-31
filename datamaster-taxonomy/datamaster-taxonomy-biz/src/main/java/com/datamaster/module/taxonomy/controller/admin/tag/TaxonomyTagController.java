

package com.datamaster.module.taxonomy.controller.admin.tag;

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
import com.datamaster.common.core.page.PageResult;
import com.datamaster.common.enums.BusinessType;
import com.datamaster.common.utils.object.BeanUtils;
import com.datamaster.common.utils.poi.ExcelUtil;
import com.datamaster.module.taxonomy.controller.admin.tag.vo.TaxonomyTagPageReqVO;
import com.datamaster.module.taxonomy.controller.admin.tag.vo.TaxonomyTagRespVO;
import com.datamaster.module.taxonomy.controller.admin.tag.vo.TaxonomyTagSaveReqVO;
import com.datamaster.module.taxonomy.convert.Tag.TaxonomyTagConvert;
import com.datamaster.module.taxonomy.dal.dataobject.Tag.TaxonomyTagDO;
import com.datamaster.module.taxonomy.service.Tag.ITaxonomyTagService;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.util.Arrays;
import java.util.List;

/**
 * 标签管理Controller
 *
 * @author DATAMASTER
 * @date 2025-07-11
 */
@Tag(name = "标签管理")
@RestController
@RequestMapping("/att/tag")
@Validated
public class TaxonomyTagController extends BaseController {
    @Resource
    private ITaxonomyTagService TaxonomyTagService;

    @Operation(summary = "查询标签管理列表")
    @PreAuthorize("@ss.hasPermi('att:tag:list')")
    @GetMapping("/list")
    public CommonResult<PageResult<TaxonomyTagRespVO>> list(TaxonomyTagPageReqVO TaxonomyTag) {
        PageResult<TaxonomyTagDO> page = TaxonomyTagService.getAttTagPage(TaxonomyTag);
        return CommonResult.success(BeanUtils.toBean(page, TaxonomyTagRespVO.class));
    }

    @Operation(summary = "查询标签管理列表")
    @GetMapping("/listDict")
    public CommonResult<List<TaxonomyTagRespVO>> list() {
        List<TaxonomyTagDO> TaxonomyTagList = TaxonomyTagService.getAttTagList();
        return CommonResult.success(BeanUtils.toBean(TaxonomyTagList, TaxonomyTagRespVO.class));
    }

    @Operation(summary = "导出标签管理列表")
    @PreAuthorize("@ss.hasPermi('att:tag:export')")
    @Log(title = "标签管理", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, TaxonomyTagPageReqVO exportReqVO) {
        exportReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<TaxonomyTagDO> list = (List<TaxonomyTagDO>) TaxonomyTagService.getAttTagPage(exportReqVO).getRows();
        ExcelUtil<TaxonomyTagRespVO> util = new ExcelUtil<>(TaxonomyTagRespVO.class);
        util.exportExcel(response, TaxonomyTagConvert.INSTANCE.convertToRespVOList(list), "应用管理数据");
    }

    @Operation(summary = "导入标签管理列表")
    @PreAuthorize("@ss.hasPermi('att:tag:import')")
    @Log(title = "标签管理", businessType = BusinessType.IMPORT)
    @PostMapping("/importData")
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception {
        ExcelUtil<TaxonomyTagRespVO> util = new ExcelUtil<>(TaxonomyTagRespVO.class);
        List<TaxonomyTagRespVO> importExcelList = util.importExcel(file.getInputStream());
        String operName = getUsername();
        String message = TaxonomyTagService.importAttTag(importExcelList, updateSupport, operName);
        return success(message);
    }

    @Operation(summary = "获取标签管理详细信息")
    @PreAuthorize("@ss.hasPermi('att:tag:query')")
    @GetMapping(value = "/{id}")
    public CommonResult<TaxonomyTagRespVO> getInfo(@PathVariable("id") Long id) {
        TaxonomyTagRespVO TaxonomyTagDO = TaxonomyTagService.getAttTagById(id);
        return CommonResult.success(TaxonomyTagDO);
    }

    @Operation(summary = "新增标签管理")
    @PreAuthorize("@ss.hasPermi('att:tag:add')")
    @Log(title = "标签管理", businessType = BusinessType.INSERT)
    @PostMapping
    public CommonResult<Long> add(@Valid @RequestBody TaxonomyTagSaveReqVO TaxonomyTag) {
        TaxonomyTag.setCreatorId(getUserId());
        TaxonomyTag.setCreateBy(getNickName());
        TaxonomyTag.setCreateTime(DateUtil.date());
        return CommonResult.toAjax(TaxonomyTagService.createAttTag(TaxonomyTag));
    }

    @Operation(summary = "修改标签管理")
    @PreAuthorize("@ss.hasPermi('att:tag:edit')")
    @Log(title = "标签管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public CommonResult<Integer> edit(@Valid @RequestBody TaxonomyTagSaveReqVO TaxonomyTag) {
        TaxonomyTag.setUpdatorId(getUserId());
        TaxonomyTag.setUpdateBy(getNickName());
        TaxonomyTag.setUpdateTime(DateUtil.date());
        return CommonResult.toAjax(TaxonomyTagService.updateAttTag(TaxonomyTag));
    }

    @Operation(summary = "删除标签管理")
    @PreAuthorize("@ss.hasPermi('att:tag:remove')")
    @Log(title = "标签管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public CommonResult<Integer> remove(@PathVariable Long[] ids) {
        return CommonResult.toAjax(TaxonomyTagService.removeAttTag(Arrays.asList(ids)));
    }

}
