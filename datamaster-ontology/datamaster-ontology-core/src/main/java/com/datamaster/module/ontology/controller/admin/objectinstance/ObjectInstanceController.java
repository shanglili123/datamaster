package com.datamaster.module.ontology.controller.admin.objectinstance;

import com.datamaster.common.core.domain.CommonResult;
import com.datamaster.module.ontology.api.IObjectInstanceApiService;
import com.datamaster.module.ontology.api.dto.ObjectInstanceApiDTO;
import com.datamaster.module.ontology.controller.admin.objectinstance.vo.ObjectInstanceQueryReqVO;
import com.datamaster.module.ontology.controller.admin.objectinstance.vo.ObjectInstanceQueryRespVO;
import com.datamaster.module.ontology.controller.admin.objectinstance.vo.ObjectLineageRespVO;
import com.datamaster.module.ontology.controller.admin.objectinstance.vo.RelationJumpReqVO;
import com.datamaster.module.ontology.controller.admin.objectinstance.vo.RowOperateReqVO;
import com.datamaster.module.ontology.controller.admin.objectinstance.vo.RowOperateRespVO;
import com.datamaster.module.ontology.service.IObjectInstanceOperateService;
import com.datamaster.module.ontology.service.IObjectInstanceQueryService;
import com.datamaster.module.ontology.service.IObjectLineageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.List;

/**
 * 对象实例层 Controller — 对象浏览器
 *
 * 把已绑定物理表的概念暴露为可查询对象实例（帕朗提尔 Object Instance 思路）。
 * 权限：查询走 checkTableAccess（entrance=ONTOLOGY_OBJECT_QUERY），表级语义。
 */
@Tag(name = "对象实例层")
@RestController
@RequestMapping("/ont/object-instance")
@Validated
public class ObjectInstanceController {

    @Resource
    private IObjectInstanceApiService objectInstanceApiService;
    @Resource
    private IObjectInstanceQueryService objectInstanceQueryService;
    @Resource
    private IObjectLineageService objectLineageService;
    @Resource
    private IObjectInstanceOperateService objectInstanceOperateService;

    @Operation(summary = "查询本体下的对象集（概念 + 物理表绑定）")
    @PreAuthorize("@ss.hasPermi('ont:object-instance:query')")
    @GetMapping("/object-sets")
    public CommonResult<List<ObjectInstanceApiDTO>> listObjectSets(@RequestParam(required = false) Long ontologyId) {
        return CommonResult.success(objectInstanceApiService.listObjectSets(ontologyId));
    }

    @Operation(summary = "分页查询对象实例")
    @PreAuthorize("@ss.hasPermi('ont:object-instance:query')")
    @GetMapping("/objects")
    public CommonResult<ObjectInstanceQueryRespVO> queryObjects(ObjectInstanceQueryReqVO reqVO) {
        return CommonResult.success(objectInstanceQueryService.queryObjects(reqVO));
    }

    @Operation(summary = "跨对象关系跳转（档位 B：同页内嵌展开关联对象）")
    @PreAuthorize("@ss.hasPermi('ont:object-instance:query')")
    @PostMapping("/related")
    public CommonResult<ObjectInstanceQueryRespVO> relatedObjects(@RequestBody RelationJumpReqVO reqVO) {
        return CommonResult.success(objectInstanceQueryService.queryRelatedObjects(reqVO));
    }

    @Operation(summary = "对象行操作-提交预览（新增/修改/删除，生成SQL+dry-run+按需建审批链）")
    @PreAuthorize("@ss.hasPermi('ont:object-instance:query')")
    @PostMapping("/row/preview")
    public CommonResult<RowOperateRespVO> rowPreview(@Valid @RequestBody RowOperateReqVO reqVO) {
        return CommonResult.success(objectInstanceOperateService.preview(reqVO));
    }

    @Operation(summary = "对象行操作-确认执行（审批通过+执行，弹框确定=审批通过）")
    @PreAuthorize("@ss.hasPermi('ont:object-instance:query')")
    @PostMapping("/row/confirm")
    public CommonResult<RowOperateRespVO> rowConfirm(@Valid @RequestBody RowOperateReqVO reqVO) {
        return CommonResult.success(objectInstanceOperateService.confirm(reqVO));
    }

    @Operation(summary = "查询对象血缘（四维度聚合：数据/决策/版本/权限）")
    @PreAuthorize("@ss.hasPermi('ont:object-instance:query')")
    @GetMapping("/lineage/{conceptId}")
    public CommonResult<ObjectLineageRespVO> objectLineage(@PathVariable("conceptId") Long conceptId,
                                                           @RequestParam(required = false) Long spaceId,
                                                           @RequestParam(required = false) String spaceCode) {
        return CommonResult.success(objectLineageService.objectLineage(conceptId, spaceId, spaceCode));
    }
}
