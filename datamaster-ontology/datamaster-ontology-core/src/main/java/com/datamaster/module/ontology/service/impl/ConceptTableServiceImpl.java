package com.datamaster.module.ontology.service.impl;

import com.datamaster.common.core.page.PageResult;
import com.datamaster.common.database.DataSourceFactory;
import com.datamaster.common.database.DbQuery;
import com.datamaster.common.database.constants.DbQueryProperty;
import com.datamaster.common.database.core.DbColumn;
import com.datamaster.common.datasource.mgmt.api.IDatasourceApiService;
import com.datamaster.common.datasource.mgmt.api.dto.DatasourceRespDTO;
import com.datamaster.module.ontology.convert.ConceptTableConvert;
import com.datamaster.module.ontology.controller.admin.concepttable.vo.ConceptTablePageReqVO;
import com.datamaster.module.ontology.controller.admin.concepttable.vo.ConceptTablePreviewRespVO;
import com.datamaster.module.ontology.controller.admin.concepttable.vo.ConceptTableRespVO;
import com.datamaster.module.ontology.controller.admin.concepttable.vo.ConceptTableSaveReqVO;
import com.datamaster.module.ontology.dal.dataobject.ConceptTableDO;
import com.datamaster.module.ontology.dal.mapper.ConceptTableMapper;
import com.datamaster.module.ontology.dal.mapper.PropertyColumnMapper;
import com.datamaster.module.ontology.service.IConceptTableService;
import com.datamaster.module.ontology.service.query.TypedQueryBuilder;
import com.datamaster.module.ontology.service.query.TypedQuerySpec;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import org.springframework.transaction.annotation.Transactional;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 概念表绑定 Service 实现
 */
@Service
@Validated
public class ConceptTableServiceImpl implements IConceptTableService {

    @Resource
    private ConceptTableMapper conceptTableMapper;
    @Resource
    private PropertyColumnMapper propertyColumnMapper;
    @Resource
    private IDatasourceApiService datasourceApiService;
    @Resource
    private DataSourceFactory dataSourceFactory;
    @Resource
    private MetricReferenceGuard metricReferenceGuard;
    @Resource private com.datamaster.module.ontology.service.analysis.ConceptTableConsumptionGuard consumption;
    @Resource private com.datamaster.module.ontology.dal.mapper.MetricMetadataLockMapper metadataLocks;

    @Override
    public PageResult<ConceptTableRespVO> getConceptTablePage(ConceptTablePageReqVO pageReqVO) {
        ConceptTableDO scope = new ConceptTableDO(); scope.setConceptId(pageReqVO.getConceptId()); consumption.ownership(scope);
        PageResult<ConceptTableDO> pageResult = conceptTableMapper.selectPage(pageReqVO);
        List<ConceptTableRespVO> list = ConceptTableConvert.INSTANCE.convertList(pageResult.getRows());
        return new PageResult<>(list, pageResult.getTotal());
    }

    @Override
    public List<ConceptTableRespVO> getConceptTableByConceptId(Long conceptId) {
        ConceptTableDO scope = new ConceptTableDO(); scope.setConceptId(conceptId); consumption.ownership(scope);
        List<ConceptTableDO> list = conceptTableMapper.selectByConceptId(conceptId);
        return ConceptTableConvert.INSTANCE.convertList(list);
    }

    @Override
    public ConceptTableRespVO getConceptTableById(Long id) {
        ConceptTableDO entity = conceptTableMapper.selectById(id);
        if (entity != null) consumption.ownership(entity);
        return entity != null ? ConceptTableConvert.INSTANCE.convert(entity) : null;
    }

    @Override
    public List<DbColumn> getPhysicalColumns(Long id) {
        ConceptTableDO entity = conceptTableMapper.selectById(id);
        if (entity == null) {
            throw new RuntimeException("概念表绑定不存在: id=" + id);
        }
        com.datamaster.module.ontology.dal.dataobject.OntologyDO ontology = consumption.source(entity);
        consumption.admit(entity, ontology, null);
        DatasourceRespDTO ds = datasourceApiService.getDatasourceById(entity.getDatasourceId());
        if (ds == null || Boolean.FALSE.equals(ds.getValidFlag())) {
            throw new RuntimeException("数据源不存在: id=" + entity.getDatasourceId());
        }
        DbQueryProperty property = new DbQueryProperty(
                ds.getDatasourceType(), ds.getIp(), ds.getPort(), ds.getDatasourceConfig());
        consumption.connection(entity, property);
        DbQuery dbQuery = dataSourceFactory.createDbQuery(property);
        try {
            List<DbColumn> columns = dbQuery.getTableColumns(property, entity.getTableName());
            List<String> names = new ArrayList<>(); if (columns != null) for (DbColumn column : columns) names.add(column.getColName());
            consumption.admit(entity, ontology, names);
            return columns;
        } finally {
            if (dbQuery != null) {
                dbQuery.close();
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createConceptTable(ConceptTableSaveReqVO createReqVO) {
        if (createReqVO == null) throw new com.datamaster.common.exception.ServiceException("概念表定义不能为空");
        ConceptTableDO entity = ConceptTableConvert.INSTANCE.convert(createReqVO);
        if (entity.getId() != null) throw new com.datamaster.common.exception.ServiceException("新建概念表不得指定ID");
        com.datamaster.module.ontology.dal.dataobject.OntologyDO owner = consumption.source(entity);
        if (metadataLocks.lockOntology(owner.getId()) == null) throw new com.datamaster.common.exception.ServiceException("本体不存在");
        consumption.source(entity);
        if (conceptTableMapper.insert(entity) != 1) throw new com.datamaster.common.exception.ServiceException("概念表保存失败");
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Integer updateConceptTable(ConceptTableSaveReqVO updateReqVO) {
        if (updateReqVO == null || updateReqVO.getId() == null) throw new com.datamaster.common.exception.ServiceException("编辑概念表必须指定ID");
        ConceptTableDO previous = conceptTableMapper.selectById(updateReqVO.getId());
        com.datamaster.module.ontology.dal.dataobject.OntologyDO owner = consumption.ownership(previous);
        if (metadataLocks.lockOntology(owner.getId()) == null) throw new com.datamaster.common.exception.ServiceException("本体不存在");
        previous = conceptTableMapper.selectById(updateReqVO.getId());
        consumption.ownership(previous);
        ConceptTableDO entity = ConceptTableConvert.INSTANCE.convert(updateReqVO);
        if (!java.util.Objects.equals(previous.getConceptId(), entity.getConceptId())) throw new com.datamaster.common.exception.ServiceException("普通编辑不能变更概念表所属概念");
        consumption.source(entity);
        return conceptTableMapper.updateById(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Integer deleteConceptTable(Long id) {
        consumption.ownership(conceptTableMapper.selectById(id));
        metricReferenceGuard.assertTableBindingDeletable(id);
        propertyColumnMapper.deleteByConceptTableId(id);
        return conceptTableMapper.deleteById(id);
    }

    @Override
    public ConceptTablePreviewRespVO previewData(Long id, Integer limit, String filters) {
        ConceptTableDO entity = conceptTableMapper.selectById(id);
        if (entity == null) {
            throw new RuntimeException("概念表绑定不存在: id=" + id);
        }
        if (entity.getDatasourceId() == null) {
            throw new RuntimeException("概念表绑定缺少数据源配置: id=" + id);
        }
        com.datamaster.module.ontology.dal.dataobject.OntologyDO ontology = consumption.source(entity);
        consumption.admit(entity, ontology, null);
        DatasourceRespDTO ds = datasourceApiService.getDatasourceById(entity.getDatasourceId());
        if (ds == null || Boolean.FALSE.equals(ds.getValidFlag())) {
            throw new RuntimeException("数据源不存在: id=" + entity.getDatasourceId());
        }
        DbQueryProperty property = new DbQueryProperty(
                ds.getDatasourceType(), ds.getIp(), ds.getPort(), ds.getDatasourceConfig());
        consumption.connection(entity, property);
        DbQuery dbQuery = dataSourceFactory.createDbQuery(property);
        try {
            int safeLimit = (limit == null || limit <= 0) ? 20 : Math.min(limit, 100);
            // 必须取得可验证物理字段；字段读取失败时拒绝预览而非回退SELECT *。
            List<String> columns = new ArrayList<>();
            java.util.Set<String> textColumns = new java.util.HashSet<>();
            try {
                for (DbColumn col : dbQuery.getTableColumns(property, entity.getTableName())) {
                    columns.add(col.getColName());
                    if (TypedQueryBuilder.isTextType(col.getDataType())) {
                        textColumns.add(col.getColName());
                    }
                }
            } catch (Exception e) {
                columns = Collections.emptyList();
            }
            // 类型化过滤/排序/投影/关键字：统一白名单校验 + NamedParameter 防注入；
            // 跨列模糊仅作用于文本列，避免数值/日期列 LIKE 类型错误
            consumption.admit(entity, ontology, columns);
            List<String> whitelist = Collections.unmodifiableList(columns);
            TypedQuerySpec spec = TypedQueryBuilder.parseStrict(filters);
            TypedQueryBuilder.Compiled compiled = TypedQueryBuilder.compileStrict(spec, whitelist, textColumns);

            String physicalTable = consumption.physicalTable(entity, property);
            StringBuilder sql = new StringBuilder();
            if (compiled.hasProjection()) {
                sql.append("SELECT ").append(compiled.projection).append(" FROM ").append(physicalTable);
            } else {
                sql.append("SELECT ").append(String.join(", ", columns)).append(" FROM ").append(physicalTable);
            }
            if (compiled.hasWhere()) {
                sql.append(" WHERE ").append(compiled.where);
            }
            if (compiled.hasOrderBy()) {
                sql.append(" ORDER BY ").append(compiled.orderBy);
            }
            com.datamaster.common.database.core.PageResult<Map<String,Object>> page = dbQuery.queryByPage(sql.toString(), compiled.params, 0, safeLimit, 0);
            if (page == null || page.getData() == null) throw new com.datamaster.common.exception.ServiceException("概念预览未取得有效结果，不能当作空数据");
            List<Map<String,Object>> rows = page.getData();
            ConceptTablePreviewRespVO respVO = new ConceptTablePreviewRespVO();
            respVO.setTableName(entity.getTableName());
            respVO.setColumns(compiled.hasProjection() ? new ArrayList<>(spec.getColumns()) : columns);
            respVO.setRows(consumption.mask(entity, ontology, rows));
            return respVO;
        } finally {
            if (dbQuery != null) {
                try {
                    dbQuery.close();
                } catch (Exception e) {
                    // 忽略关闭失败
                }
            }
        }
    }
}
