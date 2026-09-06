package com.datamaster.module.ontology.service;

import com.datamaster.module.ontology.controller.admin.objectinstance.vo.ObjectInstanceQueryReqVO;
import com.datamaster.module.ontology.controller.admin.objectinstance.vo.ObjectInstanceQueryRespVO;
import com.datamaster.module.ontology.controller.admin.objectinstance.vo.RelationJumpReqVO;

/**
 * 对象实例层查询 Service — 管理端对象浏览器
 *
 * 将已绑定物理表的概念暴露为可查询对象实例（帕朗提尔 Object Instance 思路）：
 * 解析概念表绑定 → DbQuery 执行 → 复用 checkTableAccess 统一表级/字段级权限。
 */
public interface IObjectInstanceQueryService {

    /**
     * 分页查询对象实例
     *
     * @param reqVO 查询参数（概念 + 表绑定 + 分页 + 过滤 + 空间上下文）
     * @return 对象集元信息 + 表头 + 实例行 + 总数
     */
    ObjectInstanceQueryRespVO queryObjects(ObjectInstanceQueryReqVO reqVO);

    /**
     * 跨对象关系跳转（档位 B：同页内嵌展开关联对象）
     *
     * 给定源对象实例行上的关联值（sourceValues），沿关系字段绑定
     * （{@code RelationColumnDO} sourceColumn → targetColumn）在目标概念物理表上做值过滤查询，
     * 返回目标对象实例。不做跨表物理 JOIN（d8）：目标按 targetColumn IN/=(源值) 过滤。
     * 源表与目标表分别调用 checkTableAccess（d6），绝不放宽权限。
     *
     * @param reqVO 关系跳转请求（源概念 + 关系 + 源关联值 + 目标过滤/排序/投影 + 空间上下文）
     * @return 目标概念对象实例（含目标对象集元信息 + 表头 + 行 + 总数）
     */
    ObjectInstanceQueryRespVO queryRelatedObjects(RelationJumpReqVO reqVO);
}
