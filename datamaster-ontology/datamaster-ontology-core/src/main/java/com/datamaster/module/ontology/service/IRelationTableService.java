package com.datamaster.module.ontology.service;

import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.ontology.controller.admin.relationtable.vo.RelationTablePageReqVO;
import com.datamaster.module.ontology.controller.admin.relationtable.vo.RelationTableRespVO;
import com.datamaster.module.ontology.controller.admin.relationtable.vo.RelationTableSaveReqVO;

import java.util.List;

/**
 * 关系关联表绑定 Service 接口
 */
public interface IRelationTableService {

    PageResult<RelationTableRespVO> getRelationTablePage(RelationTablePageReqVO pageReqVO);

    List<RelationTableRespVO> getRelationTablesByRelationId(Long relationId);

    RelationTableRespVO getRelationTableById(Long id);

    Long createRelationTable(RelationTableSaveReqVO createReqVO);

    Integer updateRelationTable(RelationTableSaveReqVO updateReqVO);

    Integer deleteRelationTable(Long id);
}
