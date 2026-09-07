package com.datamaster.module.ontology.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.datamaster.common.exception.ServiceException;
import com.datamaster.common.utils.StringUtils;
import com.datamaster.metadata.api.column.dto.CatalogColumnRespDTO;
import com.datamaster.metadata.api.service.column.CatalogColumnApiService;
import com.datamaster.metadata.api.service.table.CatalogTableApiService;
import com.datamaster.metadata.api.table.dto.CatalogTableRespDTO;
import com.datamaster.module.ai.service.skill.IAiModelGatewayService;
import com.datamaster.module.ontology.controller.admin.aigenerate.vo.AiActionPreviewReqVO;
import com.datamaster.module.ontology.controller.admin.aigenerate.vo.AiActionPreviewRespVO;
import com.datamaster.module.ontology.controller.admin.aigenerate.vo.OntologyAiGenerateReqVO;
import com.datamaster.module.ontology.controller.admin.aigenerate.vo.OntologyAiGenerateRespVO;
import com.datamaster.module.ontology.controller.admin.concept.vo.ConceptSaveReqVO;
import com.datamaster.module.ontology.controller.admin.concepttable.vo.ConceptTableSaveReqVO;
import com.datamaster.module.ontology.controller.admin.ontology.vo.OntologySaveReqVO;
import com.datamaster.module.ontology.controller.admin.property.vo.PropertySaveReqVO;
import com.datamaster.module.ontology.controller.admin.propertycolumn.vo.PropertyColumnSaveReqVO;
import com.datamaster.module.ontology.controller.admin.relation.vo.RelationSaveReqVO;
import com.datamaster.module.ontology.controller.admin.relationcolumn.vo.RelationColumnSaveReqVO;
import com.datamaster.module.ontology.dal.dataobject.ConceptDO;
import com.datamaster.module.ontology.dal.dataobject.OntologyDO;
import com.datamaster.module.ontology.dal.dataobject.PropertyDO;
import com.datamaster.module.ontology.dal.dataobject.RelationDO;
import com.datamaster.module.ontology.dal.dataobject.RelationTableDO;
import com.datamaster.module.ontology.dal.mapper.ConceptMapper;
import com.datamaster.module.ontology.dal.mapper.OntologyMapper;
import com.datamaster.module.ontology.dal.mapper.PropertyMapper;
import com.datamaster.module.ontology.dal.mapper.RelationMapper;
import com.datamaster.module.ontology.dal.mapper.RelationTableMapper;
import com.datamaster.module.ontology.service.IConceptService;
import com.datamaster.module.ontology.service.IConceptTableService;
import com.datamaster.module.ontology.service.IOntologyGenerateService;
import com.datamaster.module.ontology.service.IOntologyService;
import com.datamaster.module.ontology.service.IPropertyColumnService;
import com.datamaster.module.ontology.service.IPropertyService;
import com.datamaster.module.ontology.service.IRelationColumnService;
import com.datamaster.module.ontology.service.IRelationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 本体 AI 生成 Service 实现
 */
@Service
public class OntologyGenerateServiceImpl implements IOntologyGenerateService {

    private static final Logger log = LoggerFactory.getLogger(OntologyGenerateServiceImpl.class);

    /**
     * 属性允许的 dataType 枚举
     */
    private static final Set<String> ALLOWED_DATA_TYPES =
            new HashSet<>(Arrays.asList("string", "integer", "decimal", "date", "boolean", "text"));

    @Resource
    private CatalogTableApiService catalogTableApiService;
    @Resource
    private CatalogColumnApiService catalogColumnApiService;
    @Resource
    private IAiModelGatewayService aiModelGatewayService;
    @Resource
    private IOntologyService ontologyService;
    @Resource
    private IConceptService conceptService;
    @Resource
    private IPropertyService propertyService;
    @Resource
    private IConceptTableService conceptTableService;
    @Resource
    private IPropertyColumnService propertyColumnService;
    @Resource
    private IRelationService relationService;
    @Resource
    private IRelationColumnService relationColumnService;
    @Resource
    private OntologyMapper ontologyMapper;
    @Resource
    private ConceptMapper conceptMapper;
    @Resource
    private PropertyMapper propertyMapper;
    @Resource
    private RelationMapper relationMapper;
    @Resource
    private RelationTableMapper relationTableMapper;

    @Override
    public OntologyAiGenerateRespVO preview(OntologyAiGenerateReqVO reqVO) {
        checkReqVO(reqVO);
        OntologyAiGenerateRespVO respVO = new OntologyAiGenerateRespVO();
        try {
            List<CatalogTableRespDTO> tables = resolveTables(reqVO);
            if (tables.isEmpty()) {
                respVO.setQualityWarning("未获取到表元数据，请先在元数据目录中完成该数据源的表采集");
                return respVO;
            }
            List<TableColumns> tableColumnsList = resolveAllColumns(tables);
            if (tableColumnsList.isEmpty()) {
                respVO.setQualityWarning("未获取到字段元数据，请先在元数据目录中完成字段采集");
                return respVO;
            }
            JSONObject model = callAiGenerateModel(tableColumnsList);
            if (model == null) {
                respVO.setQualityWarning("AI 未返回合法 JSON 模型");
                return respVO;
            }
            respVO.setModel(parseGeneratedModel(model));
            return respVO;
        } catch (Exception e) {
            log.warn("本体 AI 生成预览失败：{}", e.getMessage());
            respVO.setQualityWarning("AI 生成失败：" + e.getMessage());
            return respVO;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OntologyAiGenerateRespVO generate(OntologyAiGenerateReqVO reqVO) {
        checkReqVO(reqVO);
        OntologyAiGenerateRespVO respVO = new OntologyAiGenerateRespVO();
        List<CatalogTableRespDTO> tables = resolveTables(reqVO);
        if (tables.isEmpty()) {
            throw new ServiceException("未获取到表元数据，请先在元数据目录中完成该数据源的表采集");
        }
        List<TableColumns> tableColumnsList = resolveAllColumns(tables);
        if (tableColumnsList.isEmpty()) {
            throw new ServiceException("未获取到字段元数据，请先在元数据目录中完成字段采集");
        }
        JSONObject model = callAiGenerateModel(tableColumnsList);
        if (model == null) {
            throw new ServiceException("AI 未返回合法 JSON 模型，无法生成");
        }
        List<GeneratedConceptContext> conceptContexts = buildGenerateContexts(model);

        // 1. 本体：指定 ontologyId 则校验存在，否则自动创建
        Long ontologyId = reqVO.getOntologyId();
        if (ontologyId == null) {
            ontologyId = createOntologyIfAbsent(contextOntologyName(model), contextOntologyCode(model, reqVO),
                    model.getString("ontologyDescription"), reqVO.getSpaceId(), reqVO.getSpaceCode());
        } else {
            OntologyDO ontology = ontologyMapper.selectById(ontologyId);
            if (ontology == null) {
                throw new ServiceException("目标本体不存在：" + ontologyId);
            }
        }
        respVO.setOntologyId(ontologyId);

        // 2. 逐概念落库：概念 + 属性 + 表绑定 + 字段映射
        List<OntologyAiGenerateRespVO.GeneratedConceptRespVO> savedConcepts = new ArrayList<>();
        List<Long> allPropertyIds = new ArrayList<>();
        Map<String, Long> conceptIdByCode = new HashMap<>();
        Map<String, Long> conceptTableIdByCode = new HashMap<>();
        for (GeneratedConceptContext conceptCtx : conceptContexts) {
            CatalogTableRespDTO table = findTable(tableColumnsList, conceptCtx.tableName);
            if (table == null) {
                throw new ServiceException("AI 返回的概念未匹配到物理表：" + conceptCtx.tableName
                        + "，请重新预览（本体编码：" + contextOntologyCode(model, reqVO) + "）");
            }
            List<CatalogColumnRespDTO> tableColumns = findColumns(tableColumnsList, conceptCtx.tableName);
            Set<String> physicalColumns = new HashSet<>();
            for (CatalogColumnRespDTO column : tableColumns) {
                if (column.getColumnName() != null) {
                    physicalColumns.add(column.getColumnName());
                }
            }

            ConceptSaveReqVO conceptReq = new ConceptSaveReqVO();
            conceptReq.setOntologyId(ontologyId);
            conceptReq.setName(conceptCtx.name);
            conceptReq.setCode(conceptCtx.code);
            conceptReq.setDescription(conceptCtx.description);
            conceptReq.setIcon(conceptCtx.icon);
            conceptReq.setColor(conceptCtx.color);
            conceptReq.setSortOrder(1);
            conceptReq.setStatus(0);
            checkConceptCodeUnique(ontologyId, conceptCtx.code);
            Long conceptId = conceptService.createConcept(conceptReq);
            conceptIdByCode.put(conceptCtx.code, conceptId);

            // 属性 + 属性列映射
            List<Long> propertyIds = new ArrayList<>();
            List<PropertyColumnSaveReqVO> columnMappings = new ArrayList<>();
            int sortOrder = 1;
            for (GeneratedPropertyContext property : conceptCtx.properties) {
                PropertySaveReqVO propertyReq = new PropertySaveReqVO();
                propertyReq.setConceptId(conceptId);
                propertyReq.setName(property.name);
                propertyReq.setCode(property.code);
                propertyReq.setDataType(property.dataType);
                propertyReq.setDescription(property.description);
                propertyReq.setIsPrimary(property.isPrimary);
                propertyReq.setIsRequired(property.isRequired);
                propertyReq.setDefaultValue(property.defaultValue);
                propertyReq.setSortOrder(sortOrder++);
                checkPropertyCodeUnique(conceptId, property.code);
                Long propertyId = propertyService.createProperty(propertyReq);
                propertyIds.add(propertyId);

                if (StringUtils.isNotBlank(property.columnName)) {
                    if (!physicalColumns.contains(property.columnName)) {
                        throw new ServiceException("AI 返回的物理列名不存在于表中：" + property.columnName
                                + "，请检查表结构或重新预览");
                    }
                    PropertyColumnSaveReqVO mapping = new PropertyColumnSaveReqVO();
                    mapping.setPropertyId(propertyId);
                    mapping.setColumnName(property.columnName);
                    columnMappings.add(mapping);
                }
            }
            allPropertyIds.addAll(propertyIds);

            // 概念表绑定 + 批量属性列映射
            ConceptTableSaveReqVO tableReq = new ConceptTableSaveReqVO();
            tableReq.setConceptId(conceptId);
            tableReq.setDatasourceId(reqVO.getDatasourceId());
            tableReq.setDatabaseName(table.getDbName());
            tableReq.setTableName(table.getTableName());
            tableReq.setSchemaName(table.getSchemaName());
            Long conceptTableId = conceptTableService.createConceptTable(tableReq);
            conceptTableIdByCode.put(conceptCtx.code, conceptTableId);

            if (!columnMappings.isEmpty()) {
                propertyColumnService.batchSavePropertyColumns(conceptTableId, columnMappings);
            }

            OntologyAiGenerateRespVO.GeneratedConceptRespVO savedConcept =
                    new OntologyAiGenerateRespVO.GeneratedConceptRespVO();
            savedConcept.setName(conceptCtx.name);
            savedConcept.setCode(conceptCtx.code);
            savedConcept.setConceptId(conceptId);
            savedConcept.setConceptTableId(conceptTableId);
            savedConcept.setPropertyIds(propertyIds);
            savedConcepts.add(savedConcept);
        }
        respVO.setSavedConcepts(savedConcepts);

        // 3. 关系落库：按概念编码关联源/目标概念，并建立关系字段映射
        List<GeneratedRelationContext> relationContexts = buildGenerateRelations(model);
        List<OntologyAiGenerateRespVO.GeneratedRelationRespVO> savedRelations = new ArrayList<>();
        List<Long> relationIds = new ArrayList<>();
        if (!relationContexts.isEmpty() && conceptIdByCode.size() > 1) {
            for (GeneratedRelationContext relationCtx : relationContexts) {
                Long sourceConceptId = conceptIdByCode.get(relationCtx.sourceConceptCode);
                Long targetConceptId = conceptIdByCode.get(relationCtx.targetConceptCode);
                if (sourceConceptId == null || targetConceptId == null) {
                    throw new ServiceException("AI 返回的关系引用了不存在的概念编码：" + relationCtx.sourceConceptCode
                            + "/" + relationCtx.targetConceptCode + "，请重新预览");
                }
                Long sourceConceptTableId = conceptTableIdByCode.get(relationCtx.sourceConceptCode);
                Long targetConceptTableId = conceptTableIdByCode.get(relationCtx.targetConceptCode);
                if (sourceConceptTableId == null || targetConceptTableId == null) {
                    throw new ServiceException("AI 返回的关系缺少概念表绑定：" + relationCtx.sourceConceptCode
                            + "/" + relationCtx.targetConceptCode + "，请重新预览");
                }

                RelationSaveReqVO relationReq = new RelationSaveReqVO();
                relationReq.setOntologyId(ontologyId);
                relationReq.setName(firstNonBlank(relationCtx.name, relationCtx.code));
                relationReq.setCode(normalizeCode(relationCtx.code));
                relationReq.setSourceConceptId(sourceConceptId);
                relationReq.setTargetConceptId(targetConceptId);
                relationReq.setRelationType(normalizeRelationType(relationCtx.relationType));
                relationReq.setDescription(relationCtx.description);
                relationReq.setSortOrder(1);
                checkRelationCodeUnique(ontologyId, relationReq.getCode());
                Long relationId = relationService.createRelation(relationReq);
                relationIds.add(relationId);

                // 关系字段映射：源/目标概念表绑定 + 关联物理列
                if (StringUtils.isNotBlank(relationCtx.sourceColumn) && StringUtils.isNotBlank(relationCtx.targetColumn)) {
                    List<RelationColumnSaveReqVO> relationColumns = new ArrayList<>();
                    RelationColumnSaveReqVO relationColumn = new RelationColumnSaveReqVO();
                    relationColumn.setSourceConceptTableId(sourceConceptTableId);
                    relationColumn.setSourceColumn(relationCtx.sourceColumn);
                    relationColumn.setTargetConceptTableId(targetConceptTableId);
                    relationColumn.setTargetColumn(relationCtx.targetColumn);
                    relationColumns.add(relationColumn);
                    relationColumnService.batchSaveRelationColumns(relationId, relationColumns);
                }

                OntologyAiGenerateRespVO.GeneratedRelationRespVO savedRelation =
                        new OntologyAiGenerateRespVO.GeneratedRelationRespVO();
                savedRelation.setName(relationReq.getName());
                savedRelation.setCode(relationReq.getCode());
                savedRelation.setSourceConceptCode(relationCtx.sourceConceptCode);
                savedRelation.setTargetConceptCode(relationCtx.targetConceptCode);
                savedRelation.setRelationType(relationReq.getRelationType());
                savedRelation.setDescription(relationCtx.description);
                savedRelation.setSourceColumn(relationCtx.sourceColumn);
                savedRelation.setTargetColumn(relationCtx.targetColumn);
                savedRelation.setRelationId(relationId);
                savedRelation.setSourceConceptId(sourceConceptId);
                savedRelation.setTargetConceptId(targetConceptId);
                savedRelation.setSourceConceptTableId(sourceConceptTableId);
                savedRelation.setTargetConceptTableId(targetConceptTableId);
                savedRelations.add(savedRelation);
            }
        }
        respVO.setSavedRelations(savedRelations);
        respVO.setRelationIds(relationIds);
        respVO.setPropertyIds(allPropertyIds);
        if (!savedConcepts.isEmpty()) {
            respVO.setConceptId(savedConcepts.get(0).getConceptId());
            respVO.setConceptTableId(savedConcepts.get(0).getConceptTableId());
        }
        respVO.setModel(parseGeneratedModel(model));
        return respVO;
    }

    @Override
    public List<CatalogTableRespDTO> listCatalogTables(Long datasourceId) {
        if (datasourceId == null) {
            return Collections.emptyList();
        }
        List<CatalogTableRespDTO> tables = catalogTableApiService.listByDatasourceId(datasourceId);
        return tables == null ? Collections.emptyList() : tables;
    }

    private void checkReqVO(OntologyAiGenerateReqVO reqVO) {
        if (reqVO == null || reqVO.getDatasourceId() == null) {
            throw new ServiceException("请选择数据源");
        }
    }

    /**
     * 解析目标表集合：指定表返回单张表；未指定表返回该数据源的全部表。
     */
    private List<CatalogTableRespDTO> resolveTables(OntologyAiGenerateReqVO reqVO) {
        if (StringUtils.isNotBlank(reqVO.getTableName())) {
            CatalogTableRespDTO table = catalogTableApiService
                    .getByDatasourceIdAndTableName(reqVO.getDatasourceId(), reqVO.getTableName());
            return table == null ? Collections.emptyList() : Collections.singletonList(table);
        }
        List<CatalogTableRespDTO> tables = catalogTableApiService.listByDatasourceId(reqVO.getDatasourceId());
        return tables == null ? Collections.emptyList() : tables;
    }

    private List<CatalogColumnRespDTO> resolveColumns(CatalogTableRespDTO table) {
        if (table == null || table.getId() == null) {
            return Collections.emptyList();
        }
        List<CatalogColumnRespDTO> columns = catalogColumnApiService.listByTableId(table.getId());
        return columns == null ? Collections.emptyList() : columns;
    }

    /**
     * 解析全部目标表的字段列表（过滤无字段的表）。
     */
    private List<TableColumns> resolveAllColumns(List<CatalogTableRespDTO> tables) {
        List<TableColumns> result = new ArrayList<>();
        for (CatalogTableRespDTO table : tables) {
            List<CatalogColumnRespDTO> columns = resolveColumns(table);
            if (columns.isEmpty()) {
                continue;
            }
            TableColumns tableColumns = new TableColumns();
            tableColumns.table = table;
            tableColumns.columns = columns;
            result.add(tableColumns);
        }
        return result;
    }

    private CatalogTableRespDTO findTable(List<TableColumns> tableColumnsList, String tableName) {
        if (StringUtils.isBlank(tableName)) {
            return null;
        }
        for (TableColumns tableColumns : tableColumnsList) {
            if (tableName.equals(tableColumns.table.getTableName())) {
                return tableColumns.table;
            }
        }
        return null;
    }

    private List<CatalogColumnRespDTO> findColumns(List<TableColumns> tableColumnsList, String tableName) {
        if (StringUtils.isBlank(tableName)) {
            return Collections.emptyList();
        }
        for (TableColumns tableColumns : tableColumnsList) {
            if (tableName.equals(tableColumns.table.getTableName())) {
                return tableColumns.columns;
            }
        }
        return Collections.emptyList();
    }

    /**
     * 调用直连大模型生成本体模型 JSON。
     * <p>
     * 与 AI Skill 生成共用 {@link IAiModelGatewayService} 直连网关（ai.skill-model 配置），
     * 模型由系统配置决定（默认 qwen-plus），不再支持请求级指定模型。
     */
    private JSONObject callAiGenerateModel(List<TableColumns> tableColumnsList) {
        String systemPrompt = buildSystemPrompt();
        String userPrompt = buildUserPrompt(tableColumnsList);

        String reply = aiModelGatewayService.complete(systemPrompt, userPrompt);
        if (StringUtils.isBlank(reply)) {
            return null;
        }
        return extractJsonObject(reply);
    }

    /**
     * 从 LLM 回复中提取 JSON 对象：优先取 ```json 代码块，其次取首个 { 到最后一个 } 区间。
     */
    private JSONObject extractJsonObject(String reply) {
        String candidate = reply;
        // 尝试 ```json 代码块
        int fenceStart = reply.indexOf("```json");
        if (fenceStart >= 0) {
            int contentStart = fenceStart + "```json".length();
            int fenceEnd = reply.indexOf("```", contentStart);
            if (fenceEnd > contentStart) {
                candidate = reply.substring(contentStart, fenceEnd);
            }
        } else {
            int fenceStartPlain = reply.indexOf("```");
            int fenceEndPlain = fenceStartPlain >= 0 ? reply.indexOf("```", fenceStartPlain + 3) : -1;
            if (fenceStartPlain >= 0 && fenceEndPlain > fenceStartPlain + 3) {
                candidate = reply.substring(fenceStartPlain + 3, fenceEndPlain);
            }
        }
        // 截取首个 { 到最后一个 }
        int start = candidate.indexOf('{');
        int end = candidate.lastIndexOf('}');
        if (start >= 0 && end > start) {
            candidate = candidate.substring(start, end + 1);
        }
        try {
            return JSON.parseObject(candidate);
        } catch (Exception e) {
            log.warn("AI 回复 JSON 解析失败：{}", e.getMessage());
            return null;
        }
    }

    private String buildSystemPrompt() {
        StringBuilder builder = new StringBuilder();
        builder.append("你是 DataMaster 数据治理平台的数据建模专家。");
        builder.append("请根据给定的数据库表结构，生成对应的本体模型（Ontology Model）。\n");
        builder.append("本体模型包含：本体（Ontology）、概念（Concept）、属性（Property）、关系（Relation）。\n");
        builder.append("本体是对业务域的建模容器；一张业务表对应一个概念；属性对应表中的业务字段，并与物理列建立映射；");
        builder.append("关系描述概念与概念之间的业务关联（如客户拥有订单），并建立关联字段映射。\n");
        builder.append("要求：\n");
        builder.append("1. 只返回一个合法 JSON 对象，不要返回 Markdown，不要使用任何解释性文字。\n");
        builder.append("2. JSON 结构如下：\n");
        builder.append("{\n");
        builder.append("  \"ontologyName\": \"本体名称（中文）\",\n");
        builder.append("  \"ontologyCode\": \"本体编码（英文小写下划线，如 customer）\",\n");
        builder.append("  \"ontologyDescription\": \"本体描述\",\n");
        builder.append("  \"concepts\": [\n");
        builder.append("    {\n");
        builder.append("      \"name\": \"概念名称（中文）\",\n");
        builder.append("      \"code\": \"概念编码（英文小写下划线）\",\n");
        builder.append("      \"tableName\": \"该概念对应的物理表名（必须与给定表名完全一致）\",\n");
        builder.append("      \"description\": \"概念描述\",\n");
        builder.append("      \"icon\": \"图标标识（可选，如 user）\",\n");
        builder.append("      \"color\": \"颜色（可选，如 #409EFF）\",\n");
        builder.append("      \"properties\": [\n");
        builder.append("        {\n");
        builder.append("          \"name\": \"属性名称（中文）\",\n");
        builder.append("          \"code\": \"属性编码（英文小写下划线）\",\n");
        builder.append("          \"dataType\": \"string|integer|decimal|date|boolean|text\",\n");
        builder.append("          \"description\": \"属性描述\",\n");
        builder.append("          \"isPrimary\": false,\n");
        builder.append("          \"isRequired\": false,\n");
        builder.append("          \"defaultValue\": \"默认值（无则 null）\",\n");
        builder.append("          \"columnName\": \"对应物理列名（必须与给定列名完全一致）\"\n");
        builder.append("        }\n");
        builder.append("      ]\n");
        builder.append("    }\n");
        builder.append("  ],\n");
        builder.append("  \"relations\": [\n");
        builder.append("    {\n");
        builder.append("      \"name\": \"关系名称（中文，如 拥有订单）\",\n");
        builder.append("      \"code\": \"关系编码（英文小写下划线，如 has_order）\",\n");
        builder.append("      \"sourceConceptCode\": \"源概念编码（必须与 concepts 中某个 concept 的 code 完全一致）\",\n");
        builder.append("      \"targetConceptCode\": \"目标概念编码（必须与 concepts 中某个 concept 的 code 完全一致）\",\n");
        builder.append("      \"relationType\": \"one_to_one|one_to_many|many_to_one|many_to_many\",\n");
        builder.append("      \"description\": \"关系描述\",\n");
        builder.append("      \"sourceColumn\": \"源概念关联物理列名（必须与源概念所在表的给定列名完全一致）\",\n");
        builder.append("      \"targetColumn\": \"目标概念关联物理列名（必须与目标概念所在表的给定列名完全一致）\"\n");
        builder.append("    }\n");
        builder.append("  ]\n");
        builder.append("}\n");
        builder.append("3. dataType 只能从 string/integer/decimal/date/boolean/text 中选择，根据列类型合理映射。\n");
        builder.append("4. 主键列对应属性 isPrimary=true，非空列对应属性 isRequired=true。\n");
        builder.append("5. 每个有业务含义的列都应生成一个属性，columnName 必须精确等于给定物理列名，不能自造。\n");
        builder.append("6. 每一张给出的业务表都必须生成一个概念，concepts 数组的元素个数必须与给出的表数量一致，且 tableName 必须与给定表名完全一致。\n");
        builder.append("7. 若表名或列名本身即业务含义（如 customer_name），可直接按语义生成中文属性名。\n");
        builder.append("8. 若表与表之间存在外键或业务关联（如同一业务主题、共享主键、名称相同的关联列等），");
        builder.append("则在 relations 中生成关系，否则 relations 可为空数组。\n");
        builder.append("9. relationType 只能从 one_to_one/one_to_many/many_to_one/many_to_many 中选择，根据关联列的性质判断：");
        builder.append("关联列在源表唯一、在目标表可重复为 one_to_many；两边都唯一为 one_to_one；两边都可重复为 many_to_many。\n");
        builder.append("10. sourceConceptCode/targetConceptCode 必须引用 concepts 中已定义的 concept.code，不能引用不存在的概念；");
        builder.append("sourceColumn/targetColumn 必须精确等于关联列在两表中实际存在的物理列名，不能自造。\n");
        return builder.toString();
    }

    private String buildUserPrompt(List<TableColumns> tableColumnsList) {
        StringBuilder builder = new StringBuilder();
        builder.append("请根据以下 ").append(tableColumnsList.size()).append(" 张表的结构生成本体模型：\n");
        int index = 1;
        for (TableColumns tableColumns : tableColumnsList) {
            CatalogTableRespDTO table = tableColumns.table;
            builder.append("\n【表 ").append(index++).append("】\n");
            builder.append("表名：").append(nullToEmpty(table.getTableName())).append("\n");
            builder.append("表注释：").append(nullToEmpty(table.getTableComment())).append("\n");
            builder.append("库名：").append(nullToEmpty(table.getDbName())).append("\n");
            builder.append("Schema：").append(nullToEmpty(table.getSchemaName())).append("\n");
            if (StringUtils.isNotBlank(table.getPrimaryKey())) {
                builder.append("主键列：").append(table.getPrimaryKey()).append("\n");
            }
            builder.append("字段信息：\n");
            for (CatalogColumnRespDTO column : tableColumns.columns) {
                builder.append("- ").append(nullToEmpty(column.getColumnName()))
                        .append(" | 类型 ").append(nullToEmpty(column.getColumnType()))
                        .append(columnLengthText(column))
                        .append(" | 注释 ").append(nullToEmpty(column.getColumnComment()))
                        .append(" | 主键 ").append("1".equals(column.getPkFlag()) ? "是" : "否")
                        .append(" | 非空 ").append("0".equals(column.getNullableFlag()) ? "是" : "否")
                        .append("\n");
            }
        }
        return builder.toString();
    }

    private String columnLengthText(CatalogColumnRespDTO column) {
        if (column.getColumnLength() == null) {
            return "";
        }
        return "(" + column.getColumnLength() + ")";
    }

    /**
     * 将 AI 返回的 JSON 解析为预览结构。
     */
    private OntologyAiGenerateRespVO.GeneratedModelRespVO parseGeneratedModel(JSONObject model) {
        OntologyAiGenerateRespVO.GeneratedModelRespVO modelResp = new OntologyAiGenerateRespVO.GeneratedModelRespVO();
        modelResp.setOntologyName(model.getString("ontologyName"));
        modelResp.setOntologyCode(model.getString("ontologyCode"));
        modelResp.setOntologyDescription(model.getString("ontologyDescription"));

        JSONArray concepts = model.getJSONArray("concepts");
        if (concepts != null) {
            List<OntologyAiGenerateRespVO.GeneratedConceptRespVO> conceptList = new ArrayList<>();
            for (int i = 0; i < concepts.size(); i++) {
                JSONObject conceptObj = concepts.getJSONObject(i);
                if (conceptObj == null) {
                    continue;
                }
                OntologyAiGenerateRespVO.GeneratedConceptRespVO conceptResp =
                        new OntologyAiGenerateRespVO.GeneratedConceptRespVO();
                conceptResp.setName(conceptObj.getString("name"));
                conceptResp.setCode(conceptObj.getString("code"));
                conceptResp.setTableName(conceptObj.getString("tableName"));
                conceptResp.setDescription(conceptObj.getString("description"));
                conceptResp.setIcon(conceptObj.getString("icon"));
                conceptResp.setColor(conceptObj.getString("color"));

                JSONArray properties = conceptObj.getJSONArray("properties");
                if (properties != null) {
                    List<OntologyAiGenerateRespVO.GeneratedPropertyRespVO> propertyList = new ArrayList<>();
                    for (int j = 0; j < properties.size(); j++) {
                        JSONObject propertyObj = properties.getJSONObject(j);
                        if (propertyObj == null) {
                            continue;
                        }
                        OntologyAiGenerateRespVO.GeneratedPropertyRespVO propertyResp =
                                new OntologyAiGenerateRespVO.GeneratedPropertyRespVO();
                        propertyResp.setName(propertyObj.getString("name"));
                        propertyResp.setCode(propertyObj.getString("code"));
                        propertyResp.setDataType(normalizeDataType(propertyObj.getString("dataType")));
                        propertyResp.setDescription(propertyObj.getString("description"));
                        propertyResp.setIsPrimary(propertyObj.getBoolean("isPrimary"));
                        propertyResp.setIsRequired(propertyObj.getBoolean("isRequired"));
                        propertyResp.setDefaultValue(propertyObj.getString("defaultValue"));
                        propertyResp.setColumnName(propertyObj.getString("columnName"));
                        propertyResp.setSortOrder(j + 1);
                        propertyList.add(propertyResp);
                    }
                    conceptResp.setProperties(propertyList);
                }
                conceptList.add(conceptResp);
            }
            modelResp.setConcepts(conceptList);
        }

        JSONArray relations = model.getJSONArray("relations");
        if (relations != null) {
            List<OntologyAiGenerateRespVO.GeneratedRelationRespVO> relationList = new ArrayList<>();
            for (int i = 0; i < relations.size(); i++) {
                JSONObject relationObj = relations.getJSONObject(i);
                if (relationObj == null) {
                    continue;
                }
                OntologyAiGenerateRespVO.GeneratedRelationRespVO relationResp =
                        new OntologyAiGenerateRespVO.GeneratedRelationRespVO();
                relationResp.setName(relationObj.getString("name"));
                relationResp.setCode(relationObj.getString("code"));
                relationResp.setSourceConceptCode(relationObj.getString("sourceConceptCode"));
                relationResp.setTargetConceptCode(relationObj.getString("targetConceptCode"));
                relationResp.setRelationType(normalizeRelationType(relationObj.getString("relationType")));
                relationResp.setDescription(relationObj.getString("description"));
                relationResp.setSourceColumn(relationObj.getString("sourceColumn"));
                relationResp.setTargetColumn(relationObj.getString("targetColumn"));
                relationList.add(relationResp);
            }
            modelResp.setRelations(relationList);
        }
        return modelResp;
    }

    /**
     * 生成落库所需上下文（本体 + 多概念上下文）。
     */
    private List<GeneratedConceptContext> buildGenerateContexts(JSONObject model) {
        JSONArray concepts = model.getJSONArray("concepts");
        if (concepts == null || concepts.isEmpty()) {
            throw new ServiceException("AI 未返回概念，无法生成");
        }
        List<GeneratedConceptContext> contexts = new ArrayList<>();
        for (int i = 0; i < concepts.size(); i++) {
            JSONObject conceptObj = concepts.getJSONObject(i);
            if (conceptObj == null) {
                continue;
            }
            GeneratedConceptContext context = new GeneratedConceptContext();
            context.tableName = conceptObj.getString("tableName");
            context.name = firstNonBlank(conceptObj.getString("name"), "概念" + (i + 1));
            context.code = firstNonBlank(conceptObj.getString("code"), "concept_" + (i + 1));
            context.description = conceptObj.getString("description");
            context.icon = conceptObj.getString("icon");
            context.color = conceptObj.getString("color");

            List<GeneratedPropertyContext> properties = new ArrayList<>();
            JSONArray propertyArr = conceptObj.getJSONArray("properties");
            if (propertyArr != null) {
                for (int j = 0; j < propertyArr.size(); j++) {
                    JSONObject propertyObj = propertyArr.getJSONObject(j);
                    if (propertyObj == null) {
                        continue;
                    }
                    GeneratedPropertyContext property = new GeneratedPropertyContext();
                    property.name = firstNonBlank(propertyObj.getString("name"), "属性" + (j + 1));
                    property.code = firstNonBlank(propertyObj.getString("code"), "prop_" + (j + 1));
                    property.dataType = normalizeDataType(propertyObj.getString("dataType"));
                    property.description = propertyObj.getString("description");
                    property.isPrimary = Boolean.TRUE.equals(propertyObj.getBoolean("isPrimary"));
                    property.isRequired = Boolean.TRUE.equals(propertyObj.getBoolean("isRequired"));
                    property.defaultValue = propertyObj.getString("defaultValue");
                    property.columnName = propertyObj.getString("columnName");
                    properties.add(property);
                }
            }
            if (properties.isEmpty()) {
                throw new ServiceException("AI 未返回属性，无法生成：" + context.name);
            }
            context.properties = properties;
            contexts.add(context);
        }
        return contexts;
    }

    /**
     * 生成关系落库上下文：解析 AI 返回的 relations 数组。
     *
     * <p>模型可能未返回 relations 或返回空数组，此时返回空列表（不落关系）。</p>
     */
    private List<GeneratedRelationContext> buildGenerateRelations(JSONObject model) {
        JSONArray relations = model.getJSONArray("relations");
        if (relations == null || relations.isEmpty()) {
            return Collections.emptyList();
        }
        List<GeneratedRelationContext> contexts = new ArrayList<>();
        for (int i = 0; i < relations.size(); i++) {
            JSONObject relationObj = relations.getJSONObject(i);
            if (relationObj == null) {
                continue;
            }
            GeneratedRelationContext context = new GeneratedRelationContext();
            context.name = firstNonBlank(relationObj.getString("name"), "关系" + (i + 1));
            context.code = firstNonBlank(relationObj.getString("code"), "relation_" + (i + 1));
            context.sourceConceptCode = relationObj.getString("sourceConceptCode");
            context.targetConceptCode = relationObj.getString("targetConceptCode");
            context.relationType = relationObj.getString("relationType");
            context.description = relationObj.getString("description");
            context.sourceColumn = relationObj.getString("sourceColumn");
            context.targetColumn = relationObj.getString("targetColumn");
            contexts.add(context);
        }
        return contexts;
    }

    private String contextOntologyName(JSONObject model) {
        return firstNonBlank(model.getString("ontologyName"), "AI 生成本体");
    }

    private String contextOntologyCode(JSONObject model, OntologyAiGenerateReqVO reqVO) {
        if (StringUtils.isNotBlank(model.getString("ontologyCode"))) {
            return normalizeCode(model.getString("ontologyCode"));
        }
        if (StringUtils.isNotBlank(reqVO.getTableName())) {
            return normalizeCode(reqVO.getTableName());
        }
        return "auto_model_" + System.currentTimeMillis() % 10000;
    }

    private Long createOntologyIfAbsent(String name, String code, String description, Long spaceId, String spaceCode) {
        Long count = ontologyMapper.selectCount(Wrappers.<OntologyDO>lambdaQuery()
                .eq(StringUtils.isNotBlank(code), OntologyDO::getCode, code));
        if (count != null && count > 0) {
            code = code + "_" + System.currentTimeMillis() % 10000;
        }
        OntologySaveReqVO ontologyReq = new OntologySaveReqVO();
        ontologyReq.setName(name);
        ontologyReq.setCode(code);
        ontologyReq.setDescription(description);
        ontologyReq.setSpaceId(spaceId);
        ontologyReq.setSpaceCode(spaceCode);
        ontologyReq.setStatus(0);
        return ontologyService.createOntology(ontologyReq);
    }

    private void checkConceptCodeUnique(Long ontologyId, String code) {
        if (StringUtils.isBlank(code)) {
            return;
        }
        Long count = conceptMapper.selectCount(Wrappers.<ConceptDO>lambdaQuery()
                .eq(ConceptDO::getOntologyId, ontologyId)
                .eq(ConceptDO::getCode, code));
        if (count != null && count > 0) {
            throw new ServiceException("概念编码已存在：" + code + "，请调整 AI 结果或修改概念编码");
        }
    }

    private void checkPropertyCodeUnique(Long conceptId, String code) {
        if (StringUtils.isBlank(code)) {
            return;
        }
        Long count = propertyMapper.selectCount(Wrappers.<PropertyDO>lambdaQuery()
                .eq(PropertyDO::getConceptId, conceptId)
                .eq(PropertyDO::getCode, code));
        if (count != null && count > 0) {
            throw new ServiceException("属性编码已存在：" + code + "，请调整 AI 结果或修改属性编码");
        }
    }

    private void checkRelationCodeUnique(Long ontologyId, String code) {
        if (StringUtils.isBlank(code)) {
            return;
        }
        Long count = relationMapper.selectCount(Wrappers.<RelationDO>lambdaQuery()
                .eq(RelationDO::getOntologyId, ontologyId)
                .eq(RelationDO::getCode, code));
        if (count != null && count > 0) {
            throw new ServiceException("关系编码已存在：" + code + "，请调整 AI 结果或修改关系编码");
        }
    }

    /**
     * 规范化 dataType：AI 可能返回物理类型（varchar 等），收敛到允许枚举。
     */
    private String normalizeDataType(String dataType) {
        if (StringUtils.isBlank(dataType)) {
            return "string";
        }
        String lower = dataType.trim().toLowerCase(Locale.ROOT);
        if (ALLOWED_DATA_TYPES.contains(lower)) {
            return lower;
        }
        // 物理类型映射
        if (lower.contains("decimal") || lower.contains("numeric") || lower.contains("number")
                || lower.contains("double") || lower.contains("float") || lower.contains("real")
                || lower.contains("money")) {
            return "decimal";
        }
        if (lower.contains("int") || lower.contains("serial")) {
            return "integer";
        }
        if (lower.contains("date") || lower.contains("time")) {
            return "date";
        }
        if (lower.contains("bool") || lower.contains("bit")) {
            return "boolean";
        }
        if (lower.contains("text") || lower.contains("clob") || lower.contains("json")
                || lower.contains("xml") || lower.contains("blob")) {
            return "text";
        }
        return "string";
    }

    /**
     * 规范化关系类型：AI 可能返回有下划线、连字符或中文的类型，收敛到枚举。
     */
    private String normalizeRelationType(String relationType) {
        if (StringUtils.isBlank(relationType)) {
            return null;
        }
        String lower = relationType.trim().toLowerCase(Locale.ROOT).replace("-", "_");
        if (lower.contains("one_to_many") || lower.contains("一对多") || lower.contains("1_to_n") || lower.contains("1:n")) {
            return "one_to_many";
        }
        if (lower.contains("many_to_one") || lower.contains("多对一") || lower.contains("n_to_1") || lower.contains("n:1")) {
            return "many_to_one";
        }
        if (lower.contains("many_to_many") || lower.contains("多对多") || lower.contains("n_to_m") || lower.contains("n:m")) {
            return "many_to_many";
        }
        if (lower.contains("one_to_one") || lower.contains("一对一") || lower.contains("1_to_1") || lower.contains("1:1")) {
            return "one_to_one";
        }
        return null;
    }

    private String normalizeCode(String value) {
        if (StringUtils.isBlank(value)) {
            return "model";
        }
        String lower = value.trim().toLowerCase(Locale.ROOT);
        StringBuilder builder = new StringBuilder();
        for (char c : lower.toCharArray()) {
            if (Character.isLetterOrDigit(c)) {
                builder.append(c);
            }
        }
        String result = builder.toString();
        if (result.isEmpty()) {
            result = "model";
        }
        if (Character.isDigit(result.charAt(0))) {
            result = "m_" + result;
        }
        return result;
    }

    private String firstNonBlank(String first, String second) {
        return StringUtils.isNotBlank(first) ? first : second;
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    // ==================== AI 生成动作 ====================

    @Override
    public AiActionPreviewRespVO generateActionsPreview(AiActionPreviewReqVO reqVO) {
        if (reqVO == null || reqVO.getOntologyId() == null) {
            throw new ServiceException("本体ID不能为空");
        }
        if (StringUtils.isBlank(reqVO.getPrompt())) {
            throw new ServiceException("动作描述不能为空");
        }
        AiActionPreviewRespVO respVO = new AiActionPreviewRespVO();
        try {
            // 1. 读取本体概念/关系/属性上下文
            List<ConceptDO> concepts = conceptMapper.selectList(
                    Wrappers.<ConceptDO>lambdaQuery().eq(ConceptDO::getOntologyId, reqVO.getOntologyId()));
            if (concepts.isEmpty()) {
                respVO.setQualityWarning("该本体下没有概念，请先创建概念");
                return respVO;
            }

            // 如果指定了概念ID，过滤只保留该概念
            List<ConceptDO> targetConcepts = concepts;
            if (reqVO.getConceptId() != null) {
                targetConcepts = concepts.stream()
                        .filter(c -> c.getId().equals(reqVO.getConceptId()))
                        .collect(java.util.stream.Collectors.toList());
                if (targetConcepts.isEmpty()) {
                    respVO.setQualityWarning("指定的概念不存在于该本体中");
                    return respVO;
                }
            }

            // 2. 读取关系（整个本体的）
            List<RelationDO> allRelations = relationMapper.selectList(
                    Wrappers.<RelationDO>lambdaQuery().eq(RelationDO::getOntologyId, reqVO.getOntologyId()));

            // 过滤：只保留与目标概念相关的关系
            Set<Long> targetConceptIds = new HashSet<>();
            for (ConceptDO c : targetConcepts) {
                targetConceptIds.add(c.getId());
            }
            List<RelationDO> relations = new ArrayList<>();
            for (RelationDO r : allRelations) {
                if (targetConceptIds.contains(r.getSourceConceptId()) || targetConceptIds.contains(r.getTargetConceptId())) {
                    relations.add(r);
                }
            }

            // 动作虽然从一个触发对象发起，但多步骤可能修改关系另一端对象；
            // 将相邻关系端点概念一并提供给模型，避免“人物入职公司”只能看到人物属性。
            List<ConceptDO> promptConcepts = new ArrayList<>(targetConcepts);
            Set<Long> promptConceptIds = new HashSet<>(targetConceptIds);
            Set<Long> addedPromptConceptIds = new HashSet<>(targetConceptIds);
            for (RelationDO relation : relations) {
                promptConceptIds.add(relation.getSourceConceptId());
                promptConceptIds.add(relation.getTargetConceptId());
            }
            for (ConceptDO concept : concepts) {
                if (promptConceptIds.contains(concept.getId()) && addedPromptConceptIds.add(concept.getId())) {
                    promptConcepts.add(concept);
                }
            }

            // 3. 构建概念code→name映射
            Map<Long, String> conceptNameById = new HashMap<>();
            Map<Long, String> conceptCodeById = new HashMap<>();
            for (ConceptDO c : concepts) {
                conceptNameById.put(c.getId(), c.getName());
                conceptCodeById.put(c.getId(), c.getCode());
            }

            // 4. 读取每个目标概念的属性
            Map<Long, List<PropertyDO>> propertiesByConcept = new HashMap<>();
            for (ConceptDO c : promptConcepts) {
                List<PropertyDO> props = propertyMapper.selectList(
                        Wrappers.<PropertyDO>lambdaQuery().eq(PropertyDO::getConceptId, c.getId()));
                propertiesByConcept.put(c.getId(), props);
            }

            // 5. 构建 prompt
            String systemPrompt = buildActionSystemPrompt();
            String userPrompt = buildActionUserPrompt(reqVO, promptConcepts, relations, propertiesByConcept,
                    conceptNameById, conceptCodeById);

            // 6. 调用 AI
            String reply = aiModelGatewayService.complete(systemPrompt, userPrompt);
            if (StringUtils.isBlank(reply)) {
                respVO.setQualityWarning("AI 未返回有效结果");
                return respVO;
            }

            JSONObject result = extractJsonObject(reply);
            if (result == null) {
                respVO.setQualityWarning("AI 返回的 JSON 解析失败");
                return respVO;
            }

            // 7. 解析结果
            respVO.setActions(parseActionPreviews(result, reqVO.getActionTypes()));
            return respVO;
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            log.warn("AI 生成动作预览失败：{}", e.getMessage());
            respVO.setQualityWarning("AI 生成失败：" + e.getMessage());
            return respVO;
        }
    }

    private String buildActionSystemPrompt() {
        StringBuilder builder = new StringBuilder();
        builder.append("你是 DataMaster 数据治理平台的动作建模专家。\n");
        builder.append("请根据给定的本体模型（概念、属性、关系）和用户的动作功能描述，生成合适的动作定义。\n");
        builder.append("支持两种动作类型：\n");
        builder.append("1. COMPOSITE（多步骤动作）：按顺序执行多个操作（CREATE/UPDATE/DELETE），步骤可操作概念对象或关系关联表。\n");
        builder.append("2. FUNCTION（函数动作）：用 TypeScript 或 Python 编写自定义处理逻辑。\n\n");
        builder.append("要求：\n");
        builder.append("1. 只返回一个合法 JSON 对象，不要返回 Markdown，不要使用任何解释性文字。\n");
        builder.append("2. JSON 结构如下：\n");
        builder.append("{\n");
        builder.append("  \"actions\": [\n");
        builder.append("    {\n");
        builder.append("      \"actionName\": \"动作名称（中文）\",\n");
        builder.append("      \"actionDescription\": \"动作功能描述\",\n");
        builder.append("      \"actionType\": \"COMPOSITE 或 FUNCTION\",\n");
        builder.append("      \"triggerConceptCode\": \"触发动作的概念编码\",\n");
        // COMPOSITE 格式
        builder.append("      \"executionSteps\": [\n");
        builder.append("        {\n");
        builder.append("          \"targetType\": \"CONCEPT 或 RELATION\",\n");
        builder.append("          \"conceptCode\": \"targetType=CONCEPT 时填写概念编码\",\n");
        builder.append("          \"relationCode\": \"targetType=RELATION 时填写关系编码\",\n");
        builder.append("          \"actionType\": \"CREATE 或 UPDATE 或 DELETE\",\n");
        builder.append("          \"paramConfig\": [\n");
        builder.append("            {\"propertyCode\": \"概念属性编码或关系属性字段\", \"relationEndpoint\": \"关系步骤的 SOURCE 或 TARGET，端点取值时填写\", \"valueMode\": \"direct|placeholder|object|expression\", \"valueTemplate\": \"目标值、参数名或触发对象属性编码\"}\n");
        builder.append("          ],\n");
        builder.append("          \"conditionConfig\": [\n");
        builder.append("            {\"propertyCode\": \"概念属性编码或关系属性字段\", \"relationEndpoint\": \"关系步骤的 SOURCE 或 TARGET，端点条件时填写\", \"operator\": \"eq\", \"negate\": false, \"sourceType\": \"OBJECT或PARAM\", \"sourcePropertyCode\": \"触发对象属性编码或执行参数名\"}\n");
        builder.append("          ]\n");
        builder.append("        }\n");
        builder.append("      ],\n");
        // FUNCTION 格式
        builder.append("      \"functionBody\": \"const result = ...; console.log(JSON.stringify(result));\",\n");
        builder.append("      \"functionLang\": \"TYPESCRIPT 或 PYTHON\",\n");
        builder.append("      \"functionParams\": [\"param1\", \"param2\"],\n");
        builder.append("      \"sourceConceptCode\": \"数据来源概念编码\",\n");
        builder.append("      \"outputConceptCode\": \"输出目标概念编码\"\n");
        builder.append("    }\n");
        builder.append("  ]\n");
        builder.append("}\n\n");
        builder.append("COMPOSITE 动作步骤规则：\n");
        builder.append("- targetType=CONCEPT 时 conceptCode 必须引用给定概念列表中的 code，propertyCode 使用概念属性 code\n");
        builder.append("- targetType=RELATION 时 relationCode 必须引用给定关系列表中的 code；主体、客体不要填写物理字段名，分别使用 relationEndpoint=SOURCE、TARGET\n");
        builder.append("- 入职、任职、加入、分配、绑定、解除、离职等描述关系变化的动作，优先生成 RELATION 步骤，不要用修改两个概念对象代替关系记录\n");
        builder.append("- UPDATE/DELETE 类型步骤必须有 conditionConfig\n");
        builder.append("- conditionConfig 的 sourceType=OBJECT 时 sourcePropertyCode 是触发对象属性编码；sourceType=PARAM 时是执行参数名\n");
        builder.append("- valueMode 支持：direct（固定值）、placeholder（引用参数 ${paramName}）、object（引用触发对象属性，valueTemplate 写属性编码）、expression（SQL 表达式）\n");
        builder.append("- 先判断 triggerConceptCode 位于关系的 SOURCE 还是 TARGET：触发概念对应端优先用 object，另一端通常用 placeholder；禁止固定假设 SOURCE 就是触发对象\n\n");
        builder.append("FUNCTION 动作规则：\n");
        builder.append("- functionBody 是可直接执行的脚本体，不要只声明未调用的函数；运行环境提供全局 input 对象\n");
        builder.append("- input.source.rows 是来源数据，input.relations 是关联数据，普通运行参数直接位于 input[paramName]\n");
        builder.append("- TypeScript 必须用 console.log(JSON.stringify(result)) 输出结果；Python 必须用 print(json.dumps(result, ensure_ascii=False)) 输出结果\n");
        builder.append("- 输出必须是 JSON 数组（输出到目标概念的数据）\n");
        builder.append("- sourceConceptCode/outputConceptCode 必须引用给定概念列表中的 code\n");
        return builder.toString();
    }

    private String buildActionUserPrompt(AiActionPreviewReqVO reqVO,
                                          List<ConceptDO> targetConcepts,
                                          List<RelationDO> relations,
                                          Map<Long, List<PropertyDO>> propertiesByConcept,
                                          Map<Long, String> conceptNameById,
                                          Map<Long, String> conceptCodeById) {
        StringBuilder builder = new StringBuilder();
        builder.append("用户需求：").append(reqVO.getPrompt()).append("\n\n");

        if (reqVO.getConceptId() != null) {
            builder.append("当前动作触发概念：")
                    .append(conceptCodeById.getOrDefault(reqVO.getConceptId(), String.valueOf(reqVO.getConceptId())))
                    .append("。生成结果的 triggerConceptCode 必须使用该编码。\n\n");
        } else {
            builder.append("未预选触发概念，请为每个动作明确返回 triggerConceptCode。\n\n");
        }

        builder.append("本体概念列表：\n");
        for (ConceptDO c : targetConcepts) {
            builder.append("- 概念编码：").append(c.getCode())
                    .append("，名称：").append(c.getName())
                    .append("，描述：").append(nullToEmpty(c.getDescription())).append("\n");
            List<PropertyDO> props = propertiesByConcept.get(c.getId());
            if (props != null && !props.isEmpty()) {
                builder.append("  属性列表：\n");
                for (PropertyDO p : props) {
                    builder.append("  - ").append(p.getCode())
                            .append("（").append(p.getName()).append("）")
                            .append("，类型：").append(p.getDataType())
                            .append(p.getIsPrimary() ? "，主键" : "")
                            .append(p.getIsRequired() ? "，必填" : "")
                            .append("\n");
                }
            }
        }

        if (!relations.isEmpty()) {
            builder.append("\n概念关系列表：\n");
            for (RelationDO r : relations) {
                String sourceName = conceptNameById.getOrDefault(r.getSourceConceptId(), r.getSourceConceptId().toString());
                String targetName = conceptNameById.getOrDefault(r.getTargetConceptId(), r.getTargetConceptId().toString());
                String sourceCode = conceptCodeById.getOrDefault(r.getSourceConceptId(), r.getSourceConceptId().toString());
                String targetCode = conceptCodeById.getOrDefault(r.getTargetConceptId(), r.getTargetConceptId().toString());
                builder.append("- ").append(r.getName())
                        .append("（").append(r.getCode()).append("）：")
                        .append("SOURCE=").append(sourceName).append("[").append(sourceCode).append("]")
                        .append(" -> TARGET=").append(targetName).append("[").append(targetCode).append("]")
                        .append("，类型：").append(r.getRelationType())
                        .append("\n");
                List<RelationTableDO> relationTables = relationTableMapper.selectByRelationId(r.getId());
                if (relationTables != null && !relationTables.isEmpty()) {
                    RelationTableDO table = relationTables.get(0);
                    RelationActionColumnContext columnContext = parseRelationActionColumnContext(table.getColumnNames());
                    builder.append("  关联表：").append(table.getTableName())
                            .append("，主体/客体端点映射：")
                            .append(columnContext.endpointConfigured ? "已配置" : "未完整配置")
                            .append("，可写关系属性：").append(columnContext.attributeColumns)
                            .append("\n");
                } else {
                    builder.append("  未绑定关联表：不能生成 RELATION 写入步骤\n");
                }
            }
        }

        return builder.toString();
    }

    private List<AiActionPreviewRespVO.GeneratedActionPreview> parseActionPreviews(
            JSONObject result, List<String> actionTypes) {
        List<AiActionPreviewRespVO.GeneratedActionPreview> previews = new ArrayList<>();
        JSONArray actions = result.getJSONArray("actions");
        if (actions == null) {
            return previews;
        }
        Set<String> typeFilter = actionTypes != null && !actionTypes.isEmpty()
                ? new HashSet<>(actionTypes) : null;

        for (int i = 0; i < actions.size(); i++) {
            JSONObject action = actions.getJSONObject(i);
            String actionType = action.getString("actionType");
            if (actionType == null) continue;
            actionType = actionType.toUpperCase(Locale.ROOT);
            if (typeFilter != null && !typeFilter.contains(actionType)) continue;

            AiActionPreviewRespVO.GeneratedActionPreview preview = new AiActionPreviewRespVO.GeneratedActionPreview();
            preview.setActionName(action.getString("actionName"));
            preview.setActionDescription(action.getString("actionDescription"));
            preview.setActionType(actionType);
            preview.setTriggerConceptCode(action.getString("triggerConceptCode"));

            if ("COMPOSITE".equals(actionType)) {
                preview.setExecutionSteps(action.getJSONArray("executionSteps") != null
                        ? action.getJSONArray("executionSteps").toJSONString() : "[]");
            } else if ("FUNCTION".equals(actionType)) {
                preview.setFunctionBody(action.getString("functionBody"));
                preview.setFunctionLang(action.getString("functionLang"));
                preview.setFunctionParams(action.getJSONArray("functionParams") != null
                        ? action.getJSONArray("functionParams").toJSONString() : "[]");
                preview.setSourceConceptCode(action.getString("sourceConceptCode"));
                preview.setOutputConceptCode(action.getString("outputConceptCode"));
            }
            previews.add(preview);
        }
        return previews;
    }

    private RelationActionColumnContext parseRelationActionColumnContext(String json) {
        RelationActionColumnContext context = new RelationActionColumnContext();
        if (StringUtils.isBlank(json)) return context;
        try {
            String trimmed = json.trim();
            if (trimmed.startsWith("[")) {
                JSONArray columns = JSON.parseArray(trimmed);
                context.endpointConfigured = columns.size() >= 2
                        && StringUtils.isNotBlank(columns.getString(0))
                        && StringUtils.isNotBlank(columns.getString(1));
                for (int i = 2; i < columns.size(); i++) {
                    if (StringUtils.isNotBlank(columns.getString(i))) context.attributeColumns.add(columns.getString(i));
                }
            } else {
                JSONObject value = JSON.parseObject(trimmed);
                context.endpointConfigured = StringUtils.isNotBlank(value.getString("sourceColumn"))
                        && StringUtils.isNotBlank(value.getString("targetColumn"));
                JSONArray attributes = value.getJSONArray("attributeColumns");
                if (attributes == null) attributes = value.getJSONArray("columns");
                if (attributes != null) {
                    for (int i = 0; i < attributes.size(); i++) {
                        if (StringUtils.isNotBlank(attributes.getString(i))) context.attributeColumns.add(attributes.getString(i));
                    }
                }
            }
        } catch (Exception ignored) {
            // 旧数据或异常配置由动作保存/执行阶段继续做严格校验。
        }
        return context;
    }

    private static class RelationActionColumnContext {
        private boolean endpointConfigured;
        private final List<String> attributeColumns = new ArrayList<>();
    }

    /**
     * 概念落库上下文
     */
    private static class GeneratedConceptContext {
        private String tableName;
        private String name;
        private String code;
        private String description;
        private String icon;
        private String color;
        private List<GeneratedPropertyContext> properties;
    }

    /**
     * 属性上下文
     */
    private static class GeneratedPropertyContext {
        private String name;
        private String code;
        private String dataType;
        private String description;
        private boolean isPrimary;
        private boolean isRequired;
        private String defaultValue;
        private String columnName;
    }

    /**
     * 关系落库上下文
     */
    private static class GeneratedRelationContext {
        private String name;
        private String code;
        private String sourceConceptCode;
        private String targetConceptCode;
        private String relationType;
        private String description;
        private String sourceColumn;
        private String targetColumn;
    }

    /**
     * 表 + 字段集合
     */
    private static class TableColumns {
        private CatalogTableRespDTO table;
        private List<CatalogColumnRespDTO> columns;
    }
}
