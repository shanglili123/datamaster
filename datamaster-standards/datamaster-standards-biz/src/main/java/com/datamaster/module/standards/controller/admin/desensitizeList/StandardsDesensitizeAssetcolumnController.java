

package com.datamaster.module.standards.controller.admin.desensitizeList;

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
import com.datamaster.module.standards.controller.admin.desensitizeList.vo.StandardsDesensitizeAssetcolumnPageReqVO;
import com.datamaster.module.standards.controller.admin.desensitizeList.vo.StandardsDesensitizeAssetcolumnRespVO;
import com.datamaster.module.standards.controller.admin.desensitizeList.vo.StandardsDesensitizeAssetcolumnSaveReqVO;
import com.datamaster.module.standards.convert.desensitizeList.StandardsDesensitizeAssetcolumnConvert;
import com.datamaster.module.standards.dal.dataobject.desensitizeList.StandardsDesensitizeAssetcolumnDO;
import com.datamaster.module.standards.service.desensitizeList.IStandardsDesensitizeAssetcolumnService;

/**
 * 脱敏清单关联关系Controller
 *
 * @author DATAMASTER
 * @date 2026-04-12
 */
@Tag(name = "脱敏清单关联关系")
@RestController
@RequestMapping("/cat/standardsDesensitizeList")
@Validated
public class StandardsDesensitizeAssetcolumnController extends BaseController {
    @Resource
    private IStandardsDesensitizeAssetcolumnService StandardsDesensitizeAssetcolumnService;

    @Operation(summary = "查询脱敏清单关联关系列表")
    @PreAuthorize("@ss.hasPermi('dg:Standardsdesensitizelist:list')")
    @GetMapping("/list")
    public CommonResult<PageResult<StandardsDesensitizeAssetcolumnRespVO>> list(StandardsDesensitizeAssetcolumnPageReqVO StandardsDesensitizeAssetcolumn) {
        PageResult<StandardsDesensitizeAssetcolumnDO> page = StandardsDesensitizeAssetcolumnService.getDgDesensitizeAssetcolumnPage(StandardsDesensitizeAssetcolumn);
        return CommonResult.success(BeanUtils.toBean(page, StandardsDesensitizeAssetcolumnRespVO.class));
    }

    @Operation(summary = "查询脱敏清单关联关系列表")
    @PreAuthorize("@ss.hasPermi('dg:Standardsdesensitizelist:list')")
    @GetMapping("/listByRuleId")
    public CommonResult<PageResult<StandardsDesensitizeAssetcolumnRespVO>> listByRuleId(StandardsDesensitizeAssetcolumnPageReqVO StandardsDesensitizeAssetcolumn) {
        PageResult<StandardsDesensitizeAssetcolumnDO> page = StandardsDesensitizeAssetcolumnService.getDgDesensitizePagebyRuleId(StandardsDesensitizeAssetcolumn);
        return CommonResult.success(BeanUtils.toBean(page, StandardsDesensitizeAssetcolumnRespVO.class));
    }



    @Operation(summary = "导出脱敏清单关联关系列表")
    @PreAuthorize("@ss.hasPermi('dg:Standardsdesensitizelist:export')")
    @Log(title = "脱敏清单关联关系", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, StandardsDesensitizeAssetcolumnPageReqVO exportReqVO) {
        exportReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<StandardsDesensitizeAssetcolumnDO> list = (List<StandardsDesensitizeAssetcolumnDO>) StandardsDesensitizeAssetcolumnService.getDgDesensitizeAssetcolumnPage(exportReqVO).getRows();
        ExcelUtil<StandardsDesensitizeAssetcolumnRespVO> util = new ExcelUtil<>(StandardsDesensitizeAssetcolumnRespVO.class);
        util.exportExcel(response, StandardsDesensitizeAssetcolumnConvert.INSTANCE.convertToRespVOList(list), "应用管理数据");
    }

    @Operation(summary = "导入脱敏清单关联关系列表")
    @PreAuthorize("@ss.hasPermi('dg:Standardsdesensitizelist:import')")
    @Log(title = "脱敏清单关联关系", businessType = BusinessType.IMPORT)
    @PostMapping("/importData")
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception {
        ExcelUtil<StandardsDesensitizeAssetcolumnRespVO> util = new ExcelUtil<>(StandardsDesensitizeAssetcolumnRespVO.class);
        List<StandardsDesensitizeAssetcolumnRespVO> importExcelList = util.importExcel(file.getInputStream());
        String operName = getUsername();
        String message = StandardsDesensitizeAssetcolumnService.importDgDesensitizeAssetcolumn(importExcelList, updateSupport, operName);
        return success(message);
    }

    @Operation(summary = "获取脱敏清单关联关系详细信息")
    @PreAuthorize("@ss.hasPermi('dg:Standardsdesensitizelist:query')")
    @GetMapping(value = "/{id}")
    public CommonResult<StandardsDesensitizeAssetcolumnRespVO> getInfo(@PathVariable("id") Long id) {
        StandardsDesensitizeAssetcolumnDO StandardsDesensitizeAssetcolumnDO = StandardsDesensitizeAssetcolumnService.getDgDesensitizeAssetcolumnById(id);
        return CommonResult.success(BeanUtils.toBean(StandardsDesensitizeAssetcolumnDO, StandardsDesensitizeAssetcolumnRespVO.class));
    }

    @Operation(summary = "新增脱敏清单关联关系")
    @PreAuthorize("@ss.hasPermi('dg:Standardsdesensitizelist:add')")
    @Log(title = "脱敏清单关联关系", businessType = BusinessType.INSERT)
    @PostMapping
    public CommonResult<Long> add(@Valid @RequestBody StandardsDesensitizeAssetcolumnSaveReqVO StandardsDesensitizeAssetcolumn) {
        StandardsDesensitizeAssetcolumn.setCreatorId(getUserId());
        StandardsDesensitizeAssetcolumn.setCreateBy(getNickName());
        StandardsDesensitizeAssetcolumn.setCreateTime(DateUtil.date());
        return CommonResult.toAjax(StandardsDesensitizeAssetcolumnService.createDgDesensitizeAssetcolumn(StandardsDesensitizeAssetcolumn));
    }

    @Operation(summary = "修改脱敏清单关联关系")
    @PreAuthorize("@ss.hasPermi('dg:Standardsdesensitizelist:edit')")
    @Log(title = "脱敏清单关联关系", businessType = BusinessType.UPDATE)
    @PutMapping
    public CommonResult<Integer> edit(@Valid @RequestBody StandardsDesensitizeAssetcolumnSaveReqVO StandardsDesensitizeAssetcolumn) {
        StandardsDesensitizeAssetcolumn.setUpdatorId(getUserId());
        StandardsDesensitizeAssetcolumn.setUpdateBy(getNickName());
        StandardsDesensitizeAssetcolumn.setUpdateTime(DateUtil.date());
        return CommonResult.toAjax(StandardsDesensitizeAssetcolumnService.updateDgDesensitizeAssetcolumn(StandardsDesensitizeAssetcolumn));
    }

    @Operation(summary = "删除脱敏清单关联关系")
    @PreAuthorize("@ss.hasPermi('dg:Standardsdesensitizelist:remove')")
    @Log(title = "脱敏清单关联关系", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public CommonResult<Integer> remove(@PathVariable Long[] ids) {
        return CommonResult.toAjax(StandardsDesensitizeAssetcolumnService.removeDgDesensitizeAssetcolumn(Arrays.asList(ids)));
    }




}
