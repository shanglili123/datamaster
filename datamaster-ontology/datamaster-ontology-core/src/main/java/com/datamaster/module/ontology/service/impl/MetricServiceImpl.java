package com.datamaster.module.ontology.service.impl;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.common.datasource.mgmt.api.IDatasourceApiService;
import com.datamaster.common.datasource.mgmt.api.dto.DatasourceRespDTO;
import com.datamaster.common.exception.ServiceException;
import com.datamaster.module.ontology.controller.admin.metric.vo.*;
import com.datamaster.module.ontology.dal.dataobject.*;
import com.datamaster.module.ontology.dal.mapper.*;
import com.datamaster.module.ontology.service.IMetricService;
import com.datamaster.module.ontology.service.metric.MetricRules;
import com.datamaster.module.ontology.service.analysis.MetricQueryBuilder;
import com.datamaster.mybatis.core.util.MyBatisUtils;
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
    @Resource private OntologyMapper ontologyMapper;
    @Resource private com.datamaster.module.governance.api.flow.ISpaceFlowApi spaceFlows;

    @Override
    public PageResult<MetricRespVO> getPage(MetricPageReqVO req) {
        accessGuard.assertOntologyAccess(req.getOntologyId());
        PageResult<MetricDO> page = metricMapper.selectPage(req);
        return new PageResult<>(page.getRows().stream().map(this::response).collect(Collectors.toList()), page.getTotal());
    }

    @Override
    public PageResult<MetricRespVO> getPublishedPage(MetricPageReqVO req, String code) {
        MetricRules.require(req != null && req.getOntologyId() != null, "本体ID不能为空");
        MetricRules.require(code == null || code.length() <= 100 && code.matches("[A-Za-z][A-Za-z0-9_]*"), "指标编码检索格式无效");
        accessGuard.assertOntologyAccess(req.getOntologyId());
        MetricRules.require(req.getPageNum() != null && req.getPageNum() > 0
                && req.getPageSize() != null && req.getPageSize() >= 1 && req.getPageSize() <= 100,
                "分页参数无效");
        IPage<MetricDO> page = metricMapper.selectPublishedPage(MyBatisUtils.buildPage(req), req, code);
        List<MetricRespVO> published = page.getRecords().stream().map(metric -> {
            MetricRespVO snapshot = loadVersion(metric.getId(), metric.getPublishedVersion());
            MetricQueryBuilder.checkIdentity(
                    metric, snapshot, metric.getId(), metric.getPublishedVersion());
            return snapshot;
        }).collect(Collectors.toList());
        return new PageResult<>(published, page.getTotal());
    }

    @Override
    public MetricRespVO get(Long id) {
        MetricDO metric = accessible(id, false);
        MetricRespVO result = response(metric);
        if (metric.getPublishedVersion() > 0) {
            MetricRespVO publication = loadVersion(id, metric.getPublishedVersion());
            MetricQueryBuilder.checkIdentity(metric, publication, id, metric.getPublishedVersion());
            result.setSourceSnapshot(publication.getSourceSnapshot());
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(MetricSaveReqVO req) {
        MetricRules.require(req != null && req.getId() == null, "新增指标不能指定ID");
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
            MetricRules.require(metricMapper.insert(metric) == 1 && metric.getId() != null, "指标草稿保存失败，未取得有效记录");
        } catch (DuplicateKeyException ex) {
            throw new ServiceException("该本体下指标编码已存在");
        }
        return metric.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Integer update(MetricSaveReqVO req) {
        MetricRules.require(req != null && req.getId() != null, "修改指标必须指定ID");
        MetricDO metric = accessible(req.getId(), true);
        MetricRules.checkRevision(req.getRevision(), metric.getRevision());
        MetricRules.checkIdentity(metric, req);
        MetricSaveReqVO definition = MetricRules.normalize(req);
        validateDraftReferences(definition);
        String json = MetricRules.definitionJson(definition);
        if (json.equals(metric.getDefinitionJson())) return 0;
        applyDefinition(metric, definition);
        // Preserve PUBLISHED/DISABLED and the currently available immutable version.
        MetricRespVO publication = null;
        if (metric.getPublishedVersion() > 0) {
            publication = loadVersion(metric.getId(), metric.getPublishedVersion());
            MetricQueryBuilder.checkIdentity(metric, publication, metric.getId(), metric.getPublishedVersion());
        }
        metric.setDraftChanged(publication != null && !json.equals(MetricRules.definitionJson(publication)));
        metric.setRevision(MetricRules.nextCounter(metric.getRevision()));
        MetricRules.require(metricMapper.updateById(metric) == 1, "指标草稿保存失败，请重新加载后重试");
        return 1;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Integer delete(Long id) {
        MetricDO metric = accessible(id, true);
        MetricRules.require("DRAFT".equals(metric.getStatus()) && metric.getPublishedVersion() == 0,
                "只有从未发布的草稿指标可删除，已发布指标请停用");
        MetricRules.require(metricMapper.deleteById(id) == 1, "指标草稿删除失败，请重新加载后重试");
        return 1;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void validate(Long id) { validate(id, null); }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void validate(Long id, Integer revision) {
        MetricDO metric = accessible(id, true);
        // Legacy internal callers may omit revision; an explicitly supplied revision is never ignored.
        if (revision != null) MetricRules.checkRevision(revision, metric.getRevision());
        validateForPublication(response(metric));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void publish(Long id, Integer revision) {
        MetricDO metric = accessible(id, true);
        MetricRules.checkRevision(revision, metric.getRevision());
        MetricRespVO definition = response(metric);
        Map<String, Object> source = validateForPublication(definition);
        MetricRespVO currentPublication = null;
        if (metric.getPublishedVersion() > 0) {
            currentPublication = loadVersion(id, metric.getPublishedVersion());
            MetricQueryBuilder.checkIdentity(metric, currentPublication, id, metric.getPublishedVersion());
        }
        if ("PUBLISHED".equals(metric.getStatus()) && currentPublication != null
                && MetricRules.samePublishedDefinition(definition, source, currentPublication)) {
            return;
        }
        int nextVersion = MetricRules.nextCounter(metric.getPublishedVersion());
        int nextRevision = MetricRules.nextCounter(metric.getRevision());
        MetricRespVO snapshot = MetricRules.snapshot(definition, source, nextVersion, nextRevision);
        MetricVersionDO version = new MetricVersionDO();
        version.setMetricId(id);
        version.setVersion(nextVersion);
        version.setSnapshotJson(JSON.toJSONString(snapshot,
                com.alibaba.fastjson2.JSONWriter.Feature.WriteMapNullValue));
        // INSERT ONLY. Never update/upsert a historical snapshot.
        MetricRules.require(metricVersionMapper.insert(version) == 1, "指标发布快照保存失败");
        metric.setStatus("PUBLISHED");
        metric.setPublishedVersion(nextVersion);
        metric.setDraftChanged(false);
        metric.setRevision(nextRevision);
        MetricRules.require(metricMapper.updateById(metric) == 1, "指标发布状态更新失败");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void disable(Long id, Integer revision) {
        MetricDO metric = accessible(id, true);
        MetricRules.checkRevision(revision, metric.getRevision());
        if ("DISABLED".equals(metric.getStatus())) return;
        MetricRules.require(metric.getPublishedVersion() > 0, "未发布的草稿不能停用");
        metric.setStatus("DISABLED");
        metric.setRevision(MetricRules.nextCounter(metric.getRevision()));
        MetricRules.require(metricMapper.updateById(metric) == 1, "指标停用状态更新失败");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Integer restoreDraft(Long id, Integer version, Integer revision) {
        MetricDO current = accessible(id, true);
        MetricRules.checkRevision(revision, current.getRevision());
        MetricRules.require(current.getPublishedVersion() != null && current.getPublishedVersion() > 0, "未发布指标没有可恢复历史");
        MetricRespVO historical = loadVersion(id, version);
        MetricQueryBuilder.checkIdentity(current, historical, id, version);
        MetricSaveReqVO definition = MetricRules.normalize(historical);
        validateDraftReferences(definition);
        String json = MetricRules.definitionJson(definition);
        if (json.equals(current.getDefinitionJson())) return 0;
        applyDefinition(current, definition);
        current.setRevision(MetricRules.nextCounter(current.getRevision()));
        MetricRespVO latest = loadVersion(id, current.getPublishedVersion());
        MetricQueryBuilder.checkIdentity(current, latest, id, current.getPublishedVersion());
        current.setDraftChanged(!json.equals(MetricRules.definitionJson(latest)));
        if (metricMapper.updateById(current) != 1) throw new ServiceException("指标草稿恢复未更新记录");
        return 1;
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
        MetricDO current = accessible(id, false);
        MetricRespVO publication = loadVersion(id, version);
        MetricQueryBuilder.checkIdentity(current, publication, id, version);
        return publication;
    }

    private MetricRespVO loadVersion(Long id, Integer version) {
        MetricRules.require(version != null && version > 0, "发布版本号必须大于0");
        MetricVersionDO row = metricVersionMapper.selectOne(new LambdaQueryWrapperX<MetricVersionDO>()
                .eq(MetricVersionDO::getMetricId, id).eq(MetricVersionDO::getVersion, version));
        if (row == null) throw new ServiceException("指标发布版本不存在");
        try {
            MetricRespVO publication = JSON.parseObject(row.getSnapshotJson(), MetricRespVO.class);
            MetricRules.require(publication != null && Objects.equals(id, publication.getId())
                    && Objects.equals(version, publication.getPublishedVersion()) && "PUBLISHED".equals(publication.getStatus()), "指标发布快照身份无效");
            return publication;
        } catch (ServiceException invalid) { throw invalid; }
        catch (RuntimeException malformed) { throw new ServiceException("指标发布快照不可用"); }
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
        MetricSaveReqVO definition;
        try {
            definition = JSON.parseObject(metric.getDefinitionJson(), MetricSaveReqVO.class);
            MetricRules.require(definition != null, "指标草稿定义缺失，请联系管理员核对");
        } catch (ServiceException invalid) { throw invalid; }
        catch (RuntimeException malformed) { throw new ServiceException("指标草稿定义损坏，请联系管理员核对"); }
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

    /** Metadata only. Does not connect to the source, execute SQL, verify data or grant source access. */
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
        MetricRules.require(datasource != null && !Boolean.FALSE.equals(datasource.getValidFlag()), "指标来源数据源不存在或已停用");
        OntologyDO ontology = ontologyMapper.selectById(vo.getOntologyId());
        MetricRules.require(ontology != null && (ontology.getSpaceId() != null || MetricRules.notBlank(ontology.getSpaceCode())), "本体必须明确可信空间");
        spaceFlows.policy(ontology.getSpaceId(), ontology.getSpaceCode());
        com.datamaster.module.ontology.service.analysis.MetricSqlDialect dialect = com.datamaster.module.ontology.service.analysis.MetricSqlDialect.of(datasource.getDatasourceType());
        dialect.validateSource(table.getDatabaseName(), table.getSchemaName(), table.getTableName(), false);
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
            MetricRules.require(column != null && property != null, "属性或当前概念表字段绑定已失效: " + id);
            dialect.quote(column.getColumnName());
            MetricQueryBuilder.validateDataType(property.getDataType());
            if (Objects.equals(id, vo.getValuePropertyId())
                    && ("SUM".equals(vo.getAggregation()) || "AVG".equals(vo.getAggregation()))) {
                MetricRules.require(MetricRules.numeric(property.getDataType()), "SUM/AVG必须使用数值属性");
            }
            if (com.datamaster.module.ontology.service.metric.MetricDerivedRules.active(vo)) {
                if (Objects.equals(id, vo.getValuePropertyId()) && !vo.getAggregation().startsWith("COUNT")) MetricRules.require(MetricRules.numeric(property.getDataType()), "派生主聚合必须返回数值");
                if (Objects.equals(id, vo.getSecondaryValuePropertyId()) && !vo.getSecondaryAggregation().startsWith("COUNT")) MetricRules.require(MetricRules.numeric(property.getDataType()), "派生第二聚合必须返回数值");
            }
            if (Objects.equals(id, vo.getTimePropertyId())) {
                MetricRules.require(MetricRules.temporal(property.getDataType()), "时间属性必须是date/datetime/timestamp类型");
                if ("date".equalsIgnoreCase(property.getDataType())) MetricRules.require(com.datamaster.module.ontology.service.analysis.MetricTimeRules.businessShiftMinutes(vo) == 0, "date属性不能配置不同时区偏移");
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
        snapshot.put("sourceMode", "SPACE_FLOW");
        return snapshot;
    }
}
