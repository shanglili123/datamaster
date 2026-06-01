

package com.datamaster.module.standards.controller.admin.dataLevel;

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
import com.datamaster.module.standards.controller.admin.dataLevel.vo.StandardsDataLevelPageReqVO;
import com.datamaster.module.standards.controller.admin.dataLevel.vo.StandardsDataLevelRespVO;
import com.datamaster.module.standards.controller.admin.dataLevel.vo.StandardsDataLevelSaveReqVO;
import com.datamaster.module.standards.convert.dataLevel.StandardsDataLevelConvert;
import com.datamaster.module.standards.dal.dataobject.dataLevel.StandardsDataLevelDO;
import com.datamaster.module.standards.service.dataLevel.IStandardsDataLevelService;

/**
 * 数据分级Controller
 *
 * @author DATAMASTER
 * @date 2026-04-03
 */
@Tag(name = "数据分级")
@RestController
@RequestMapping("/cat/dataLevel")
@Validated
public class StandardsDataLevelController extends BaseController {
    @Resource
    private IStandardsDataLevelService StandardsDataLevelService;

    @Operation(summary = "查询数据分级列表")
    @PreAuthorize("@ss.hasPermi('dg:datalevel:list')")
    @GetMapping("/list")
    public CommonResult<PageResult<StandardsDataLevelRespVO>> list(StandardsDataLevelPageReqVO StandardsDataLevel) {
        PageResult<StandardsDataLevelDO> page = StandardsDataLevelService.getDgDataLevelPage(StandardsDataLevel);
        return CommonResult.success(BeanUtils.toBean(page, StandardsDataLevelRespVO.class));
    }

    @Operation(summary = "查询数据分级列表")
    @PreAuthorize("@ss.hasPermi('dg:datalevel:list')")
    @GetMapping("/listAll")
    public CommonResult<List<StandardsDataLevelRespVO>> listAll(StandardsDataLevelPageReqVO StandardsDataLevel) {
        List<StandardsDataLevelDO> list = StandardsDataLevelService.getDgDataLevelListAll(StandardsDataLevel);
        return CommonResult.success(BeanUtils.toBean(list, StandardsDataLevelRespVO.class));
    }

    @Operation(summary = "导出数据分级列表")
    @PreAuthorize("@ss.hasPermi('dg:datalevel:export')")
    @Log(title = "数据分级", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, StandardsDataLevelPageReqVO exportReqVO) {
        exportReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<StandardsDataLevelDO> list = (List<StandardsDataLevelDO>) StandardsDataLevelService.getDgDataLevelPage(exportReqVO).getRows();
        ExcelUtil<StandardsDataLevelRespVO> util = new ExcelUtil<>(StandardsDataLevelRespVO.class);
        util.exportExcel(response, StandardsDataLevelConvert.INSTANCE.convertToRespVOList(list), "应用管理数据");
    }

    @Operation(summary = "导入数据分级列表")
    @PreAuthorize("@ss.hasPermi('dg:datalevel:import')")
    @Log(title = "数据分级", businessType = BusinessType.IMPORT)
    @PostMapping("/importData")
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception {
        ExcelUtil<StandardsDataLevelRespVO> util = new ExcelUtil<>(StandardsDataLevelRespVO.class);
        List<StandardsDataLevelRespVO> importExcelList = util.importExcel(file.getInputStream());
        String operName = getUsername();
        String message = StandardsDataLevelService.importDgDataLevel(importExcelList, updateSupport, operName);
        return success(message);
    }

    @Operation(summary = "获取数据分级详细信息")
    @PreAuthorize("@ss.hasPermi('dg:datalevel:query')")
    @GetMapping(value = "/{id}")
    public CommonResult<StandardsDataLevelRespVO> getInfo(@PathVariable("id") Long id) {
        StandardsDataLevelDO StandardsDataLevelDO = StandardsDataLevelService.getDgDataLevelById(id);
        return CommonResult.success(BeanUtils.toBean(StandardsDataLevelDO, StandardsDataLevelRespVO.class));
    }

    @Operation(summary = "新增数据分级")
    @PreAuthorize("@ss.hasPermi('dg:datalevel:add')")
    @Log(title = "数据分级", businessType = BusinessType.INSERT)
    @PostMapping
    public CommonResult<Long> add(@Valid @RequestBody StandardsDataLevelSaveReqVO StandardsDataLevel) {
        StandardsDataLevel.setCreatorId(getUserId());
        StandardsDataLevel.setCreateBy(getNickName());
        StandardsDataLevel.setCreateTime(DateUtil.date());
        return CommonResult.toAjax(StandardsDataLevelService.createDgDataLevel(StandardsDataLevel));
    }



    @Operation(summary = "修改数据分级")
    @PreAuthorize("@ss.hasPermi('dg:datalevel:edit')")
    @Log(title = "数据分级", businessType = BusinessType.UPDATE)
    @PutMapping
    public CommonResult<Integer> edit(@Valid @RequestBody StandardsDataLevelSaveReqVO StandardsDataLevel) {
        StandardsDataLevel.setUpdatorId(getUserId());
        StandardsDataLevel.setUpdateBy(getNickName());
        StandardsDataLevel.setUpdateTime(DateUtil.date());
        return CommonResult.toAjax(StandardsDataLevelService.updateDgDataLevel(StandardsDataLevel));
    }

    @Operation(summary = "删除数据分级")
    @PreAuthorize("@ss.hasPermi('dg:datalevel:remove')")
    @Log(title = "数据分级", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public CommonResult<Integer> remove(@PathVariable Long[] ids) {
        return CommonResult.toAjax(StandardsDataLevelService.removeDgDataLevel(Arrays.asList(ids)));
    }

}
