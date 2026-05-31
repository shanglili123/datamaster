

package com.datamaster.module.taxonomy.controller.admin.cat;

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
import org.springframework.web.multipart.MultipartFile;
import com.datamaster.common.core.domain.AjaxResult;
import com.datamaster.common.core.page.PageParam;
import com.datamaster.common.annotation.Log;
import com.datamaster.common.core.controller.BaseController;
import com.datamaster.common.core.domain.CommonResult;
import com.datamaster.common.enums.BusinessType;
import com.datamaster.common.utils.object.BeanUtils;
import com.datamaster.common.utils.poi.ExcelUtil;
import com.datamaster.module.taxonomy.controller.admin.cat.vo.TaxonomyTaskCatPageReqVO;
import com.datamaster.module.taxonomy.controller.admin.cat.vo.TaxonomyTaskCatRespVO;
import com.datamaster.module.taxonomy.controller.admin.cat.vo.TaxonomyTaskCatSaveReqVO;
import com.datamaster.module.taxonomy.convert.cat.TaxonomyTaskCatConvert;
import com.datamaster.module.taxonomy.dal.dataobject.cat.TaxonomyTaskCatDO;
import com.datamaster.module.taxonomy.service.cat.ITaxonomyTaskCatService;

/**
 * 数据集成任务类目管理Controller
 *
 * @author DATAMASTER
 * @date 2025-03-11
 */
@Tag(name = "数据集成任务类目管理")
@RestController
@RequestMapping("/att/taskCat")
@Validated
public class TaxonomyTaskCatController extends BaseController {
    @Resource
    private ITaxonomyTaskCatService TaxonomyTaskCatService;

    @Operation(summary = "查询数据集成任务类目管理列表")
    @GetMapping("/list")
    public CommonResult<List<TaxonomyTaskCatRespVO>> list(TaxonomyTaskCatPageReqVO TaxonomyTaskCat) {
        List<TaxonomyTaskCatDO> TaxonomyTaskCatDOList = TaxonomyTaskCatService.getAttTaskCatList(TaxonomyTaskCat);
        return CommonResult.success(BeanUtils.toBean(TaxonomyTaskCatDOList, TaxonomyTaskCatRespVO.class));
    }

    @Operation(summary = "导出数据集成任务类目管理列表")
    @PreAuthorize("@ss.hasPermi('att:taskCat:export')")
    @Log(title = "数据集成任务类目管理", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, TaxonomyTaskCatPageReqVO exportReqVO) {
        exportReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<TaxonomyTaskCatDO> list = (List<TaxonomyTaskCatDO>) TaxonomyTaskCatService.getAttTaskCatPage(exportReqVO).getRows();
        ExcelUtil<TaxonomyTaskCatRespVO> util = new ExcelUtil<>(TaxonomyTaskCatRespVO.class);
        util.exportExcel(response, TaxonomyTaskCatConvert.INSTANCE.convertToRespVOList(list), "应用管理数据");
    }

    @Operation(summary = "导入数据集成任务类目管理列表")
    @PreAuthorize("@ss.hasPermi('att:taskCat:import')")
    @Log(title = "数据集成任务类目管理", businessType = BusinessType.IMPORT)
    @PostMapping("/importData")
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception {
        ExcelUtil<TaxonomyTaskCatRespVO> util = new ExcelUtil<>(TaxonomyTaskCatRespVO.class);
        List<TaxonomyTaskCatRespVO> importExcelList = util.importExcel(file.getInputStream());
        String operName = getUsername();
        String message = TaxonomyTaskCatService.importAttTaskCat(importExcelList, updateSupport, operName);
        return success(message);
    }

    @Operation(summary = "获取数据集成任务类目管理详细信息")
    @PreAuthorize("@ss.hasPermi('att:taskCat:query')")
    @GetMapping(value = "/{id}")
    public CommonResult<TaxonomyTaskCatRespVO> getInfo(@PathVariable("id") Long id) {
        TaxonomyTaskCatDO TaxonomyTaskCatDO = TaxonomyTaskCatService.getAttTaskCatById(id);
        return CommonResult.success(BeanUtils.toBean(TaxonomyTaskCatDO, TaxonomyTaskCatRespVO.class));
    }

    @Operation(summary = "新增数据集成任务类目管理")
    @PreAuthorize("@ss.hasPermi('att:taskCat:add')")
    @Log(title = "数据集成任务类目管理", businessType = BusinessType.INSERT)
    @PostMapping
    public CommonResult<Long> add(@Valid @RequestBody TaxonomyTaskCatSaveReqVO TaxonomyTaskCat) {
        TaxonomyTaskCat.setCreatorId(getUserId());
        TaxonomyTaskCat.setCreateBy(getNickName());
        TaxonomyTaskCat.setCreateTime(DateUtil.date());
        return CommonResult.toAjax(TaxonomyTaskCatService.createAttTaskCat(TaxonomyTaskCat));
    }

    @Operation(summary = "修改数据集成任务类目管理")
    @PreAuthorize("@ss.hasPermi('att:taskCat:edit')")
    @Log(title = "数据集成任务类目管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public CommonResult<Integer> edit(@Valid @RequestBody TaxonomyTaskCatSaveReqVO TaxonomyTaskCat) {
        TaxonomyTaskCat.setUpdatorId(getUserId());
        TaxonomyTaskCat.setUpdateBy(getNickName());
        TaxonomyTaskCat.setUpdateTime(DateUtil.date());
        return CommonResult.toAjax(TaxonomyTaskCatService.updateAttTaskCat(TaxonomyTaskCat));
    }

    @Operation(summary = "删除数据集成任务类目管理")
    @PreAuthorize("@ss.hasPermi('att:taskCat:remove')")
    @Log(title = "数据集成任务类目管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public CommonResult<Integer> remove(@PathVariable Long[] ids) {
        return CommonResult.toAjax(TaxonomyTaskCatService.removeAttTaskCat(Arrays.asList(ids)));
    }

}
