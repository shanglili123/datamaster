package com.datamaster.module.ontology.service;

import com.datamaster.module.ontology.controller.admin.relationcolumn.vo.RelationColumnRespVO;
import com.datamaster.module.ontology.controller.admin.relationcolumn.vo.RelationColumnSaveReqVO;

import java.util.List;

/**
 * 关系字段绑定 Service 接口
 */
public interface IRelationColumnService {

    List<RelationColumnRespVO> getRelationColumnsByRelationId(Long relationId);

    void batchSaveRelationColumns(Long relationId, List<RelationColumnSaveReqVO> list);

    Integer deleteRelationColumn(Long id);
}
