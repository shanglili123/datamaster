

package com.datamaster.module.standards.controller.admin.desensitizeRules;

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
import com.datamaster.module.standards.controller.admin.desensitizeRules.vo.StandardsDesensitizeIntervalPageReqVO;
import com.datamaster.module.standards.controller.admin.desensitizeRules.vo.StandardsDesensitizeIntervalRespVO;
import com.datamaster.module.standards.controller.admin.desensitizeRules.vo.StandardsDesensitizeIntervalSaveReqVO;
import com.datamaster.module.standards.convert.desensitizeRules.StandardsDesensitizeIntervalConvert;
import com.datamaster.module.standards.dal.dataobject.desensitizeRules.StandardsDesensitizeIntervalDO;
import com.datamaster.module.standards.service.desensitizeRules.IStandardsDesensitizeIntervalService;

/**
 * 脱敏区间Controller
 *
 * @author DATAMASTER
 * @date 2026-04-10
 */
@Tag(name = "脱敏区间")
@RestController
@RequestMapping("/cat/desensitizeInterval")
@Validated
public class StandardsDesensitizeIntervalController extends BaseController {
    @Resource
    private IStandardsDesensitizeIntervalService StandardsDesensitizeIntervalService;

    @Operation(summary = "查询脱敏区间列表")
    @PreAuthorize("@ss.hasPermi('dg:desensitizeinterval:list')")
    @GetMapping("/list")
    public CommonResult<PageResult<StandardsDesensitizeIntervalRespVO>> list(StandardsDesensitizeIntervalPageReqVO StandardsDesensitizeInterval) {
        PageResult<StandardsDesensitizeIntervalDO> page = StandardsDesensitizeIntervalService.getDgDesensitizeIntervalPage(StandardsDesensitizeInterval);
        return CommonResult.success(BeanUtils.toBean(page, StandardsDesensitizeIntervalRespVO.class));
    }

    @Operation(summary = "导出脱敏区间列表")
    @PreAuthorize("@ss.hasPermi('dg:desensitizeinterval:export')")
    @Log(title = "脱敏区间", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, StandardsDesensitizeIntervalPageReqVO exportReqVO) {
        exportReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<StandardsDesensitizeIntervalDO> list = (List<StandardsDesensitizeIntervalDO>) StandardsDesensitizeIntervalService.getDgDesensitizeIntervalPage(exportReqVO).getRows();
        ExcelUtil<StandardsDesensitizeIntervalRespVO> util = new ExcelUtil<>(StandardsDesensitizeIntervalRespVO.class);
        util.exportExcel(response, StandardsDesensitizeIntervalConvert.INSTANCE.convertToRespVOList(list), "应用管理数据");
    }

    @Operation(summary = "导入脱敏区间列表")
    @PreAuthorize("@ss.hasPermi('dg:desensitizeinterval:import')")
    @Log(title = "脱敏区间", businessType = BusinessType.IMPORT)
    @PostMapping("/importData")
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception {
        ExcelUtil<StandardsDesensitizeIntervalRespVO> util = new ExcelUtil<>(StandardsDesensitizeIntervalRespVO.class);
        List<StandardsDesensitizeIntervalRespVO> importExcelList = util.importExcel(file.getInputStream());
        String operName = getUsername();
        String message = StandardsDesensitizeIntervalService.importDgDesensitizeInterval(importExcelList, updateSupport, operName);
        return success(message);
    }

    @Operation(summary = "获取脱敏区间详细信息")
    @PreAuthorize("@ss.hasPermi('dg:desensitizeinterval:query')")
    @GetMapping(value = "/{id}")
    public CommonResult<StandardsDesensitizeIntervalRespVO> getInfo(@PathVariable("id") Long id) {
        StandardsDesensitizeIntervalDO StandardsDesensitizeIntervalDO = StandardsDesensitizeIntervalService.getDgDesensitizeIntervalById(id);
        return CommonResult.success(BeanUtils.toBean(StandardsDesensitizeIntervalDO, StandardsDesensitizeIntervalRespVO.class));
    }

    @Operation(summary = "新增脱敏区间")
    @PreAuthorize("@ss.hasPermi('dg:desensitizeinterval:add')")
    @Log(title = "脱敏区间", businessType = BusinessType.INSERT)
    @PostMapping
    public CommonResult<Long> add(@Valid @RequestBody StandardsDesensitizeIntervalSaveReqVO StandardsDesensitizeInterval) {
        StandardsDesensitizeInterval.setCreatorId(getUserId());
        StandardsDesensitizeInterval.setCreateBy(getNickName());
        StandardsDesensitizeInterval.setCreateTime(DateUtil.date());
        return CommonResult.toAjax(StandardsDesensitizeIntervalService.createDgDesensitizeInterval(StandardsDesensitizeInterval));
    }

    @Operation(summary = "修改脱敏区间")
    @PreAuthorize("@ss.hasPermi('dg:desensitizeinterval:edit')")
    @Log(title = "脱敏区间", businessType = BusinessType.UPDATE)
    @PutMapping
    public CommonResult<Integer> edit(@Valid @RequestBody StandardsDesensitizeIntervalSaveReqVO StandardsDesensitizeInterval) {
        StandardsDesensitizeInterval.setUpdatorId(getUserId());
        StandardsDesensitizeInterval.setUpdateBy(getNickName());
        StandardsDesensitizeInterval.setUpdateTime(DateUtil.date());
        return CommonResult.toAjax(StandardsDesensitizeIntervalService.updateDgDesensitizeInterval(StandardsDesensitizeInterval));
    }

    @Operation(summary = "删除脱敏区间")
    @PreAuthorize("@ss.hasPermi('dg:desensitizeinterval:remove')")
    @Log(title = "脱敏区间", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public CommonResult<Integer> remove(@PathVariable Long[] ids) {
        return CommonResult.toAjax(StandardsDesensitizeIntervalService.removeDgDesensitizeInterval(Arrays.asList(ids)));
    }

}
