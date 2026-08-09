

package com.datamaster.module.governance.controller.admin.space;

import cn.hutool.core.date.DateUtil;
import cn.hutool.json.JSONObject;
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
import com.datamaster.common.core.domain.entity.SysUser;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.common.core.page.TableDataInfo;
import com.datamaster.common.enums.BusinessType;
import com.datamaster.common.utils.object.BeanUtils;
import com.datamaster.common.utils.poi.ExcelUtil;
import com.datamaster.module.governance.controller.admin.space.vo.TaxonomySpacePageReqVO;
import com.datamaster.module.governance.controller.admin.space.vo.TaxonomySpaceRespVO;
import com.datamaster.module.governance.controller.admin.space.vo.TaxonomySpaceSaveReqVO;
import com.datamaster.module.governance.controller.admin.space.vo.TaxonomySysUserReqVO;
import com.datamaster.module.governance.dal.dataobject.space.TaxonomySpaceDO;
import com.datamaster.module.governance.service.space.ITaxonomySpaceService;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.Arrays;
import java.util.List;

/**
 * 空间 Controller
 *
 * @author shu
 * @date 2025-01-20
 */
@Tag(name = "空间")
@RestController
@RequestMapping("/tax/space")
@Validated
public class TaxonomySpaceController extends BaseController {
    @Resource
    private ITaxonomySpaceService taxonomySpaceService;

    @Operation(summary = "查询空间列表")
    @PreAuthorize("@ss.hasPermi('tax:space:list')")
    @GetMapping("/list")
    public CommonResult<PageResult<TaxonomySpaceRespVO>> list(TaxonomySpacePageReqVO TaxonomySpace) {
        PageResult<TaxonomySpaceDO> page = taxonomySpaceService.getSpacePage(TaxonomySpace);
        return CommonResult.success(BeanUtils.toBean(page, TaxonomySpaceRespVO.class));
    }

    @Operation(summary = "查询当前用户所属的空间列表")
    @GetMapping("/currentUser/list")
    public CommonResult<List<TaxonomySpaceRespVO>> currentUser() {
        List<TaxonomySpaceDO> list = taxonomySpaceService.getCurrentUserSpaceList(getUserId());
        return CommonResult.success(BeanUtils.toBean(list, TaxonomySpaceRespVO.class));
    }

    /**
     * 获取用户列表，排除当前空间已经存在的用户
     */
    @PreAuthorize("@ss.hasPermi('tax:space:list')")
    @PostMapping("/noSpaceUser/list")
    public TableDataInfo list(TaxonomySysUserReqVO user) {
        List<SysUser> list = taxonomySpaceService.selectUsersNotInSpace(user);
        return getDataTable(list);
    }

    @Operation(summary = "导入空间列表")
    @PreAuthorize("@ss.hasPermi('tax:space:import')")
    @Log(title = "空间", businessType = BusinessType.IMPORT)
    @PostMapping("/importData")
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception {
        ExcelUtil<TaxonomySpaceRespVO> util = new ExcelUtil<>(TaxonomySpaceRespVO.class);
        List<TaxonomySpaceRespVO> importExcelList = util.importExcel(file.getInputStream());
        String operName = getUsername();
        String message = taxonomySpaceService.importSpace(importExcelList, updateSupport, operName);
        return success(message);
    }

    @Operation(summary = "获取空间详细信息")
    @PreAuthorize("@ss.hasPermi('tax:space:query')")
    @GetMapping(value = "/{ID}")
    public CommonResult<TaxonomySpaceRespVO> getInfo(@PathVariable("ID") Long ID) {
        TaxonomySpaceDO TaxonomySpaceDO = taxonomySpaceService.getSpaceById(ID);
        return CommonResult.success(BeanUtils.toBean(TaxonomySpaceDO, TaxonomySpaceRespVO.class));
    }

    @Operation(summary = "校验当前用户是否具备空间成员管理权限")
    @PreAuthorize("@ss.hasPermi('tax:space:query')")
    @GetMapping(value = "/managePermission/{id}")
    public CommonResult<JSONObject> checkUserSpaceManagePermission(@PathVariable("id") Long id) {
        return CommonResult.success(taxonomySpaceService.checkUserSpaceManagePermission(getUserId(), id));
    }

    @Operation(summary = "新增空间")
    @PreAuthorize("@ss.hasPermi('tax:space:add')")
    @Log(title = "空间", businessType = BusinessType.INSERT)
    @PostMapping
    public CommonResult<Long> add(@Valid @RequestBody TaxonomySpaceSaveReqVO TaxonomySpace) {
        TaxonomySpace.setCreatorId(getUserId());
        TaxonomySpace.setCreateBy(getNickName());
        TaxonomySpace.setCreateTime(DateUtil.date());
        Long createdSpaceId = taxonomySpaceService.createSpace(TaxonomySpace);
        if (createdSpaceId == -1) {
            return CommonResult.error(createdSpaceId.intValue(), "创建失败，请检查海豚调度器是否宕机或者是否存在该数据!");
        }
        if (createdSpaceId == -2) {
            return CommonResult.error(createdSpaceId.intValue(), "创建失败!");
        }
        return CommonResult.toAjax(createdSpaceId);
    }

    @Operation(summary = "修改空间")
    @PreAuthorize("@ss.hasPermi('tax:space:edit')")
    @Log(title = "空间", businessType = BusinessType.UPDATE)
    @PutMapping
    public CommonResult<Integer> edit(@Valid @RequestBody TaxonomySpaceSaveReqVO TaxonomySpace) {
        TaxonomySpace.setUpdatorId(getUserId());
        TaxonomySpace.setUpdateBy(getNickName());
        TaxonomySpace.setUpdateTime(DateUtil.date());
        int i = taxonomySpaceService.updateSpace(TaxonomySpace);
        if (i == -1) {
            return CommonResult.error(i, "修改失败！");
        }
        return CommonResult.toAjax(i);
    }

    @Operation(summary = "修改空间状态")
    @PreAuthorize("@ss.hasPermi('tax:space:query')")
    @GetMapping(value = "/editSpaceStatus/{id}/{status}")
    public AjaxResult updateSpaceStatus(@PathVariable Long id, @PathVariable Long status) {
        Boolean isOk = taxonomySpaceService.updateSpaceStatus(id, status);
        if (!isOk) {
            return AjaxResult.error("任务状态修改失败，请联系系统管理员");
        }
        return AjaxResult.success("修改成功");
    }

    @Operation(summary = "删除空间")
    @PreAuthorize("@ss.hasPermi('tax:space:remove')")
    @Log(title = "空间", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public CommonResult<Integer> remove(@PathVariable Long[] ids) {
        int result = taxonomySpaceService.removeSpace(Arrays.asList(ids));
        if (result == -1) {
            return CommonResult.error(500, "删除失败，空间有人员存在!");
        } else if (result == -2) {
            return CommonResult.error(500, "删除失败，检查海豚调度器是否宕机!");
        }
        return CommonResult.toAjax(result);
    }

}
