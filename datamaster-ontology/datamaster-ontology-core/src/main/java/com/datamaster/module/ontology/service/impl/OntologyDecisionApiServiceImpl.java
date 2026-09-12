package com.datamaster.module.ontology.service.impl;

import com.datamaster.module.ontology.api.IOntologyDecisionApiService;
import com.datamaster.module.ontology.api.dto.OntologyDecisionContextDTO;
import com.datamaster.module.ontology.dal.dataobject.ActionDO;
import com.datamaster.module.ontology.dal.dataobject.ConceptDO;
import com.datamaster.module.ontology.dal.dataobject.OntologyDO;
import com.datamaster.module.ontology.dal.dataobject.PropertyDO;
import com.datamaster.module.ontology.dal.dataobject.RelationDO;
import com.datamaster.module.ontology.dal.dataobject.ConceptTableDO;
import com.datamaster.module.ontology.dal.dataobject.PropertyColumnDO;
import com.datamaster.module.ontology.dal.dataobject.RelationColumnDO;
import com.datamaster.module.ontology.dal.dataobject.RelationTableDO;
import com.datamaster.module.ontology.dal.mapper.ActionMapper;
import com.datamaster.module.ontology.dal.mapper.ConceptMapper;
import com.datamaster.module.ontology.dal.mapper.ConceptTableMapper;
import com.datamaster.module.ontology.dal.mapper.OntologyMapper;
import com.datamaster.module.ontology.dal.mapper.PropertyColumnMapper;
import com.datamaster.module.ontology.dal.mapper.PropertyMapper;
import com.datamaster.module.ontology.dal.mapper.RelationMapper;
import com.datamaster.module.ontology.dal.mapper.RelationColumnMapper;
import com.datamaster.module.ontology.dal.mapper.RelationTableMapper;
import com.datamaster.common.datasource.mgmt.api.IDatasourceApiService;
import com.datamaster.common.datasource.mgmt.api.dto.DatasourceRespDTO;
import com.datamaster.mybatis.core.query.LambdaQueryWrapperX;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 本体决策上下文只读实现。 */
@Service
public class OntologyDecisionApiServiceImpl implements IOntologyDecisionApiService {

    @Resource private OntologyMapper ontologyMapper;
    @Resource private ConceptMapper conceptMapper;
    @Resource private PropertyMapper propertyMapper;
    @Resource private ConceptTableMapper conceptTableMapper;
    @Resource private PropertyColumnMapper propertyColumnMapper;
    @Resource private RelationMapper relationMapper;
    @Resource private RelationTableMapper relationTableMapper;
    @Resource private RelationColumnMapper relationColumnMapper;
    @Resource private ActionMapper actionMapper;
    @Resource private IDatasourceApiService datasourceApiService;

    @Override
    public OntologyDecisionContextDTO getDecisionContext(Long ontologyId) {
        if (ontologyId == null) return null;
        OntologyDO ontology = ontologyMapper.selectById(ontologyId);
        if (ontology == null) return null;

        OntologyDecisionContextDTO result = new OntologyDecisionContextDTO();
        result.setOntologyId(ontology.getId());
        result.setOntologyName(ontology.getName());
        result.setOntologyCode(ontology.getCode());
        result.setOntologyDescription(ontology.getDescription());
        Map<Long, DatasourceRespDTO> datasourceCache = new LinkedHashMap<>();
        Map<Long, ConceptTableDO> conceptTableCache = new LinkedHashMap<>();

        List<ConceptDO> concepts = conceptMapper.selectList(new LambdaQueryWrapperX<ConceptDO>()
                .eq(ConceptDO::getOntologyId, ontologyId)
                .orderByAsc(ConceptDO::getSortOrder));
        List<OntologyDecisionContextDTO.ConceptDTO> conceptDTOs = new ArrayList<>();
        if (concepts != null) {
            for (ConceptDO concept : concepts) {
                OntologyDecisionContextDTO.ConceptDTO dto = new OntologyDecisionContextDTO.ConceptDTO();
                dto.setId(concept.getId());
                dto.setName(concept.getName());
                dto.setCode(concept.getCode());
                dto.setDescription(concept.getDescription());
                List<ConceptTableDO> conceptTables = conceptTableMapper.selectByConceptId(concept.getId());
                List<OntologyDecisionContextDTO.TableBindingDTO> tableBindings = new ArrayList<>();
                if (conceptTables != null) {
                    for (ConceptTableDO table : conceptTables) {
                        conceptTableCache.put(table.getId(), table);
                        tableBindings.add(toTableBinding(table, datasourceCache));
                    }
                }
                dto.setTableBindings(tableBindings);
                List<PropertyDO> properties = propertyMapper.selectList(new LambdaQueryWrapperX<PropertyDO>()
                        .eq(PropertyDO::getConceptId, concept.getId())
                        .orderByAsc(PropertyDO::getSortOrder));
                List<OntologyDecisionContextDTO.PropertyDTO> propertyDTOs = new ArrayList<>();
                if (properties != null) {
                    for (PropertyDO property : properties) {
                        OntologyDecisionContextDTO.PropertyDTO item = new OntologyDecisionContextDTO.PropertyDTO();
                        item.setId(property.getId());
                        item.setName(property.getName());
                        item.setCode(property.getCode());
                        item.setDataType(property.getDataType());
                        item.setDescription(property.getDescription());
                        item.setPrimary(property.getIsPrimary());
                        item.setRequired(property.getIsRequired());
                        List<PropertyColumnDO> columns = propertyColumnMapper.selectByPropertyId(property.getId());
                        List<OntologyDecisionContextDTO.ColumnBindingDTO> columnBindings = new ArrayList<>();
                        if (columns != null) {
                            for (PropertyColumnDO column : columns) {
                                ConceptTableDO table = conceptTableCache.get(column.getConceptTableId());
                                if (table == null && column.getConceptTableId() != null) {
                                    table = conceptTableMapper.selectById(column.getConceptTableId());
                                    if (table != null) conceptTableCache.put(table.getId(), table);
                                }
                                columnBindings.add(toColumnBinding(column, table, datasourceCache));
                            }
                        }
                        item.setColumnBindings(columnBindings);
                        propertyDTOs.add(item);
                    }
                }
                dto.setProperties(propertyDTOs);
                conceptDTOs.add(dto);
            }
        }
        result.setConcepts(conceptDTOs);

        List<RelationDO> relations = relationMapper.selectList(new LambdaQueryWrapperX<RelationDO>()
                .eq(RelationDO::getOntologyId, ontologyId)
                .orderByAsc(RelationDO::getSortOrder));
        List<OntologyDecisionContextDTO.RelationDTO> relationDTOs = new ArrayList<>();
        if (relations != null) {
            for (RelationDO relation : relations) {
                OntologyDecisionContextDTO.RelationDTO dto = new OntologyDecisionContextDTO.RelationDTO();
                dto.setId(relation.getId());
                dto.setName(relation.getName());
                dto.setCode(relation.getCode());
                dto.setRelationType(relation.getRelationType());
                dto.setSourceConceptId(relation.getSourceConceptId());
                dto.setTargetConceptId(relation.getTargetConceptId());
                ConceptDO source = relation.getSourceConceptId() == null ? null : conceptMapper.selectById(relation.getSourceConceptId());
                ConceptDO target = relation.getTargetConceptId() == null ? null : conceptMapper.selectById(relation.getTargetConceptId());
                if (source != null) {
                    dto.setSourceConceptName(source.getName());
                    dto.setSourceConceptCode(source.getCode());
                }
                if (target != null) {
                    dto.setTargetConceptName(target.getName());
                    dto.setTargetConceptCode(target.getCode());
                }
                dto.setDescription(relation.getDescription());
                List<RelationTableDO> relationTables = relationTableMapper.selectByRelationId(relation.getId());
                List<OntologyDecisionContextDTO.TableBindingDTO> relationTableBindings = new ArrayList<>();
                if (relationTables != null) {
                    for (RelationTableDO table : relationTables) {
                        relationTableBindings.add(toTableBinding(table, datasourceCache));
                    }
                }
                dto.setTableBindings(relationTableBindings);
                List<RelationColumnDO> relationColumns = relationColumnMapper.selectByRelationId(relation.getId());
                List<OntologyDecisionContextDTO.RelationColumnBindingDTO> relationColumnBindings = new ArrayList<>();
                if (relationColumns != null) {
                    for (RelationColumnDO column : relationColumns) {
                        ConceptTableDO sourceTable = conceptTableCache.get(column.getSourceConceptTableId());
                        if (sourceTable == null && column.getSourceConceptTableId() != null) {
                            sourceTable = conceptTableMapper.selectById(column.getSourceConceptTableId());
                            if (sourceTable != null) conceptTableCache.put(sourceTable.getId(), sourceTable);
                        }
                        ConceptTableDO targetTable = conceptTableCache.get(column.getTargetConceptTableId());
                        if (targetTable == null && column.getTargetConceptTableId() != null) {
                            targetTable = conceptTableMapper.selectById(column.getTargetConceptTableId());
                            if (targetTable != null) conceptTableCache.put(targetTable.getId(), targetTable);
                        }
                        relationColumnBindings.add(toRelationColumnBinding(column, sourceTable, targetTable, datasourceCache));
                    }
                }
                dto.setColumnBindings(relationColumnBindings);
                relationDTOs.add(dto);
            }
        }
        result.setRelations(relationDTOs);

        List<ActionDO> actions = actionMapper.selectByOntologyId(ontologyId);
        List<OntologyDecisionContextDTO.ActionDTO> actionDTOs = new ArrayList<>();
        if (actions != null) {
            for (ActionDO action : actions) {
                OntologyDecisionContextDTO.ActionDTO dto = new OntologyDecisionContextDTO.ActionDTO();
                dto.setId(action.getId());
                dto.setName(action.getName());
                dto.setActionType(action.getActionType());
                dto.setTriggerConceptId(action.getConceptId());
                dto.setDescription(action.getDescription());
                dto.setParamConfig(action.getParamConfig());
                dto.setExecutionSteps(action.getExecutionSteps());
                dto.setSubmissionCriteria(action.getSubmissionCriteria());
                dto.setNeedsApproval(action.getNeedsApproval());
                dto.setApprovalLevels(action.getApprovalLevels());
                actionDTOs.add(dto);
            }
        }
        result.setActions(actionDTOs);
        List<OntologyDecisionContextDTO.DatasourceDTO> datasourceDTOs = new ArrayList<>();
        for (DatasourceRespDTO datasource : datasourceCache.values()) {
            if (datasource == null) continue;
            OntologyDecisionContextDTO.DatasourceDTO item = new OntologyDecisionContextDTO.DatasourceDTO();
            item.setId(datasource.getId());
            item.setName(datasource.getDatasourceName());
            item.setType(datasource.getDatasourceType());
            item.setIp(datasource.getIp());
            item.setPort(datasource.getPort());
            datasourceDTOs.add(item);
        }
        result.setDatasources(datasourceDTOs);
        return result;
    }

    private OntologyDecisionContextDTO.TableBindingDTO toTableBinding(ConceptTableDO table,
                                                                        Map<Long, DatasourceRespDTO> datasourceCache) {
        OntologyDecisionContextDTO.TableBindingDTO item = new OntologyDecisionContextDTO.TableBindingDTO();
        item.setId(table.getId());
        item.setDatasourceId(table.getDatasourceId());
        item.setDatabaseName(table.getDatabaseName());
        item.setSchemaName(table.getSchemaName());
        item.setTableName(table.getTableName());
        item.setDatasourceName(resolveDatasourceName(table.getDatasourceId(), datasourceCache));
        item.setDatasourceType(resolveDatasourceType(table.getDatasourceId(), datasourceCache));
        return item;
    }

    private OntologyDecisionContextDTO.TableBindingDTO toTableBinding(RelationTableDO table,
                                                                        Map<Long, DatasourceRespDTO> datasourceCache) {
        OntologyDecisionContextDTO.TableBindingDTO item = new OntologyDecisionContextDTO.TableBindingDTO();
        item.setId(table.getId());
        item.setDatasourceId(table.getDatasourceId());
        item.setDatabaseName(table.getDatabaseName());
        item.setSchemaName(table.getSchemaName());
        item.setTableName(table.getTableName());
        item.setColumnNames(table.getColumnNames());
        item.setDatasourceName(resolveDatasourceName(table.getDatasourceId(), datasourceCache));
        item.setDatasourceType(resolveDatasourceType(table.getDatasourceId(), datasourceCache));
        return item;
    }

    private OntologyDecisionContextDTO.ColumnBindingDTO toColumnBinding(PropertyColumnDO column,
                                                                          ConceptTableDO table,
                                                                          Map<Long, DatasourceRespDTO> datasourceCache) {
        OntologyDecisionContextDTO.ColumnBindingDTO item = new OntologyDecisionContextDTO.ColumnBindingDTO();
        item.setId(column.getId());
        item.setPropertyId(column.getPropertyId());
        item.setConceptTableId(column.getConceptTableId());
        item.setColumnName(column.getColumnName());
        if (table != null) {
            item.setDatasourceId(table.getDatasourceId());
            item.setDatabaseName(table.getDatabaseName());
            item.setSchemaName(table.getSchemaName());
            item.setTableName(table.getTableName());
            item.setDatasourceName(resolveDatasourceName(table.getDatasourceId(), datasourceCache));
            item.setDatasourceType(resolveDatasourceType(table.getDatasourceId(), datasourceCache));
        }
        return item;
    }

    private OntologyDecisionContextDTO.RelationColumnBindingDTO toRelationColumnBinding(RelationColumnDO column,
                                                                                          ConceptTableDO sourceTable,
                                                                                          ConceptTableDO targetTable,
                                                                                          Map<Long, DatasourceRespDTO> datasourceCache) {
        OntologyDecisionContextDTO.RelationColumnBindingDTO item = new OntologyDecisionContextDTO.RelationColumnBindingDTO();
        item.setId(column.getId());
        item.setRelationId(column.getRelationId());
        item.setSourceConceptTableId(column.getSourceConceptTableId());
        item.setSourceColumn(column.getSourceColumn());
        item.setTargetConceptTableId(column.getTargetConceptTableId());
        item.setTargetColumn(column.getTargetColumn());
        if (sourceTable != null) {
            item.setSourceDatasourceId(sourceTable.getDatasourceId());
            item.setSourceDatasourceName(resolveDatasourceName(sourceTable.getDatasourceId(), datasourceCache));
            item.setSourceDatabaseName(sourceTable.getDatabaseName());
            item.setSourceSchemaName(sourceTable.getSchemaName());
            item.setSourceTableName(sourceTable.getTableName());
        }
        if (targetTable != null) {
            item.setTargetDatasourceId(targetTable.getDatasourceId());
            item.setTargetDatasourceName(resolveDatasourceName(targetTable.getDatasourceId(), datasourceCache));
            item.setTargetDatabaseName(targetTable.getDatabaseName());
            item.setTargetSchemaName(targetTable.getSchemaName());
            item.setTargetTableName(targetTable.getTableName());
        }
        return item;
    }

    private DatasourceRespDTO datasource(Long datasourceId, Map<Long, DatasourceRespDTO> cache) {
        if (datasourceId == null) return null;
        if (!cache.containsKey(datasourceId)) {
            cache.put(datasourceId, datasourceApiService.getDatasourceById(datasourceId));
        }
        return cache.get(datasourceId);
    }

    private String resolveDatasourceName(Long datasourceId, Map<Long, DatasourceRespDTO> cache) {
        DatasourceRespDTO datasource = datasource(datasourceId, cache);
        return datasource == null ? null : datasource.getDatasourceName();
    }

    private String resolveDatasourceType(Long datasourceId, Map<Long, DatasourceRespDTO> cache) {
        DatasourceRespDTO datasource = datasource(datasourceId, cache);
        return datasource == null ? null : datasource.getDatasourceType();
    }
}
