package com.datamaster.module.ontology.controller.admin.function;

import com.datamaster.common.core.domain.CommonResult;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.ontology.controller.admin.function.vo.*;
import com.datamaster.module.ontology.service.IFunctionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.List;

@Tag(name = "本体函数管理")
@RestController
@RequestMapping("/ont/function")
@Validated
public class FunctionController {

    @Resource
    private IFunctionService functionService;

    @Operation(summary = "分页查询函数")
    @PreAuthorize("@ss.hasPermi('ont:function:list')")
    @GetMapping("/page")
    public CommonResult<PageResult<FunctionRespVO>> page(FunctionPageReqVO pageReqVO) {
        return CommonResult.success(functionService.getFunctionPage(pageReqVO));
    }

    @Operation(summary = "按本体ID查询函数列表（本体ID为空时返回共享函数全量）")
    @PreAuthorize("@ss.hasPermi('ont:function:list')")
    @GetMapping("/list")
    public CommonResult<List<FunctionRespVO>> list(@RequestParam(required = false) Long ontologyId) {
        if (ontologyId == null) {
            return CommonResult.success(functionService.listAllFunctions());
        }
        return CommonResult.success(functionService.getFunctionsByOntologyId(ontologyId));
    }

    @Operation(summary = "获取函数详情")
    @PreAuthorize("@ss.hasPermi('ont:function:query')")
    @GetMapping("/{id}")
    public CommonResult<FunctionRespVO> get(@PathVariable Long id) {
        return CommonResult.success(functionService.getFunctionById(id));
    }

    @Operation(summary = "新增函数")
    @PreAuthorize("@ss.hasPermi('ont:function:add')")
    @PostMapping
    public CommonResult<Long> add(@Valid @RequestBody FunctionSaveReqVO createReqVO) {
        return CommonResult.success(functionService.createFunction(createReqVO));
    }

    @Operation(summary = "修改函数")
    @PreAuthorize("@ss.hasPermi('ont:function:edit')")
    @PutMapping
    public CommonResult<Integer> edit(@Valid @RequestBody FunctionSaveReqVO updateReqVO) {
        return CommonResult.success(functionService.updateFunction(updateReqVO));
    }

    @Operation(summary = "删除函数")
    @PreAuthorize("@ss.hasPermi('ont:function:remove')")
    @DeleteMapping("/{id}")
    public CommonResult<Integer> delete(@PathVariable Long id) {
        return CommonResult.success(functionService.deleteFunction(id));
    }

    @Operation(summary = "提交执行（进入审批）")
    @PreAuthorize("@ss.hasPermi('ont:function:edit')")
    @PostMapping("/exec/submit")
    public CommonResult<FunctionExecRespVO> submitExecution(@Valid @RequestBody FunctionExecReqVO reqVO) {
        return CommonResult.success(functionService.submitExecution(reqVO));
    }

    @Operation(summary = "审批通过")
    @PreAuthorize("@ss.hasPermi('ont:function:edit')")
    @PostMapping("/exec/approve")
    public CommonResult<Boolean> approveExecution(@Valid @RequestBody FunctionApprovalReqVO reqVO) {
        functionService.approveExecution(reqVO);
        return CommonResult.success(true);
    }

    @Operation(summary = "审批拒绝")
    @PreAuthorize("@ss.hasPermi('ont:function:edit')")
    @PostMapping("/exec/reject")
    public CommonResult<Boolean> rejectExecution(@Valid @RequestBody FunctionApprovalReqVO reqVO) {
        functionService.rejectExecution(reqVO);
        return CommonResult.success(true);
    }

    @Operation(summary = "执行已批准的函数")
    @PreAuthorize("@ss.hasPermi('ont:function:edit')")
    @PostMapping("/exec/run/{id}")
    public CommonResult<FunctionExecRespVO> execute(@PathVariable Long id) {
        return CommonResult.success(functionService.executeFunction(id));
    }

    @Operation(summary = "获取执行记录")
    @PreAuthorize("@ss.hasPermi('ont:function:query')")
    @GetMapping("/exec/{id}")
    public CommonResult<FunctionExecRespVO> getExecution(@PathVariable Long id) {
        return CommonResult.success(functionService.getExecutionById(id));
    }

    @Operation(summary = "分页查询执行记录")
    @PreAuthorize("@ss.hasPermi('ont:function:list')")
    @GetMapping("/exec/page")
    public CommonResult<PageResult<FunctionExecRespVO>> execPage(FunctionExecPageReqVO pageReqVO) {
        return CommonResult.success(functionService.getExecutionPage(pageReqVO));
    }

    @Operation(summary = "查询待审批列表（本体ID为空时查询全部）")
    @PreAuthorize("@ss.hasPermi('ont:function:list')")
    @GetMapping("/exec/pending")
    public CommonResult<List<FunctionExecRespVO>> pendingApprovals(@RequestParam(required = false) Long ontologyId) {
        return CommonResult.success(functionService.getPendingApprovals(ontologyId));
    }
}
