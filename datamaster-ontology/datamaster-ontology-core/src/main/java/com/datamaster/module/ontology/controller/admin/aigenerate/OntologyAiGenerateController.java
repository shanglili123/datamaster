package com.datamaster.module.ontology.controller.admin.aigenerate;

import com.datamaster.common.core.domain.CommonResult;
import com.datamaster.metadata.api.table.dto.CatalogTableRespDTO;
import com.datamaster.module.ontology.controller.admin.aigenerate.vo.OntologyAiGenerateReqVO;
import com.datamaster.module.ontology.controller.admin.aigenerate.vo.OntologyAiGenerateRespVO;
import com.datamaster.module.ontology.service.IOntologyGenerateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.List;

/**
 * 本体 AI 生成 Controller
 *
 * <p>根据数据源中的业务表结构，调用 AI 生成本体模型（本体 + 概念 + 属性 + 表/字段绑定）。</p>
 */
@Tag(name = "本体AI生成")
@RestController
@RequestMapping("/ont/ai-generate")
@Validated
public class OntologyAiGenerateController {

    @Resource
    private IOntologyGenerateService ontologyGenerateService;

    @Operation(summary = "AI 生成预览（不落库）")
    @PreAuthorize("@ss.hasPermi('ont:ontology:query')")
    @PostMapping("/preview")
    public CommonResult<OntologyAiGenerateRespVO> preview(@Valid @RequestBody OntologyAiGenerateReqVO reqVO) {
        return CommonResult.success(ontologyGenerateService.preview(reqVO));
    }

    @Operation(summary = "AI 生成并落库（本体/概念/属性/表绑定/字段绑定）")
    @PreAuthorize("@ss.hasPermi('ont:ontology:add')")
    @PostMapping("/generate")
    public CommonResult<OntologyAiGenerateRespVO> generate(@Valid @RequestBody OntologyAiGenerateReqVO reqVO) {
        return CommonResult.success(ontologyGenerateService.generate(reqVO));
    }

    /**
     * 获取 AI 生成可选业务表列表（读元数据目录，不直连数据源）。
     *
     * <p>与 preview/generate 内部消费的目录数据同源（按表名去重取最新版本），
     * 避免表下拉直连数据源导致"数据库连接失败"且与生成逻辑不一致。</p>
     *
     * @param datasourceId 数据源 ID
     * @return 元数据目录中该数据源已采集的表列表
     */
    @Operation(summary = "获取 AI 生成可选业务表（元数据目录）")
    @PreAuthorize("@ss.hasPermi('ont:ontology:query')")
    @GetMapping("/tables")
    public CommonResult<List<CatalogTableRespDTO>> tables(@RequestParam("datasourceId") Long datasourceId) {
        return CommonResult.success(ontologyGenerateService.listCatalogTables(datasourceId));
    }
}