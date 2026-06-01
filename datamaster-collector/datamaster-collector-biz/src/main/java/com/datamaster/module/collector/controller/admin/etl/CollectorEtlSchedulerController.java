

package com.datamaster.module.collector.controller.admin.etl;

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
import com.datamaster.module.collector.controller.admin.etl.vo.CollectorEtlSchedulerPageReqVO;
import com.datamaster.module.collector.controller.admin.etl.vo.CollectorEtlSchedulerRespVO;
import com.datamaster.module.collector.controller.admin.etl.vo.CollectorEtlSchedulerSaveReqVO;
import com.datamaster.module.collector.convert.etl.CollectorEtlSchedulerConvert;
import com.datamaster.module.collector.dal.dataobject.etl.CollectorEtlSchedulerDO;
import com.datamaster.module.collector.service.etl.ICollectorEtlSchedulerService;

/**
 * 数据集成调度信息Controller
 *
 * @author DATAMASTER
 * @date 2025-02-13
 */
@Tag(name = "数据集成调度信息")
@RestController
@RequestMapping("/col/etlScheduler")
@Validated
public class CollectorEtlSchedulerController extends BaseController {
    @Resource
    private ICollectorEtlSchedulerService CollectorEtlSchedulerService;

    @Operation(summary = "查询数据集成调度信息列表")
//    @PreAuthorize("@ss.hasPermi('dpp:etlScheduler:list')")
    @GetMapping("/list")
    public CommonResult<PageResult<CollectorEtlSchedulerRespVO>> list(CollectorEtlSchedulerPageReqVO CollectorEtlScheduler) {
        PageResult<CollectorEtlSchedulerDO> page = CollectorEtlSchedulerService.getCollectorEtlSchedulerPage(CollectorEtlScheduler);
        return CommonResult.success(BeanUtils.toBean(page, CollectorEtlSchedulerRespVO.class));
    }

    @Operation(summary = "导出数据集成调度信息列表")
//    @PreAuthorize("@ss.hasPermi('dpp:etlScheduler:export')")
    @Log(title = "数据集成调度信息", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, CollectorEtlSchedulerPageReqVO exportReqVO) {
        exportReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<CollectorEtlSchedulerDO> list = (List<CollectorEtlSchedulerDO>) CollectorEtlSchedulerService.getCollectorEtlSchedulerPage(exportReqVO).getRows();
        ExcelUtil<CollectorEtlSchedulerRespVO> util = new ExcelUtil<>(CollectorEtlSchedulerRespVO.class);
        util.exportExcel(response, CollectorEtlSchedulerConvert.INSTANCE.convertToRespVOList(list), "应用管理数据");
    }

    @Operation(summary = "导入数据集成调度信息列表")
//    @PreAuthorize("@ss.hasPermi('dpp:etlScheduler:import')")
    @Log(title = "数据集成调度信息", businessType = BusinessType.IMPORT)
    @PostMapping("/importData")
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception {
        ExcelUtil<CollectorEtlSchedulerRespVO> util = new ExcelUtil<>(CollectorEtlSchedulerRespVO.class);
        List<CollectorEtlSchedulerRespVO> importExcelList = util.importExcel(file.getInputStream());
        String operName = getUsername();
        String message = CollectorEtlSchedulerService.importCollectorEtlScheduler(importExcelList, updateSupport, operName);
        return success(message);
    }

    @Operation(summary = "获取数据集成调度信息详细信息")
//    @PreAuthorize("@ss.hasPermi('dpp:etlScheduler:query')")
    @GetMapping(value = "/{id}")
    public CommonResult<CollectorEtlSchedulerRespVO> getInfo(@PathVariable("id") Long id) {
        CollectorEtlSchedulerDO CollectorEtlSchedulerDO = CollectorEtlSchedulerService.getCollectorEtlSchedulerById(id);
        return CommonResult.success(BeanUtils.toBean(CollectorEtlSchedulerDO, CollectorEtlSchedulerRespVO.class));
    }

    @Operation(summary = "新增数据集成调度信息")
//    @PreAuthorize("@ss.hasPermi('dpp:etlScheduler:add')")
    @Log(title = "数据集成调度信息", businessType = BusinessType.INSERT)
    @PostMapping
    public CommonResult<Long> add(@Valid @RequestBody CollectorEtlSchedulerSaveReqVO CollectorEtlScheduler) {
        CollectorEtlScheduler.setCreatorId(getUserId());
        CollectorEtlScheduler.setCreateBy(getNickName());
        CollectorEtlScheduler.setCreateTime(DateUtil.date());
        return CommonResult.toAjax(CollectorEtlSchedulerService.createCollectorEtlScheduler(CollectorEtlScheduler));
    }

    @Operation(summary = "修改数据集成调度信息")
//    @PreAuthorize("@ss.hasPermi('dpp:etlScheduler:edit')")
    @Log(title = "数据集成调度信息", businessType = BusinessType.UPDATE)
    @PutMapping
    public CommonResult<Integer> edit(@Valid @RequestBody CollectorEtlSchedulerSaveReqVO CollectorEtlScheduler) {
        CollectorEtlScheduler.setUpdatorId(getUserId());
        CollectorEtlScheduler.setUpdateBy(getNickName());
        CollectorEtlScheduler.setUpdateTime(DateUtil.date());
        return CommonResult.toAjax(CollectorEtlSchedulerService.updateCollectorEtlScheduler(CollectorEtlScheduler));
    }

    @Operation(summary = "删除数据集成调度信息")
//    @PreAuthorize("@ss.hasPermi('dpp:etlScheduler:remove')")
    @Log(title = "数据集成调度信息", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public CommonResult<Integer> remove(@PathVariable Long[] ids) {
        return CommonResult.toAjax(CollectorEtlSchedulerService.removeCollectorEtlScheduler(Arrays.asList(ids)));
    }

}
