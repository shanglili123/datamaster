package com.datamaster.module.ontology.api;

import com.datamaster.module.ontology.api.dto.ManualEditDTO;

import java.util.List;

/**
 * 人为编辑登记查询接口 — 供数据接收工程（datamaster-ingestion）冲突仲裁使用
 *
 * <p>仅暴露只读查询，将本体动作执行记录（ONT_ACTION_EXECUTION）中已生效的人为编辑，
 * 按目标物理表暴露给接收工程，实现「周期增量写数仓」与「人工编辑登记」两条写路径的冲突仲裁。</p>
 *
 * <p>实现位于 datamaster-ontology-core（ManualEditQueryServiceImpl），由 datamaster-server 聚合注入。</p>
 */
public interface IManualEditQueryService {

    /**
     * 查询某目标物理表最近的人为编辑登记（EXECUTED / ROLLED_BACK），按执行时间倒序。
     *
     * @param targetTable 目标物理表名
     * @param limit       最多返回条数（建议 &gt;0，避免一次拉回全表编辑史）
     * @return 人为编辑列表，无记录时返回空列表
     */
    List<ManualEditDTO> listLatestManualEdits(String targetTable, Integer limit);
}