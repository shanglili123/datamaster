package com.datamaster.module.ontology.service;

import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.ontology.controller.admin.ontology.vo.OntologyPageReqVO;
import com.datamaster.module.ontology.controller.admin.ontology.vo.OntologyRespVO;
import com.datamaster.module.ontology.controller.admin.ontology.vo.OntologySaveReqVO;

/**
 * 本体 Service 接口
 */
public interface IOntologyService {

    /**
     * 分页查询本体
     *
     * @param pageReqVO 分页查询参数
     * @return 本体分页结果
     */
    PageResult<OntologyRespVO> getOntologyPage(OntologyPageReqVO pageReqVO);

    /**
     * 根据编号获取本体详情
     *
     * @param id 编号
     * @return 本体详情
     */
    OntologyRespVO getOntologyById(Long id);

    /**
     * 创建本体
     *
     * @param createReqVO 创建参数
     * @return 新增编号
     */
    Long createOntology(OntologySaveReqVO createReqVO);

    /**
     * 更新本体
     *
     * @param updateReqVO 更新参数
     * @return 影响行数
     */
    Integer updateOntology(OntologySaveReqVO updateReqVO);

    /**
     * 删除本体
     *
     * @param id 编号
     * @return 影响行数
     */
    Integer deleteOntology(Long id);
}
