package com.datamaster.module.ontology.service;

import com.datamaster.module.ontology.controller.admin.objectinstance.vo.ObjectLineageRespVO;

/**
 * 对象血缘 Service — 对象血缘四维度聚合读取。
 *
 * <p>
 * 读取侧把四个维度的血缘实时合成：
 * <ul>
 *   <li>数据维度/决策维度：来自 Neo4j（ObjectRepository.objectLineage）</li>
 *   <li>版本维度：由 ONT_ACTION_EXECUTION 的 before/after 数据快照派生时间线</li>
 *   <li>权限维度：复用资产统一权限入口（resolveTable）实时计算</li>
 * </ul>
 */
public interface IObjectLineageService {

    /**
     * 查询指定概念的完整血缘（四维度聚合）。
     *
     * @param conceptId 概念ID（对象类型ID）
     * @param spaceId   空间ID（权限维度上下文，可为空）
     * @param spaceCode 空间编码（权限维度上下文，可为空）
     * @return 四维血缘视图
     */
    ObjectLineageRespVO objectLineage(Long conceptId, Long spaceId, String spaceCode);
}
