

package com.datamaster.module.collector.controller.admin.etl;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import com.datamaster.api.ds.api.service.etl.IDsEtlExecutorService;
import com.datamaster.common.core.domain.AjaxResult;
import com.datamaster.common.core.domain.CommonResult;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.common.enums.ExecuteType;
import com.datamaster.common.utils.object.BeanUtils;
import com.datamaster.module.collector.controller.admin.etl.vo.CollectorEtlNodePageReqVO;
import com.datamaster.module.collector.controller.admin.etl.vo.CollectorEtlNodeRespVO;
import com.datamaster.module.collector.dal.dataobject.etl.CollectorEtlNodeDO;
import com.datamaster.module.collector.service.etl.ICollectorEtlTaskInstanceService;

import javax.annotation.Resource;

/**
 * <P>
 * 用途:
 * </p>
 *
 * @author: FXB
 * @create: 2025-03-27 14:39
 **/
@Tag(name = "调度执行")
@RestController
@RequestMapping("/col/etlExecutors")
public class CollectorEtlExecutorController {

    @Resource
    private ICollectorEtlTaskInstanceService CollectorEtlTaskInstanceService;

    @Operation(summary = "执行命令")
    @PostMapping("/execute/{taskInstanceId}/{executeType}")
    public AjaxResult execute(@PathVariable("taskInstanceId") Long taskInstanceId, @PathVariable("executeType") ExecuteType executeType) {
        return CollectorEtlTaskInstanceService.execute(taskInstanceId, executeType);
    }

}
