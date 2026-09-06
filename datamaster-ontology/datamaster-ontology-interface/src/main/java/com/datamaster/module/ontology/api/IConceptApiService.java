package com.datamaster.module.ontology.api;

import com.datamaster.module.ontology.api.dto.SemanticTableDTO;

import java.util.List;

/**
 * 本体概念语义查询接口 — 供 AI 问数、Skill 生成等上层模块调用
 *
 * 仅暴露只读查询，不包含本体编辑能力。
 * 实现位于 datamaster-ontology-core，由 datamaster-server 聚合注入。
 */
public interface IConceptApiService {

    /**
     * 按数据源查询已绑定本体概念的语义化表结构
     *
     * 返回该数据源下所有 ONT_CONCEPT_TABLE 绑定的概念表，
     * 每条记录包含概念元信息 + 属性业务语义 + 物理列映射。
     *
     * @param datasourceId 数据源ID
     * @return 语义化表列表，无绑定时返回空列表
     */
    List<SemanticTableDTO> listSemanticTablesByDatasource(Long datasourceId);
}
