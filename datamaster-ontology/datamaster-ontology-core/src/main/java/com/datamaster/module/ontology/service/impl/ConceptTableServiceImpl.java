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

    @Override
    public PageResult<ConceptTableRespVO> getConceptTablePage(ConceptTablePageReqVO pageReqVO) {
        PageResult<ConceptTableDO> pageResult = conceptTableMapper.selectPage(pageReqVO);
        List<ConceptTableRespVO> list = ConceptTableConvert.INSTANCE.convertList(pageResult.getRows());
        return new PageResult<>(list, pageResult.getTotal());
    }

    @Override
    public List<ConceptTableRespVO> getConceptTableByConceptId(Long conceptId) {
        List<ConceptTableDO> list = conceptTableMapper.selectByConceptId(conceptId);
        return ConceptTableConvert.INSTANCE.convertList(list);
    }

    @Override
    public ConceptTableRespVO getConceptTableById(Long id) {
        ConceptTableDO entity = conceptTableMapper.selectById(id);
        return entity != null ? ConceptTableConvert.INSTANCE.convert(entity) : null;
    }

    @Override
    public List<DbColumn> getPhysicalColumns(Long id) {
        ConceptTableDO entity = conceptTableMapper.selectById(id);
        if (entity == null) {
            throw new RuntimeException("概念表绑定不存在: id=" + id);
        }
        DatasourceRespDTO ds = datasourceApiService.getDatasourceById(entity.getDatasourceId());
        if (ds == null) {
            throw new RuntimeException("数据源不存在: id=" + entity.getDatasourceId());
        }
        DbQueryProperty property = new DbQueryProperty(
                ds.getDatasourceType(), ds.getIp(), ds.getPort(), ds.getDatasourceConfig());
        DbQuery dbQuery = dataSourceFactory.createDbQuery(property);
        try {
            return dbQuery.getTableColumns(property, entity.getTableName());
        } finally {
            if (dbQuery != null) {
                dbQuery.close();
            }
        }
    }

    @Override
    public Long createConceptTable(ConceptTableSaveReqVO createReqVO) {
        ConceptTableDO entity = ConceptTableConvert.INSTANCE.convert(createReqVO);
        conceptTableMapper.insert(entity);
        return entity.getId();
    }

    @Override
    public Integer updateConceptTable(ConceptTableSaveReqVO updateReqVO) {
        ConceptTableDO entity = ConceptTableConvert.INSTANCE.convert(updateReqVO);
        return conceptTableMapper.updateById(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Integer deleteConceptTable(Long id) {
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
        DatasourceRespDTO ds = datasourceApiService.getDatasourceById(entity.getDatasourceId());
        if (ds == null) {
            throw new RuntimeException("数据源不存在: id=" + entity.getDatasourceId());
        }
        DbQueryProperty property = new DbQueryProperty(
                ds.getDatasourceType(), ds.getIp(), ds.getPort(), ds.getDatasourceConfig());
        DbQuery dbQuery = dataSourceFactory.createDbQuery(property);
        try {
            int safeLimit = (limit == null || limit <= 0) ? 20 : Math.min(limit, 100);
            // 列以元数据为准（保证顺序与类型），取不到元数据时退化为按首行 key 兜底
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
            List<String> whitelist = columns.isEmpty() ? Collections.emptyList()
                    : Collections.unmodifiableList(columns);
            TypedQuerySpec spec = TypedQueryBuilder.parse(filters);
            TypedQueryBuilder.Compiled compiled = TypedQueryBuilder.compile(spec, whitelist, textColumns);

            StringBuilder sql = new StringBuilder();
            if (compiled.hasProjection()) {
                sql.append("SELECT ").append(compiled.projection).append(" FROM ").append(entity.getTableName());
            } else {
                sql.append("SELECT * FROM ").append(entity.getTableName());
            }
            if (compiled.hasWhere()) {
                sql.append(" WHERE ").append(compiled.where);
            }
            if (compiled.hasOrderBy()) {
                sql.append(" ORDER BY ").append(compiled.orderBy);
            }
            sql.append(" LIMIT ").append(safeLimit);
            List<Map<String, Object>> rows = dbQuery.queryList(sql.toString(), compiled.params, 0);
            if (columns.isEmpty() && !rows.isEmpty()) {
                columns = new ArrayList<>(rows.get(0).keySet());
            }
            ConceptTablePreviewRespVO respVO = new ConceptTablePreviewRespVO();
            respVO.setTableName(entity.getTableName());
            respVO.setColumns(columns);
            respVO.setRows(rows);
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
