package com.datamaster.module.collector.controller.admin.etl;

import com.datamaster.common.core.domain.CommonResult;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.collector.controller.admin.etl.vo.CollectorEtlTaskOpsPolicySaveReqVO;
import com.datamaster.module.collector.dal.dataobject.etl.CollectorEtlTaskOpsEventDO;
import com.datamaster.module.collector.dal.dataobject.etl.CollectorEtlTaskOpsPolicyDO;
import com.datamaster.module.collector.service.etl.ICollectorEtlTaskOpsEventService;
import com.datamaster.module.collector.service.etl.ICollectorEtlTaskOpsPolicyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@Tag(name = "数据任务运维托管")
@RestController
@RequestMapping("/col/etlTaskOps")
@Validated
public class CollectorEtlTaskOpsController {

    @Resource
    private ICollectorEtlTaskOpsPolicyService policyService;
    @Resource
    private ICollectorEtlTaskOpsEventService eventService;

    @Operation(summary = "获取任务运维策略")
    @GetMapping("/policy")
    public CommonResult<CollectorEtlTaskOpsPolicyDO> getPolicy(@RequestParam Long taskId) {
        return CommonResult.success(policyService.getByTaskId(taskId));
    }

    @Operation(summary = "保存任务运维策略")
    @PostMapping("/policy")
    public CommonResult<CollectorEtlTaskOpsPolicyDO> savePolicy(@RequestBody CollectorEtlTaskOpsPolicySaveReqVO reqVO) {
        return CommonResult.success(policyService.saveOrUpdatePolicy(reqVO));
    }

    @Operation(summary = "查询任务运维事件")
    @GetMapping("/events")
    public CommonResult<PageResult<CollectorEtlTaskOpsEventDO>> events(@RequestParam(required = false) Long taskId,
                                                                       @RequestParam(required = false) Integer pageNum,
                                                                       @RequestParam(required = false) Integer pageSize) {
        return CommonResult.success(eventService.pageByTask(taskId, pageNum, pageSize));
    }
}
