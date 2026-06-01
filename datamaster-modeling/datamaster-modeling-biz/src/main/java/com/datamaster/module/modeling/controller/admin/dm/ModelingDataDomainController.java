

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
import com.datamaster.module.modeling.controller.admin.dm.vo.ModelingDataDomainPageReqVO;
import com.datamaster.module.modeling.controller.admin.dm.vo.ModelingDataDomainRespVO;
import com.datamaster.module.modeling.controller.admin.dm.vo.ModelingDataDomainSaveReqVO;
import com.datamaster.module.modeling.convert.dm.ModelingDataDomainConvert;
import com.datamaster.module.modeling.dal.dataobject.dm.ModelingDataDomainDO;
import com.datamaster.module.modeling.service.dm.IModelingDataDomainService;

/**
 * 数据域管理Controller
 *
 * @author FXB
 * @date 2026-03-24
 */
@Tag(name = "数据域管理")
@RestController
@RequestMapping("/mdl/dataDomain")
@Validated
public class ModelingDataDomainController extends BaseController {
    @Resource
    private IModelingDataDomainService ModelingDataDomainService;

    @Operation(summary = "查询数据域管理列表")
    @PreAuthorize("@ss.hasPermi('dm:dataDomain:list')")
    @GetMapping("/list")
    public CommonResult<PageResult<ModelingDataDomainRespVO>> list(ModelingDataDomainPageReqVO ModelingDataDomain) {
        PageResult<ModelingDataDomainDO> page = ModelingDataDomainService.getModelingDataDomainPage(ModelingDataDomain);
        return CommonResult.success(BeanUtils.toBean(page, ModelingDataDomainRespVO.class));
    }

    @Operation(summary = "导出数据域管理列表")
    @PreAuthorize("@ss.hasPermi('dm:dataDomain:export')")
    @Log(title = "数据域管理", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, ModelingDataDomainPageReqVO exportReqVO) {
        exportReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<ModelingDataDomainDO> list = (List<ModelingDataDomainDO>) ModelingDataDomainService.getModelingDataDomainPage(exportReqVO).getRows();
        ExcelUtil<ModelingDataDomainRespVO> util = new ExcelUtil<>(ModelingDataDomainRespVO.class);
        util.exportExcel(response, ModelingDataDomainConvert.INSTANCE.convertToRespVOList(list), "应用管理数据");
    }

    @Operation(summary = "导入数据域管理列表")
    @PreAuthorize("@ss.hasPermi('dm:dataDomain:import')")
    @Log(title = "数据域管理", businessType = BusinessType.IMPORT)
    @PostMapping("/importData")
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception {
        ExcelUtil<ModelingDataDomainRespVO> util = new ExcelUtil<>(ModelingDataDomainRespVO.class);
        List<ModelingDataDomainRespVO> importExcelList = util.importExcel(file.getInputStream());
        String operName = getUsername();
        String message = ModelingDataDomainService.importModelingDataDomain(importExcelList, updateSupport, operName);
        return success(message);
    }

    @Operation(summary = "获取数据域管理详细信息")
    @PreAuthorize("@ss.hasPermi('dm:dataDomain:query')")
    @GetMapping(value = "/{id}")
    public CommonResult<ModelingDataDomainRespVO> getInfo(@PathVariable("id") Long id) {
        ModelingDataDomainDO ModelingDataDomainDO = ModelingDataDomainService.getModelingDataDomainById(id);
        return CommonResult.success(BeanUtils.toBean(ModelingDataDomainDO, ModelingDataDomainRespVO.class));
    }

    @Operation(summary = "新增数据域管理")
    @PreAuthorize("@ss.hasPermi('dm:dataDomain:add')")
    @Log(title = "数据域管理", businessType = BusinessType.INSERT)
    @PostMapping
    public CommonResult<Long> add(@Valid @RequestBody ModelingDataDomainSaveReqVO ModelingDataDomain) {
        ModelingDataDomain.setCreatorId(getUserId());
        ModelingDataDomain.setCreateBy(getNickName());
        ModelingDataDomain.setCreateTime(DateUtil.date());
        return CommonResult.toAjax(ModelingDataDomainService.createModelingDataDomain(ModelingDataDomain));
    }

    @Operation(summary = "修改数据域管理")
    @PreAuthorize("@ss.hasPermi('dm:dataDomain:edit')")
    @Log(title = "数据域管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public CommonResult<Integer> edit(@Valid @RequestBody ModelingDataDomainSaveReqVO ModelingDataDomain) {
        ModelingDataDomain.setUpdatorId(getUserId());
        ModelingDataDomain.setUpdateBy(getNickName());
        ModelingDataDomain.setUpdateTime(DateUtil.date());
        return CommonResult.toAjax(ModelingDataDomainService.updateModelingDataDomain(ModelingDataDomain));
    }

    @Operation(summary = "删除数据域管理")
    @PreAuthorize("@ss.hasPermi('dm:dataDomain:remove')")
    @Log(title = "数据域管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public CommonResult<Integer> remove(@PathVariable Long[] ids) {
        return CommonResult.toAjax(ModelingDataDomainService.removeModelingDataDomain(Arrays.asList(ids)));
    }

    @Operation(summary = "查询数据域管理列通过业务分类id")
    @PreAuthorize("@ss.hasPermi('dm:dataDomain:list')")
    @GetMapping("/listByCategoryId")
    public CommonResult<PageResult<ModelingDataDomainRespVO>> listByCategoryId(ModelingDataDomainPageReqVO ModelingDataDomain) {
        PageResult<ModelingDataDomainDO> page = ModelingDataDomainService.getModelingDataDomainByCategoryId(ModelingDataDomain);
        return CommonResult.success(BeanUtils.toBean(page, ModelingDataDomainRespVO.class));
    }
}
