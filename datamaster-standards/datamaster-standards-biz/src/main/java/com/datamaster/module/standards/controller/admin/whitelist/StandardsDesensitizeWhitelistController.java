

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
import com.datamaster.module.standards.controller.admin.whitelist.vo.StandardsDesensitizeWhitelistPageReqVO;
import com.datamaster.module.standards.controller.admin.whitelist.vo.StandardsDesensitizeWhitelistRespVO;
import com.datamaster.module.standards.controller.admin.whitelist.vo.StandardsDesensitizeWhitelistSaveReqVO;
import com.datamaster.module.standards.convert.whitelist.StandardsDesensitizeWhitelistConvert;
import com.datamaster.module.standards.dal.dataobject.whitelist.StandardsDesensitizeWhitelistDO;
import com.datamaster.module.standards.service.whitelist.IStandardsDesensitizeWhitelistService;

/**
 * 脱敏白名单Controller
 *
 * @author DATAMASTER
 * @date 2026-04-09
 */
@Tag(name = "脱敏白名单")
@RestController
@RequestMapping("/cat/desensitizeWhitelist")
@Validated
public class StandardsDesensitizeWhitelistController extends BaseController {
    @Resource
    private IStandardsDesensitizeWhitelistService StandardsDesensitizeWhitelistService;

    @Operation(summary = "查询脱敏白名单列表")
    @PreAuthorize("@ss.hasPermi('dg:desensitizewhitelist:list')")
    @GetMapping("/list")
    public CommonResult<PageResult<StandardsDesensitizeWhitelistRespVO>> list(StandardsDesensitizeWhitelistPageReqVO StandardsDesensitizeWhitelist) {
        PageResult<StandardsDesensitizeWhitelistDO> page = StandardsDesensitizeWhitelistService.getDgDesensitizeWhitelistPage(StandardsDesensitizeWhitelist);
        return CommonResult.success(BeanUtils.toBean(page, StandardsDesensitizeWhitelistRespVO.class));
    }

    @Operation(summary = "导出脱敏白名单列表")
    @PreAuthorize("@ss.hasPermi('dg:desensitizewhitelist:export')")
    @Log(title = "脱敏白名单", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, StandardsDesensitizeWhitelistPageReqVO exportReqVO) {
        exportReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<StandardsDesensitizeWhitelistDO> list = (List<StandardsDesensitizeWhitelistDO>) StandardsDesensitizeWhitelistService.getDgDesensitizeWhitelistPage(exportReqVO).getRows();
        ExcelUtil<StandardsDesensitizeWhitelistRespVO> util = new ExcelUtil<>(StandardsDesensitizeWhitelistRespVO.class);
        util.exportExcel(response, StandardsDesensitizeWhitelistConvert.INSTANCE.convertToRespVOList(list), "应用管理数据");
    }

    @Operation(summary = "导入脱敏白名单列表")
    @PreAuthorize("@ss.hasPermi('dg:desensitizewhitelist:import')")
    @Log(title = "脱敏白名单", businessType = BusinessType.IMPORT)
    @PostMapping("/importData")
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception {
        ExcelUtil<StandardsDesensitizeWhitelistRespVO> util = new ExcelUtil<>(StandardsDesensitizeWhitelistRespVO.class);
        List<StandardsDesensitizeWhitelistRespVO> importExcelList = util.importExcel(file.getInputStream());
        String operName = getUsername();
        String message = StandardsDesensitizeWhitelistService.importDgDesensitizeWhitelist(importExcelList, updateSupport, operName);
        return success(message);
    }

    @Operation(summary = "获取脱敏白名单详细信息")
    @PreAuthorize("@ss.hasPermi('dg:desensitizewhitelist:query')")
    @GetMapping(value = "/{id}")
    public CommonResult<StandardsDesensitizeWhitelistRespVO> getInfo(@PathVariable("id") Long id) {
        StandardsDesensitizeWhitelistDO StandardsDesensitizeWhitelistDO = StandardsDesensitizeWhitelistService.getDgDesensitizeWhitelistById(id);
        return CommonResult.success(BeanUtils.toBean(StandardsDesensitizeWhitelistDO, StandardsDesensitizeWhitelistRespVO.class));
    }

    @Operation(summary = "新增脱敏白名单")
    @PreAuthorize("@ss.hasPermi('dg:desensitizewhitelist:add')")
    @Log(title = "脱敏白名单", businessType = BusinessType.INSERT)
    @PostMapping
    public CommonResult<Long> add(@Valid @RequestBody StandardsDesensitizeWhitelistSaveReqVO StandardsDesensitizeWhitelist) {
        StandardsDesensitizeWhitelist.setCreatorId(getUserId());
        StandardsDesensitizeWhitelist.setCreateBy(getNickName());
        StandardsDesensitizeWhitelist.setCreateTime(DateUtil.date());
        return CommonResult.toAjax(StandardsDesensitizeWhitelistService.createDgDesensitizeWhitelist(StandardsDesensitizeWhitelist));
    }

    @Operation(summary = "修改脱敏白名单")
    @PreAuthorize("@ss.hasPermi('dg:desensitizewhitelist:edit')")
    @Log(title = "脱敏白名单", businessType = BusinessType.UPDATE)
    @PutMapping
    public CommonResult<Integer> edit(@Valid @RequestBody StandardsDesensitizeWhitelistSaveReqVO StandardsDesensitizeWhitelist) {
        StandardsDesensitizeWhitelist.setUpdatorId(getUserId());
        StandardsDesensitizeWhitelist.setUpdateBy(getNickName());
        StandardsDesensitizeWhitelist.setUpdateTime(DateUtil.date());
        return CommonResult.toAjax(StandardsDesensitizeWhitelistService.updateDgDesensitizeWhitelist(StandardsDesensitizeWhitelist));
    }

    @Operation(summary = "删除脱敏白名单")
    @PreAuthorize("@ss.hasPermi('dg:desensitizewhitelist:remove')")
    @Log(title = "脱敏白名单", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public CommonResult<Integer> remove(@PathVariable Long[] ids) {
        return CommonResult.toAjax(StandardsDesensitizeWhitelistService.removeDgDesensitizeWhitelist(Arrays.asList(ids)));
    }

}
