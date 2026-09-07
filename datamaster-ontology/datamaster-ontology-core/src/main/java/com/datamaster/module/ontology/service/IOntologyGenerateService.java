package com.datamaster.module.ontology.service;

import com.datamaster.metadata.api.table.dto.CatalogTableRespDTO;
import com.datamaster.module.ontology.controller.admin.aigenerate.vo.AiActionPreviewReqVO;
import com.datamaster.module.ontology.controller.admin.aigenerate.vo.AiActionPreviewRespVO;
import com.datamaster.module.ontology.controller.admin.aigenerate.vo.OntologyAiGenerateReqVO;
import com.datamaster.module.ontology.controller.admin.aigenerate.vo.OntologyAiGenerateRespVO;

import java.util.List;

/**
 * 本体 AI 生成 Service 接口
 *
 * <p>根据数据源中一张业务表的元数据（表注释、字段注释、字段类型），
 * 调用外部 AI（DB-GPT）生成本体模型（本体 + 概念 + 属性 + 物理列映射），
 * 支持预览（不落库）与直接生成（事务落库）。</p>
 */
public interface IOntologyGenerateService {

    /**
     * 预览：调用 AI 生成本体模型，不落库。
     *
     * @param reqVO 生成参数
     * @return AI 生成的模型预览
     */
    OntologyAiGenerateRespVO preview(OntologyAiGenerateReqVO reqVO);

    /**
     * 生成：调用 AI 生成本体模型并事务落库。
     *
     * <p>ontologyId 为空时自动创建新本体；否则将概念挂载到指定本体下。
     * 落库顺序：本体（可选）-> 概念 -> 属性 -> 概念表绑定 -> 属性列映射。</p>
     *
     * @param reqVO 生成参数
     * @return 生成的模型 + 落库后的实体 ID
     */
    OntologyAiGenerateRespVO generate(OntologyAiGenerateReqVO reqVO);

    /**
     * 查询数据源在元数据目录中已采集的全部表（按表名去重取最新版本）。
     *
     * <p>供前端"AI 生成"弹窗选择业务表使用，与 preview/generate 消费的目录数据同源；
     * 只读平台元数据目录，不直连目标数据源。</p>
     *
     * @param datasourceId 数据源 ID
     * @return 元数据目录表列表，datasourceId 为空时返回空列表
     */
    List<CatalogTableRespDTO> listCatalogTables(Long datasourceId);

    /**
     * AI 生成动作预览：根据用户描述 + 本体概念/关系/属性上下文，调用 LLM 生成动作定义，不落库。
     *
     * @param reqVO 生成参数（含本体ID、可选概念ID、用户描述）
     * @return 生成的动作预览列表
     */
    AiActionPreviewRespVO generateActionsPreview(AiActionPreviewReqVO reqVO);
}