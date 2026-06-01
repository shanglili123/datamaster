

package com.datamaster.module.collector.controller.admin.etl;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;

import cn.hutool.core.date.DateUtil;

import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.datamaster.common.core.domain.AjaxResult;
import com.datamaster.common.core.domain.ReturnT;
import com.datamaster.common.core.page.PageParam;
import com.datamaster.common.annotation.Log;
import com.datamaster.common.core.controller.BaseController;
import com.datamaster.common.core.domain.CommonResult;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.common.enums.BusinessType;
import com.datamaster.common.utils.object.BeanUtils;
import com.datamaster.common.utils.poi.ExcelUtil;
import com.datamaster.common.exception.enums.GlobalErrorCodeConstants;
import com.datamaster.module.collector.api.etl.dto.CollectorEtlTaskInstanceLogStatusRespDTO;
import com.datamaster.module.collector.controller.admin.etl.vo.CollectorEtlNodeInstancePageReqVO;
import com.datamaster.module.collector.controller.admin.etl.vo.CollectorEtlNodeInstanceRespVO;
import com.datamaster.module.collector.controller.admin.etl.vo.CollectorEtlNodeInstanceSaveReqVO;
import com.datamaster.module.collector.convert.etl.CollectorEtlNodeInstanceConvert;
import com.datamaster.module.collector.dal.dataobject.etl.CollectorEtlNodeInstanceDO;
import com.datamaster.module.collector.service.etl.ICollectorEtlNodeInstanceLogService;
import com.datamaster.module.collector.service.etl.ICollectorEtlNodeInstanceService;
import com.datamaster.module.collector.utils.TaskConverter;
import com.datamaster.redis.service.IRedisService;

/**
 * 数据集成节点实例Controller
 *
 * @author DATAMASTER
 * @date 2025-02-13
 */
@Tag(name = "数据集成节点实例")
@RestController
@RequestMapping("/col/etlNodeInstance")
@Validated
public class CollectorEtlNodeInstanceController extends BaseController {
    @Resource
    private ICollectorEtlNodeInstanceService CollectorEtlNodeInstanceService;

    @Resource
    private IRedisService redisService;

    @Resource
    private ICollectorEtlNodeInstanceLogService CollectorEtlNodeInstanceLogService;

    @Operation(summary = "查询数据集成节点实例列表")
//    @PreAuthorize("@ss.hasPermi('dpp:etlNodeInstance:list')")
    @GetMapping("/list")
    public CommonResult<PageResult<CollectorEtlNodeInstanceRespVO>> list(CollectorEtlNodeInstancePageReqVO CollectorEtlNodeInstance) {
        PageResult<CollectorEtlNodeInstanceDO> page = CollectorEtlNodeInstanceService.getCollectorEtlNodeInstancePage(CollectorEtlNodeInstance);
        return CommonResult.success(BeanUtils.toBean(page, CollectorEtlNodeInstanceRespVO.class));
    }

    @Operation(summary = "导出数据集成节点实例列表")
//    @PreAuthorize("@ss.hasPermi('dpp:etlNodeInstance:export')")
    @Log(title = "数据集成节点实例", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, CollectorEtlNodeInstancePageReqVO exportReqVO) {
        exportReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<CollectorEtlNodeInstanceDO> list = (List<CollectorEtlNodeInstanceDO>) CollectorEtlNodeInstanceService.getCollectorEtlNodeInstancePage(exportReqVO).getRows();
        ExcelUtil<CollectorEtlNodeInstanceRespVO> util = new ExcelUtil<>(CollectorEtlNodeInstanceRespVO.class);
        util.exportExcel(response, CollectorEtlNodeInstanceConvert.INSTANCE.convertToRespVOList(list), "应用管理数据");
    }

    @Operation(summary = "导入数据集成节点实例列表")
//    @PreAuthorize("@ss.hasPermi('dpp:etlNodeInstance:import')")
    @Log(title = "数据集成节点实例", businessType = BusinessType.IMPORT)
    @PostMapping("/importData")
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception {
        ExcelUtil<CollectorEtlNodeInstanceRespVO> util = new ExcelUtil<>(CollectorEtlNodeInstanceRespVO.class);
        List<CollectorEtlNodeInstanceRespVO> importExcelList = util.importExcel(file.getInputStream());
        String operName = getUsername();
        String message = CollectorEtlNodeInstanceService.importCollectorEtlNodeInstance(importExcelList, updateSupport, operName);
        return success(message);
    }

    @Operation(summary = "获取数据集成节点实例详细信息")
//    @PreAuthorize("@ss.hasPermi('dpp:etlNodeInstance:query')")
    @GetMapping(value = "/{id}")
    public CommonResult<CollectorEtlNodeInstanceRespVO> getInfo(@PathVariable("id") Long id) {
        CollectorEtlNodeInstanceDO CollectorEtlNodeInstanceDO = CollectorEtlNodeInstanceService.getCollectorEtlNodeInstanceById(id);
        return CommonResult.success(BeanUtils.toBean(CollectorEtlNodeInstanceDO, CollectorEtlNodeInstanceRespVO.class));
    }

    @Operation(summary = "新增数据集成节点实例")
//    @PreAuthorize("@ss.hasPermi('dpp:etlNodeInstance:add')")
    @Log(title = "数据集成节点实例", businessType = BusinessType.INSERT)
    @PostMapping
    public CommonResult<Long> add(@Valid @RequestBody CollectorEtlNodeInstanceSaveReqVO CollectorEtlNodeInstance) {
        CollectorEtlNodeInstance.setCreatorId(getUserId());
        CollectorEtlNodeInstance.setCreateBy(getNickName());
        CollectorEtlNodeInstance.setCreateTime(DateUtil.date());
        return CommonResult.toAjax(CollectorEtlNodeInstanceService.createCollectorEtlNodeInstance(CollectorEtlNodeInstance));
    }

    @Operation(summary = "修改数据集成节点实例")
//    @PreAuthorize("@ss.hasPermi('dpp:etlNodeInstance:edit')")
    @Log(title = "数据集成节点实例", businessType = BusinessType.UPDATE)
    @PutMapping
    public CommonResult<Integer> edit(@Valid @RequestBody CollectorEtlNodeInstanceSaveReqVO CollectorEtlNodeInstance) {
        CollectorEtlNodeInstance.setUpdatorId(getUserId());
        CollectorEtlNodeInstance.setUpdateBy(getNickName());
        CollectorEtlNodeInstance.setUpdateTime(DateUtil.date());
        return CommonResult.toAjax(CollectorEtlNodeInstanceService.updateCollectorEtlNodeInstance(CollectorEtlNodeInstance));
    }

    @Operation(summary = "删除数据集成节点实例")
//    @PreAuthorize("@ss.hasPermi('dpp:etlNodeInstance:remove')")
    @Log(title = "数据集成节点实例", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public CommonResult<Integer> remove(@PathVariable Long[] ids) {
        return CommonResult.toAjax(CollectorEtlNodeInstanceService.removeCollectorEtlNodeInstance(Arrays.asList(ids)));
    }

    @Operation(summary = "查看日志详情")
//    @PreAuthorize("@ss.hasPermi('dpp:etlNodeInstance:query')")
    @GetMapping(value = "/log/{id}")
    public AjaxResult getLogInfo(@PathVariable("id") Long id) {
        CollectorEtlNodeInstanceDO CollectorEtlNodeInstanceDO = CollectorEtlNodeInstanceService.getCollectorEtlNodeInstanceById(id);
        String content = "";
        String taskInstanceLogKey = TaskConverter.TASK_INSTANCE_LOG_KEY+ CollectorEtlNodeInstanceDO.getId();
        if (redisService.hasKey(taskInstanceLogKey)) {
            content += redisService.get(taskInstanceLogKey) + "\n";
        } else {
            //获取表中的日志
            String logContent = CollectorEtlNodeInstanceLogService.getLog(CollectorEtlNodeInstanceDO.getId());
            if (logContent != null) {
                content += logContent + "\n";
            }
        }
        return AjaxResult.success(content);
    }

    @RequestMapping(value = "/downloadLog", method = RequestMethod.POST)
    @Operation(summary = "下载日志文件")
    public void downloadLog(HttpServletResponse response, Long nodeInstanceId,String name) {
        try {
            // 获取日志
            String log = CollectorEtlNodeInstanceService.getLogByNodeInstanceId(nodeInstanceId);
            // 如果文件存在
            // 设置响应的内容类型为文件下载
            response.setContentType("application/octet-stream");
            // 设置下载文件名
            response.setHeader("Content-Disposition", "attachment;filename=" + name + ".log");

            // 创建文件输入流
            try (InputStream in = new ByteArrayInputStream(log.getBytes("UTF-8"));
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

}
