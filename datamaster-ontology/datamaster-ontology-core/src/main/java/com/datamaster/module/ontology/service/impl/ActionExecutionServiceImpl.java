package com.datamaster.module.ontology.service.impl;

import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.ontology.controller.admin.action.vo.*;
import com.datamaster.module.ontology.convert.ActionConvert;
import com.datamaster.module.ontology.dal.dataobject.*;
import com.datamaster.module.ontology.dal.mapper.*;
import com.datamaster.module.ontology.service.IActionExecutionService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.util.*;

@Service
@Validated
public class ActionExecutionServiceImpl implements IActionExecutionService {

    private static final Logger log = LoggerFactory.getLogger(ActionExecutionServiceImpl.class);

    @Resource private ActionExecutionMapper executionMapper;
    @Resource private ActionMapper actionMapper;
    @Resource private ConceptMapper conceptMapper;
    @Resource private ConceptTableMapper conceptTableMapper;
    @Resource private PropertyColumnMapper propertyColumnMapper;
    @Resource private PropertyMapper propertyMapper;
    @Resource private ObjectMapper objectMapper;

    private static final String STATUS_PENDING = "PENDING_APPROVAL";
    private static final String STATUS_APPROVED = "APPROVED";
    private static final String STATUS_REJECTED = "REJECTED";
    private static final String STATUS_EXECUTED = "EXECUTED";
    private static final String STATUS_FAILED = "FAILED";

    @Override
    @Transactional
    public ExecutionRespVO submitExecution(ExecutionSubmitReqVO reqVO) {
        ActionDO action = actionMapper.selectById(reqVO.getActionId());
        if (action == null) {
            throw new RuntimeException("动作不存在: " + reqVO.getActionId());
        }
        Map<String, Object> params = parseParams(reqVO.getInputParams());
        String sql = generateSql(action, params);
        Map<String, Object> preview = executeDryRun(action.getOntologyId(), sql, action.getActionType());
        String beforeData = null;
        if ("UPDATE".equals(action.getActionType()) || "DELETE".equals(action.getActionType())) {
            beforeData = captureSnapshot(action.getOntologyId(), sql);
        }
        ActionExecutionDO exec = ActionExecutionDO.builder()
                .actionId(action.getId()).ontologyId(action.getOntologyId())
                .inputParams(reqVO.getInputParams()).generatedSql(sql)
                .previewResult(toJson(preview)).beforeData(beforeData)
                .status(STATUS_PENDING).build();
        executionMapper.insert(exec);
        return ActionConvert.INSTANCE.convert(exec);
    }

    @Override
    @Transactional
    public void approveExecution(ApprovalReqVO reqVO) {
        ActionExecutionDO exec = getExecutionOrThrow(reqVO.getExecutionId());
        if (!STATUS_PENDING.equals(exec.getStatus())) {
            throw new RuntimeException("当前状态不允许审批: " + exec.getStatus());
        }
        exec.setStatus(STATUS_APPROVED);
        exec.setApprovalReason(reqVO.getApprovalReason());
        exec.setApproveTime(new Date());
        executionMapper.updateById(exec);
    }

    @Override
    @Transactional
    public void rejectExecution(ApprovalReqVO reqVO) {
        ActionExecutionDO exec = getExecutionOrThrow(reqVO.getExecutionId());
        if (!STATUS_PENDING.equals(exec.getStatus())) {
            throw new RuntimeException("当前状态不允许拒绝: " + exec.getStatus());
        }
        exec.setStatus(STATUS_REJECTED);
        exec.setApprovalReason(reqVO.getApprovalReason());
        exec.setApproveTime(new Date());
        executionMapper.updateById(exec);
    }

    @Override
    @Transactional
    public ExecutionRespVO executeExecution(Long executionId) {
        ActionExecutionDO exec = getExecutionOrThrow(executionId);
        if (!STATUS_APPROVED.equals(exec.getStatus())) {
            throw new RuntimeException("只有已批准的才能执行，当前状态: " + exec.getStatus());
        }
        try {
            JdbcTemplate jdbc = getJdbcTemplate(exec.getOntologyId());
            String sql = exec.getGeneratedSql();
            if (sql.toUpperCase().trim().startsWith("SELECT")) {
                List<Map<String, Object>> rows = jdbc.queryForList(sql);
                exec.setPreviewResult(toJson(rows));
            } else {
                int affected = jdbc.update(sql);
                exec.setPreviewResult(toJson(Collections.singletonMap("affectedRows", affected)));
            }
            exec.setStatus(STATUS_EXECUTED);
            exec.setExecuteTime(new Date());
        } catch (Exception e) {
            log.error("执行SQL失败", e);
            exec.setStatus(STATUS_FAILED);
            exec.setErrorMessage(e.getMessage());
        }
        executionMapper.updateById(exec);
        return ActionConvert.INSTANCE.convert(exec);
    }

    @Override
    public ExecutionRespVO getExecutionById(Long id) {
        return ActionConvert.INSTANCE.convert(executionMapper.selectById(id));
    }

    @Override
    public PageResult<ExecutionRespVO> getExecutionPage(ExecutionPageReqVO pageReqVO) {
        PageResult<ActionExecutionDO> page = executionMapper.selectPage(pageReqVO);
        return new PageResult<>(ActionConvert.INSTANCE.convertExecutionList(page.getRows()), page.getTotal());
    }

    @Override
    public List<ExecutionRespVO> getPendingApprovals(Long ontologyId) {
        return ActionConvert.INSTANCE.convertExecutionList(executionMapper.selectPendingApprovals(ontologyId));
    }

    // ================================================================
    //  SQL Engine
    // ================================================================

    /**
     * ColumnMapping pairs a physical column with its semantic property info.
     */
    private static class ColumnMapping {
        final String physicalColumn;   // ConceptTableDO.tableName + PropertyColumnDO.columnName
        final String semanticName;     // PropertyDO.code (used as param key)
        final boolean primaryKey;      // PropertyDO.isPrimary

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

    private String generateSql(ActionDO action, Map<String, Object> params) {
        Long conceptId = action.getConceptId();
        List<ConceptTableDO> tables = conceptTableMapper.selectByConceptId(conceptId);
        if (tables.isEmpty()) {
            throw new RuntimeException("概念未绑定物理表: conceptId=" + conceptId);
        }
        ConceptTableDO table = tables.get(0);
        String physicalTable = table.getTableName();
        List<ColumnMapping> mappings = resolveColumnMappings(conceptId, table.getId());
        switch (action.getActionType()) {
            case "SELECT": return buildSelectSql(physicalTable, mappings, params);
            case "CREATE": return buildInsertSql(physicalTable, mappings, params);
            case "UPDATE": return buildUpdateSql(physicalTable, mappings, params);
            case "DELETE": return buildDeleteSql(physicalTable, mappings, params);
            default: throw new RuntimeException("不支持的动作类型: " + action.getActionType());
        }
    }

    private String buildSelectSql(String table, List<ColumnMapping> mappings, Map<String, Object> params) {
        StringBuilder sb = new StringBuilder("SELECT ");
        boolean first = true;
        for (ColumnMapping m : mappings) {
            if (!first) sb.append(", ");
            sb.append(m.physicalColumn);
            first = false;
        }
        sb.append(" FROM ").append(table);
        String where = buildWhereClause(mappings, params);
        if (!where.isEmpty()) sb.append(" WHERE ").append(where);
        sb.append(" LIMIT 100");
        return sb.toString();
    }

    private String buildInsertSql(String table, List<ColumnMapping> mappings, Map<String, Object> params) {
        StringBuilder cols = new StringBuilder();
        StringBuilder vals = new StringBuilder();
        boolean first = true;
        for (ColumnMapping m : mappings) {
            if (params.containsKey(m.semanticName)) {
                if (!first) { cols.append(", "); vals.append(", "); }
                cols.append(m.physicalColumn);
                vals.append("'").append(escape(params.get(m.semanticName))).append("'");
                first = false;
            }
        }
        if (first) throw new RuntimeException("没有有效的输入参数");
        return "INSERT INTO " + table + " (" + cols + ") VALUES (" + vals + ")";
    }

    private String buildUpdateSql(String table, List<ColumnMapping> mappings, Map<String, Object> params) {
        StringBuilder setClauses = new StringBuilder();
        String whereClause = "";
        boolean first = true;
        for (ColumnMapping m : mappings) {
            if (params.containsKey(m.semanticName)) {
                if (m.primaryKey) {
                    whereClause = m.physicalColumn + " = '" + escape(params.get(m.semanticName)) + "'";
                } else {
                    if (!first) setClauses.append(", ");
                    setClauses.append(m.physicalColumn).append(" = '").append(escape(params.get(m.semanticName))).append("'");
                    first = false;
                }
            }
        }
        if (whereClause.isEmpty()) throw new RuntimeException("UPDATE 必须包含主键条件");
        return "UPDATE " + table + " SET " + setClauses + " WHERE " + whereClause;
    }

    private String buildDeleteSql(String table, List<ColumnMapping> mappings, Map<String, Object> params) {
        String whereClause = buildWhereClause(mappings, params);
        if (whereClause.isEmpty()) throw new RuntimeException("DELETE 必须包含条件");
        return "DELETE FROM " + table + " WHERE " + whereClause;
    }

    private String buildWhereClause(List<ColumnMapping> mappings, Map<String, Object> params) {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (ColumnMapping m : mappings) {
            if (params.containsKey(m.semanticName)) {
                if (!first) sb.append(" AND ");
                sb.append(m.physicalColumn).append(" = '").append(escape(params.get(m.semanticName))).append("'");
                first = false;
            }
        }
        return sb.toString();
    }

    // ================================================================
    //  Dry-run & Snapshot
    // ================================================================

    private Map<String, Object> executeDryRun(Long ontologyId, String sql, String actionType) {
        try {
            JdbcTemplate jdbc = getJdbcTemplate(ontologyId);
            if ("SELECT".equals(actionType)) {
                List<Map<String, Object>> rows = jdbc.queryForList(sql);
                return Collections.singletonMap("rows", rows);
            } else {
                int affected = jdbc.update(sql);
                return Collections.singletonMap("affectedRows", affected);
            }
        } catch (Exception e) {
            log.warn("dry-run 异常: {}", e.getMessage());
            return Collections.singletonMap("error", e.getMessage());
        }
    }

    private String captureSnapshot(Long ontologyId, String sql) {
        try {
            JdbcTemplate jdbc = getJdbcTemplate(ontologyId);
            String selectSql = convertToSelect(sql);
            if (selectSql != null) {
                List<Map<String, Object>> rows = jdbc.queryForList(selectSql);
                return toJson(rows);
            }
        } catch (Exception e) {
            log.warn("快照获取失败: {}", e.getMessage());
        }
        return null;
    }

    private String convertToSelect(String dmlSql) {
        String upper = dmlSql.trim().toUpperCase();
        if (upper.startsWith("UPDATE")) {
            int setIdx = upper.indexOf(" SET ");
            int whereIdx = upper.indexOf(" WHERE ", setIdx);
            String fromTable = dmlSql.substring(6, setIdx).trim();
            String where = whereIdx > 0 ? dmlSql.substring(whereIdx) : "";
            return "SELECT * FROM " + fromTable + " " + where;
        }
        if (upper.startsWith("DELETE")) {
            int whereIdx = upper.indexOf(" WHERE ");
            String fromTable = dmlSql.substring(7, whereIdx > 0 ? whereIdx : dmlSql.length()).trim();
            String where = whereIdx > 0 ? dmlSql.substring(whereIdx) : "";
            return "SELECT * FROM " + fromTable + " " + where;
        }
        return null;
    }

    // ================================================================
    //  Utilities
    // ================================================================

    private JdbcTemplate getJdbcTemplate(Long ontologyId) {
        DriverManagerDataSource ds = new DriverManagerDataSource();
        ds.setDriverClassName("org.postgresql.Driver");
        ds.setUrl("jdbc:postgresql://192.168.93.174:5432/datamaster");
        ds.setUsername("postgres");
        ds.setPassword("postgres");
        return new JdbcTemplate(ds);
    }

    private ActionExecutionDO getExecutionOrThrow(Long id) {
        ActionExecutionDO exec = executionMapper.selectById(id);
        if (exec == null) throw new RuntimeException("执行记录不存在: " + id);
        return exec;
    }

    private Map<String, Object> parseParams(String inputParams) {
        if (inputParams == null || inputParams.isEmpty()) return Collections.emptyMap();
        try {
            return objectMapper.readValue(inputParams, new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            throw new RuntimeException("参数解析失败: " + e.getMessage());
        }
    }

    private String toJson(Object obj) {
        try { return objectMapper.writeValueAsString(obj); }
        catch (Exception e) { return "[]"; }
    }

    private Object escape(Object val) {
        if (val == null) return "NULL";
        return val.toString().replace("'", "''");
    }
}
