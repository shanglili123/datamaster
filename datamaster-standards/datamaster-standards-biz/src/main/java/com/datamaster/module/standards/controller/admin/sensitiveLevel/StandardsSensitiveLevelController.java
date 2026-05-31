package com.datamaster.module.standards.controller.admin.sensitiveLevel;

import cn.hutool.core.date.DateUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import com.datamaster.common.annotation.Log;
import com.datamaster.common.core.controller.BaseController;
import com.datamaster.common.core.domain.AjaxResult;
import com.datamaster.common.core.domain.CommonResult;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.common.enums.BusinessType;
import com.datamaster.common.utils.object.BeanUtils;
import com.datamaster.module.standards.controller.admin.sensitiveLevel.vo.StandardsSensitiveLevelPageReqVO;
import com.datamaster.module.standards.controller.admin.sensitiveLevel.vo.StandardsSensitiveLevelRespVO;
import com.datamaster.module.standards.controller.admin.sensitiveLevel.vo.StandardsSensitiveLevelSaveReqVO;
import com.datamaster.module.standards.dal.dataobject.sensitiveLevel.StandardsSensitiveLevelDO;
import com.datamaster.module.standards.service.sensitiveLevel.IStandardsSensitiveLevelService;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.Arrays;

/**
 * 敏感等级Controller
 *
 * @author Chaos
 * @date 2025-01-21
 */
@Tag(name = "敏感等级")
@RestController
@RequestMapping("/dg/sensitiveLevel")
@Validated
public class StandardsSensitiveLevelController extends BaseController {
    @Resource
    private IStandardsSensitiveLevelService service;

    @Operation(summary = "查询敏感等级列表")
    @PreAuthorize("@ss.hasPermi('dg:sensitiveLevel:list')")
    @GetMapping("/list")
    public CommonResult<PageResult<StandardsSensitiveLevelRespVO>> list(StandardsSensitiveLevelPageReqVO StandardsSensitiveLevel) {
        PageResult<StandardsSensitiveLevelDO> page = service.getDgSensitiveLevelPage(StandardsSensitiveLevel);
        return CommonResult.success(BeanUtils.toBean(page, StandardsSensitiveLevelRespVO.class));
    }

    @Operation(summary = "获取敏感等级详细信息")
    @PreAuthorize("@ss.hasPermi('dg:sensitiveLevel:query')")
    @GetMapping(value = "/{id}")
    public CommonResult<StandardsSensitiveLevelRespVO> getInfo(@PathVariable("id") Long id) {
        StandardsSensitiveLevelDO StandardsSensitiveLevelDO = service.getDgSensitiveLevelById(id);
        return CommonResult.success(BeanUtils.toBean(StandardsSensitiveLevelDO, StandardsSensitiveLevelRespVO.class));
    }

    @Operation(summary = "新增敏感等级")
    @PreAuthorize("@ss.hasPermi('dg:sensitiveLevel:add')")
    @Log(title = "敏感等级", businessType = BusinessType.INSERT)
    @PostMapping
    public CommonResult<Long> add(@Valid @RequestBody StandardsSensitiveLevelSaveReqVO StandardsSensitiveLevel) {
        StandardsSensitiveLevel.setCreatorId(getUserId());
        StandardsSensitiveLevel.setCreateBy(getNickName());
        StandardsSensitiveLevel.setCreateTime(DateUtil.date());
        return CommonResult.toAjax(service.createDgSensitiveLevel(StandardsSensitiveLevel));
    }

    @Operation(summary = "修改敏感等级")
    @PreAuthorize("@ss.hasPermi('dg:sensitiveLevel:edit')")
    @Log(title = "敏感等级", businessType = BusinessType.UPDATE)
    @PutMapping
    public CommonResult<Integer> edit(@Valid @RequestBody StandardsSensitiveLevelSaveReqVO StandardsSensitiveLevel) {
        StandardsSensitiveLevel.setUpdatorId(getUserId());
        StandardsSensitiveLevel.setUpdateBy(getNickName());
        StandardsSensitiveLevel.setUpdateTime(DateUtil.date());
        return CommonResult.toAjax(service.updateDgSensitiveLevel(StandardsSensitiveLevel));
    }

    @Operation(summary = "修改敏感等级状态")
    @PreAuthorize("@ss.hasPermi('dg:sensitiveLevel:edit')")
    @Log(title = "敏感等级", businessType = BusinessType.UPDATE)
    @PostMapping("/updateStatus/{id}/{status}")
    public AjaxResult updateStatus(@PathVariable Long id, @PathVariable Long status) {
        if (!service.updateStatus(id, status)) {
            return AjaxResult.error("已被使用，不允许下线！");
        }
        return AjaxResult.success("修改成功");
    }

    @Operation(summary = "删除敏感等级")
    @PreAuthorize("@ss.hasPermi('dg:sensitiveLevel:remove')")
    @Log(title = "敏感等级", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public CommonResult<Integer> remove(@PathVariable Long[] ids) {
        return CommonResult.toAjax(service.removeDgSensitiveLevel(Arrays.asList(ids)));
    }

}
