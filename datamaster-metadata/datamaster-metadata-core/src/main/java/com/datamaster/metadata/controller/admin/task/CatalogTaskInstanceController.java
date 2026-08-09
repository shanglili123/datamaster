package com.datamaster.metadata.controller.admin.task;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.datamaster.common.core.controller.BaseController;
import com.datamaster.common.core.domain.CommonResult;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.common.utils.object.BeanUtils;
import com.datamaster.metadata.controller.admin.task.vo.CatalogTaskInstancePageReqVO;
import com.datamaster.metadata.controller.admin.task.vo.CatalogTaskInstanceRespVO;
import com.datamaster.metadata.controller.admin.task.vo.CatalogTaskRespVO;
import com.datamaster.metadata.dal.dataobject.task.CatalogTaskInstanceDO;
import com.datamaster.metadata.dal.dataobject.task.CatalogTaskInstanceLogDO;
import com.datamaster.metadata.service.task.ICatalogTaskInstanceLogService;
import com.datamaster.metadata.service.task.ICatalogTaskInstanceService;
import com.datamaster.metadata.service.task.ICatalogTaskService;

import javax.annotation.Resource;

/**
 * 采集任务实例Controller
 *
 * @author DATAMASTER
 * @date 2026-08-04
 */
@Tag(name = "采集任务实例")
@RestController
@RequestMapping("/cat/taskInstance")
@Validated
public class CatalogTaskInstanceController extends BaseController {

    @Resource
    private ICatalogTaskInstanceService catalogTaskInstanceService;

    @Resource
    private ICatalogTaskInstanceLogService catalogTaskInstanceLogService;

    @Resource
    private ICatalogTaskService catalogTaskService;

    /**
     * 查询采集任务实例列表
     */
    @Operation(summary = "查询采集任务实例列表")
    @PreAuthorize("@ss.hasPermi('cat:taskInstance:list')")
    @GetMapping("/list")
    public CommonResult<PageResult<CatalogTaskInstanceRespVO>> list(CatalogTaskInstancePageReqVO reqVO) {
        PageResult<CatalogTaskInstanceDO> page = catalogTaskInstanceService.getCatalogTaskInstancePage(reqVO);
        return CommonResult.success(BeanUtils.toBean(page, CatalogTaskInstanceRespVO.class));
    }

    /**
     * 获取采集任务实例详细信息
     */
    @Operation(summary = "获取采集任务实例详细信息")
    @PreAuthorize("@ss.hasPermi('cat:taskInstance:info')")
    @GetMapping(value = "/{id}")
    public CommonResult<CatalogTaskInstanceRespVO> getInfo(@PathVariable("id") Long id) {
        CatalogTaskInstanceDO instance = catalogTaskInstanceService.getCatalogTaskInstanceById(id);
        if (instance == null) {
            return CommonResult.error(500, "采集任务实例不存在");
        }
        if (instance.getTaskId() != null) {
            CatalogTaskRespVO task = catalogTaskService.getCatalogTaskByIdNew(instance.getTaskId());
            if (task != null) {
                instance.setName(task.getName());
                instance.setTaskStatus(task.getStatus());
                instance.setDatasourceName(task.getDatasourceName());
                instance.setDatasourceType(task.getDatasourceType());
            }
        }
        return CommonResult.success(BeanUtils.toBean(instance, CatalogTaskInstanceRespVO.class));
    }

    /**
     * 获取采集任务实例日志内容
     */
    @Operation(summary = "获取采集任务实例日志内容")
    @PreAuthorize("@ss.hasPermi('cat:taskInstance:log')")
    @GetMapping(value = "/log/{id}")
    public CommonResult<String> getLog(@PathVariable("id") Long id) {
        CatalogTaskInstanceLogDO logDO = catalogTaskInstanceLogService.lambdaQuery()
                .eq(CatalogTaskInstanceLogDO::getTaskInstanceId, id)
                .orderByDesc(CatalogTaskInstanceLogDO::getCreateTime)
                .last("LIMIT 1")
                .one();
        return CommonResult.success(logDO == null ? "" : logDO.getLogContent());
    }
}
