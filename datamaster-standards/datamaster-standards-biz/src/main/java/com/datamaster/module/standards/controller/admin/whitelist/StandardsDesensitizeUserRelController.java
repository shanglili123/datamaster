

package com.datamaster.module.standards.controller.admin.whitelist;

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
import com.datamaster.module.standards.controller.admin.whitelist.vo.StandardsDesensitizeUserRelPageReqVO;
import com.datamaster.module.standards.controller.admin.whitelist.vo.StandardsDesensitizeUserRelRespVO;
import com.datamaster.module.standards.controller.admin.whitelist.vo.StandardsDesensitizeUserRelSaveReqVO;
import com.datamaster.module.standards.convert.whitelist.StandardsDesensitizeUserRelConvert;
import com.datamaster.module.standards.dal.dataobject.whitelist.StandardsDesensitizeUserRelDO;
import com.datamaster.module.standards.service.whitelist.IStandardsDesensitizeUserRelService;

/**
 * 脱敏白名单与用户关联关系Controller
 *
 * @author DATAMASTER
 * @date 2026-04-09
 */
@Tag(name = "脱敏白名单与用户关联关系")
@RestController
@RequestMapping("/dg/desensitizeUserRel")
@Validated
public class StandardsDesensitizeUserRelController extends BaseController {
    @Resource
    private IStandardsDesensitizeUserRelService StandardsDesensitizeUserRelService;

    @Operation(summary = "查询脱敏白名单与用户关联关系列表")
    @PreAuthorize("@ss.hasPermi('dg:desensitizewhitelist:list')")
    @GetMapping("/list")
    public CommonResult<PageResult<StandardsDesensitizeUserRelRespVO>> list(StandardsDesensitizeUserRelPageReqVO StandardsDesensitizeUserRel) {
        PageResult<StandardsDesensitizeUserRelDO> page = StandardsDesensitizeUserRelService.getDgDesensitizeUserRelPage(StandardsDesensitizeUserRel);
        return CommonResult.success(BeanUtils.toBean(page, StandardsDesensitizeUserRelRespVO.class));
    }

    @Operation(summary = "导出脱敏白名单与用户关联关系列表")
    @PreAuthorize("@ss.hasPermi('dg:desensitizewhitelist:export')")
    @Log(title = "脱敏白名单与用户关联关系", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, StandardsDesensitizeUserRelPageReqVO exportReqVO) {
        exportReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<StandardsDesensitizeUserRelDO> list = (List<StandardsDesensitizeUserRelDO>) StandardsDesensitizeUserRelService.getDgDesensitizeUserRelPage(exportReqVO).getRows();
        ExcelUtil<StandardsDesensitizeUserRelRespVO> util = new ExcelUtil<>(StandardsDesensitizeUserRelRespVO.class);
        util.exportExcel(response, StandardsDesensitizeUserRelConvert.INSTANCE.convertToRespVOList(list), "应用管理数据");
    }

    @Operation(summary = "导入脱敏白名单与用户关联关系列表")
    @PreAuthorize("@ss.hasPermi('dg:desensitizewhitelist:import')")
    @Log(title = "脱敏白名单与用户关联关系", businessType = BusinessType.IMPORT)
    @PostMapping("/importData")
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception {
        ExcelUtil<StandardsDesensitizeUserRelRespVO> util = new ExcelUtil<>(StandardsDesensitizeUserRelRespVO.class);
        List<StandardsDesensitizeUserRelRespVO> importExcelList = util.importExcel(file.getInputStream());
        String operName = getUsername();
        String message = StandardsDesensitizeUserRelService.importDgDesensitizeUserRel(importExcelList, updateSupport, operName);
        return success(message);
    }

    @Operation(summary = "获取脱敏白名单与用户关联关系详细信息")
    @PreAuthorize("@ss.hasPermi('dg:desensitizewhitelist:query')")
    @GetMapping(value = "/{id}")
    public CommonResult<StandardsDesensitizeUserRelRespVO> getInfo(@PathVariable("id") Long id) {
        StandardsDesensitizeUserRelDO StandardsDesensitizeUserRelDO = StandardsDesensitizeUserRelService.getDgDesensitizeUserRelById(id);
        return CommonResult.success(BeanUtils.toBean(StandardsDesensitizeUserRelDO, StandardsDesensitizeUserRelRespVO.class));
    }

    @Operation(summary = "新增脱敏白名单与用户关联关系")
    @PreAuthorize("@ss.hasPermi('dg:desensitizewhitelist:add')")
    @Log(title = "脱敏白名单与用户关联关系", businessType = BusinessType.INSERT)
    @PostMapping
    public CommonResult<Long> add(@Valid @RequestBody StandardsDesensitizeUserRelSaveReqVO StandardsDesensitizeUserRel) {
        StandardsDesensitizeUserRel.setCreatorId(getUserId());
        StandardsDesensitizeUserRel.setCreateBy(getNickName());
        StandardsDesensitizeUserRel.setCreateTime(DateUtil.date());
        return CommonResult.toAjax(StandardsDesensitizeUserRelService.createDgDesensitizeUserRel(StandardsDesensitizeUserRel));
    }

    @Operation(summary = "修改脱敏白名单与用户关联关系")
    @PreAuthorize("@ss.hasPermi('dg:desensitizewhitelist:edit')")
    @Log(title = "脱敏白名单与用户关联关系", businessType = BusinessType.UPDATE)
    @PutMapping
    public CommonResult<Integer> edit(@Valid @RequestBody StandardsDesensitizeUserRelSaveReqVO StandardsDesensitizeUserRel) {
        StandardsDesensitizeUserRel.setUpdatorId(getUserId());
        StandardsDesensitizeUserRel.setUpdateBy(getNickName());
        StandardsDesensitizeUserRel.setUpdateTime(DateUtil.date());
        return CommonResult.toAjax(StandardsDesensitizeUserRelService.updateDgDesensitizeUserRel(StandardsDesensitizeUserRel));
    }

    @Operation(summary = "删除脱敏白名单与用户关联关系")
    @PreAuthorize("@ss.hasPermi('dg:desensitizewhitelist:remove')")
    @Log(title = "脱敏白名单与用户关联关系", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public CommonResult<Integer> remove(@PathVariable Long[] ids) {
        return CommonResult.toAjax(StandardsDesensitizeUserRelService.removeDgDesensitizeUserRel(Arrays.asList(ids)));
    }

}
