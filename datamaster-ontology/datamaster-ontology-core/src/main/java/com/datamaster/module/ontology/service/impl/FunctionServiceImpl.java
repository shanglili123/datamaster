package com.datamaster.module.ontology.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.ontology.controller.admin.function.vo.*;
import com.datamaster.module.ontology.convert.FunctionConvert;
import com.datamaster.module.ontology.dal.dataobject.*;
import com.datamaster.module.ontology.dal.mapper.*;
import com.datamaster.module.ontology.service.IFunctionService;
import com.datamaster.mybatis.core.query.LambdaQueryWrapperX;
import com.datamaster.common.database.DataSourceFactory;
import com.datamaster.common.database.DbQuery;
import com.datamaster.common.database.constants.DbQueryProperty;
import com.datamaster.common.datasource.mgmt.api.IDatasourceApiService;
import com.datamaster.common.datasource.mgmt.api.dto.DatasourceRespDTO;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Validated
public class FunctionServiceImpl implements IFunctionService {

    private static final Logger log = LoggerFactory.getLogger(FunctionServiceImpl.class);
    private static final String STATUS_PENDING = "PENDING_APPROVAL";
    private static final String STATUS_APPROVED = "APPROVED";
    private static final String STATUS_REJECTED = "REJECTED";
    private static final String STATUS_EXECUTED = "EXECUTED";
    private static final String STATUS_FAILED = "FAILED";
    /** 主概念/关系数据读取行数默认上限（防大批量读取拖垮执行） */
    private static final int DEFAULT_READ_LIMIT = 5000;

    @Resource private FunctionMapper functionMapper;
    @Resource private FunctionExecutionMapper executionMapper;
    @Resource private ConceptMapper conceptMapper;
    @Resource private ConceptTableMapper conceptTableMapper;
    @Resource private PropertyColumnMapper propertyColumnMapper;
    @Resource private PropertyMapper propertyMapper;
    @Resource private RelationMapper relationMapper;
    @Resource private RelationTableMapper relationTableMapper;
    @Resource private IDatasourceApiService datasourceApiService;
    @Resource private DataSourceFactory dataSourceFactory;
    @Resource private ObjectMapper objectMapper;

    @Override
    public Long createFunction(FunctionSaveReqVO reqVO) {
        FunctionDO func = FunctionConvert.INSTANCE.convert(reqVO);
        func.setVersion(1);
        functionMapper.insert(func);
        return func.getId();
    }

    @Override
    public Integer updateFunction(FunctionSaveReqVO reqVO) {
        FunctionDO existing = functionMapper.selectById(reqVO.getId());
        if (existing == null) {
            throw new RuntimeException("函数不存在: " + reqVO.getId());
        }
        FunctionDO func = FunctionConvert.INSTANCE.convert(reqVO);
        func.setVersion((existing.getVersion() == null ? 1 : existing.getVersion()) + 1);
        LambdaUpdateWrapper<FunctionDO> wrapper = new LambdaUpdateWrapper<FunctionDO>()
                .eq(FunctionDO::getId, reqVO.getId());
        if (existing.getVersion() == null) {
            wrapper.isNull(FunctionDO::getVersion);
        } else {
            wrapper.eq(FunctionDO::getVersion, existing.getVersion());
        }
        int updated = functionMapper.update(func, wrapper);
        if (updated == 0) {
            throw new RuntimeException("函数已被其他请求修改，请刷新后重试");
        }
        return updated;
    }

    @Override
    public Integer deleteFunction(Long id) {
        return functionMapper.deleteById(id);
    }

    @Override
    public FunctionRespVO getFunctionById(Long id) {
        return FunctionConvert.INSTANCE.convert(functionMapper.selectById(id));
    }

    @Override
    public PageResult<FunctionRespVO> getFunctionPage(FunctionPageReqVO pageReqVO) {
        PageResult<FunctionDO> page = functionMapper.selectPage(pageReqVO);
        return new PageResult<>(FunctionConvert.INSTANCE.convertList(page.getRows()), page.getTotal());
    }

    @Override
    public List<FunctionRespVO> getFunctionsByOntologyId(Long ontologyId) {
        return FunctionConvert.INSTANCE.convertList(functionMapper.selectByOntologyId(ontologyId));
    }

    @Override
    public List<FunctionRespVO> listAllFunctions() {
        return FunctionConvert.INSTANCE.convertList(functionMapper.selectAll());
    }

    @Override
    public String runFunctionDirect(Long functionId, String inputParams) {
        return runFunctionWithBinding(functionId, null, null, null, null, inputParams);
    }

    @Override
    public String runFunctionWithBinding(Long functionId, Long sourceConceptId, String sourceRelationIds,
                                         Long outputConceptId, Integer readLimit, String inputParams) {
        FunctionDO func = functionMapper.selectById(functionId);
        if (func == null) throw new RuntimeException("函数不存在: " + functionId);
        try {
            BindingConfig binding = new BindingConfig(sourceConceptId, sourceRelationIds, outputConceptId, readLimit);
            return runFunction(func, binding, inputParams);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("函数执行失败: " + e.getMessage(), e);
        }
    }

    @Override
    public String resolveFunctionBody(Long functionId, String inputParams) {
        FunctionDO func = functionMapper.selectById(functionId);
        if (func == null) throw new RuntimeException("函数不存在: " + functionId);
        Map<String, Object> params = parseParams(inputParams);
        return substituteParams(func.getBody(), params, func.getLang());
    }


    @Override
    @Transactional
    public FunctionExecRespVO submitExecution(FunctionExecReqVO reqVO) {
        FunctionDO func = functionMapper.selectById(reqVO.getFunctionId());
        if (func == null) throw new RuntimeException("函数不存在: " + reqVO.getFunctionId());
        FunctionExecutionDO exec = FunctionExecutionDO.builder()
                .functionId(func.getId())
                .ontologyId(func.getOntologyId())
                .inputParams(reqVO.getInputParams())
                .status(STATUS_PENDING)
                .build();
        executionMapper.insert(exec);
        return FunctionConvert.INSTANCE.convertExec(exec);
    }

    @Override
    @Transactional
    public void approveExecution(FunctionApprovalReqVO reqVO) {
        FunctionExecutionDO exec = executionMapper.selectById(reqVO.getExecutionId());
        if (exec == null) throw new RuntimeException("执行记录不存在");
        if (!STATUS_PENDING.equals(exec.getStatus())) throw new RuntimeException("当前状态不允许审批: " + exec.getStatus());
        exec.setStatus(STATUS_APPROVED);
        exec.setApprovalReason(reqVO.getApprovalReason());
        exec.setApproveTime(new Date());
        executionMapper.updateById(exec);
    }

    @Override
    @Transactional
    public void rejectExecution(FunctionApprovalReqVO reqVO) {
        FunctionExecutionDO exec = executionMapper.selectById(reqVO.getExecutionId());
        if (exec == null) throw new RuntimeException("执行记录不存在");
        if (!STATUS_PENDING.equals(exec.getStatus())) throw new RuntimeException("当前状态不允许拒绝: " + exec.getStatus());
        exec.setStatus(STATUS_REJECTED);
        exec.setApprovalReason(reqVO.getApprovalReason());
        exec.setApproveTime(new Date());
        executionMapper.updateById(exec);
    }

    @Override
    @Transactional
    public FunctionExecRespVO executeFunction(Long executionId) {
        FunctionExecutionDO exec = executionMapper.selectById(executionId);
        if (exec == null) throw new RuntimeException("执行记录不存在");
        if (!STATUS_APPROVED.equals(exec.getStatus())) {
            throw new RuntimeException("只有已批准的才能执行，当前状态: " + exec.getStatus());
        }
        FunctionDO func = functionMapper.selectById(exec.getFunctionId());
        long start = System.currentTimeMillis();
        try {
            String result = runFunction(func, null, exec.getInputParams());
            exec.setOutputResult(result);
            exec.setStatus(STATUS_EXECUTED);
            exec.setExecuteTime(new Date());
        } catch (Exception e) {
            log.error("函数执行失败", e);
            exec.setStatus(STATUS_FAILED);
            exec.setErrorMessage(e.getMessage());
        }
        exec.setDurationMs(System.currentTimeMillis() - start);
        executionMapper.updateById(exec);
        return FunctionConvert.INSTANCE.convertExec(exec);
    }

    @Override
    public FunctionExecRespVO getExecutionById(Long id) {
        return FunctionConvert.INSTANCE.convertExec(executionMapper.selectById(id));
    }

    @Override
    public PageResult<FunctionExecRespVO> getExecutionPage(FunctionExecPageReqVO pageReqVO) {
        PageResult<FunctionExecutionDO> page = executionMapper.selectPage(pageReqVO);
        return new PageResult<>(FunctionConvert.INSTANCE.convertExecList(page.getRows()), page.getTotal());
    }

    @Override
    public List<FunctionExecRespVO> getPendingApprovals(Long ontologyId) {
        if (ontologyId == null) {
            List<FunctionExecutionDO> all = executionMapper.selectList(
                    new LambdaQueryWrapperX<FunctionExecutionDO>()
                            .eq(FunctionExecutionDO::getStatus, STATUS_PENDING)
                            .orderByDesc(FunctionExecutionDO::getId));
            return FunctionConvert.INSTANCE.convertExecList(all);
        }
        return FunctionConvert.INSTANCE.convertExecList(executionMapper.selectPendingApprovals(ontologyId));
    }

    // ================================================================
    //  Execution Engine
    // ================================================================

    private String runFunction(FunctionDO func, BindingConfig binding, String inputParams) throws Exception {
        Map<String, Object> params = parseParams(inputParams);
        String body = substituteParams(func.getBody(), params, func.getLang());
        // 组装脚本 input：params 平铺保留（兼容旧脚本 input.paramName）；
        // 动作绑定数据来源主概念时额外注入 source（主概念数据）与 relations（关联关系数据）
        Map<String, Object> input = new HashMap<>(params);
        if (binding != null && binding.sourceConceptId != null) {
            Map<String, Object> conceptInput = buildConceptInput(func, binding);
            input.put("source", conceptInput.get("source"));
            input.put("relations", conceptInput.get("relations"));
        }
        String output;
        switch (func.getLang()) {
            case "TYPESCRIPT": output = runTypeScript(body, input); break;
            case "PYTHON": output = runPython(body, input); break;
            default: throw new RuntimeException("不支持的语言: " + func.getLang());
        }
        // 输出目标概念：脚本输出 JSON 数组（键=输出概念属性 code）→ 按主键 UPSERT 到该概念物理表
        if (binding != null && binding.outputConceptId != null) {
            return writeBackOutput(func, binding, output);
        }
        return output;
    }

    /**
     * 函数执行绑定配置：概念绑定（数据来源主概念/关联关系/输出目标概念）与读取上限。
     * 这些配置由「动作绑定函数」时确定（FUNCTION 类型动作的绑定字段），
     * 共享函数本身不持有绑定配置——不同动作绑定同一函数可使用不同概念与限制。
     */
    private static class BindingConfig {
        final Long sourceConceptId;
        final String sourceRelationIds;
        final Long outputConceptId;
        final Integer readLimit;

        BindingConfig(Long sourceConceptId, String sourceRelationIds, Long outputConceptId, Integer readLimit) {
            this.sourceConceptId = sourceConceptId;
            this.sourceRelationIds = sourceRelationIds;
            this.outputConceptId = outputConceptId;
            this.readLimit = readLimit;
        }
    }

    /**
     * 组装概念数据源输入：
     * - source: { conceptId, conceptName, table, rows:[...] }，rows 键=属性 code，只查所选属性列
     * - relations: { <关系code>: [ {物理列:值}, ... ] }，按绑定 SOURCE_RELATION_IDS 指定的关系查关联表
     * 未绑定主概念返回空结构（保持纯脚本行为）。
     */
    private Map<String, Object> buildConceptInput(FunctionDO func, BindingConfig binding) {
        Long conceptId = binding.sourceConceptId;
        List<ConceptTableDO> tables = conceptTableMapper.selectByConceptId(conceptId);
        if (tables.isEmpty()) {
            throw new RuntimeException("数据来源概念未绑定物理表: conceptId=" + conceptId);
        }
        ConceptTableDO table = tables.get(0);
        int readLimit = binding.readLimit != null && binding.readLimit > 0
                ? binding.readLimit : DEFAULT_READ_LIMIT;
        Map<String, Object> source = new LinkedHashMap<>();
        ConceptDO concept = conceptMapper.selectById(conceptId);
        source.put("conceptId", conceptId);
        source.put("conceptName", concept != null ? concept.getName() : null);
        source.put("table", table.getTableName());
        source.put("rows", queryConceptRows(func, conceptId, table, readLimit));
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("source", source);
        result.put("relations", queryRelations(binding, readLimit));
        return result;
    }

    /** 查询主概念物理表数据，键从物理列重命名为属性 code；PARAMS 指定属性 code 时只查这些列 */
    private List<Map<String, Object>> queryConceptRows(FunctionDO func, Long conceptId,
                                                       ConceptTableDO table, int readLimit) {
        List<ColumnMapping> mappings = resolveColumnMappings(conceptId, table.getId());
        List<String> selectedCodes = parseParamNames(func.getParamNames());
        List<String> cols = new ArrayList<>();
        for (ColumnMapping m : mappings) {
            if (selectedCodes.isEmpty() || selectedCodes.contains(m.semanticName)) {
                cols.add(m.physicalColumn);
            }
        }
        if (cols.isEmpty()) {
            cols.add("*"); // 无属性绑定或属性未匹配物理列，回退全列
        }
        String sql = "SELECT " + String.join(", ", cols) + " FROM " + table.getTableName() + " LIMIT " + readLimit;
        DbQuery dbQuery = getDbQuery(table.getDatasourceId());
        try {
            List<Map<String, Object>> rows = dbQuery.queryList(sql);
            if (mappings.isEmpty()) {
                return rows; // 无属性绑定，返回物理列原样
            }
            // 键重命名：物理列 → 属性 code
            List<Map<String, Object>> renamed = new ArrayList<>(rows.size());
            for (Map<String, Object> row : rows) {
                Map<String, Object> r = new LinkedHashMap<>();
                for (ColumnMapping m : mappings) {
                    if (row.containsKey(m.physicalColumn)) {
                        r.put(m.semanticName, row.get(m.physicalColumn));
                    }
                }
                renamed.add(r);
            }
            return renamed;
        } finally {
            closeDbQuery(dbQuery);
        }
    }

    /** 按绑定 SOURCE_RELATION_IDS 查询各关系关联表数据，键=关系 code，值=行数组（物理列名） */
    private Map<String, Object> queryRelations(BindingConfig binding, int readLimit) {
        Map<String, Object> relations = new LinkedHashMap<>();
        for (Long relId : parseRelationIds(binding.sourceRelationIds)) {
            RelationDO rel = relationMapper.selectById(relId);
            if (rel == null) {
                continue;
            }
            List<RelationTableDO> relTables = relationTableMapper.selectByRelationId(relId);
            if (relTables.isEmpty()) {
                continue;
            }
            RelationTableDO relTable = relTables.get(0);
            List<String> cols = parseColumnNames(relTable.getColumnNames());
            String sql = cols.isEmpty()
                    ? "SELECT * FROM " + relTable.getTableName() + " LIMIT " + readLimit
                    : "SELECT " + String.join(", ", cols) + " FROM " + relTable.getTableName() + " LIMIT " + readLimit;
            DbQuery dbQuery = getDbQuery(relTable.getDatasourceId());
            try {
                relations.put(rel.getCode() != null ? rel.getCode() : String.valueOf(relId),
                        dbQuery.queryList(sql));
            } finally {
                closeDbQuery(dbQuery);
            }
        }
        return relations;
    }

    /**
     * 输出落库：脚本输出按 JSON 解析（数组每行键=输出概念属性 code；单对象视为一行），
     * 映射到输出概念物理列后按主键 UPSERT。
     * 非 JSON 输出（纯文本）仅回显不落库；返回 {"output","inserted","updated","skipped"} 统计。
     */
    private String writeBackOutput(FunctionDO func, BindingConfig binding, String output) throws Exception {
        String trimmed = output == null ? "" : output.trim();
        if (trimmed.isEmpty()) {
            return output;
        }
        List<Map<String, Object>> outRows;
        try {
            Object parsed = objectMapper.readValue(trimmed, new TypeReference<Object>() {});
            if (parsed instanceof List) {
                outRows = new ArrayList<>();
                for (Object item : (List<?>) parsed) {
                    if (item instanceof Map) {
                        outRows.add((Map<String, Object>) item);
                    }
                }
            } else if (parsed instanceof Map) {
                outRows = new ArrayList<>();
                outRows.add((Map<String, Object>) parsed);
            } else {
                return output; // JSON 标量，无对象可落库
            }
        } catch (Exception e) {
            return output; // 纯文本输出，仅回显
        }
        if (outRows.isEmpty()) {
            return output;
        }
        Long conceptId = binding.outputConceptId;
        List<ConceptTableDO> tables = conceptTableMapper.selectByConceptId(conceptId);
        if (tables.isEmpty()) {
            throw new RuntimeException("输出目标概念未绑定物理表: conceptId=" + conceptId);
        }
        ConceptTableDO table = tables.get(0);
        DbQuery dbQuery = getDbQuery(table.getDatasourceId());
        try {
            List<ColumnMapping> mappings = resolveColumnMappings(conceptId, table.getId());
            List<String> pkCols = new ArrayList<>();
            for (ColumnMapping m : mappings) {
                if (m.primaryKey) {
                    pkCols.add(m.physicalColumn);
                }
            }
            if (pkCols.isEmpty()) {
                throw new RuntimeException("输出目标概念未配置主键属性，无法 UPSERT");
            }
            int inserted = 0, updated = 0, skipped = 0;
            for (Map<String, Object> row : outRows) {
                // 行键=属性 code → 映射为物理列；缺主键值的行跳过
                Map<String, Object> physical = new LinkedHashMap<>();
                List<String> whereCols = new ArrayList<>();
                for (ColumnMapping m : mappings) {
                    if (row.containsKey(m.semanticName)) {
                        physical.put(m.physicalColumn, row.get(m.semanticName));
                        if (m.primaryKey) {
                            whereCols.add(m.physicalColumn);
                        }
                    }
                }
                if (physical.isEmpty() || whereCols.isEmpty()) {
                    skipped++;
                    continue;
                }
                if (existsByPk(dbQuery, table.getTableName(), whereCols, physical)) {
                    List<String> setCols = new ArrayList<>(physical.keySet());
                    setCols.removeAll(whereCols);
                    if (setCols.isEmpty()) {
                        skipped++;
                        continue; // 只有主键，无需更新
                    }
                    dbQuery.update(buildUpdateSql(table.getTableName(), setCols, whereCols, physical));
                    updated++;
                } else {
                    dbQuery.update(buildInsertSql(table.getTableName(), physical));
                    inserted++;
                }
            }
            Map<String, Object> summary = new LinkedHashMap<>();
            summary.put("output", trimmed);
            summary.put("inserted", inserted);
            summary.put("updated", updated);
            summary.put("skipped", skipped);
            return toJson(summary);
        } finally {
            closeDbQuery(dbQuery);
        }
    }

    private boolean existsByPk(DbQuery dbQuery, String table, List<String> pkCols, Map<String, Object> physical) {
        StringBuilder where = new StringBuilder();
        for (String pk : pkCols) {
            if (where.length() > 0) {
                where.append(" AND ");
            }
            where.append(pk).append(" = ").append(sqlValue(physical.get(pk)));
        }
        List<Map<String, Object>> rows = dbQuery.queryList("SELECT COUNT(1) FROM " + table + " WHERE " + where);
        if (rows == null || rows.isEmpty()) {
            return false;
        }
        Object cnt = rows.get(0).values().iterator().next();
        return cnt != null && Long.parseLong(cnt.toString()) > 0;
    }

    private String buildUpdateSql(String table, List<String> setCols, List<String> whereCols,
                                  Map<String, Object> physical) {
        StringBuilder sb = new StringBuilder("UPDATE ").append(table).append(" SET ");
        boolean first = true;
        for (String col : setCols) {
            if (!first) {
                sb.append(", ");
            }
            sb.append(col).append(" = ").append(sqlValue(physical.get(col)));
            first = false;
        }
        sb.append(" WHERE ");
        first = true;
        for (String col : whereCols) {
            if (!first) {
                sb.append(" AND ");
            }
            sb.append(col).append(" = ").append(sqlValue(physical.get(col)));
            first = false;
        }
        return sb.toString();
    }

    private String buildInsertSql(String table, Map<String, Object> physical) {
        StringBuilder cols = new StringBuilder();
        StringBuilder vals = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, Object> en : physical.entrySet()) {
            if (!first) {
                cols.append(", ");
                vals.append(", ");
            }
            cols.append(en.getKey());
            vals.append(sqlValue(en.getValue()));
            first = false;
        }
        return "INSERT INTO " + table + " (" + cols + ") VALUES (" + vals + ")";
    }

    /** SQL 值字面量：null → NULL，Number/Boolean 原样，其余单引号包裹并转义 */
    private String sqlValue(Object val) {
        if (val == null) {
            return "NULL";
        }
        if (val instanceof Number || val instanceof Boolean) {
            return val.toString();
        }
        return "'" + val.toString().replace("'", "''") + "'";
    }

    /** 属性绑定 → 物理列映射（conceptTableId 定位），与动作 SQL 引擎同一套语义 */
    private static class ColumnMapping {
        final String physicalColumn;
        final String semanticName;
        final boolean primaryKey;

        ColumnMapping(String physicalColumn, String semanticName, boolean primaryKey) {
            this.physicalColumn = physicalColumn;
            this.semanticName = semanticName;
            this.primaryKey = primaryKey;
        }
    }

    private List<ColumnMapping> resolveColumnMappings(Long conceptId, Long conceptTableId) {
        List<PropertyColumnDO> bindingCols = propertyColumnMapper.selectByConceptTableId(conceptTableId);
        List<ColumnMapping> mappings = new ArrayList<>();
        for (PropertyColumnDO binding : bindingCols) {
            PropertyDO prop = propertyMapper.selectById(binding.getPropertyId());
            String propName = (prop != null && prop.getCode() != null) ? prop.getCode() : binding.getColumnName();
            boolean isPk = (prop != null && prop.getIsPrimary() != null && prop.getIsPrimary());
            mappings.add(new ColumnMapping(binding.getColumnName(), propName, isPk));
        }
        return mappings;
    }

    /** 按数据源 ID 解析真实数据源并创建 DbQuery（与动作执行同一链路） */
    private DbQuery getDbQuery(Long datasourceId) {
        if (datasourceId == null) {
            throw new RuntimeException("概念绑定的物理表缺少数据源配置");
        }
        DatasourceRespDTO ds = datasourceApiService.getDatasourceById(datasourceId);
        if (ds == null) {
            throw new RuntimeException("数据源不存在: id=" + datasourceId);
        }
        DbQueryProperty property = new DbQueryProperty(
                ds.getDatasourceType(), ds.getIp(), ds.getPort(), ds.getDatasourceConfig());
        return dataSourceFactory.createDbQuery(property);
    }

    private void closeDbQuery(DbQuery dbQuery) {
        if (dbQuery != null) {
            try {
                dbQuery.close();
            } catch (Exception e) {
                log.debug("关闭 DbQuery 失败", e);
            }
        }
    }

    private List<String> parseParamNames(String json) {
        if (json == null || json.trim().isEmpty()) {
            return Collections.emptyList();
        }
        try {
            List<String> list = objectMapper.readValue(json, new TypeReference<List<String>>() {});
            return list == null ? Collections.emptyList() : list;
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private List<Long> parseRelationIds(String json) {
        if (json == null || json.trim().isEmpty()) {
            return Collections.emptyList();
        }
        try {
            List<Long> list = objectMapper.readValue(json, new TypeReference<List<Long>>() {});
            return list == null ? Collections.emptyList() : list;
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private List<String> parseColumnNames(String json) {
        if (json == null || json.trim().isEmpty()) {
            return Collections.emptyList();
        }
        try {
            List<String> list = objectMapper.readValue(json, new TypeReference<List<String>>() {});
            return list == null ? Collections.emptyList() : list;
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    /**
     * 代码变量注入：将代码体中的 ${paramName} 占位符替换为实际值。
     * 未提供的参数一律替换为 null/None，保证注入后不再残留任何 ${...}，
     * 避免脚本语法错误。本源逻辑被执行、直接执行、审批展示（resolveFunctionBody）共用。
     */
    private String substituteParams(String body, Map<String, Object> params, String lang) {
        if (body == null) {
            return "";
        }
        Pattern pattern = Pattern.compile("\\$\\{([A-Za-z0-9_]+)\\}");
        Matcher matcher = pattern.matcher(body);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String name = matcher.group(1);
            Object value = params.get(name);
            matcher.appendReplacement(sb, Matcher.quoteReplacement(formatParamValue(value, lang)));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    /**
     * 按语言格式化占位符值：
     * TYPESCRIPT——null→null、Boolean→true/false、Number→原样、String/Map/List→JSON（双引号是合法 JS）；
     * PYTHON——null→None、Boolean→True/False、Number→原样、String→repr()、Map/List→json.dumps(ensure_ascii=False)。
     */
    private String formatParamValue(Object v, String lang) {
        if ("PYTHON".equals(lang)) {
            if (v == null) return "None";
            if (v instanceof Boolean) return ((Boolean) v) ? "True" : "False";
            if (v instanceof Number) return String.valueOf(v);
            if (v instanceof String) return pythonRepr((String) v);
            return objectMapperJson(v);
        }
        // TYPESCRIPT 或默认
        if (v == null) return "null";
        if (v instanceof Boolean) return ((Boolean) v) ? "true" : "false";
        if (v instanceof Number) return String.valueOf(v);
        return objectMapperJson(v);
    }

    /** Python repr() 风格字符串字面量（单引号，含引号/换行/控制字符时转义） */
    private String pythonRepr(String s) {
        StringBuilder sb = new StringBuilder("'");
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '\\': sb.append("\\\\"); break;
                case '\'': sb.append("\\'"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (c < 0x20) {
                        sb.append(String.format("\\x%02x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        sb.append("'");
        return sb.toString();
    }

    /** Map/List/String 等结构值输出为 JSON（ensure_ascii=False，非 ASCII 原样输出） */
    private String objectMapperJson(Object v) {
        try {
            return objectMapper.writeValueAsString(v);
        } catch (Exception e) {
            throw new RuntimeException("参数格式化失败: " + e.getMessage());
        }
    }

    private String runTypeScript(String body, Map<String, Object> input) throws Exception {
        String wrappedScript = "var input = " + toJson(input) + ";\n"
                + "(function() {\n" + body + "\n})();";
        ProcessBuilder pb = new ProcessBuilder("node", "-e", wrappedScript);
        pb.redirectErrorStream(true);
        Process proc = pb.start();
        String output = readStream(proc.getInputStream());
        int exit = proc.waitFor();
        if (exit != 0) throw new RuntimeException("TypeScript执行失败 (exit=" + exit + "): " + output);
        return output.trim();
    }

    private String runPython(String body, Map<String, Object> input) throws Exception {
        File tmpFile = File.createTempFile("ont_func_", ".py");
        tmpFile.deleteOnExit();
        String script = "import json\n"
                + "input = json.loads('''" + toJson(input) + "''')\n"
                + body;
        try (FileWriter fw = new FileWriter(tmpFile)) { fw.write(script); }
        // Windows 常无 python3 只有 python；Linux/macOS 常用 python3。按候选取名，找不到则回退下一候选。
        String os = System.getProperty("os.name", "").toLowerCase();
        boolean win = os.contains("win");
        String[] candidates = win
                ? new String[]{"python", "python3"}
                : new String[]{"python3", "python"};
        IOException lastIo = null;
        for (String candidate : candidates) {
            try {
                ProcessBuilder pb = new ProcessBuilder(candidate, tmpFile.getAbsolutePath());
                pb.redirectErrorStream(true);
                Process proc = pb.start();
                String output = readStream(proc.getInputStream());
                int exit = proc.waitFor();
                if (exit != 0) throw new RuntimeException("Python执行失败 (exit=" + exit + "): " + output);
                return output.trim();
            } catch (IOException io) {
                lastIo = io; // 命令不可用（command not found）→ 尝试下一个候选
            }
        }
        throw new RuntimeException("未找到可用的 Python 解释器（已尝试 " + String.join(", ", candidates) + "）："
                + (lastIo == null ? "" : lastIo.getMessage()));
    }

    private String readStream(InputStream is) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
            String line;
            while ((line = br.readLine()) != null) sb.append(line).append("\n");
        }
        return sb.toString();
    }

    private Map<String, Object> parseParams(String inputParams) {
        if (inputParams == null || inputParams.isEmpty()) return Collections.emptyMap();
        try {
            return objectMapper.readValue(inputParams, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            throw new RuntimeException("参数解析失败: " + e.getMessage());
        }
    }

    private String toJson(Object obj) {
        try { return objectMapper.writeValueAsString(obj); }
        catch (Exception e) { return "{}"; }
    }
}
