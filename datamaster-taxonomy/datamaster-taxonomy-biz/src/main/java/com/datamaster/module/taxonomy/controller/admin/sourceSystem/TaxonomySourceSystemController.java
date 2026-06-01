

package com.datamaster.module.taxonomy.controller.admin.sourceSystem;

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
import com.datamaster.module.taxonomy.controller.admin.sourceSystem.vo.TaxonomySourceSystemPageReqVO;
import com.datamaster.module.taxonomy.controller.admin.sourceSystem.vo.TaxonomySourceSystemRespVO;
import com.datamaster.module.taxonomy.controller.admin.sourceSystem.vo.TaxonomySourceSystemSaveReqVO;
import com.datamaster.module.taxonomy.convert.sourceSystem.TaxonomySourceSystemConvert;
import com.datamaster.module.taxonomy.dal.dataobject.sourceSystem.TaxonomySourceSystemDO;
import com.datamaster.module.taxonomy.service.sourceSystem.ITaxonomySourceSystemService;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.util.Arrays;
import java.util.List;

/**
 * 来源系统Controller
 *
 * @author DATAMASTER
 * @date 2026-04-03
 */
@Tag(name = "来源系统")
@RestController
@RequestMapping("/tax/sourceSystem")
@Validated
public class TaxonomySourceSystemController extends BaseController {
    @Resource
    private ITaxonomySourceSystemService TaxonomySourceSystemService;

    @Operation(summary = "查询来源系统列表")
    @PreAuthorize("@ss.hasPermi('att:sourcesystem:list')")
    @GetMapping("/list")
    public CommonResult<PageResult<TaxonomySourceSystemRespVO>> list(TaxonomySourceSystemPageReqVO TaxonomySourceSystem) {
        PageResult<TaxonomySourceSystemDO> page = TaxonomySourceSystemService.getAttSourceSystemPage(TaxonomySourceSystem);
        return CommonResult.success(BeanUtils.toBean(page, TaxonomySourceSystemRespVO.class));
    }

    @Operation(summary = "查询来源系统列表")
    @PreAuthorize("@ss.hasPermi('att:sourcesystem:list')")
    @GetMapping("/listValid")
    public CommonResult<List<TaxonomySourceSystemRespVO>> list() {
        List<TaxonomySourceSystemDO> TaxonomySourceSystemList = TaxonomySourceSystemService.getAttSourceSystemListByValidFlag(true);
        return CommonResult.success(BeanUtils.toBean(TaxonomySourceSystemList, TaxonomySourceSystemRespVO.class));
    }

    @Operation(summary = "导出来源系统列表")
    @PreAuthorize("@ss.hasPermi('att:sourcesystem:export')")
    @Log(title = "来源系统", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, TaxonomySourceSystemPageReqVO exportReqVO) {
        exportReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<TaxonomySourceSystemDO> list = (List<TaxonomySourceSystemDO>) TaxonomySourceSystemService.getAttSourceSystemPage(exportReqVO).getRows();
        ExcelUtil<TaxonomySourceSystemRespVO> util = new ExcelUtil<>(TaxonomySourceSystemRespVO.class);
        util.exportExcel(response, TaxonomySourceSystemConvert.INSTANCE.convertToRespVOList(list), "应用管理数据");
    }

    @Operation(summary = "导入来源系统列表")
    @PreAuthorize("@ss.hasPermi('att:sourcesystem:import')")
    @Log(title = "来源系统", businessType = BusinessType.IMPORT)
    @PostMapping("/importData")
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception {
        ExcelUtil<TaxonomySourceSystemRespVO> util = new ExcelUtil<>(TaxonomySourceSystemRespVO.class);
        List<TaxonomySourceSystemRespVO> importExcelList = util.importExcel(file.getInputStream());
        String operName = getUsername();
        String message = TaxonomySourceSystemService.importAttSourceSystem(importExcelList, updateSupport, operName);
        return success(message);
    }

    @Operation(summary = "获取来源系统详细信息")
    @PreAuthorize("@ss.hasPermi('att:sourcesystem:query')")
    @GetMapping(value = "/{id}")
    public CommonResult<TaxonomySourceSystemRespVO> getInfo(@PathVariable("id") Long id) {
        TaxonomySourceSystemDO TaxonomySourceSystemDO = TaxonomySourceSystemService.getAttSourceSystemById(id);
        return CommonResult.success(BeanUtils.toBean(TaxonomySourceSystemDO, TaxonomySourceSystemRespVO.class));
    }

    @Operation(summary = "新增来源系统")
    @PreAuthorize("@ss.hasPermi('att:sourcesystem:add')")
    @Log(title = "来源系统", businessType = BusinessType.INSERT)
    @PostMapping
    public CommonResult<Long> add(@Valid @RequestBody TaxonomySourceSystemSaveReqVO TaxonomySourceSystem) {
        TaxonomySourceSystem.setCreatorId(getUserId());
        TaxonomySourceSystem.setCreateBy(getNickName());
        TaxonomySourceSystem.setCreateTime(DateUtil.date());
        return CommonResult.toAjax(TaxonomySourceSystemService.createAttSourceSystem(TaxonomySourceSystem));
    }

    @Operation(summary = "修改来源系统")
    @PreAuthorize("@ss.hasPermi('att:sourcesystem:edit')")
    @Log(title = "来源系统", businessType = BusinessType.UPDATE)
    @PutMapping
    public CommonResult<Integer> edit(@Valid @RequestBody TaxonomySourceSystemSaveReqVO TaxonomySourceSystem) {
        TaxonomySourceSystem.setUpdatorId(getUserId());
        TaxonomySourceSystem.setUpdateBy(getNickName());
        TaxonomySourceSystem.setUpdateTime(DateUtil.date());
        return CommonResult.toAjax(TaxonomySourceSystemService.updateAttSourceSystem(TaxonomySourceSystem));
    }

    @Operation(summary = "删除来源系统")
    @PreAuthorize("@ss.hasPermi('att:sourcesystem:remove')")
    @Log(title = "来源系统", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public CommonResult<Integer> remove(@PathVariable Long[] ids) {
        return CommonResult.toAjax(TaxonomySourceSystemService.removeAttSourceSystem(Arrays.asList(ids)));
    }

}
