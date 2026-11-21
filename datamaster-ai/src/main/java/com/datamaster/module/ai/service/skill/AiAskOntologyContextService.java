package com.datamaster.module.ai.service.skill;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.datamaster.common.api.space.ISpaceApi;
import com.datamaster.common.core.domain.entity.DatasourceDO;
import com.datamaster.common.core.domain.entity.DatasourceSpaceRelDO;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.common.datasource.mgmt.mapper.DatasourceSpaceRelMgmtMapper;
import com.datamaster.common.datasource.mgmt.service.IDatasourceMgmtService;
import com.datamaster.common.exception.ServiceException;
import com.datamaster.common.utils.StringUtils;
import com.datamaster.module.ai.controller.admin.skill.vo.AiAskOntologyContextRespVO;
import com.datamaster.module.ontology.controller.admin.metric.vo.MetricPageReqVO;
import com.datamaster.module.ontology.controller.admin.metric.vo.MetricRespVO;
import com.datamaster.module.ontology.dal.dataobject.*;
import com.datamaster.module.ontology.dal.mapper.*;
import com.datamaster.module.ontology.service.IMetricService;
import org.springframework.stereotype.Service;
import javax.annotation.Resource;
import java.util.*;

/** Resolves only active ontology-bound sources; never substitutes formal metric source checks. */
@Service
public class AiAskOntologyContextService {
    @Resource private OntologyMapper ontologyMapper;
    @Resource private ConceptMapper conceptMapper;
    @Resource private ConceptTableMapper conceptTableMapper;
    @Resource private RelationMapper relationMapper;
    @Resource private RelationTableMapper relationTableMapper;
    @Resource private IDatasourceMgmtService datasourceService;
    @Resource private DatasourceSpaceRelMgmtMapper datasourceSpaces;
    @Resource private ISpaceApi spaceApi;
    @Resource private IMetricService metricService;

    public AiAskOntologyContextRespVO context(Long ontologyId, Long spaceId, String spaceCode, Long userId) {
        OntologyDO ontology = authorize(ontologyId, spaceId, spaceCode, userId);
        AiAskOntologyContextRespVO result = new AiAskOntologyContextRespVO();
        result.setOntologyId(ontologyId);
        result.setDatasources(sources(ontology));
        // Published API returns immutable snapshot metadata, not mutable draft names.
        for (int page = 1; page <= 100; page++) {
            MetricPageReqVO request = new MetricPageReqVO();
            request.setOntologyId(ontologyId); request.setPageNum(page); request.setPageSize(100);
            PageResult<MetricRespVO> response = metricService.getPublishedPage(request, null);
            List<MetricRespVO> rows = response.getRows();
            for (MetricRespVO row : rows) {
                AiAskOntologyContextRespVO.Metric metric = new AiAskOntologyContextRespVO.Metric();
                metric.setId(row.getId()); metric.setName(row.getName()); metric.setPublishedVersion(row.getPublishedVersion());
                result.getMetrics().add(metric);
            }
            if (result.getMetrics().size() >= response.getTotal()) return result;
            if (rows.isEmpty()) throw new ServiceException("本体指标列表已变化，请重新选择本体");
        }
        throw new ServiceException("本体指标数量超出配置加载上限");
    }

    /** Null ontology preserves legacy API behavior; an ontology can never use an unrelated source. */
    public Long resolveDatasource(Long ontologyId, Long selectedId, Long spaceId, String spaceCode,
                                  Long userId, boolean required) {
        if (ontologyId == null) return selectedId;
        List<AiAskOntologyContextRespVO.Datasource> options = sources(authorize(ontologyId, spaceId, spaceCode, userId));
        if (selectedId != null) {
            if (options.stream().noneMatch(item -> selectedId.equals(item.getId())))
                throw new ServiceException("所选数据源不是当前本体绑定的可用数据源，请重新选择本体");
            return selectedId;
        }
        if (!options.isEmpty()) return options.get(0).getId();
        if (required) throw new ServiceException("当前本体尚未绑定可用数据源，请先配置概念或关系的表绑定");
        return null;
    }

    private OntologyDO authorize(Long ontologyId, Long requestedSpace, String requestedCode, Long userId) {
        if (ontologyId == null) throw new ServiceException("请先选择业务本体");
        OntologyDO ontology = ontologyMapper.selectById(ontologyId);
        if (ontology == null || !active(ontology.getDelFlag())) throw new ServiceException("本体不存在或已删除");
        Long space = ontology.getSpaceId();
        if (space == null && StringUtils.isNotBlank(ontology.getSpaceCode()))
            space = spaceApi.getSpaceIdBySpaceCode(ontology.getSpaceCode());
        if (space == null || space <= 0) throw new ServiceException("本体所属空间缺失");
        String code = spaceApi.getSpaceCodeBySpaceId(space);
        if (StringUtils.isBlank(code) || StringUtils.isNotBlank(ontology.getSpaceCode()) && !code.equals(ontology.getSpaceCode())
                || requestedSpace != null && !space.equals(requestedSpace)
                || StringUtils.isNotBlank(requestedCode) && !code.equals(requestedCode))
            throw new ServiceException("本体与当前空间不一致");
        if (userId == null || !spaceApi.hasUserSpace(userId, space, code))
            throw new ServiceException("当前用户无权访问本体所属空间");
        ontology.setSpaceId(space); ontology.setSpaceCode(code);
        return ontology;
    }

    private List<AiAskOntologyContextRespVO.Datasource> sources(OntologyDO ontology) {
        Set<Long> ids = new TreeSet<>();
        List<ConceptDO> concepts = conceptMapper.selectList(Wrappers.<ConceptDO>lambdaQuery()
                .eq(ConceptDO::getOntologyId, ontology.getId()));
        for (ConceptDO concept : concepts) {
            if (!active(concept.getDelFlag()) || !ontology.getId().equals(concept.getOntologyId())) continue;
            for (ConceptTableDO binding : conceptTableMapper.selectByConceptId(concept.getId()))
                if (active(binding.getDelFlag()) && concept.getId().equals(binding.getConceptId()) && binding.getDatasourceId() != null)
                    ids.add(binding.getDatasourceId());
        }
        List<RelationDO> relations = relationMapper.selectList(Wrappers.<RelationDO>lambdaQuery()
                .eq(RelationDO::getOntologyId, ontology.getId()));
        for (RelationDO relation : relations) {
            if (!active(relation.getDelFlag()) || !ontology.getId().equals(relation.getOntologyId())) continue;
            for (RelationTableDO binding : relationTableMapper.selectByRelationId(relation.getId()))
                if (active(binding.getDelFlag()) && relation.getId().equals(binding.getRelationId()) && binding.getDatasourceId() != null)
                    ids.add(binding.getDatasourceId());
        }
        List<AiAskOntologyContextRespVO.Datasource> result = new ArrayList<>();
        for (Long id : ids) {
            DatasourceDO source = datasourceService.getDatasourceDOById(id);
            if (source == null || Boolean.TRUE.equals(source.getDelFlag()) || !Boolean.TRUE.equals(source.getValidFlag())) continue;
            Long count = datasourceSpaces.selectCount(Wrappers.<DatasourceSpaceRelDO>lambdaQuery()
                    .eq(DatasourceSpaceRelDO::getDatasourceId, id)
                    .eq(DatasourceSpaceRelDO::getSpaceId, ontology.getSpaceId())
                    .eq(DatasourceSpaceRelDO::getSpaceCode, ontology.getSpaceCode()));
            if (count == null || count <= 0) continue;
            AiAskOntologyContextRespVO.Datasource option = new AiAskOntologyContextRespVO.Datasource();
            option.setId(id); option.setDatasourceName(source.getDatasourceName()); option.setDbgptSyncStatus(source.getDbgptSyncStatus());
            result.add(option);
        }
        return result;
    }

    private static boolean active(Integer deleted) { return deleted == null || deleted == 0; }
}
