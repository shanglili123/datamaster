package com.datamaster.module.ontology.api;

import com.datamaster.module.ontology.api.dto.ObjectInstanceApiDTO;

import java.util.List;

/**
 * 本体对象实例层只读查询接口 — 供 AI 问数、对象浏览器等上层模块调用
 *
 * 对象实例层把已绑定物理表的概念暴露为可查询的 Object（对象），
 * 偏 Palantir Object Type 思路：概念 = 对象类型，物理表 = 对象存储。
 *
 * 本接口仅暴露只读语义信息，不包含任何权限校验与 SQL 执行逻辑；
 * 权限校验（checkTableAccess）与物理查询（DbQuery）均由各调用方按 entrance 完成，
 * 复用 {@code AssetsTableGovernanceApiServiceImpl} 统一表级/字段级权限。
 *
 * 实现位于 datamaster-ontology-core，由 datamaster-server 聚合注入。
 */
public interface IObjectInstanceApiService {

    /**
     * 按本体查询其全部已绑定物理表的概念对象集
     *
     * 返回该本体下所有 ONT_CONCEPT_TABLE 绑定的概念，
     * 每条记录包含对象类型（概念）元信息 + 命中物理表 + 属性语义映射。
     *
     * @param ontologyId 本体ID，为空时返回空列表
     * @return 对象集列表，无绑定时返回空列表
     */
    List<ObjectInstanceApiDTO> listObjectSets(Long ontologyId);
}
