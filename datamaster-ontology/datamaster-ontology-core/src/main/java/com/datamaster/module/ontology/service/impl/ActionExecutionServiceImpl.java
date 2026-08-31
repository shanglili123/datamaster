package com.datamaster.module.ontology.service.impl;

import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.ontology.controller.admin.action.vo.*;
import com.datamaster.module.ontology.convert.ActionConvert;
import com.datamaster.module.ontology.dal.dataobject.*;
import com.datamaster.module.ontology.dal.mapper.*;
import com.datamaster.module.ontology.service.IActionExecutionService;
import com.datamaster.module.ontology.service.IFunctionService;
import com.datamaster.common.database.DataSourceFactory;
import com.datamaster.common.database.DbQuery;
import com.datamaster.common.database.constants.DbQueryProperty;
import com.datamaster.common.datasource.mgmt.api.IDatasourceApiService;
import com.datamaster.common.datasource.mgmt.api.dto.DatasourceRespDTO;
import com.datamaster.module.assets.api.governance.dto.AssetsTableGovernanceReqDTO;
import com.datamaster.module.assets.api.service.governance.IAssetsTableGovernanceApiService;
import com.datamaster.neo4j.node.ActionExecutionNode;
import com.datamaster.neo4j.service.LineageDataService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.Statement;
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
    @Resource private IDatasourceApiService datasourceApiService;
    @Resource private DataSourceFactory dataSourceFactory;
    @Resource private IAssetsTableGovernanceApiService tableGovernanceApiService;
    @Resource private IFunctionService functionService;

    /**
     * 动作血缘写入（可插拔）：
     * LINEAGE_ENABLED=true 时由 LineageDataService 提供 Neo4j 写入；
     * 未开启/未部署 Neo4j 时该 Bean 不存在，注入为 null，不影响动作执行主流程。
     */
    @Autowired(required = false)
    private LineageDataService lineageDataService;

private static final String STATUS_PENDING = "PENDING_APPROVAL";
    private static final String STATUS_APPROVED = "APPROVED";
    private static final String STATUS_REJECTED = "REJECTED";
    private static final String STATUS_EXECUTED = "EXECUTED";
    private static final String STATUS_FAILED = "FAILED";
    private static final String STATUS_ROLLED_BACK = "ROLLED_BACK";

    /** 治理 API 入口标识：查询维持原值，增删改按动作类型区分（写操作严格模式） */
    private static final String ENTRANCE_ONTOLOGY_ACTION = "ONTOLOGY_ACTION";
    private static final String ENTRANCE_ONTOLOGY_CREATE = "ONTOLOGY_CREATE";
    private static final String ENTRANCE_ONTOLOGY_UPDATE = "ONTOLOGY_UPDATE";
    private static final String ENTRANCE_ONTOLOGY_DELETE = "ONTOLOGY_DELETE";

    @Override
    @Transactional
public ExecutionRespVO submitExecution(ExecutionSubmitReqVO reqVO) {
        ActionDO action = actionMapper.selectById(reqVO.getActionId());
        if (action == null) {
            throw new RuntimeException("动作不存在: " + reqVO.getActionId());
        }
        // FUNCTION 类型动作：不生成SQL、不干跑、不做表权限校验。
        // 直接校验函数与参数后，将替换后的解析代码体写入 generatedSql 供审批展示。
        if ("FUNCTION".equals(action.getActionType())) {
            if (action.getFunctionId() == null) {
                throw new RuntimeException("函数类型动作未绑定共享函数");
            }
            String resolvedBody = functionService.resolveFunctionBody(action.getFunctionId(), reqVO.getInputParams());
            if (resolvedBody == null || resolvedBody.trim().isEmpty()) {
                resolvedBody = "[函数类型动作]";
            }
            ActionExecutionDO functionExec = ActionExecutionDO.builder()
                    .actionId(action.getId()).ontologyId(action.getOntologyId())
                    .inputParams(reqVO.getInputParams()).generatedSql(resolvedBody)
                    .previewResult(null)
                    .spaceId(reqVO.getSpaceId()).spaceCode(reqVO.getSpaceCode())
                    .status(STATUS_PENDING).build();
            executionMapper.insert(functionExec);
            return ActionConvert.INSTANCE.convert(functionExec);
        }
        // 提交前先校验当前空间对目标物理表的访问权限；
        // 增删改（CREATE/UPDATE/DELETE）额外按涉及列做字段级严格校验（主张5接入）
        Map<String, Object> rawInputParams = parseParams(reqVO.getInputParams());
        Map<String, Object> params = applyParamConfig(action, rawInputParams);
        assertTableAccess(action, reqVO.getSpaceId(), reqVO.getSpaceCode(),
                extractActionColumns(action, params), action.getActionType());
String sql = generateSql(action, params);
        Map<String, Object> preview = executeDryRun(action.getConceptId(), sql, action.getActionType());
        // beforeData 不在提交时抓取：提交→执行之间存在时间差（审批、排队），
        // 提交时抓的快照可能与执行前一刻的数据不一致（如提交后行被改/被删/新插入）。
        // 「可溯源回退」要求修改前数据=执行前一刻的旧值，故在 executeExecution 执行 DML 前重查。
        ActionExecutionDO exec = ActionExecutionDO.builder()
                .actionId(action.getId()).ontologyId(action.getOntologyId())
                .inputParams(reqVO.getInputParams()).generatedSql(sql)
                .previewResult(toJson(preview))
                .spaceId(reqVO.getSpaceId()).spaceCode(reqVO.getSpaceCode())
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
        ActionDO action = actionMapper.selectById(exec.getActionId());
        if (action == null) {
            throw new RuntimeException("动作不存在: " + exec.getActionId());
        }
        // FUNCTION 类型动作：直接调用共享函数执行，无需SQL/快照/血缘/表权限校验。
        // 概念绑定/读取限制/输出目标等来自动作绑定函数的绑定配置；
        // 输出内容一律以 JSON 记录（有输出目标概念时同时 UPSERT 落库并记录汇总 JSON）。
        if ("FUNCTION".equals(action.getActionType())) {
            try {
                if (action.getFunctionId() == null) {
                    throw new RuntimeException("函数类型动作未绑定共享函数");
                }
                // 字段映射入参（param_config 中 kind=field 的条目）：把脚本入参映射到数据来源概念属性，
                // 注入 input[paramName] = 属性 code，脚本通过入参名动态引用要处理的字段（不写死属性 code）。
                String effectiveInputParams = applyFunctionParamMapping(action, parseParams(exec.getInputParams()));
                String output = functionService.runFunctionWithBinding(
                        action.getFunctionId(),
                        action.getSourceConceptId(),
                        action.getSourceRelationIds(),
                        action.getOutputConceptId(),
                        action.getReadLimit(),
                        effectiveInputParams);
                exec.setPreviewResult(toJson(Collections.singletonMap("output", output)));
                exec.setStatus(STATUS_EXECUTED);
                exec.setExecuteTime(new Date());
            } catch (Exception e) {
                log.error("执行函数失败", e);
                exec.setStatus(STATUS_FAILED);
                exec.setErrorMessage(e.getMessage());
            }
            executionMapper.updateById(exec);
            return ActionConvert.INSTANCE.convert(exec);
        }
        DbQuery dbQuery = getDbQuery(action.getConceptId());
        try {
            // 执行前二次校验：防提交审批后到实际执行之间权限被回收而绕过控制；
            // 复用提交快照的空间上下文（exec.spaceId/spaceCode）+ 与生成SQL一致的有效参数解析列
            Map<String, Object> execParams = applyParamConfig(action, parseParams(exec.getInputParams()));
            assertTableAccess(action, exec.getSpaceId(), exec.getSpaceCode(),
                    extractActionColumns(action, execParams), action.getActionType());
            String sql = exec.getGeneratedSql();
            if (sql.toUpperCase().trim().startsWith("SELECT")) {
                List<Map<String, Object>> rows = dbQuery.queryList(sql);
                exec.setPreviewResult(toJson(rows));
            } else {
                // 执行前一刻抓取旧值快照（可溯源回退要求「修改前」= 执行前瞬间的旧值，
                // 而非提交时的快照——提交→执行之间数据可能已变化）
                if ("UPDATE".equals(action.getActionType()) || "DELETE".equals(action.getActionType())) {
                    exec.setBeforeData(captureSnapshot(action.getConceptId(), sql));
                }
                int affected = dbQuery.update(sql);
                exec.setPreviewResult(toJson(Collections.singletonMap("affectedRows", affected)));
                // 执行后数据快照（可溯源回退）：
                // UPDATE 重查新值 / DELETE 重查空集（行已删，beforeData 保留用于回退=重INSERT）
                // / CREATE 按主键回查新插入行
                exec.setAfterData(captureAfterData(action, sql, execParams));
            }
            exec.setStatus(STATUS_EXECUTED);
            exec.setExecuteTime(new Date());
        } catch (Exception e) {
            log.error("执行SQL失败", e);
            exec.setStatus(STATUS_FAILED);
            exec.setErrorMessage(e.getMessage());
        } finally {
            closeDbQuery(dbQuery);
        }
// 动作血缘写节点（可插拔：LINEAGE_ENABLED=false 时 lineageDataService 为 null 跳过）
        writeActionLineageSilently(exec, action);
        executionMapper.updateById(exec);
        return ActionConvert.INSTANCE.convert(exec);
    }

    @Override
    @Transactional
    public ExecutionRespVO rollbackExecution(Long executionId) {
        ActionExecutionDO exec = getExecutionOrThrow(executionId);
        if (!STATUS_EXECUTED.equals(exec.getStatus())) {
            throw new RuntimeException("只有已执行(EXECUTED)的记录才能回退，当前状态: " + exec.getStatus());
        }
        if (exec.getRollbackTime() != null) {
            throw new RuntimeException("该记录已回退过（" + exec.getRollbackTime() + "），请勿重复回退");
        }
        ActionDO action = actionMapper.selectById(exec.getActionId());
        if (action == null) {
            throw new RuntimeException("动作不存在: " + exec.getActionId());
        }
        // 函数类型动作无物理表，不支持回退
        if ("FUNCTION".equals(action.getActionType())) {
            throw new RuntimeException("函数类型动作不支持回退");
        }
        // 回退属于写操作：复用提交快照的空间上下文，按原动作类型走字段级严格校验
        Map<String, Object> execParams = applyParamConfig(action, parseParams(exec.getInputParams()));
        assertTableAccess(action, exec.getSpaceId(), exec.getSpaceCode(),
                extractActionColumns(action, execParams), action.getActionType());
        DbQuery dbQuery = getDbQuery(action.getConceptId());
        List<String> rollbackSqls = buildRollbackSqls(action, exec, dbQuery);
        if (rollbackSqls.isEmpty()) {
            throw new RuntimeException("没有可回退的数据（快照为空）");
        }
        String rollbackSql = String.join(";\n", rollbackSqls);
        // 回退后数据快照：UPDATE/DELETE 回退=还原的旧值快照 / CREATE 回退=删除新行后为空集
        String rollbackAfterData = "CREATE".equals(action.getActionType()) ? "[]" : exec.getBeforeData();
        try {
            // 事务内执行全部还原语句，任一失败整体回滚，避免出现半还原状态
            Connection con = dbQuery.getConnection();
            try {
                con.setAutoCommit(false);
                int totalAffected = 0;
                try (Statement st = con.createStatement()) {
                    for (String sql : rollbackSqls) {
                        totalAffected += st.executeUpdate(sql);
                    }
                }
                con.commit();
                // 原执行记录保留 EXECUTED，仅补回退标记（防止重复回退 + 留痕）：
                // 回退前数据 = 原执行后数据（回退动作执行时的当前状态）
                exec.setRollbackSql(rollbackSql);
                exec.setRollbackTime(new Date());
                exec.setErrorMessage(null);
                exec.setRollbackBeforeData(exec.getAfterData());
                exec.setRollbackAfterData(rollbackAfterData);
                // 回退动作本身新增一条独立执行记录（ROLLED_BACK）：
                // 列表可见「回退记录」，beforeData=回退前 / afterData=回退后，可独立查看前后对比
                ActionExecutionDO rollbackExec = ActionExecutionDO.builder()
                        .actionId(exec.getActionId())
                        .ontologyId(exec.getOntologyId())
                        .spaceId(exec.getSpaceId())
                        .spaceCode(exec.getSpaceCode())
                        .inputParams(exec.getInputParams())
                        .generatedSql(rollbackSql)
                        .previewResult(toJson(Collections.singletonMap("affectedRows", totalAffected)))
                        .beforeData(exec.getAfterData())
                        .afterData(rollbackAfterData)
                        .status(STATUS_ROLLED_BACK)
                        .executeTime(new Date())
                        .build();
                executionMapper.insert(rollbackExec);
            } catch (Exception e) {
                try { con.rollback(); } catch (Exception rbEx) { log.debug("回退事务回滚失败", rbEx); }
                throw e;
            } finally {
                try { con.close(); } catch (Exception cEx) { log.debug("回退连接关闭失败", cEx); }
            }
        } catch (Exception e) {
            log.error("回退SQL失败", e);
            exec.setStatus(STATUS_FAILED);
            exec.setErrorMessage(e.getMessage());
        } finally {
            closeDbQuery(dbQuery);
        }
        executionMapper.updateById(exec);
        return ActionConvert.INSTANCE.convert(exec);
    }

    /**
     * 构建回退 SQL（可溯源回退）：
     * - UPDATE：按 beforeData 旧值全列写回，主键列进 WHERE 精确定位
     * - DELETE：按 beforeData 重 INSERT 被删行
     * - CREATE：按 afterData 主键 DELETE 新插行
     * SELECT 不涉及数据变更，返回空表（无回退动作）。
     * 主键定位双保险：1) 概念属性 isPrimary 标记的列；2) 物理表真实主键(colKey)兜底，
     * 避免属性未标记主键时回退无法精确定位行。
     */
    private List<String> buildRollbackSqls(ActionDO action, ActionExecutionDO exec, DbQuery dbQuery) {
        Long conceptId = action.getConceptId();
        List<ConceptTableDO> tables = conceptTableMapper.selectByConceptId(conceptId);
        if (tables.isEmpty()) {
            throw new RuntimeException("概念未绑定物理表: conceptId=" + conceptId);
        }
        String table = tables.get(0).getTableName();
        List<ColumnMapping> mappings = resolveColumnMappings(conceptId, tables.get(0).getId());
        List<String> pkCols = new ArrayList<>();
        for (ColumnMapping m : mappings) {
            if (m.primaryKey) {
                pkCols.add(m.physicalColumn);
            }
        }
        List<Map<String, Object>> beforeRows = parseSnapshotRows(exec.getBeforeData());
        List<Map<String, Object>> afterRows = parseSnapshotRows(exec.getAfterData());
        // 物理主键兜底：仅当属性级主键缺失时，用快照行 keys 交集物理表主键(colKey)精确回退
        if (pkCols.isEmpty()) {
            pkCols = resolvePhysicalPkColumns(dbQuery, table, beforeRows, afterRows);
        }
        List<String> sqls = new ArrayList<>();
        switch (action.getActionType()) {
            case "UPDATE":
                if (beforeRows.isEmpty()) {
                    throw new RuntimeException("缺少执行前快照，无法回退 UPDATE");
                }
                if (pkCols.isEmpty()) {
                    throw new RuntimeException("表未配置主键属性且物理表主键无法识别，无法精确定位回退行");
                }
                for (Map<String, Object> row : beforeRows) {
                    sqls.add(buildRollbackUpdate(table, pkCols, row));
                }
                break;
            case "DELETE":
                if (beforeRows.isEmpty()) {
                    throw new RuntimeException("缺少执行前快照，无法回退 DELETE");
                }
                for (Map<String, Object> row : beforeRows) {
                    sqls.add(buildRollbackInsert(table, row));
                }
                break;
            case "CREATE":
                if (afterRows.isEmpty()) {
                    throw new RuntimeException("缺少执行后快照，无法回退 CREATE");
                }
                if (pkCols.isEmpty()) {
                    throw new RuntimeException("表未配置主键属性且物理表主键无法识别，无法精确定位回退行");
                }
                for (Map<String, Object> row : afterRows) {
                    sqls.add(buildRollbackDelete(table, pkCols, row));
                }
                break;
            default:
                // SELECT 无数据变更，无需回退
                log.warn("动作类型 [{}] 不支持回退", action.getActionType());
        }
        return sqls;
    }

    /**
     * 物理表真实主键列兜底解析：
     * 通过已打开连接的 DatabaseMetaData.getPrimaryKeys 获取物理表主键列，
     * 与快照行 keys 求交集（仅保留快照中确有数据的列），避免引入快照外列导致 SQL 报错。
     * 属性级主键已存在时该方法不会被调用。
     */
    private List<String> resolvePhysicalPkColumns(DbQuery dbQuery, String table,
                                                  List<Map<String, Object>> beforeRows,
                                                  List<Map<String, Object>> afterRows) {
        Set<String> snapshotKeys = new LinkedHashSet<>();
        for (Map<String, Object> row : beforeRows) {
            snapshotKeys.addAll(row.keySet());
        }
        for (Map<String, Object> row : afterRows) {
            snapshotKeys.addAll(row.keySet());
        }
        List<String> pkCols = new ArrayList<>();
        if (snapshotKeys.isEmpty()) {
            return pkCols;
        }
        // 大小写不敏感交集：快照列名（结果集 label）与 COLUMN_NAME 可能因方言大小写规则不同
        Map<String, String> snapshotLower = new HashMap<>();
        for (String k : snapshotKeys) {
            snapshotLower.put(k.toLowerCase(), k);
        }
        Connection con = null;
        try {
            con = dbQuery.getConnection();
            DatabaseMetaData meta = con.getMetaData();
            try (ResultSet rs = meta.getPrimaryKeys(null, null, table)) {
                while (rs.next()) {
                    String col = rs.getString("COLUMN_NAME");
                    if (col != null) {
                        String actualKey = snapshotLower.get(col.toLowerCase());
                        if (actualKey != null) {
                            pkCols.add(actualKey);
                        }
                    }
                }
            }
            if (!pkCols.isEmpty()) {
                log.info("回退主键兜底：物理表 [{}] 主键列 {} 与快照交集定位", table, pkCols);
            } else {
                log.warn("回退主键兜底：物理表 [{}] 无主键或快照不含主键列，快照列={}",
                        table, snapshotKeys);
            }
        } catch (Exception e) {
            log.warn("回退物理主键解析失败，将按原主键校验报错: {}", e.getMessage());
        } finally {
            if (con != null) {
                try { con.close(); } catch (Exception cEx) { log.debug("主键解析连接关闭失败", cEx); }
            }
        }
        return pkCols;
    }

    private List<Map<String, Object>> parseSnapshotRows(String json) {
        if (json == null || json.trim().isEmpty()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<Map<String, Object>>>() {});
        } catch (Exception e) {
            log.warn("快照解析失败: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /** UPDATE 回退：SET 非主键列 = 旧值，WHERE 主键 = 旧主键 */
    private String buildRollbackUpdate(String table, List<String> pkCols, Map<String, Object> row) {
        StringBuilder set = new StringBuilder();
        List<String> where = new ArrayList<>();
        for (Map.Entry<String, Object> en : row.entrySet()) {
            String col = en.getKey();
            if (pkCols.contains(col)) {
                where.add(col + " = " + rollbackSqlValue(en.getValue()));
            } else {
                if (set.length() > 0) set.append(", ");
                set.append(col).append(" = ").append(rollbackSqlValue(en.getValue()));
            }
        }
        if (where.isEmpty()) {
            throw new RuntimeException("快照行缺少主键值，无法回退");
        }
        return "UPDATE " + table + " SET " + set + " WHERE " + String.join(" AND ", where);
    }

    /** DELETE 回退：按被删行重新 INSERT */
    private String buildRollbackInsert(String table, Map<String, Object> row) {
        StringBuilder cols = new StringBuilder();
        StringBuilder vals = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, Object> en : row.entrySet()) {
            if (!first) {
                cols.append(", ");
                vals.append(", ");
            }
            cols.append(en.getKey());
            vals.append(rollbackSqlValue(en.getValue()));
            first = false;
        }
        return "INSERT INTO " + table + " (" + cols + ") VALUES (" + vals + ")";
    }

    /** CREATE 回退：按新插行主键 DELETE */
    private String buildRollbackDelete(String table, List<String> pkCols, Map<String, Object> row) {
        List<String> where = new ArrayList<>();
        for (String pk : pkCols) {
            if (row.containsKey(pk)) {
                where.add(pk + " = " + rollbackSqlValue(row.get(pk)));
            }
        }
        if (where.isEmpty()) {
            throw new RuntimeException("快照行缺少主键值，无法回退");
        }
        return "DELETE FROM " + table + " WHERE " + String.join(" AND ", where);
    }

    /** 回退值渲染：null → NULL（不套引号），其余复用 sqlValue 的引号转义 */
    private String rollbackSqlValue(Object val) {
        if (val == null) {
            return "NULL";
        }
        return sqlValue(val);
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
        List<ConditionSpec> conditionSpecs = resolveConditionSpecs(action);
        switch (action.getActionType()) {
            case "SELECT": return buildSelectSql(physicalTable, mappings, params);
            case "CREATE": return buildInsertSql(physicalTable, mappings, params);
            case "UPDATE": return buildUpdateSql(physicalTable, mappings, params, conditionSpecs);
            case "DELETE": return buildDeleteSql(physicalTable, mappings, params, conditionSpecs);
            default: throw new RuntimeException("不支持的动作类型: " + action.getActionType());
        }
    }

    /**
     * 读取动作参数配置(PARAM_CONFIG)中显式勾选的「条件字段」：
     * condition=true 的列按配置顺序参与 UPDATE/DELETE 的 WHERE 构建。
     * 每个条件可带逻辑组合：
     * - conditionLink: 与前一个条件的连接符，AND(且) / OR(或)，首个条件忽略
     * - conditionNegate: 是否取反(NOT)，true 时生成 NOT (col = value)
     * 未配置 PARAM_CONFIG 或配置中无 condition 标记的旧动作返回空表，
     * 此时保持旧行为（UPDATE 退化为仅属性级主键、DELETE 退化为全部有值列）。
     */
    private List<ConditionSpec> resolveConditionSpecs(ActionDO action) {
        List<ConditionSpec> specs = new ArrayList<>();
        String configJson = action.getParamConfig();
        if (configJson == null || configJson.trim().isEmpty()) {
            return specs;
        }
        try {
            List<Map<String, Object>> configs = objectMapper.readValue(configJson,
                    new TypeReference<List<Map<String, Object>>>() {});
            for (Map<String, Object> cfg : configs) {
                boolean condition = Boolean.TRUE.equals(cfg.get("condition"))
                        || "true".equalsIgnoreCase(String.valueOf(cfg.get("condition")));
                if (!condition || cfg.get("propertyCode") == null) {
                    continue;
                }
                String link = "OR".equalsIgnoreCase(String.valueOf(cfg.get("conditionLink"))) ? "OR" : "AND";
                boolean negate = Boolean.TRUE.equals(cfg.get("conditionNegate"))
                        || "true".equalsIgnoreCase(String.valueOf(cfg.get("conditionNegate")));
                specs.add(new ConditionSpec(String.valueOf(cfg.get("propertyCode")), link, negate));
            }
        } catch (Exception e) {
            log.warn("动作参数配置解析失败（条件字段识别）: {}", e.getMessage());
        }
        return specs;
    }

    /**
     * 条件字段规格：属性语义名 + 与前一个条件的连接符(首个忽略) + 是否取反(NOT)。
     */
    private static class ConditionSpec {
        final String semanticName;
        final String link;      // "AND" 或 "OR"
        final boolean negate;   // NOT 取反

        ConditionSpec(String semanticName, String link, boolean negate) {
            this.semanticName = semanticName;
            this.link = link;
            this.negate = negate;
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
                vals.append(sqlValue(params.get(m.semanticName)));
                first = false;
            }
        }
        if (first) throw new RuntimeException("没有有效的输入参数");
        return "INSERT INTO " + table + " (" + cols + ") VALUES (" + vals + ")";
    }

    private String buildUpdateSql(String table, List<ColumnMapping> mappings, Map<String, Object> params,
                                  List<ConditionSpec> conditionSpecs) {
        StringBuilder setClauses = new StringBuilder();
        boolean first = true;
        for (ColumnMapping m : mappings) {
            if (!params.containsKey(m.semanticName)) {
                continue;
            }
            // WHERE 条件 = 属性级主键 或 动作配置中显式勾选的「条件字段」：这些列不进 SET 子句
            boolean isCondition = m.primaryKey || containsSpec(conditionSpecs, m.semanticName);
            if (isCondition) {
                continue;
            }
            if (!first) setClauses.append(", ");
            setClauses.append(m.physicalColumn).append(" = ").append(sqlValue(params.get(m.semanticName)));
            first = false;
        }
        // 配置条件按序拼接（AND/OR/NOT），主键条件附加到末尾（AND）
        String whereClause = buildWhereWithConditions(mappings, params, conditionSpecs, true);
        if (whereClause.isEmpty()) throw new RuntimeException("UPDATE 必须包含主键或条件字段");
        if (first) throw new RuntimeException("UPDATE 没有可更新的字段");
        return "UPDATE " + table + " SET " + setClauses + " WHERE " + whereClause;
    }

    private String buildDeleteSql(String table, List<ColumnMapping> mappings, Map<String, Object> params,
                                  List<ConditionSpec> conditionSpecs) {
        String whereClause;
        // 显式配置了「条件字段」则严格只用条件字段（含 AND/OR/NOT 组合）；否则回退全部有值列（旧行为兼容）
        if (!conditionSpecs.isEmpty()) {
            whereClause = buildWhereWithConditions(mappings, params, conditionSpecs, false);
        } else {
            whereClause = buildWhereClause(mappings, params);
        }
        if (whereClause.isEmpty()) throw new RuntimeException("DELETE 必须包含条件字段");
        return "DELETE FROM " + table + " WHERE " + whereClause;
    }

    private boolean containsSpec(List<ConditionSpec> specs, String semanticName) {
        for (ConditionSpec s : specs) {
            if (s.semanticName.equals(semanticName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 按条件规格构建 WHERE 子句（支持 AND/OR/NOT 逻辑组合）：
     * - 配置条件按 paramConfig 顺序拼接：expr_0 [link] expr_1 [link] expr_2 ...
     * - NOT(非) 对单个条件取反：NOT (col = value)
     * - includePrimaryKeys=true 时，未在配置条件中的属性级主键补充到末尾（AND 连接）
     */
    private String buildWhereWithConditions(List<ColumnMapping> mappings, Map<String, Object> params,
                                            List<ConditionSpec> conditionSpecs, boolean includePrimaryKeys) {
        List<String> exprs = new ArrayList<>();
        List<String> links = new ArrayList<>();
        // 1. 配置条件（按顺序）
        for (ConditionSpec spec : conditionSpecs) {
            ColumnMapping m = findMapping(mappings, spec.semanticName);
            if (m == null || !params.containsKey(m.semanticName)) {
                continue;
            }
            exprs.add(buildConditionExpr(m, params, spec.negate));
            links.add(spec.link);
        }
        // 2. 补充未配置的属性级主键（AND 连接）
        if (includePrimaryKeys) {
            for (ColumnMapping m : mappings) {
                if (m.primaryKey && !containsSpec(conditionSpecs, m.semanticName)
                        && params.containsKey(m.semanticName)) {
                    exprs.add(buildConditionExpr(m, params, false));
                    links.add("AND");
                }
            }
        }
        return joinWhere(exprs, links);
    }

    private ColumnMapping findMapping(List<ColumnMapping> mappings, String semanticName) {
        for (ColumnMapping m : mappings) {
            if (m.semanticName.equals(semanticName)) {
                return m;
            }
        }
        return null;
    }

    private String buildConditionExpr(ColumnMapping m, Map<String, Object> params, boolean negate) {
        String expr = m.physicalColumn + " = " + sqlValue(params.get(m.semanticName));
        return negate ? "NOT (" + expr + ")" : expr;
    }

    private String joinWhere(List<String> exprs, List<String> links) {
        if (exprs.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder(exprs.get(0));
        for (int i = 1; i < exprs.size(); i++) {
            sb.append(" ").append(links.get(i - 1)).append(" ").append(exprs.get(i));
        }
        return sb.toString();
    }

    private String buildWhereClause(List<ColumnMapping> mappings, Map<String, Object> params) {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (ColumnMapping m : mappings) {
            if (params.containsKey(m.semanticName)) {
                if (!first) sb.append(" AND ");
                sb.append(m.physicalColumn).append(" = ").append(sqlValue(params.get(m.semanticName)));
                first = false;
            }
        }
        return sb.toString();
    }

    // ================================================================
    //  Param Config（属性选择 + 目标值配置）
    // ================================================================

    /**
     * 按动作定义的参数配置(PARAM_CONFIG)解析执行参数。
     * 三种目标值模式：
     * - direct:      固定字面值，直接取配置值（正常加引号转义）
     * - placeholder: 引用执行入参，模板形如 ${orderId}
     * - expression:  原生SQL表达式（如 CASE WHEN ... END），拼装时不加引号
     * 未配置 PARAM_CONFIG 的动作保持旧行为：直接使用入参生成SQL。
     */
    private Map<String, Object> applyParamConfig(ActionDO action, Map<String, Object> inputParams) {
        String configJson = action.getParamConfig();
        if (configJson == null || configJson.trim().isEmpty()) {
            return inputParams;
        }
        List<Map<String, Object>> configs;
        try {
            configs = objectMapper.readValue(configJson,
                    new TypeReference<List<Map<String, Object>>>() {});
        } catch (Exception e) {
            throw new RuntimeException("动作参数配置解析失败: " + e.getMessage());
        }
        Map<String, Object> effective = new LinkedHashMap<>();
        for (Map<String, Object> cfg : configs) {
            if (cfg.get("propertyCode") == null) {
                continue;
            }
            String propertyCode = String.valueOf(cfg.get("propertyCode"));
            String valueMode = cfg.get("valueMode") == null ? "direct" : String.valueOf(cfg.get("valueMode"));
            String valueTemplate = cfg.get("valueTemplate") == null ? "" : String.valueOf(cfg.get("valueTemplate"));
            boolean required = Boolean.TRUE.equals(cfg.get("required"))
                    || "true".equalsIgnoreCase(String.valueOf(cfg.get("required")));
            switch (valueMode) {
                case "direct":
                    effective.put(propertyCode, valueTemplate);
                    break;
                case "placeholder": {
                    String paramName = stripPlaceholder(valueTemplate);
                    Object v = inputParams.get(paramName);
                    if (v == null && required) {
                        throw new RuntimeException("缺少必填执行参数: " + paramName);
                    }
                    if (v != null) {
                        effective.put(propertyCode, v);
                    }
                    break;
                }
                case "expression":
                    if (valueTemplate.trim().isEmpty()) {
                        throw new RuntimeException("表达式目标值不能为空: " + propertyCode);
                    }
                    effective.put(propertyCode, new RawExpression(valueTemplate));
                    break;
                default:
                    throw new RuntimeException("不支持的目标值模式: " + valueMode);
            }
        }
        // 配置未覆盖的入参向后兼容合并（如主键条件）
        for (Map.Entry<String, Object> en : inputParams.entrySet()) {
            effective.putIfAbsent(en.getKey(), en.getValue());
        }
        return effective;
    }

    private String stripPlaceholder(String template) {
        String t = template.trim();
        if (t.startsWith("${") && t.endsWith("}")) {
            return t.substring(2, t.length() - 1).trim();
        }
        return t;
    }

    /**
     * SQL 值渲染：RawExpression 原样输出，其余加引号并转义。
     */
    private String sqlValue(Object val) {
        if (val instanceof RawExpression) {
            return ((RawExpression) val).expr;
        }
        return "'" + escape(val) + "'";
    }

    /**
     * 原生SQL表达式值：仅允许出现在 PARAM_CONFIG 的 expression 模式中。
     */
    private static class RawExpression {
        final String expr;
        RawExpression(String expr) { this.expr = expr; }
        @Override public String toString() { return expr; }
    }

    // ================================================================
    //  Dry-run & Snapshot
    // ================================================================

    /**
     * 预演执行：提交阶段仅预览，不得修改业务数据。
     * SELECT 直接查询返回行；DML 在事务内执行后立即回滚，只返回影响行数。
     * 真正落库只允许发生在 approve 之后的 executeExecution 阶段。
     */
    private Map<String, Object> executeDryRun(Long conceptId, String sql, String actionType) {
        DbQuery dbQuery = null;
        Connection con = null;
        try {
            dbQuery = getDbQuery(conceptId);
            if ("SELECT".equals(actionType)) {
                List<Map<String, Object>> rows = dbQuery.queryList(sql);
                return Collections.singletonMap("rows", rows);
            }
            // DML 在事务内执行后回滚，仅预览影响行数，业务库纹丝不动
            con = dbQuery.getConnection();
            con.setAutoCommit(false);
            int affected;
            try (Statement st = con.createStatement()) {
                affected = st.executeUpdate(sql);
            }
            con.rollback();
            return Collections.singletonMap("affectedRows", affected);
        } catch (Exception e) {
            log.warn("dry-run 异常: {}", e.getMessage());
            if (con != null) {
                try { con.rollback(); } catch (Exception rbEx) { log.debug("dry-run 回滚失败", rbEx); }
            }
            return Collections.singletonMap("error", e.getMessage());
        } finally {
            if (con != null) {
                try { con.close(); } catch (Exception cEx) { log.debug("dry-run 连接关闭失败", cEx); }
            }
            closeDbQuery(dbQuery);
        }
    }

    private String captureSnapshot(Long conceptId, String sql) {
        DbQuery dbQuery = null;
        try {
            dbQuery = getDbQuery(conceptId);
            String selectSql = convertToSelect(sql);
            if (selectSql != null) {
                List<Map<String, Object>> rows = dbQuery.queryList(selectSql);
                return toJson(rows);
            }
        } catch (Exception e) {
            log.warn("快照获取失败: {}", e.getMessage());
        } finally {
            closeDbQuery(dbQuery);
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

    /**
     * 捕获执行后的数据快照（可溯源回退）：
     * UPDATE：复用 convertToSelect 重查变更后的新值；
     * DELETE：convertToSelect 重查为空集（行已删，beforeData 保留用于回退=按 beforeData 重 INSERT）；
     * CREATE：INSERT 无 WHERE 可转换，按主键参数构造 SELECT 回查新插入行。
     */
    private String captureAfterData(ActionDO action, String sql, Map<String, Object> params) {
        DbQuery dbQuery = null;
        try {
            dbQuery = getDbQuery(action.getConceptId());
            String selectSql = convertToSelect(sql);
            if (selectSql == null && "CREATE".equals(action.getActionType())) {
                selectSql = buildSelectByPk(action, params);
            }
            if (selectSql != null) {
                List<Map<String, Object>> rows = dbQuery.queryList(selectSql);
                return toJson(rows);
            }
        } catch (Exception e) {
            log.warn("执行后快照获取失败: {}", e.getMessage());
        } finally {
            closeDbQuery(dbQuery);
        }
        return null;
    }

    /**
     * CREATE 动作执行后按主键参数回查新插入行。
     */
    private String buildSelectByPk(ActionDO action, Map<String, Object> params) {
        Long conceptId = action.getConceptId();
        List<ConceptTableDO> tables = conceptTableMapper.selectByConceptId(conceptId);
        if (tables.isEmpty()) {
            return null;
        }
        ConceptTableDO table = tables.get(0);
        List<ColumnMapping> mappings = resolveColumnMappings(conceptId, table.getId());
        List<String> whereParts = new ArrayList<>();
        for (ColumnMapping m : mappings) {
            if (m.primaryKey && params.containsKey(m.semanticName)) {
                whereParts.add(m.physicalColumn + " = " + sqlValue(params.get(m.semanticName)));
            }
        }
        if (whereParts.isEmpty()) {
            return null;
        }
        return "SELECT * FROM " + table.getTableName() + " WHERE " + String.join(" AND ", whereParts);
    }

    /**
     * 动作血缘写入（可插拔，静默降级）：
     * LINEAGE_ENABLED=false 时 lineageDataService 为 null 直接跳过；
     * 写入失败仅记 warn，绝不影响动作执行主流程。
     */
    private void writeActionLineageSilently(ActionExecutionDO exec, ActionDO action) {
        if (lineageDataService == null) {
            return;
        }
        try {
            List<ConceptTableDO> tables = conceptTableMapper.selectByConceptId(action.getConceptId());
            if (tables.isEmpty()) {
                return;
            }
            ConceptTableDO table = tables.get(0);
            String hostPort = null;
            if (table.getDatasourceId() != null) {
                DatasourceRespDTO ds = datasourceApiService.getDatasourceById(table.getDatasourceId());
                if (ds != null) {
                    hostPort = ds.getIp() + ":" + ds.getPort();
                }
            }
            ActionExecutionNode node = ActionExecutionNode.builder()
                    .executionId(exec.getId())
                    .actionId(action.getId())
                    .actionName(action.getName())
                    .actionType(action.getActionType())
                    .tableName(table.getTableName())
                    .datasourceHostPort(hostPort)
                    .status(exec.getStatus())
                    .executeTime(exec.getExecuteTime())
                    .build();
            lineageDataService.saveActionExecution(node);
        } catch (Exception e) {
            log.warn("动作血缘写入失败，不影响执行结果: {}", e.getMessage());
        }
    }

    // ================================================================
    //  Utilities
    // ================================================================

    /**
     * 按概念解析其绑定物理表的数据源并创建 DbQuery。
     * 修复：原实现硬编码平台主库连接且忽略入参，导致所有动作都打到错误数据库。
     * 现依据 ONT_CONCEPT_TABLE.DATASOURCE_ID 经 IDatasourceApiService 解析真实数据源，
     * 由 DataSourceFactory 按方言构建连接（支持 MySQL/Oracle/PG/达梦等多种类型）。
     */
    private DbQuery getDbQuery(Long conceptId) {
        List<ConceptTableDO> tables = conceptTableMapper.selectByConceptId(conceptId);
        if (tables.isEmpty()) {
            throw new RuntimeException("概念未绑定物理表: conceptId=" + conceptId);
        }
        Long datasourceId = tables.get(0).getDatasourceId();
        if (datasourceId == null) {
            throw new RuntimeException("概念绑定的物理表缺少数据源配置: conceptId=" + conceptId);
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

    /**
     * 动作提交/执行前的权限校验（复用资产治理跨模块 API，与 AI 问数同一链路）。
     * 依据概念绑定物理表的 DATASOURCE_ID + TABLE_NAME 定位注册资产，
     * 由 checkTableAccess 完成表级（AST_ASSET_SPACE_REL，含审批放行回退）与
     * 字段级（AST_ASSET_COLUMN_SPACE_REL 允许列）校验。
     * 增删改（CREATE/UPDATE/DELETE）额外传入涉及列，由治理侧按写操作严格模式校验：
     * 任一涉及列未授权即整体拒绝；查询（SELECT）保持原有表级校验，不传列。
     * 空间上下文 spaceId/spaceCode 由前端拦截器自动注入请求（执行阶段复用提交快照）；
     * 二者均缺失时沿用平台既有语义：跳过空间权限校验。
     */
    private void assertTableAccess(ActionDO action, Long spaceId, String spaceCode,
                                   List<String> columns, String actionType) {
        List<ConceptTableDO> tables = conceptTableMapper.selectByConceptId(action.getConceptId());
        if (tables.isEmpty()) {
            return; // 概念未绑定物理表，后续 generateSql 会给出明确报错
        }
        ConceptTableDO table = tables.get(0);
        if (table.getDatasourceId() == null || table.getTableName() == null) {
            return;
        }
        AssetsTableGovernanceReqDTO reqDTO = new AssetsTableGovernanceReqDTO();
        reqDTO.setDatasourceId(table.getDatasourceId());
        reqDTO.setTableName(table.getTableName());
        reqDTO.setSpaceId(spaceId);
        reqDTO.setSpaceCode(spaceCode);
        reqDTO.setEntrance(entranceOf(actionType));
        if (columns != null && !columns.isEmpty()) {
            reqDTO.setColumnNames(columns);
        }
        tableGovernanceApiService.checkTableAccess(reqDTO);
    }

    /**
     * 提取增删改动作涉及的物理列（与 SQL 生成逻辑一一对应）：
     * - CREATE：入参覆盖的映射列 → INSERT 目标列
     * - UPDATE：入参覆盖的映射列 → SET 列 + 主键 WHERE 列
     * - DELETE：入参覆盖的映射列 → WHERE 条件列
     * 查询（SELECT）返回空列表，维持表级校验语义。
     */
    private List<String> extractActionColumns(ActionDO action, Map<String, Object> params) {
        String actionType = action.getActionType();
        if (!"CREATE".equals(actionType) && !"UPDATE".equals(actionType) && !"DELETE".equals(actionType)) {
            return Collections.emptyList();
        }
        List<ConceptTableDO> tables = conceptTableMapper.selectByConceptId(action.getConceptId());
        if (tables.isEmpty()) {
            return Collections.emptyList();
        }
        ConceptTableDO table = tables.get(0);
        List<ColumnMapping> mappings = resolveColumnMappings(action.getConceptId(), table.getId());
        List<String> columns = new ArrayList<>();
        for (ColumnMapping m : mappings) {
            if (params.containsKey(m.semanticName)) {
                columns.add(m.physicalColumn);
            }
        }
        return columns;
    }

    /** 动作类型 → 治理 API 入口标识（写操作走严格模式，查询维持原 entrance） */
    private String entranceOf(String actionType) {
        switch (actionType) {
            case "CREATE": return ENTRANCE_ONTOLOGY_CREATE;
            case "UPDATE": return ENTRANCE_ONTOLOGY_UPDATE;
            case "DELETE": return ENTRANCE_ONTOLOGY_DELETE;
            default: return ENTRANCE_ONTOLOGY_ACTION;
        }
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

    /**
     * 函数类型动作：把 param_config 中「字段映射型入参」（kind=field）解析并注入到执行参数。
     * 每条映射 {paramName, sourcePropertyCode} 使 input[paramName] = 数据来源概念属性 code，
     * 脚本通过入参名动态引用要处理的字段；固定值入参（kind=value/缺省）保持用户输入值。
     * 返回合并后的 inputParams JSON 字符串。
     */
    private String applyFunctionParamMapping(ActionDO action, Map<String, Object> params) {
        if (params == null) {
            params = new HashMap<>();
        }
        if (action.getParamConfig() == null || action.getParamConfig().isEmpty()) {
            return toJson(params);
        }
        try {
            List<Map<String, Object>> cfg = objectMapper.readValue(action.getParamConfig(),
                    new com.fasterxml.jackson.core.type.TypeReference<List<Map<String, Object>>>() {});
            for (Map<String, Object> entry : cfg) {
                String kind = entry.get("kind") == null ? "" : String.valueOf(entry.get("kind"));
                if (!"field".equals(kind)) {
                    continue; // 仅处理字段映射型，固定值型保留用户输入
                }
                Object paramName = entry.get("paramName");
                Object sourcePropertyCode = entry.get("sourcePropertyCode");
                if (paramName == null || sourcePropertyCode == null) {
                    continue;
                }
                params.put(String.valueOf(paramName), String.valueOf(sourcePropertyCode));
            }
        } catch (Exception e) {
            log.warn("解析函数动作 param_config 失败：{}", action.getParamConfig(), e);
        }
        return toJson(params);
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
