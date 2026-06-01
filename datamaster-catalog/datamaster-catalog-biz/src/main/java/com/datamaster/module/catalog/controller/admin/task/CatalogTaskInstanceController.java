package com.datamaster.module.catalog.controller.admin.task;

import cn.hutool.core.date.DateUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.datamaster.common.annotation.Log;
import com.datamaster.common.core.controller.BaseController;
import com.datamaster.common.core.domain.AjaxResult;
import com.datamaster.common.core.domain.CommonResult;
import com.datamaster.common.core.page.PageParam;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.common.enums.BusinessType;
import com.datamaster.common.utils.object.BeanUtils;
import com.datamaster.common.utils.poi.ExcelUtil;
import com.datamaster.module.catalog.controller.admin.task.vo.CatalogTaskInstancePageReqVO;
import com.datamaster.module.catalog.controller.admin.task.vo.CatalogTaskInstanceRespVO;
import com.datamaster.module.catalog.controller.admin.task.vo.CatalogTaskInstanceSaveReqVO;
import com.datamaster.module.catalog.convert.task.CatalogTaskInstanceConvert;
import com.datamaster.module.catalog.dal.dataobject.task.CatalogTaskInstanceDO;
import com.datamaster.module.catalog.service.task.ICatalogTaskInstanceService;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.util.Arrays;
import java.util.List;

/**
 * 采集任务实例Controller
 *
 * @author DATAMASTER
 * @date 2025-12-16
 */
@Tag(name = "采集任务实例")
@RestController
@RequestMapping("/cat/taskInstance")
@Validated
public class CatalogTaskInstanceController extends BaseController {
    @Resource
    private ICatalogTaskInstanceService CatalogTaskInstanceService;

    @Operation(summary = "查询采集任务实例列表")
    @GetMapping("/list")
    public CommonResult<PageResult<CatalogTaskInstanceRespVO>> list(CatalogTaskInstancePageReqVO CatalogTaskInstance) {
        PageResult<CatalogTaskInstanceDO> page = CatalogTaskInstanceService.getCatalogTaskInstancePage(CatalogTaskInstance);
        return CommonResult.success(BeanUtils.toBean(page, CatalogTaskInstanceRespVO.class));
    }

    @Operation(summary = "导出采集任务实例列表")
    @Log(title = "采集任务实例", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, CatalogTaskInstancePageReqVO exportReqVO) {
        exportReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<CatalogTaskInstanceDO> list = (List<CatalogTaskInstanceDO>) CatalogTaskInstanceService.getCatalogTaskInstancePage(exportReqVO).getRows();
        ExcelUtil<CatalogTaskInstanceRespVO> util = new ExcelUtil<>(CatalogTaskInstanceRespVO.class);
        util.exportExcel(response, CatalogTaskInstanceConvert.INSTANCE.convertToRespVOList(list), "应用管理数据");
    }

    @Operation(summary = "导入采集任务实例列表")
    @Log(title = "采集任务实例", businessType = BusinessType.IMPORT)
    @PostMapping("/importData")
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception {
        ExcelUtil<CatalogTaskInstanceRespVO> util = new ExcelUtil<>(CatalogTaskInstanceRespVO.class);
        List<CatalogTaskInstanceRespVO> importExcelList = util.importExcel(file.getInputStream());
        String operName = getUsername();
        String message = CatalogTaskInstanceService.importCatalogTaskInstance(importExcelList, updateSupport, operName);
        return success(message);
    }

    @Operation(summary = "获取采集任务实例详细信息")
    @GetMapping(value = "/{id}")
    public CommonResult<CatalogTaskInstanceRespVO> getInfo(@PathVariable("id") Long id) {
        CatalogTaskInstanceDO CatalogTaskInstanceDO = CatalogTaskInstanceService.getCatalogTaskInstanceById(id);
        return CommonResult.success(BeanUtils.toBean(CatalogTaskInstanceDO, CatalogTaskInstanceRespVO.class));
    }

    @Operation(summary = "新增采集任务实例")
    @Log(title = "采集任务实例", businessType = BusinessType.INSERT)
    @PostMapping
    public CommonResult<Long> add(@Valid @RequestBody CatalogTaskInstanceSaveReqVO CatalogTaskInstance) {
        CatalogTaskInstance.setCreatorId(getUserId());
        CatalogTaskInstance.setCreateBy(getNickName());
        CatalogTaskInstance.setCreateTime(DateUtil.date());
        return CommonResult.toAjax(CatalogTaskInstanceService.createCatalogTaskInstance(CatalogTaskInstance));
    }

    @Operation(summary = "修改采集任务实例")
    @Log(title = "采集任务实例", businessType = BusinessType.UPDATE)
    @PutMapping
    public CommonResult<Integer> edit(@Valid @RequestBody CatalogTaskInstanceSaveReqVO CatalogTaskInstance) {
        CatalogTaskInstance.setUpdatorId(getUserId());
        CatalogTaskInstance.setUpdateBy(getNickName());
        CatalogTaskInstance.setUpdateTime(DateUtil.date());
        return CommonResult.toAjax(CatalogTaskInstanceService.updateCatalogTaskInstance(CatalogTaskInstance));
    }

    @Operation(summary = "删除采集任务实例")
    @Log(title = "采集任务实例", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public CommonResult<Integer> remove(@PathVariable Long[] ids) {
        return CommonResult.toAjax(CatalogTaskInstanceService.removeCatalogTaskInstance(Arrays.asList(ids)));
    }

}
