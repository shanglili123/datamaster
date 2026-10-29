package com.datamaster.module.ontology.service.impl;

import com.alibaba.fastjson2.JSON;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.common.datasource.mgmt.api.IDatasourceApiService;
import com.datamaster.common.datasource.mgmt.api.dto.DatasourceRespDTO;
import com.datamaster.common.exception.ServiceException;
import com.datamaster.module.ontology.controller.admin.metric.vo.*;
import com.datamaster.module.ontology.dal.dataobject.*;
import com.datamaster.module.ontology.dal.mapper.*;
import com.datamaster.module.ontology.service.IMetricService;
import com.datamaster.module.ontology.service.metric.MetricRules;
import com.datamaster.mybatis.core.query.LambdaQueryWrapperX;
import org.springframework.beans.BeanUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class MetricServiceImpl implements IMetricService {
    @Resource private MetricMapper metricMapper;
    @Resource private MetricVersionMapper metricVersionMapper;
    @Resource private ConceptMapper conceptMapper;
    @Resource private ConceptTableMapper conceptTableMapper;
    @Resource private PropertyMapper propertyMapper;
    @Resource private PropertyColumnMapper propertyColumnMapper;
    @Resource private IDatasourceApiService datasourceApiService;
    @Resource private MetricAccessGuard accessGuard;
    @Resource private MetricMetadataLockMapper metadataLockMapper;

    @Override
    public PageResult<MetricRespVO> getPage(MetricPageReqVO req) {
        accessGuard.assertOntologyAccess(req.getOntologyId());
        PageResult<MetricDO> page = metricMapper.selectPage(req);
        return new PageResult<>(page.getRows().stream().map(this::response).collect(Collectors.toList()), page.getTotal());
    }

    @Override
    public MetricRespVO get(Long id) {
        MetricDO metric = accessible(id, false);
        MetricRespVO result = response(metric);
        if (metric.getPublishedVersion() > 0) {
            result.setSourceSnapshot(loadVersion(id, metric.getPublishedVersion()).getSourceSnapshot());
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(MetricSaveReqVO req) {
        MetricRules.require(req.getId() == null, "新增指标不能指定ID");
        accessGuard.assertOntologyAccess(req.getOntologyId());
        lockOntology(req.getOntologyId());
        MetricSaveReqVO definition = MetricRules.normalize(req);
        validateDraftReferences(definition);
        MetricDO metric = new MetricDO();
        metric.setOntologyId(definition.getOntologyId());
        metric.setConceptId(definition.getConceptId());
        metric.setCode(definition.getCode());
        metric.setStatus("DRAFT");
        metric.setPublishedVersion(0);
        metric.setDraftChanged(false);
        metric.setRevision(0);
        applyDefinition(metric, definition);
        try {
            metricMapper.insert(metric);
        } catch (DuplicateKeyException ex) {
            throw new ServiceException("该本体下指标编码已存在");
        }
        return metric.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Integer update(MetricSaveReqVO req) {
        MetricRules.require(req.getId() != null, "修改指标必须指定ID");
        MetricDO metric = accessible(req.getId(), true);
        MetricRules.checkRevision(req.getRevision(), metric.getRevision());
        MetricRules.checkIdentity(metric, req);
        MetricSaveReqVO definition = MetricRules.normalize(req);
        validateDraftReferences(definition);
        String json = MetricRules.definitionJson(definition);
        if (json.equals(metric.getDefinitionJson())) return 0;
        applyDefinition(metric, definition);
        // Preserve PUBLISHED/DISABLED and the currently available immutable version.
        metric.setDraftChanged(metric.getPublishedVersion() > 0 && !json.equals(MetricRules.definitionJson(
                loadVersion(metric.getId(), metric.getPublishedVersion()))));
        metric.setRevision(metric.getRevision() + 1);
        return metricMapper.updateById(metric);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Integer delete(Long id) {
        MetricDO metric = accessible(id, true);
        MetricRules.require("DRAFT".equals(metric.getStatus()) && metric.getPublishedVersion() == 0,
                "只有从未发布的草稿指标可删除，已发布指标请停用");
        return metricMapper.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void validate(Long id) {
        MetricDO metric = accessible(id, true);
        validateForPublication(response(metric));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void publish(Long id, Integer revision) {
        MetricDO metric = accessible(id, true);
        MetricRules.checkRevision(revision, metric.getRevision());
        MetricRespVO definition = response(metric);
        Map<String, Object> source = validateForPublication(definition);
        if ("PUBLISHED".equals(metric.getStatus()) && metric.getPublishedVersion() > 0
                && MetricRules.samePublishedDefinition(definition, source,
                        loadVersion(id, metric.getPublishedVersion()))) {
            return;
        }
        int nextVersion = metric.getPublishedVersion() + 1;
        int nextRevision = metric.getRevision() + 1;
        MetricRespVO snapshot = MetricRules.snapshot(definition, source, nextVersion, nextRevision);
        MetricVersionDO version = new MetricVersionDO();
        version.setMetricId(id);
        version.setVersion(nextVersion);
        version.setSnapshotJson(JSON.toJSONString(snapshot,
                com.alibaba.fastjson2.JSONWriter.Feature.WriteMapNullValue));
        // INSERT ONLY. Never update/upsert a historical snapshot.
        metricVersionMapper.insert(version);
        metric.setStatus("PUBLISHED");
        metric.setPublishedVersion(nextVersion);
        metric.setDraftChanged(false);
        metric.setRevision(nextRevision);
        metricMapper.updateById(metric);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void disable(Long id, Integer revision) {
        MetricDO metric = accessible(id, true);
        MetricRules.checkRevision(revision, metric.getRevision());
        if ("DISABLED".equals(metric.getStatus())) return;
        MetricRules.require(metric.getPublishedVersion() > 0, "未发布的草稿不能停用");
        metric.setStatus("DISABLED");
        metric.setRevision(metric.getRevision() + 1);
        metricMapper.updateById(metric);
    }

    @Override
    public List<MetricVersionRespVO> versions(Long id) {
        accessible(id, false);
        return metricVersionMapper.selectList(new LambdaQueryWrapperX<MetricVersionDO>()
                .eq(MetricVersionDO::getMetricId, id).orderByDesc(MetricVersionDO::getVersion))
                .stream().map(row -> {
                    MetricVersionRespVO vo = new MetricVersionRespVO();
                    BeanUtils.copyProperties(row, vo);
                    return vo;
                }).collect(Collectors.toList());
    }

    @Override
    public MetricRespVO version(Long id, Integer version) {
        accessible(id, false);
        return loadVersion(id, version);
    }

    private MetricRespVO loadVersion(Long id, Integer version) {
        MetricRules.require(version != null && version > 0, "发布版本号必须大于0");
        MetricVersionDO row = metricVersionMapper.selectOne(new LambdaQueryWrapperX<MetricVersionDO>()
                .eq(MetricVersionDO::getMetricId, id).eq(MetricVersionDO::getVersion, version));
        if (row == null) throw new ServiceException("指标发布版本不存在");
        return JSON.parseObject(row.getSnapshotJson(), MetricRespVO.class);
    }

    private MetricDO accessible(Long id, boolean lock) {
        MetricRules.require(id != null, "指标ID不能为空");
        MetricDO metric = metricMapper.selectById(id);
        if (metric == null) throw new ServiceException("指标不存在");
        accessGuard.assertOntologyAccess(metric.getOntologyId());
        if (lock) {
            lockOntology(metric.getOntologyId());
            metric = metricMapper.selectForUpdate(id);
            if (metric == null) throw new ServiceException("指标不存在");
            accessGuard.assertOntologyAccess(metric.getOntologyId());
        }
        return metric;
    }

    private void lockOntology(Long ontologyId) {
        MetricRules.require(metadataLockMapper.lockOntology(ontologyId) != null, "本体不存在或已删除");
    }

    private MetricRespVO response(MetricDO metric) {
        MetricSaveReqVO definition = JSON.parseObject(metric.getDefinitionJson(), MetricSaveReqVO.class);
        MetricRespVO vo = new MetricRespVO();
        BeanUtils.copyProperties(definition, vo);
        vo.setId(metric.getId());
        vo.setOntologyId(metric.getOntologyId());
        vo.setConceptId(metric.getConceptId());
        vo.setCode(metric.getCode());
        vo.setName(metric.getName());
        vo.setConceptTableId(metric.getConceptTableId());
        vo.setRevision(metric.getRevision());
        vo.setStatus(metric.getStatus());
        vo.setPublishedVersion(metric.getPublishedVersion());
        vo.setDraftChanged(metric.getDraftChanged());
        return vo;
    }

    private void applyDefinition(MetricDO metric, MetricSaveReqVO definition) {
        metric.setName(definition.getName());
        metric.setConceptTableId(definition.getConceptTableId());
        metric.setDefinitionJson(MetricRules.definitionJson(definition));
    }

    /** Drafts may be incomplete, never cross-concept/cross-ontology. */
    private void validateDraftReferences(MetricSaveReqVO vo) {
        ConceptDO concept = conceptMapper.selectById(vo.getConceptId());
        MetricRules.require(concept != null && Objects.equals(concept.getOntologyId(), vo.getOntologyId()),
                "概念不存在或不属于该本体");
        if (vo.getConceptTableId() != null) {
            ConceptTableDO table = conceptTableMapper.selectById(vo.getConceptTableId());
            MetricRules.require(table != null && Objects.equals(table.getConceptId(), vo.getConceptId()),
                    "表绑定不存在或不属于该概念");
        }
        for (Long id : MetricRules.usedProperties(vo)) {
            PropertyDO property = propertyMapper.selectById(id);
            MetricRules.require(property != null && Objects.equals(property.getConceptId(), vo.getConceptId()),
                    "属性不存在或不属于该概念: " + id);
        }
    }

    /** Metadata only. Does NOT connect to Doris, execute SQL, verify data or grant source access. */
    private Map<String, Object> validateForPublication(MetricRespVO input) {
        MetricSaveReqVO vo = MetricRules.normalize(input);
        MetricRules.requirePublishFields(vo);
        validateDraftReferences(vo);
        ConceptTableDO table = conceptTableMapper.selectById(vo.getConceptTableId());
        for (MetricFilterVO filter : vo.getFixedFilters()) {
            PropertyDO property = propertyMapper.selectById(filter.getPropertyId());
            MetricRules.validateTypedFilter(filter, property.getDataType());
        }
        DatasourceRespDTO datasource = table.getDatasourceId() == null ? null
                : datasourceApiService.getDatasourceById(table.getDatasourceId());
        MetricRules.require(datasource != null && "DORIS".equalsIgnoreCase(datasource.getDatasourceType()),
                "第一批指标仅支持Doris数据源元数据");
        MetricRules.require(MetricRules.notBlank(table.getDatabaseName()), "表绑定必须明确数据库名");
        MetricRules.require(MetricRules.adsSource(table.getDatabaseName(), table.getTableName()),
                "指标仅支持ads_表/视图，或ads数据库中的表/视图；命名不是数据访问授权");
        Map<Long, PropertyColumnDO> columns = new LinkedHashMap<>();
        Set<Long> used = MetricRules.usedProperties(vo);
        for (PropertyColumnDO column : propertyColumnMapper.selectByConceptTableId(table.getId())) {
            if (!used.contains(column.getPropertyId())) continue;
            MetricRules.require(!columns.containsKey(column.getPropertyId()), "同一属性在该表上存在多个字段绑定");
            MetricRules.require(MetricRules.notBlank(column.getColumnName()), "属性绑定的物理列名不能为空");
            columns.put(column.getPropertyId(), column);
        }
        Map<String, Object> mapping = new LinkedHashMap<>();
        for (Long id : used) {
            PropertyDO property = propertyMapper.selectById(id);
            PropertyColumnDO column = columns.get(id);
            MetricRules.require(column != null, "属性未绑定当前概念表字段: " + id);
            if (Objects.equals(id, vo.getValuePropertyId())
                    && ("SUM".equals(vo.getAggregation()) || "AVG".equals(vo.getAggregation()))) {
                MetricRules.require(MetricRules.numeric(property.getDataType()), "SUM/AVG必须使用数值属性");
            }
            if (Objects.equals(id, vo.getTimePropertyId())) {
                MetricRules.require(MetricRules.temporal(property.getDataType()), "时间属性必须是date/datetime/timestamp类型");
            }
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("propertyId", id);
            item.put("propertyCode", property.getCode());
            item.put("propertyName", property.getName());
            item.put("dataType", property.getDataType());
            item.put("columnName", column.getColumnName());
            mapping.put(String.valueOf(id), item);
        }
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("datasourceId", table.getDatasourceId());
        snapshot.put("datasourceType", datasource.getDatasourceType());
        snapshot.put("databaseName", table.getDatabaseName());
        snapshot.put("schemaName", table.getSchemaName());
        snapshot.put("tableName", table.getTableName());
        snapshot.put("propertyColumns", mapping);
        snapshot.put("validationScope", "METADATA_ONLY");
        return snapshot;
    }
}
