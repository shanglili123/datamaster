

package com.datamaster.module.taxonomy.controller.admin.project;

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
import com.datamaster.module.taxonomy.controller.admin.project.vo.TaxonomyProjectPageReqVO;
import com.datamaster.module.taxonomy.controller.admin.project.vo.TaxonomyProjectRespVO;
import com.datamaster.module.taxonomy.controller.admin.project.vo.TaxonomyProjectSaveReqVO;
import com.datamaster.module.taxonomy.controller.admin.project.vo.TaxonomySysUserReqVO;
import com.datamaster.module.taxonomy.dal.dataobject.project.TaxonomyProjectDO;
import com.datamaster.module.taxonomy.service.project.ITaxonomyProjectService;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.Arrays;
import java.util.List;

/**
 * 项目Controller
 *
 * @author shu
 * @date 2025-01-20
 */
@Tag(name = "项目")
@RestController
@RequestMapping("/att/project")
@Validated
public class TaxonomyProjectController extends BaseController {
    @Resource
    private ITaxonomyProjectService TaxonomyProjectService;

    @Operation(summary = "查询项目列表")
    @PreAuthorize("@ss.hasPermi('att:project:list')")
    @GetMapping("/list")
    public CommonResult<PageResult<TaxonomyProjectRespVO>> list(TaxonomyProjectPageReqVO TaxonomyProject) {
        PageResult<TaxonomyProjectDO> page = TaxonomyProjectService.getAttProjectPage(TaxonomyProject);
        return CommonResult.success(BeanUtils.toBean(page, TaxonomyProjectRespVO.class));
    }

    @Operation(summary = "查询当前用户所属的项目列表")
    @GetMapping("/currentUser/list")
    public CommonResult<List<TaxonomyProjectRespVO>> currentUser() {
        List<TaxonomyProjectDO> list = TaxonomyProjectService.getCurrentUserList(getUserId());
        return CommonResult.success(BeanUtils.toBean(list, TaxonomyProjectRespVO.class));
    }

    /**
     * 获取用户列表排除当前项目已经存在的用户
     */
    @PreAuthorize("@ss.hasPermi('att:project:list')")
    @PostMapping("/noProjectUser/list")
    public TableDataInfo list(TaxonomySysUserReqVO user) {
        List<SysUser> list = TaxonomyProjectService.selectNoProjectUserList(user);
        return getDataTable(list);
    }

    @Operation(summary = "导入项目列表")
    @PreAuthorize("@ss.hasPermi('att:project:import')")
    @Log(title = "项目", businessType = BusinessType.IMPORT)
    @PostMapping("/importData")
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception {
        ExcelUtil<TaxonomyProjectRespVO> util = new ExcelUtil<>(TaxonomyProjectRespVO.class);
        List<TaxonomyProjectRespVO> importExcelList = util.importExcel(file.getInputStream());
        String operName = getUsername();
        String message = TaxonomyProjectService.importAttProject(importExcelList, updateSupport, operName);
        return success(message);
    }

    @Operation(summary = "获取项目详细信息")
    @PreAuthorize("@ss.hasPermi('att:project:query')")
    @GetMapping(value = "/{ID}")
    public CommonResult<TaxonomyProjectRespVO> getInfo(@PathVariable("ID") Long ID) {
        TaxonomyProjectDO TaxonomyProjectDO = TaxonomyProjectService.getAttProjectById(ID);
        return CommonResult.success(BeanUtils.toBean(TaxonomyProjectDO, TaxonomyProjectRespVO.class));
    }

    @Operation(summary = "获取当前用户是非具备用户添加和项目管理员")
    @PreAuthorize("@ss.hasPermi('att:project:query')")
    @GetMapping(value = "/addUserAndProject/{id}")
    public CommonResult<JSONObject> addUserAndProjectIsOk(@PathVariable("id") Long id) {
        return CommonResult.success(TaxonomyProjectService.addUserAndProjectIsOk(getUserId(), id));
    }

    @Operation(summary = "新增项目")
    @PreAuthorize("@ss.hasPermi('att:project:add')")
    @Log(title = "项目", businessType = BusinessType.INSERT)
    @PostMapping
    public CommonResult<Long> add(@Valid @RequestBody TaxonomyProjectSaveReqVO TaxonomyProject) {
        TaxonomyProject.setCreatorId(getUserId());
        TaxonomyProject.setCreateBy(getNickName());
        TaxonomyProject.setCreateTime(DateUtil.date());
        Long serviceAttProject = TaxonomyProjectService.createAttProject(TaxonomyProject);
        if (serviceAttProject == -1) {
            return CommonResult.error(serviceAttProject.intValue(), "创建失败，请检查海豚调度器是否宕机或者是否存在该数据!");
        }
        if (serviceAttProject == -2) {
            return CommonResult.error(serviceAttProject.intValue(), "创建失败!");
        }
        return CommonResult.toAjax(serviceAttProject);
    }

    @Operation(summary = "修改项目")
    @PreAuthorize("@ss.hasPermi('att:project:edit')")
    @Log(title = "项目", businessType = BusinessType.UPDATE)
    @PutMapping
    public CommonResult<Integer> edit(@Valid @RequestBody TaxonomyProjectSaveReqVO TaxonomyProject) {
        TaxonomyProject.setUpdatorId(getUserId());
        TaxonomyProject.setUpdateBy(getNickName());
        TaxonomyProject.setUpdateTime(DateUtil.date());
        int i = TaxonomyProjectService.updateAttProject(TaxonomyProject);
        if (i == -1) {
            return CommonResult.error(i, "修改失败！");
        }
        return CommonResult.toAjax(i);
    }

    @Operation(summary = "修改项目状态")
    @PreAuthorize("@ss.hasPermi('att:project:query')")
    @GetMapping(value = "/editProjectStatus/{id}/{status}")
    public AjaxResult editProjectStatus(@PathVariable Long id, @PathVariable Long status) {
        Boolean isOk = TaxonomyProjectService.editProjectStatus(id, status);
        if (!isOk) {
            return AjaxResult.error("任务状态修改失败，请联系系统管理员");
        }
        return AjaxResult.success("修改成功");
    }

    @Operation(summary = "删除项目")
    @PreAuthorize("@ss.hasPermi('att:project:remove')")
    @Log(title = "项目", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public CommonResult<Integer> remove(@PathVariable Long[] ids) {
        int project = TaxonomyProjectService.removeAttProject(Arrays.asList(ids));
        if (project == -1) {
            return CommonResult.error(500, "删除失败，项目有人员存在!");
        } else if (project == -2) {
            return CommonResult.error(500, "删除失败，检查海豚调度器是否宕机!");
        }
        return CommonResult.toAjax(project);
    }

}
