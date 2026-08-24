package com.datamaster.module.ontology.service;

import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.ontology.controller.admin.concept.vo.ConceptPageReqVO;
import com.datamaster.module.ontology.controller.admin.concept.vo.ConceptRespVO;
import com.datamaster.module.ontology.controller.admin.concept.vo.ConceptSaveReqVO;

/**
 * 本体概念 Service 接口
 */
public interface IConceptService {

    /**
     * 分页查询概念
     *
     * @param pageReqVO 分页查询参数
     * @return 概念分页结果
     */
    PageResult<ConceptRespVO> getConceptPage(ConceptPageReqVO pageReqVO);

    /**
     * 根据编号获取概念详情
     *
     * @param id 编号
     * @return 概念详情
     */
    ConceptRespVO getConceptById(Long id);

    /**
     * 创建概念
     *
     * @param createReqVO 创建参数
     * @return 新增编号
     */
    Long createConcept(ConceptSaveReqVO createReqVO);

    /**
     * 更新概念
     *
     * @param updateReqVO 更新参数
     * @return 影响行数
     */
    Integer updateConcept(ConceptSaveReqVO updateReqVO);

    /**
     * 删除概念
     *
     * @param id 编号
     * @return 影响行数
     */
    Integer deleteConcept(Long id);
}
