package com.datamaster.metadata.controller.admin.task;

import cn.hutool.core.date.DateUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import com.datamaster.common.annotation.Log;
import com.datamaster.common.core.controller.BaseController;
import com.datamaster.common.core.domain.AjaxResult;
import com.datamaster.common.core.domain.CommonResult;
import com.datamaster.common.enums.BusinessType;
import com.datamaster.metadata.controller.admin.task.vo.CatalogTaskQualityBindReqVO;
import com.datamaster.metadata.controller.admin.task.vo.CatalogTaskQualityRespVO;
import com.datamaster.metadata.service.task.ICatalogTaskQualityService;

import javax.annotation.Resource;
import java.util.List;

/**
 * 探查任务-质量探查任务关联Controller
 *
 * @author DATAMASTER
 * @date 2026-07-31
 */
@Tag(name = "探查任务-质量任务关联")
@RestController
@RequestMapping("/cat/taskQuality")
@Validated
public class CatalogTaskQualityController extends BaseController {

    @Resource
    private ICatalogTaskQualityService catalogTaskQualityService;

    @Operation(summary = "查询探查任务绑定的质量探查任务列表")
    @GetMapping("/listByTask/{catTaskId}")
    public CommonResult<List<CatalogTaskQualityRespVO>> listByTask(@PathVariable("catTaskId") Long catTaskId) {
        return CommonResult.success(catalogTaskQualityService.getQualityListByCatTaskId(catTaskId));
    }

    @Operation(summary = "绑定质量探查任务")
    @Log(title = "探查任务-质量任务绑定", businessType = BusinessType.INSERT)
    @PostMapping("/bind")
    public AjaxResult bind(@RequestBody CatalogTaskQualityBindReqVO bindReqVO) {
        catalogTaskQualityService.bindQualityTask(bindReqVO.getCatTaskId(), bindReqVO.getQualityTaskId());
        return success();
    }

    @Operation(summary = "解绑质量探查任务")
    @Log(title = "探查任务-质量任务解绑", businessType = BusinessType.DELETE)
    @DeleteMapping("/unbind/{id}")
    public AjaxResult unbind(@PathVariable("id") Long id) {
        catalogTaskQualityService.unbindQualityTask(id);
        return success();
    }
}
