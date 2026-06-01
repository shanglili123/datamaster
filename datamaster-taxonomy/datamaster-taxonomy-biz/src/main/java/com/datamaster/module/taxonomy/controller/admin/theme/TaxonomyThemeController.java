

package com.datamaster.module.taxonomy.controller.admin.theme;

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
import com.datamaster.module.taxonomy.controller.admin.theme.vo.TaxonomyThemePageReqVO;
import com.datamaster.module.taxonomy.controller.admin.theme.vo.TaxonomyThemeRespVO;
import com.datamaster.module.taxonomy.controller.admin.theme.vo.TaxonomyThemeSaveReqVO;
import com.datamaster.module.taxonomy.convert.theme.TaxonomyThemeConvert;
import com.datamaster.module.taxonomy.dal.dataobject.theme.TaxonomyThemeDO;
import com.datamaster.module.taxonomy.service.theme.ITaxonomyThemeService;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.util.Arrays;
import java.util.List;

/**
 * 主题Controller
 *
 * @author DATAMASTER
 * @date 2025-01-20
 */
@Tag(name = "主题")
@RestController
@RequestMapping("/tax/theme")
@Validated
public class TaxonomyThemeController extends BaseController {
    @Resource
    private ITaxonomyThemeService TaxonomyThemeService;

    @Operation(summary = "查询主题列表")
    @PreAuthorize("@ss.hasPermi('att:theme:list')")
    @GetMapping("/list")
    public CommonResult<PageResult<TaxonomyThemeRespVO>> list(TaxonomyThemePageReqVO TaxonomyTheme) {
        PageResult<TaxonomyThemeDO> page = TaxonomyThemeService.getAttThemePage(TaxonomyTheme);
        return CommonResult.success(BeanUtils.toBean(page, TaxonomyThemeRespVO.class));
    }
    @Operation(summary = "查询主题列表")
    @GetMapping("/getAttThemeListByReqVO")
    public CommonResult<List<TaxonomyThemeRespVO>> getAttThemeListByReqVO(TaxonomyThemePageReqVO TaxonomyTheme) {
        List<TaxonomyThemeDO> list = TaxonomyThemeService.getAttThemeListByReqVO(TaxonomyTheme);
        return CommonResult.success(BeanUtils.toBean(list, TaxonomyThemeRespVO.class));
    }

    @Operation(summary = "导出主题列表")
    @PreAuthorize("@ss.hasPermi('att:theme:export')")
    @Log(title = "主题", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, TaxonomyThemePageReqVO exportReqVO) {
        exportReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<TaxonomyThemeDO> list = (List<TaxonomyThemeDO>) TaxonomyThemeService.getAttThemePage(exportReqVO).getRows();
        ExcelUtil<TaxonomyThemeRespVO> util = new ExcelUtil<>(TaxonomyThemeRespVO.class);
        util.exportExcel(response, TaxonomyThemeConvert.INSTANCE.convertToRespVOList(list), "应用管理数据");
    }

    @Operation(summary = "导入主题列表")
    @PreAuthorize("@ss.hasPermi('att:theme:import')")
    @Log(title = "主题", businessType = BusinessType.IMPORT)
    @PostMapping("/importData")
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception {
        ExcelUtil<TaxonomyThemeRespVO> util = new ExcelUtil<>(TaxonomyThemeRespVO.class);
        List<TaxonomyThemeRespVO> importExcelList = util.importExcel(file.getInputStream());
        String operName = getUsername();
        String message = TaxonomyThemeService.importAttTheme(importExcelList, updateSupport, operName);
        return success(message);
    }

    @Operation(summary = "获取主题详细信息")
    @PreAuthorize("@ss.hasPermi('att:theme:query')")
    @GetMapping(value = "/{ID}")
    public CommonResult<TaxonomyThemeRespVO> getInfo(@PathVariable("ID") Long ID) {
        TaxonomyThemeDO TaxonomyThemeDO = TaxonomyThemeService.getAttThemeById(ID);
        return CommonResult.success(BeanUtils.toBean(TaxonomyThemeDO, TaxonomyThemeRespVO.class));
    }

    @Operation(summary = "新增主题")
    @PreAuthorize("@ss.hasPermi('att:theme:add')")
    @Log(title = "主题", businessType = BusinessType.INSERT)
    @PostMapping
    public CommonResult<Long> add(@Valid @RequestBody TaxonomyThemeSaveReqVO TaxonomyTheme) {
        TaxonomyTheme.setCreatorId(getUserId());
        TaxonomyTheme.setCreateBy(getNickName());
        TaxonomyTheme.setCreateTime(DateUtil.date());
        return CommonResult.toAjax(TaxonomyThemeService.createAttTheme(TaxonomyTheme));
    }

    @Operation(summary = "修改主题")
    @PreAuthorize("@ss.hasPermi('att:theme:edit')")
    @Log(title = "主题", businessType = BusinessType.UPDATE)
    @PutMapping
    public CommonResult<Integer> edit(@Valid @RequestBody TaxonomyThemeSaveReqVO TaxonomyTheme) {
        TaxonomyTheme.setUpdatorId(getUserId());
        TaxonomyTheme.setUpdateBy(getNickName());
        TaxonomyTheme.setUpdateTime(DateUtil.date());
        return CommonResult.toAjax(TaxonomyThemeService.updateAttTheme(TaxonomyTheme));
    }

    @Operation(summary = "删除主题")
    @PreAuthorize("@ss.hasPermi('att:theme:remove')")
    @Log(title = "主题", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public CommonResult<Integer> remove(@PathVariable Long[] ids) {
        return CommonResult.toAjax(TaxonomyThemeService.removeAttTheme(Arrays.asList(ids)));
    }

}
