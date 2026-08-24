package com.datamaster.module.ontology.service;

import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.ontology.controller.admin.relation.vo.RelationPageReqVO;
import com.datamaster.module.ontology.controller.admin.relation.vo.RelationRespVO;
import com.datamaster.module.ontology.controller.admin.relation.vo.RelationSaveReqVO;

/**
 * 本体关系 Service 接口
 */
public interface IRelationService {

    /**
     * 分页查询关系
     *
     * @param pageReqVO 分页查询参数
     * @return 关系分页结果
     */
    PageResult<RelationRespVO> getRelationPage(RelationPageReqVO pageReqVO);

    /**
     * 根据编号获取关系详情
     *
     * @param id 编号
     * @return 关系详情
     */
    RelationRespVO getRelationById(Long id);

    /**
     * 创建关系
     *
     * @param createReqVO 创建参数
     * @return 新增编号
     */
    Long createRelation(RelationSaveReqVO createReqVO);

    /**
     * 更新关系
     *
     * @param updateReqVO 更新参数
     * @return 影响行数
     */
    Integer updateRelation(RelationSaveReqVO updateReqVO);

    /**
     * 删除关系
     *
     * @param id 编号
     * @return 影响行数
     */
    Integer deleteRelation(Long id);
}
