

package com.datamaster.module.collector.controller.admin.etl;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.io.*;
import java.util.Arrays;

import cn.hutool.core.date.DateUtil;

import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.commons.lang3.StringUtils;
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
import com.datamaster.module.collector.api.etl.dto.CollectorEtlTaskInstanceLogStatusRespDTO;
import com.datamaster.module.collector.controller.admin.etl.vo.*;
import com.datamaster.module.collector.convert.etl.CollectorEtlTaskInstanceConvert;
import com.datamaster.module.collector.dal.dataobject.etl.CollectorEtlTaskInstanceDO;
import com.datamaster.module.collector.service.etl.ICollectorEtlTaskInstanceService;

/**
 * 数据集成任务实例Controller
 *
 * @author DATAMASTER
 * @date 2025-02-13
 */
@Tag(name = "数据集成任务实例")
@RestController
@RequestMapping("/col/etlTaskInstance")
@Validated
public class CollectorEtlTaskInstanceController extends BaseController {
    @Resource
    private ICollectorEtlTaskInstanceService CollectorEtlTaskInstanceService;

    @Operation(summary = "查询数据集成任务实例列表")
//    @PreAuthorize("@ss.hasPermi('dpp:etlTaskInstance:list')")
    @GetMapping("/list")
    public CommonResult<PageResult<CollectorEtlTaskInstanceRespVO>> list(CollectorEtlTaskInstancePageReqVO CollectorEtlTaskInstance) {
        if (StringUtils.isNotBlank(CollectorEtlTaskInstance.getTaskType())) {
            CollectorEtlTaskInstance.setTaskType("1");//默认离线数据集成
        }
        PageResult<CollectorEtlTaskInstanceDO> page = CollectorEtlTaskInstanceService.getCollectorEtlTaskInstancePage(CollectorEtlTaskInstance);
        return CommonResult.success(BeanUtils.toBean(page, CollectorEtlTaskInstanceRespVO.class));
    }

    @Operation(summary = "导出数据集成任务实例列表")
//    @PreAuthorize("@ss.hasPermi('dpp:etlTaskInstance:export')")
    @Log(title = "数据集成任务实例", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, CollectorEtlTaskInstancePageReqVO exportReqVO) {
        exportReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<CollectorEtlTaskInstanceDO> list = (List<CollectorEtlTaskInstanceDO>) CollectorEtlTaskInstanceService.getCollectorEtlTaskInstancePage(exportReqVO).getRows();
        ExcelUtil<CollectorEtlTaskInstanceRespVO> util = new ExcelUtil<>(CollectorEtlTaskInstanceRespVO.class);
        util.exportExcel(response, CollectorEtlTaskInstanceConvert.INSTANCE.convertToRespVOList(list), "应用管理数据");
    }

    @Operation(summary = "导入数据集成任务实例列表")
//    @PreAuthorize("@ss.hasPermi('dpp:etlTaskInstance:import')")
    @Log(title = "数据集成任务实例", businessType = BusinessType.IMPORT)
    @PostMapping("/importData")
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception {
        ExcelUtil<CollectorEtlTaskInstanceRespVO> util = new ExcelUtil<>(CollectorEtlTaskInstanceRespVO.class);
        List<CollectorEtlTaskInstanceRespVO> importExcelList = util.importExcel(file.getInputStream());
        String operName = getUsername();
        String message = CollectorEtlTaskInstanceService.importCollectorEtlTaskInstance(importExcelList, updateSupport, operName);
        return success(message);
    }

    @Operation(summary = "获取数据集成任务实例详细信息")
//    @PreAuthorize("@ss.hasPermi('dpp:etlTaskInstance:query')")
    @GetMapping(value = "/{id}")
    public CommonResult<CollectorEtlTaskInstanceRespVO> getInfo(@PathVariable("id") Long id) {
        CollectorEtlTaskInstanceDO CollectorEtlTaskInstanceDO = CollectorEtlTaskInstanceService.getCollectorEtlTaskInstanceById(id);
        return CommonResult.success(BeanUtils.toBean(CollectorEtlTaskInstanceDO, CollectorEtlTaskInstanceRespVO.class));
    }

    @Operation(summary = "新增数据集成任务实例")
//    @PreAuthorize("@ss.hasPermi('dpp:etlTaskInstance:add')")
    @Log(title = "数据集成任务实例", businessType = BusinessType.INSERT)
    @PostMapping
    public CommonResult<Long> add(@Valid @RequestBody CollectorEtlTaskInstanceSaveReqVO CollectorEtlTaskInstance) {
        CollectorEtlTaskInstance.setCreatorId(getUserId());
        CollectorEtlTaskInstance.setCreateBy(getNickName());
        CollectorEtlTaskInstance.setCreateTime(DateUtil.date());
        return CommonResult.toAjax(CollectorEtlTaskInstanceService.createCollectorEtlTaskInstance(CollectorEtlTaskInstance));
    }

    @Operation(summary = "修改数据集成任务实例")
//    @PreAuthorize("@ss.hasPermi('dpp:etlTaskInstance:edit')")
    @Log(title = "数据集成任务实例", businessType = BusinessType.UPDATE)
    @PutMapping
    public CommonResult<Integer> edit(@Valid @RequestBody CollectorEtlTaskInstanceSaveReqVO CollectorEtlTaskInstance) {
        CollectorEtlTaskInstance.setUpdatorId(getUserId());
        CollectorEtlTaskInstance.setUpdateBy(getNickName());
        CollectorEtlTaskInstance.setUpdateTime(DateUtil.date());
        return CommonResult.toAjax(CollectorEtlTaskInstanceService.updateCollectorEtlTaskInstance(CollectorEtlTaskInstance));
    }

    @Operation(summary = "删除数据集成任务实例")
//    @PreAuthorize("@ss.hasPermi('dpp:etlTaskInstance:remove')")
    @Log(title = "数据集成任务实例", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public CommonResult<Integer> remove(@PathVariable Long[] ids) {
        return CommonResult.toAjax(CollectorEtlTaskInstanceService.removeCollectorEtlTaskInstance(Arrays.asList(ids)));
    }

    @Operation(summary = "获取数据集成任务实例列表")
    @GetMapping("/treeList")
    public CommonResult<PageResult<CollectorEtlTaskInstanceTreeListRespVO>> treeList(CollectorEtlTaskInstanceTreeListReqVO CollectorEtlTaskInstance) {
        return CommonResult.success(CollectorEtlTaskInstanceService.treeList(CollectorEtlTaskInstance));
    }

    @Operation(summary = "获取子任务列表")
    @GetMapping("/subNodeList")
    public CommonResult<List<CollectorEtlTaskInstanceTreeListRespVO>> subNodelist(@RequestParam Long taskInstanceId, @RequestParam Long nodeInstanceId) {
        return CommonResult.success(CollectorEtlTaskInstanceService.subNodelist(taskInstanceId, nodeInstanceId));
    }

    @Operation(summary = "获取正在运行的实例")
    @GetMapping("/getRunTaskInstance")
    public CommonResult<Long> getRunTaskInstance(@RequestParam Long taskId) {
        return CommonResult.success(CollectorEtlTaskInstanceService.getRunTaskInstance(taskId));
    }

    @Operation(summary = "通过实例id获取日志")
    @GetMapping("/getLogByTaskInstanceId")
    public CommonResult<CollectorEtlTaskInstanceLogStatusRespDTO> getLogByTaskInstanceId(@RequestParam Long taskInstanceId) {
        return CommonResult.success(CollectorEtlTaskInstanceService.getLogByTaskInstanceId(taskInstanceId));
    }

    @RequestMapping(value = "/downloadLog", method = RequestMethod.POST)
    @Operation(summary = "下载日志文件")
    public void downloadLog(HttpServletResponse response, Long taskInstanceId, String name) {
        try {
            // 获取文件路径
            CollectorEtlTaskInstanceLogStatusRespDTO dto = CollectorEtlTaskInstanceService.getLogByTaskInstanceId(taskInstanceId);
            // 如果文件存在
            // 设置响应的内容类型为文件下载
            response.setContentType("application/octet-stream");
            // 设置下载文件名
            response.setHeader("Content-Disposition", "attachment;filename=" + name + ".log");

            // 创建文件输入流
            try (InputStream in = new ByteArrayInputStream(dto.getLog().getBytes("UTF-8"));
                 OutputStream out = response.getOutputStream()) {
                byte[] buffer = new byte[1024];
                int length;
                // 将文件内容写入输出流
                while ((length = in.read(buffer)) != -1) {
                    out.write(buffer, 0, length);
                }
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            try {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                response.getWriter().write("文件下载失败：" + e.getMessage());
            } catch (IOException ioException) {
                logger.error("写入错误信息失败", ioException);
            }
        }
    }

    @Operation(summary = "根据任务实例id获取数据集成任务详细信息")
    @GetMapping(value = "/getTaskInfo/{id}")
    public CommonResult<CollectorEtlTaskUpdateQueryRespVO> getTaskInfo(@PathVariable("id") Long id) {
        return CommonResult.success(CollectorEtlTaskInstanceService.getTaskInfo(id));
    }

}
