package com.datamaster.module.ontology.service;

import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.ontology.controller.admin.concepttable.vo.ConceptTablePageReqVO;
import com.datamaster.module.ontology.controller.admin.concepttable.vo.ConceptTablePreviewRespVO;
import com.datamaster.module.ontology.controller.admin.concepttable.vo.ConceptTableRespVO;
import com.datamaster.module.ontology.controller.admin.concepttable.vo.ConceptTableSaveReqVO;

import java.util.List;

/**
 * 概念表绑定 Service 接口
 */
public interface IConceptTableService {

    PageResult<ConceptTableRespVO> getConceptTablePage(ConceptTablePageReqVO pageReqVO);

    List<ConceptTableRespVO> getConceptTableByConceptId(Long conceptId);

    ConceptTableRespVO getConceptTableById(Long id);

    Long createConceptTable(ConceptTableSaveReqVO createReqVO);

    Integer updateConceptTable(ConceptTableSaveReqVO updateReqVO);

    Integer deleteConceptTable(Long id);

    /** 按绑定ID预览物理表数据（列元数据 + 前 limit 行，limit 为空默认20、上限100） */
    ConceptTablePreviewRespVO previewData(Long id, Integer limit, String filters);
}
