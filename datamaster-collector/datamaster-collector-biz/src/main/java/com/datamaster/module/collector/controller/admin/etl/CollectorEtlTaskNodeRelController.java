

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
import com.datamaster.module.collector.controller.admin.etl.vo.CollectorEtlTaskNodeRelPageReqVO;
import com.datamaster.module.collector.controller.admin.etl.vo.CollectorEtlTaskNodeRelRespVO;
import com.datamaster.module.collector.controller.admin.etl.vo.CollectorEtlTaskNodeRelSaveReqVO;
import com.datamaster.module.collector.convert.etl.CollectorEtlTaskNodeRelConvert;
import com.datamaster.module.collector.dal.dataobject.etl.CollectorEtlTaskNodeRelDO;
import com.datamaster.module.collector.service.etl.ICollectorEtlTaskNodeRelService;

/**
 * 数据集成任务节点关系Controller
 *
 * @author DATAMASTER
 * @date 2025-02-13
 */
@Tag(name = "数据集成任务节点关系")
@RestController
@RequestMapping("/col/etlTaskNodeRel")
@Validated
public class CollectorEtlTaskNodeRelController extends BaseController {
    @Resource
    private ICollectorEtlTaskNodeRelService CollectorEtlTaskNodeRelService;

    @Operation(summary = "查询数据集成任务节点关系列表")
//    @PreAuthorize("@ss.hasPermi('dpp:etlTaskNodeRel:list')")
    @GetMapping("/list")
    public CommonResult<PageResult<CollectorEtlTaskNodeRelRespVO>> list(CollectorEtlTaskNodeRelPageReqVO CollectorEtlTaskNodeRel) {
        PageResult<CollectorEtlTaskNodeRelDO> page = CollectorEtlTaskNodeRelService.getCollectorEtlTaskNodeRelPage(CollectorEtlTaskNodeRel);
        return CommonResult.success(BeanUtils.toBean(page, CollectorEtlTaskNodeRelRespVO.class));
    }

    @Operation(summary = "导出数据集成任务节点关系列表")
//    @PreAuthorize("@ss.hasPermi('dpp:etlTaskNodeRel:export')")
    @Log(title = "数据集成任务节点关系", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, CollectorEtlTaskNodeRelPageReqVO exportReqVO) {
        exportReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<CollectorEtlTaskNodeRelDO> list = (List<CollectorEtlTaskNodeRelDO>) CollectorEtlTaskNodeRelService.getCollectorEtlTaskNodeRelPage(exportReqVO).getRows();
        ExcelUtil<CollectorEtlTaskNodeRelRespVO> util = new ExcelUtil<>(CollectorEtlTaskNodeRelRespVO.class);
        util.exportExcel(response, CollectorEtlTaskNodeRelConvert.INSTANCE.convertToRespVOList(list), "应用管理数据");
    }

    @Operation(summary = "导入数据集成任务节点关系列表")
//    @PreAuthorize("@ss.hasPermi('dpp:etlTaskNodeRel:import')")
    @Log(title = "数据集成任务节点关系", businessType = BusinessType.IMPORT)
    @PostMapping("/importData")
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception {
        ExcelUtil<CollectorEtlTaskNodeRelRespVO> util = new ExcelUtil<>(CollectorEtlTaskNodeRelRespVO.class);
        List<CollectorEtlTaskNodeRelRespVO> importExcelList = util.importExcel(file.getInputStream());
        String operName = getUsername();
        String message = CollectorEtlTaskNodeRelService.importCollectorEtlTaskNodeRel(importExcelList, updateSupport, operName);
        return success(message);
    }

    @Operation(summary = "获取数据集成任务节点关系详细信息")
//    @PreAuthorize("@ss.hasPermi('dpp:etlTaskNodeRel:query')")
    @GetMapping(value = "/{id}")
    public CommonResult<CollectorEtlTaskNodeRelRespVO> getInfo(@PathVariable("id") Long id) {
        CollectorEtlTaskNodeRelDO CollectorEtlTaskNodeRelDO = CollectorEtlTaskNodeRelService.getCollectorEtlTaskNodeRelById(id);
        return CommonResult.success(BeanUtils.toBean(CollectorEtlTaskNodeRelDO, CollectorEtlTaskNodeRelRespVO.class));
    }

    @Operation(summary = "新增数据集成任务节点关系")
//    @PreAuthorize("@ss.hasPermi('dpp:etlTaskNodeRel:add')")
    @Log(title = "数据集成任务节点关系", businessType = BusinessType.INSERT)
    @PostMapping
    public CommonResult<Long> add(@Valid @RequestBody CollectorEtlTaskNodeRelSaveReqVO CollectorEtlTaskNodeRel) {
        CollectorEtlTaskNodeRel.setCreatorId(getUserId());
        CollectorEtlTaskNodeRel.setCreateBy(getNickName());
        CollectorEtlTaskNodeRel.setCreateTime(DateUtil.date());
        return CommonResult.toAjax(CollectorEtlTaskNodeRelService.createCollectorEtlTaskNodeRel(CollectorEtlTaskNodeRel));
    }

    @Operation(summary = "修改数据集成任务节点关系")
//    @PreAuthorize("@ss.hasPermi('dpp:etlTaskNodeRel:edit')")
    @Log(title = "数据集成任务节点关系", businessType = BusinessType.UPDATE)
    @PutMapping
    public CommonResult<Integer> edit(@Valid @RequestBody CollectorEtlTaskNodeRelSaveReqVO CollectorEtlTaskNodeRel) {
        CollectorEtlTaskNodeRel.setUpdatorId(getUserId());
        CollectorEtlTaskNodeRel.setUpdateBy(getNickName());
        CollectorEtlTaskNodeRel.setUpdateTime(DateUtil.date());
        return CommonResult.toAjax(CollectorEtlTaskNodeRelService.updateCollectorEtlTaskNodeRel(CollectorEtlTaskNodeRel));
    }

    @Operation(summary = "删除数据集成任务节点关系")
//    @PreAuthorize("@ss.hasPermi('dpp:etlTaskNodeRel:remove')")
    @Log(title = "数据集成任务节点关系", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public CommonResult<Integer> remove(@PathVariable Long[] ids) {
        return CommonResult.toAjax(CollectorEtlTaskNodeRelService.removeCollectorEtlTaskNodeRel(Arrays.asList(ids)));
    }

}
