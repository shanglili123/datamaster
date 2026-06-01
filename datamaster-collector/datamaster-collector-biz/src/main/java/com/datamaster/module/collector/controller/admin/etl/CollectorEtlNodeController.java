

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
import com.datamaster.module.collector.controller.admin.etl.vo.CollectorEtlNodePageReqVO;
import com.datamaster.module.collector.controller.admin.etl.vo.CollectorEtlNodeRespVO;
import com.datamaster.module.collector.controller.admin.etl.vo.CollectorEtlNodeSaveReqVO;
import com.datamaster.module.collector.convert.etl.CollectorEtlNodeConvert;
import com.datamaster.module.collector.dal.dataobject.etl.CollectorEtlNodeDO;
import com.datamaster.module.collector.service.etl.ICollectorEtlNodeService;

/**
 * 数据集成节点Controller
 *
 * @author DATAMASTER
 * @date 2025-02-13
 */
@Tag(name = "数据集成节点")
@RestController
@RequestMapping("/col/etlNode")
@Validated
public class CollectorEtlNodeController extends BaseController {
    @Resource
    private ICollectorEtlNodeService CollectorEtlNodeService;

    @Operation(summary = "查询数据集成节点列表")
//    @PreAuthorize("@ss.hasPermi('dpp:etlNode:list')")
    @GetMapping("/list")
    public CommonResult<PageResult<CollectorEtlNodeRespVO>> list(CollectorEtlNodePageReqVO CollectorEtlNode) {
        PageResult<CollectorEtlNodeDO> page = CollectorEtlNodeService.getCollectorEtlNodePage(CollectorEtlNode);
        return CommonResult.success(BeanUtils.toBean(page, CollectorEtlNodeRespVO.class));
    }

    @Operation(summary = "导出数据集成节点列表")
//    @PreAuthorize("@ss.hasPermi('dpp:etlNode:export')")
    @Log(title = "数据集成节点", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, CollectorEtlNodePageReqVO exportReqVO) {
        exportReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<CollectorEtlNodeDO> list = (List<CollectorEtlNodeDO>) CollectorEtlNodeService.getCollectorEtlNodePage(exportReqVO).getRows();
        ExcelUtil<CollectorEtlNodeRespVO> util = new ExcelUtil<>(CollectorEtlNodeRespVO.class);
        util.exportExcel(response, CollectorEtlNodeConvert.INSTANCE.convertToRespVOList(list), "应用管理数据");
    }

    @Operation(summary = "导入数据集成节点列表")
//    @PreAuthorize("@ss.hasPermi('dpp:etlNode:import')")
    @Log(title = "数据集成节点", businessType = BusinessType.IMPORT)
    @PostMapping("/importData")
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception {
        ExcelUtil<CollectorEtlNodeRespVO> util = new ExcelUtil<>(CollectorEtlNodeRespVO.class);
        List<CollectorEtlNodeRespVO> importExcelList = util.importExcel(file.getInputStream());
        String operName = getUsername();
        String message = CollectorEtlNodeService.importCollectorEtlNode(importExcelList, updateSupport, operName);
        return success(message);
    }

    @Operation(summary = "获取数据集成节点详细信息")
//    @PreAuthorize("@ss.hasPermi('dpp:etlNode:query')")
    @GetMapping(value = "/{id}")
    public CommonResult<CollectorEtlNodeRespVO> getInfo(@PathVariable("id") Long id) {
        CollectorEtlNodeDO CollectorEtlNodeDO = CollectorEtlNodeService.getCollectorEtlNodeById(id);
        return CommonResult.success(BeanUtils.toBean(CollectorEtlNodeDO, CollectorEtlNodeRespVO.class));
    }

    @Operation(summary = "新增数据集成节点")
//    @PreAuthorize("@ss.hasPermi('dpp:etlNode:add')")
    @Log(title = "数据集成节点", businessType = BusinessType.INSERT)
    @PostMapping
    public CommonResult<Long> add(@Valid @RequestBody CollectorEtlNodeSaveReqVO CollectorEtlNode) {
        CollectorEtlNode.setCreatorId(getUserId());
        CollectorEtlNode.setCreateBy(getNickName());
        CollectorEtlNode.setCreateTime(DateUtil.date());
        return CommonResult.toAjax(CollectorEtlNodeService.createCollectorEtlNode(CollectorEtlNode));
    }

    @Operation(summary = "修改数据集成节点")
//    @PreAuthorize("@ss.hasPermi('dpp:etlNode:edit')")
    @Log(title = "数据集成节点", businessType = BusinessType.UPDATE)
    @PutMapping
    public CommonResult<Integer> edit(@Valid @RequestBody CollectorEtlNodeSaveReqVO CollectorEtlNode) {
        CollectorEtlNode.setUpdatorId(getUserId());
        CollectorEtlNode.setUpdateBy(getNickName());
        CollectorEtlNode.setUpdateTime(DateUtil.date());
        return CommonResult.toAjax(CollectorEtlNodeService.updateCollectorEtlNode(CollectorEtlNode));
    }

    @Operation(summary = "删除数据集成节点")
//    @PreAuthorize("@ss.hasPermi('dpp:etlNode:remove')")
    @Log(title = "数据集成节点", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public CommonResult<Integer> remove(@PathVariable Long[] ids) {
        return CommonResult.toAjax(CollectorEtlNodeService.removeCollectorEtlNode(Arrays.asList(ids)));
    }
}
