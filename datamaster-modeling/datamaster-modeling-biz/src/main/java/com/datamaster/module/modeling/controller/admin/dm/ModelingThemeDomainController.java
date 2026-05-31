

package com.datamaster.module.modeling.controller.admin.dm;

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
import com.datamaster.common.core.page.PageResult;
import com.datamaster.common.enums.BusinessType;
import com.datamaster.common.utils.object.BeanUtils;
import com.datamaster.common.utils.poi.ExcelUtil;
import com.datamaster.common.exception.enums.GlobalErrorCodeConstants;
import com.datamaster.module.modeling.controller.admin.dm.vo.ModelingThemeDomainPageReqVO;
import com.datamaster.module.modeling.controller.admin.dm.vo.ModelingThemeDomainRespVO;
import com.datamaster.module.modeling.controller.admin.dm.vo.ModelingThemeDomainSaveReqVO;
import com.datamaster.module.modeling.convert.dm.ModelingThemeDomainConvert;
import com.datamaster.module.modeling.dal.dataobject.dm.ModelingThemeDomainDO;
import com.datamaster.module.modeling.service.dm.IModelingThemeDomainService;

/**
 * 主题域管理Controller
 *
 * @author FXB
 * @date 2026-03-24
 */
@Tag(name = "主题域管理")
@RestController
@RequestMapping("/dm/themeDomain")
@Validated
public class ModelingThemeDomainController extends BaseController {
    @Resource
    private IModelingThemeDomainService ModelingThemeDomainService;

    @Operation(summary = "查询主题域管理列表")
    @PreAuthorize("@ss.hasPermi('dm:themeDomain:list')")
    @GetMapping("/list")
    public CommonResult<List<ModelingThemeDomainRespVO>> list(ModelingThemeDomainPageReqVO ModelingThemeDomain) {
        List<ModelingThemeDomainDO> ModelingThemeDomainList = ModelingThemeDomainService.getModelingThemeDomainList(ModelingThemeDomain);
        return CommonResult.success(BeanUtils.toBean(ModelingThemeDomainList, ModelingThemeDomainRespVO.class));
    }

    @Operation(summary = "导出主题域管理列表")
    @PreAuthorize("@ss.hasPermi('dm:themeDomain:export')")
    @Log(title = "主题域管理", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, ModelingThemeDomainPageReqVO exportReqVO) {
        exportReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<ModelingThemeDomainDO> list = (List<ModelingThemeDomainDO>) ModelingThemeDomainService.getModelingThemeDomainPage(exportReqVO).getRows();
        ExcelUtil<ModelingThemeDomainRespVO> util = new ExcelUtil<>(ModelingThemeDomainRespVO.class);
        util.exportExcel(response, ModelingThemeDomainConvert.INSTANCE.convertToRespVOList(list), "应用管理数据");
    }

    @Operation(summary = "导入主题域管理列表")
    @PreAuthorize("@ss.hasPermi('dm:themeDomain:import')")
    @Log(title = "主题域管理", businessType = BusinessType.IMPORT)
    @PostMapping("/importData")
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception {
        ExcelUtil<ModelingThemeDomainRespVO> util = new ExcelUtil<>(ModelingThemeDomainRespVO.class);
        List<ModelingThemeDomainRespVO> importExcelList = util.importExcel(file.getInputStream());
        String operName = getUsername();
        String message = ModelingThemeDomainService.importModelingThemeDomain(importExcelList, updateSupport, operName);
        return success(message);
    }

    @Operation(summary = "获取主题域管理详细信息")
    @PreAuthorize("@ss.hasPermi('dm:themeDomain:query')")
    @GetMapping(value = "/{id}")
    public CommonResult<ModelingThemeDomainRespVO> getInfo(@PathVariable("id") Long id) {
        ModelingThemeDomainDO ModelingThemeDomainDO = ModelingThemeDomainService.getModelingThemeDomainById(id);
        return CommonResult.success(BeanUtils.toBean(ModelingThemeDomainDO, ModelingThemeDomainRespVO.class));
    }

    @Operation(summary = "新增主题域管理")
    @PreAuthorize("@ss.hasPermi('dm:themeDomain:add')")
    @Log(title = "主题域管理", businessType = BusinessType.INSERT)
    @PostMapping
    public CommonResult<Long> add(@Valid @RequestBody ModelingThemeDomainSaveReqVO ModelingThemeDomain) {
        ModelingThemeDomain.setCreatorId(getUserId());
        ModelingThemeDomain.setCreateBy(getNickName());
        ModelingThemeDomain.setCreateTime(DateUtil.date());
        return CommonResult.toAjax(ModelingThemeDomainService.createModelingThemeDomain(ModelingThemeDomain));
    }

    @Operation(summary = "修改主题域管理")
    @PreAuthorize("@ss.hasPermi('dm:themeDomain:edit')")
    @Log(title = "主题域管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public CommonResult<Integer> edit(@Valid @RequestBody ModelingThemeDomainSaveReqVO ModelingThemeDomain) {
        ModelingThemeDomain.setUpdatorId(getUserId());
        ModelingThemeDomain.setUpdateBy(getNickName());
        ModelingThemeDomain.setUpdateTime(DateUtil.date());
        return CommonResult.toAjax(ModelingThemeDomainService.updateModelingThemeDomain(ModelingThemeDomain));
    }

    @Operation(summary = "删除主题域管理")
    @PreAuthorize("@ss.hasPermi('dm:themeDomain:remove')")
    @Log(title = "主题域管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public CommonResult<Integer> remove(@PathVariable Long[] ids) {
        return CommonResult.toAjax(ModelingThemeDomainService.removeModelingThemeDomain(Arrays.asList(ids)));
    }

}
