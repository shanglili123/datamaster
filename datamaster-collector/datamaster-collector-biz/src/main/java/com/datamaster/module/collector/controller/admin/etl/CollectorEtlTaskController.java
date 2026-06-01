

package com.datamaster.module.collector.controller.admin.etl;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.Arrays;

import java.util.List;
import java.util.Map;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import com.datamaster.common.core.domain.AjaxResult;
import com.datamaster.common.annotation.Log;
import com.datamaster.common.core.controller.BaseController;
import com.datamaster.common.core.domain.CommonResult;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.common.enums.BusinessType;
import com.datamaster.common.utils.object.BeanUtils;
import com.datamaster.module.collector.controller.admin.etl.vo.*;
import com.datamaster.module.collector.dal.dataobject.etl.CollectorEtlTaskDO;
import com.datamaster.module.collector.service.etl.ICollectorEtlTaskService;

/**
 * 数据集成任务Controller
 *
 * @author DATAMASTER
 * @date 2025-02-13
 */
@Tag(name = "数据集成任务")
@RestController
@RequestMapping("/col/etlTask")
@Validated
public class CollectorEtlTaskController extends BaseController {
    @Resource
    private ICollectorEtlTaskService CollectorEtlTaskService;

    @Operation(summary = "查询数据集成任务列表")
//    @PreAuthorize("@ss.hasPermi('dpp:etlTask:list')")
    @GetMapping("/list")
    public CommonResult<PageResult<CollectorEtlTaskRespVO>> list(CollectorEtlTaskPageReqVO CollectorEtlTask) {
        if (StringUtils.isBlank(CollectorEtlTask.getType())) {
            CollectorEtlTask.setType("1");//默认离线数据集成
        }
        PageResult<CollectorEtlTaskDO> page = CollectorEtlTaskService.getCollectorEtlTaskPage(CollectorEtlTask);
        return CommonResult.success(BeanUtils.toBean(page, CollectorEtlTaskRespVO.class));
    }

    @Operation(summary = "查询数据集成任务列表")
//    @PreAuthorize("@ss.hasPermi('dpp:etlTask:list')")
    @GetMapping("/getCollectorEtlTaskPage")
    public CommonResult<PageResult<CollectorEtlTaskRespVO>> getCollectorEtlTaskPageList(CollectorEtlTaskPageReqVO CollectorEtlTask) {
        if (StringUtils.isBlank(CollectorEtlTask.getType())) {
            CollectorEtlTask.setType("1");//默认离线数据集成
        }
        return CommonResult.success(CollectorEtlTaskService.getCollectorEtlTaskPageList(CollectorEtlTask));
    }


    @Operation(summary = "删除数据集成任务")
//    @PreAuthorize("@ss.hasPermi('dpp:etlTask:remove')")
    @Log(title = "数据集成任务", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public CommonResult<Integer> remove(@PathVariable Long[] ids) {
        return CommonResult.toAjax(CollectorEtlTaskService.removeCollectorEtlTask(Arrays.asList(ids)));
    }

    @Operation(summary = "上下线")
    @PostMapping("/updateReleaseTask")
    @ResponseStatus(HttpStatus.CREATED)
    public CommonResult releaseTask(@Valid @RequestBody CollectorEtlNewNodeSaveReqVO CollectorEtlNewNodeSaveReqVO) {
        Map<String, Object> result = CollectorEtlTaskService.updateReleaseTask(CollectorEtlNewNodeSaveReqVO);
        return CommonResult.success(result);
    }


    @Operation(summary = "上下线-任务")
    @PostMapping("/updateReleaseJobTask")
    @ResponseStatus(HttpStatus.CREATED)
    public CommonResult updateReleaseJobTask(@Valid @RequestBody CollectorEtlNewNodeSaveReqVO CollectorEtlNewNodeSaveReqVO) {
        Map<String, Object> result = CollectorEtlTaskService.updateReleaseJobTask(CollectorEtlNewNodeSaveReqVO);
        return CommonResult.success(result);
    }

    @Operation(summary = "上下线-调度")
    @PostMapping("/updateReleaseSchedule")
    @ResponseStatus(HttpStatus.CREATED)
    public CommonResult updateReleaseSchedule(@Valid @RequestBody CollectorEtlNewNodeSaveReqVO CollectorEtlNewNodeSaveReqVO) {
        Map<String, Object> result = CollectorEtlTaskService.updateReleaseSchedule(CollectorEtlNewNodeSaveReqVO);
        return CommonResult.success(result);
    }

    @Operation(summary = "修改调度cron表达式")
    @PostMapping("/releaseTaskCrontab")
    @ResponseStatus(HttpStatus.CREATED)
    public CommonResult releaseTaskCrontab(@Valid @RequestBody CollectorEtlNewNodeSaveReqVO CollectorEtlNewNodeSaveReqVO) {
        Map<String, Object> result = CollectorEtlTaskService.releaseTaskCrontab(CollectorEtlNewNodeSaveReqVO);
        return CommonResult.success(result);
    }

    @Operation(summary = "新增数据集成节点-获取唯一健")
//    @PreAuthorize("@ss.hasPermi('dpp:etlTask:add')")
    @GetMapping("/getNodeUniqueKey")
    public CommonResult<Long> getNodeUniqueKey(CollectorEtlNewNodeSaveReqVO CollectorEtlNewNodeSaveReqVO) {
        return CommonResult.success(CollectorEtlTaskService.getNodeUniqueKey(CollectorEtlNewNodeSaveReqVO));
    }

    @Operation(summary = "新增数据集成节点-获取本地临时唯一键")
    @GetMapping("/getLocalNodeUniqueKey")
    public CommonResult<Long> getLocalNodeUniqueKey() {
        return CommonResult.success(CollectorEtlTaskService.getLocalNodeUniqueKey());
    }

    @Operation(summary = "获取数据集成任务详细信息")
//    @PreAuthorize("@ss.hasPermi('dpp:etlTask:query')")
    @GetMapping(value = "/{id}")
    public CommonResult<CollectorEtlTaskRespVO> getInfo(@PathVariable("id") Long id) {
        return CommonResult.success(CollectorEtlTaskService.getCollectorEtlTaskById(id));
    }


    @Operation(summary = "获取数据集成任务详细信息")
//    @PreAuthorize("@ss.hasPermi('dpp:etlTask:query')")
    @GetMapping(value = "/updateQuery/{id}")
    public CommonResult<CollectorEtlTaskUpdateQueryRespVO> getuUpdateQueryInfo(@PathVariable("id") Long id) {
        return CommonResult.success(CollectorEtlTaskService.getuUpdateQueryInfo(id));
    }


    @Operation(summary = "查询数据集成任务列表")
//    @PreAuthorize("@ss.hasPermi('dpp:etlTask:list')")
    @GetMapping("/getCollectorEtlTaskListTree")
    public CommonResult<List<CollectorEtlTaskTreeRespVO>> getCollectorEtlTaskListTree(CollectorEtlTaskPageReqVO CollectorEtlTask) {
        return CommonResult.success(CollectorEtlTaskService.getCollectorEtlTaskListTree(CollectorEtlTask));
    }


    @Operation(summary = "查询数据集成任务列表")
//    @PreAuthorize("@ss.hasPermi('dpp:etlTask:list')")
    @GetMapping("/getSubTaskStatusList")
    public CommonResult<List<CollectorEtlTaskRespVO>> getSubTaskStatusList(CollectorEtlTaskPageReqVO CollectorEtlTask) {
        return CommonResult.success(CollectorEtlTaskService.getSubTaskStatusList(CollectorEtlTask));
    }


    @Log(title = "触发一次定时任务", businessType = BusinessType.UPDATE)
    @PutMapping("/startCollectorEtlTask/{id}")
    public AjaxResult startCollectorEtlTask(@PathVariable("id") Long id) {
        return CollectorEtlTaskService.startCollectorEtlTask(id);
    }


    @Operation(summary = "修改数据汇聚任务")
    @PostMapping("/updateEtlTask")
    @ResponseStatus(HttpStatus.CREATED)
    public CommonResult updateEtlTask(@Valid @RequestBody CollectorEtlNewNodeSaveReqVO CollectorEtlNewNodeSaveReqVO) {
        CollectorEtlNewNodeSaveReqVO.setType("1");//默认离线数据集成
        CollectorEtlTaskSaveReqVO result = CollectorEtlTaskService.updateEtlTask(CollectorEtlNewNodeSaveReqVO);
        return CommonResult.success(result);
    }

    @Operation(summary = "新增数据汇聚任务")
    @PostMapping("/createEtlTaskFront")
    @ResponseStatus(HttpStatus.CREATED)
    public CommonResult createEtlTaskFront(@Valid @RequestBody CollectorEtlNewNodeSaveReqVO CollectorEtlNewNodeSaveReqVO) {
        if (StringUtils.isBlank(CollectorEtlNewNodeSaveReqVO.getType())) {
            CollectorEtlNewNodeSaveReqVO.setType("1");//默认离线数据集成
        }
        CollectorEtlNewNodeSaveReqVO result = CollectorEtlTaskService.createEtlTaskFront(CollectorEtlNewNodeSaveReqVO);
        return CommonResult.success(result);
    }

    @Operation(summary = "新增数据汇聚任务")
    @PostMapping("/createEtlTaskFrontPostposition")
    @ResponseStatus(HttpStatus.CREATED)
    public CommonResult createEtlTaskFrontPostposition(@Valid @RequestBody CollectorEtlNewNodeSaveReqVO CollectorEtlNewNodeSaveReqVO) {
        if (StringUtils.isBlank(CollectorEtlNewNodeSaveReqVO.getType())) {
            CollectorEtlNewNodeSaveReqVO.setType("1");//默认离线数据集成
        }
        CollectorEtlTaskSaveReqVO result = CollectorEtlTaskService.createEtlTaskFrontPostposition(CollectorEtlNewNodeSaveReqVO);
        return CommonResult.success(result);
    }

    @Operation(summary = "发布任务(创建/更新DS任务并上线)")
    @PostMapping("/publishTask")
    @ResponseStatus(HttpStatus.CREATED)
    public CommonResult publishTask(@Valid @RequestBody CollectorEtlNewNodeSaveReqVO reqVO) {
        if (StringUtils.isBlank(reqVO.getType())) {
            reqVO.setType("1");
        }
        CollectorEtlTaskSaveReqVO result = CollectorEtlTaskService.publishTask(reqVO);
        return CommonResult.success(result);
    }

    @Operation(summary = "卸载任务(从DS下线)")
    @PostMapping("/unpublishTask")
    @ResponseStatus(HttpStatus.CREATED)
    public CommonResult unpublishTask(@Valid @RequestBody CollectorEtlNewNodeSaveReqVO reqVO) {
        if (StringUtils.isBlank(reqVO.getType())) {
            reqVO.setType("1");
        }
        CollectorEtlTaskService.unpublishTask(reqVO);
        return CommonResult.success("卸载成功");
    }

    @Operation(summary = "复制数据汇聚任务")
    @PostMapping("/copyCreateEtl")
    @ResponseStatus(HttpStatus.CREATED)
    public CommonResult copyCreateEtl(@Valid @RequestBody CollectorEtlNewNodeSaveReqVO CollectorEtlNewNodeSaveReqVO) {
        if (StringUtils.isBlank(CollectorEtlNewNodeSaveReqVO.getType())) {
            CollectorEtlNewNodeSaveReqVO.setType("1");//默认离线数据集成
        }
        CollectorEtlTaskSaveReqVO result = CollectorEtlTaskService.copyCreateEtl(CollectorEtlNewNodeSaveReqVO);
        return CommonResult.success(result);
    }

    @Operation(summary = "获取数据集成任务详细信息--前置草稿任务")
//    @PreAuthorize("@ss.hasPermi('dpp:etlTask:query')")
    @GetMapping(value = "/updateQueryFront/{id}")
    public CommonResult<CollectorEtlTaskUpdateQueryRespVO> getupdateQueryFront(@PathVariable("id") Long id) {
        return CommonResult.success(CollectorEtlTaskService.getupdateQueryFront(id));
    }

}
