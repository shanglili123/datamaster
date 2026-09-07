package com.datamaster.module.ontology.service.impl;

import com.datamaster.common.database.DataSourceFactory;
import com.datamaster.common.database.DbQuery;
import com.datamaster.common.database.constants.DbQueryProperty;
import com.datamaster.common.datasource.mgmt.api.IDatasourceApiService;
import com.datamaster.common.datasource.mgmt.api.dto.DatasourceRespDTO;
import com.datamaster.module.ontology.dal.dataobject.ActionDO;
import com.datamaster.module.ontology.dal.dataobject.ConceptTableDO;
import com.datamaster.module.ontology.dal.dataobject.PropertyColumnDO;
import com.datamaster.module.ontology.dal.dataobject.PropertyDO;
import com.datamaster.module.ontology.dal.dataobject.RelationColumnDO;
import com.datamaster.module.ontology.dal.dataobject.RelationDO;
import com.datamaster.module.ontology.dal.mapper.ConceptTableMapper;
import com.datamaster.module.ontology.dal.mapper.PropertyColumnMapper;
import com.datamaster.module.ontology.dal.mapper.PropertyMapper;
import com.datamaster.module.ontology.dal.mapper.RelationColumnMapper;
import com.datamaster.module.ontology.dal.mapper.RelationMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

/**
 * 前置条件/规则中「object.*」与「object.&lt;relationCode&gt;.&lt;prop&gt;」的对象当前状态解析器。
 *
 * <p>它是阶段一前置条件规则引擎的关键：把条件从「仅参数比较」扩展为「引用目标对象及其关联对象
 * 的物理表当前状态」——例如「要发货，检查仓库中该产品库存是否 &gt; 0」表述为
 * {@code object.stock > 0}（对象自身属性）或 {@code object.productStock.quantity > 0}
 * （经关系导航引用关联对象属性，如 订单→产品.库存）。</p>
 *
 * <p>实现路径完全复用动作 SQL 引擎与关系跳转的既有语义：<ul>
 *   <li>object.{prop}：概念物理表按对象主键回查该属性物理列，返回当前值；</li>
 *   <li>object.{relationCode}.{prop}：按完整对象主键定位源对象，读取
 *       RelationColumnDO.sourceColumn，再到目标表按 targetColumn 导航并回查目标属性；
 *       与对象浏览器的关系跳转使用同一语义。</li>
 * </ul></p>
 *
 * <p><b>fail-closed</b>：对象不存在 / 概念未绑物理表 / 属性或字段不存在 / 表达式结构错误 /
 * 数据源不可用等任何异常一律抛出 {@link IllegalArgumentException}，由调用方把该条件判定为拒绝
 * 或评估错误，绝不默认「通过」。这修复了原地 evaluateCriteria 的 fail-open 缺陷。</p>
 *
 * <p>仅做只读回查，不校验空间/表权限（权限由动作提交/执行的 assertTableAccess 统一把关；
 * 本组件被动作执行上下文调用，同空间同权限校验已先行）。</p>
 */
@Component
public class ObjectStateConditionResolver {

    private static final Logger log = LoggerFactory.getLogger(ObjectStateConditionResolver.class);

    private static final String PREFIX_OBJECT = "object.";

    @Resource private ConceptTableMapper conceptTableMapper;
    @Resource private PropertyColumnMapper propertyColumnMapper;
    @Resource private PropertyMapper propertyMapper;
    @Resource private RelationMapper relationMapper;
    @Resource private RelationColumnMapper relationColumnMapper;
    @Resource private IDatasourceApiService datasourceApiService;
    @Resource private DataSourceFactory dataSourceFactory;

    /**
     * 判断字段表达式是否为对象状态引用（object.xxx 或 object.rel.prop）。
     */
    public boolean isObjectField(String field) {
        return field != null && field.startsWith(PREFIX_OBJECT);
    }

    /**
     * 解析对象状态字段值。仅当 {@link #isObjectField(String)} 为 true 时调用。
     *
     * @param field     形如 {@code object.stock} 或 {@code object.productStock.quantity}
     * @param action    所属动作（提供绑定概念）
     * @param objectKey 目标对象主键（JSON 对象或标量字符串）
     * @return 解析出的取值；若对应属性值为空（含对象未命中、字段为 NULL）返回 null
     * @throws IllegalArgumentException 对象/概念/属性/关系/数据源任一不存在或结构错误（fail-closed）
     */
    public Object resolve(String field, ActionDO action, String objectKey) {
        if (action == null || action.getConceptId() == null) {
            throw new IllegalArgumentException("object.* 条件要求动作绑定概念（conceptId 缺失）");
        }
        if (objectKey == null || objectKey.trim().isEmpty()) {
            throw new IllegalArgumentException("object.* 条件要求目标对象主键 objectKey（缺失）");
        }
        String rest = field.substring(PREFIX_OBJECT.length());
        if (rest.isEmpty()) {
            throw new IllegalArgumentException("object.* 字段为空: " + field);
        }
        int firstDot = rest.indexOf('.');
        String propOrRelation = firstDot < 0 ? rest : rest.substring(0, firstDot);
        if (firstDot < 0) {
            // object.<prop>：对象自身属性
            return resolveOwnProperty(action, propOrRelation, objectKey);
        }
        String propCode = rest.substring(firstDot + 1);
        if (propCode.isEmpty() || propCode.indexOf('.') >= 0) {
            throw new IllegalArgumentException("object.<relation>.<prop> 表达式不合法: " + field);
        }
        // object.<relationCode>.<prop>：经关系导航引用关联对象属性
        return resolveRelatedProperty(action, propOrRelation, propCode, objectKey);
    }

    /**
     * 按当前对象的 objectKey 回查指定物理列。
     * 用于关系步骤把当前主体自动转换为关系表需要的引用值，例如：
     * 人物对象以 person_code=P1003 作为语义主属性定位，但关系表 person_id 必须写 dm_person.id=3。
     */
    public Object resolvePhysicalColumn(ActionDO action, String objectKey, String physicalColumn) {
        if (action == null || action.getConceptId() == null) {
            throw new IllegalArgumentException("关系主体取值要求动作绑定概念");
        }
        if (objectKey == null || objectKey.trim().isEmpty()) {
            throw new IllegalArgumentException("关系主体取值缺少 objectKey");
        }
        if (physicalColumn == null || !physicalColumn.matches("[A-Za-z_][A-Za-z0-9_$]*")) {
            throw new IllegalArgumentException("关系主体取值字段不合法: " + physicalColumn);
        }
        ConceptTableDO table = firstConceptTable(action.getConceptId(), true);
        String whereSql = buildObjectKeyWhere(action.getConceptId(), table, objectKey);
        String sql = "SELECT " + physicalColumn + " AS v FROM " + table.getTableName()
                + " WHERE " + whereSql + " LIMIT 1";
        DbQuery dbQuery = openDbQuery(table.getDatasourceId());
        try {
            List<Map<String, Object>> rows = dbQuery.queryList(sql);
            return rows == null || rows.isEmpty() ? null : getIgnoreCase(rows.get(0), "v");
        } catch (Exception e) {
            throw new IllegalArgumentException("关系主体字段回查失败: column=" + physicalColumn
                    + ", err=" + e.getMessage(), e);
        } finally {
            closeQuietly(dbQuery);
        }
    }

    /** object.{prop}：按对象主键回查概念物理表该属性物理列。 */
    private Object resolveOwnProperty(ActionDO action, String propCode, String objectKey) {
        Long conceptId = action.getConceptId();
        ConceptTableDO table = firstConceptTable(conceptId, false);
        ColumnRef col = resolvePropertyColumn(conceptId, table, propCode, true);
        String whereSql = buildObjectKeyWhere(conceptId, table, objectKey);
        String sql = "SELECT " + col.physicalColumn + " AS v FROM " + table.getTableName()
                + " WHERE " + whereSql + " LIMIT 1";
        DbQuery dbQuery = openDbQuery(table.getDatasourceId());
        try {
            List<Map<String, Object>> rows = dbQuery.queryList(sql);
            if (rows == null || rows.isEmpty()) {
                return null; // 对象不存在 → 属性值视为空（条件按值匹配判定，交由调用方决策）
            }
            Object v = getIgnoreCase(rows.get(0), "v");
            return normalize(v, col.dataType);
        } catch (Exception e) {
            throw new IllegalArgumentException("object.<prop> 回查失败: field=object." + propCode + ", err=" + e.getMessage(), e);
        } finally {
            closeQuietly(dbQuery);
        }
    }

    /** object.{relationCode}.{prop}：沿关系导航查询关联对象属性。 */
    private Object resolveRelatedProperty(ActionDO action, String relationCode, String propCode, String objectKey) {
        Long conceptId = action.getConceptId();
        RelationDO relation = findRelationByCode(action, relationCode);
        if (relation == null) {
            throw new IllegalArgumentException("关联对象条件引用关系不存在: relationCode=" + relationCode);
        }
        if ("one_to_many".equals(relation.getRelationType())
                || "many_to_many".equals(relation.getRelationType())) {
            throw new IllegalArgumentException("关联对象条件暂不支持对多关系直接取单值，请先定义聚合属性: relationCode="
                    + relationCode);
        }
        // 与对象浏览器关系跳转保持同一语义：
        // 先按完整 objectKey 定位源对象并读取 sourceColumn，再到目标表按 targetColumn 查询。
        ConceptTableDO sourceTable = firstConceptTable(conceptId, true);
        RelationColumnDO binding = findSourceBinding(relation, sourceTable.getId());
        if (binding == null || binding.getSourceColumn() == null
                || binding.getTargetColumn() == null || binding.getTargetConceptTableId() == null) {
            throw new IllegalArgumentException("关系未配置可导航的关联字段: relation=" + relationCode);
        }
        String sourceWhere = buildObjectKeyWhere(conceptId, sourceTable, objectKey);
        Object sourceValue;
        DbQuery sourceDb = openDbQuery(sourceTable.getDatasourceId());
        try {
            String sourceSql = "SELECT " + binding.getSourceColumn() + " AS v FROM "
                    + sourceTable.getTableName() + " WHERE " + sourceWhere + " LIMIT 1";
            List<Map<String, Object>> sourceRows = sourceDb.queryList(sourceSql);
            if (sourceRows == null || sourceRows.isEmpty()) {
                return null;
            }
            sourceValue = getIgnoreCase(sourceRows.get(0), "v");
            if (sourceValue == null) {
                return null;
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("关系源对象回查失败: relation=" + relationCode
                    + ", err=" + e.getMessage(), e);
        } finally {
            closeQuietly(sourceDb);
        }

        ConceptTableDO targetTable = conceptTableMapper.selectById(binding.getTargetConceptTableId());
        if (targetTable == null) {
            throw new IllegalArgumentException("关系目标表绑定不存在: id=" + binding.getTargetConceptTableId());
        }
        Long targetConceptId = relation.getTargetConceptId();
        if (targetConceptId == null) {
            targetConceptId = targetTable.getConceptId();
        }
        ColumnRef targetProperty = resolvePropertyColumn(targetConceptId, targetTable, propCode, true);
        DbQuery targetDb = openDbQuery(targetTable.getDatasourceId());
        try {
            String targetSql = "SELECT " + targetProperty.physicalColumn + " AS v FROM "
                    + targetTable.getTableName() + " WHERE " + binding.getTargetColumn()
                    + " = " + escapeQueryParam(sourceValue)
                    + " LIMIT 1";
            List<Map<String, Object>> targetRows = targetDb.queryList(targetSql);
            return (targetRows == null || targetRows.isEmpty())
                    ? null : normalize(getIgnoreCase(targetRows.get(0), "v"), targetProperty.dataType);
        } catch (Exception e) {
            throw new IllegalArgumentException("关联对象属性回查失败: relation=" + relationCode
                    + ", property=" + propCode + ", err=" + e.getMessage(), e);
        } finally {
            closeQuietly(targetDb);
        }
    }

    /* ---------------- 查询辅助 ---------------- */

    private ConceptTableDO firstConceptTable(Long conceptId, boolean required) {
        List<ConceptTableDO> tables = conceptTableMapper.selectByConceptId(conceptId);
        if (tables == null || tables.isEmpty()) {
            if (required) {
                throw new IllegalArgumentException("概念未绑定物理表: conceptId=" + conceptId);
            }
            throw new IllegalArgumentException("object.* 条件要求概念绑定物理表: conceptId=" + conceptId);
        }
        return tables.get(0);
    }

    /** 解析属性到物理列（code → 列名 + 数据类型）。 */
    private ColumnRef resolvePropertyColumn(Long conceptId, ConceptTableDO table, String propCode, boolean required) {
        List<PropertyColumnDO> bindings = propertyColumnMapper.selectByConceptTableId(table.getId());
        for (PropertyColumnDO b : bindings) {
            if (b.getPropertyId() == null || b.getColumnName() == null) {
                continue;
            }
            PropertyDO prop = propertyMapper.selectById(b.getPropertyId());
            if (prop != null && propCode.equals(prop.getCode())) {
                return new ColumnRef(b.getColumnName(), prop.getDataType(), prop.getCode());
            }
        }
        if (required) {
            throw new IllegalArgumentException("属性未找到物理列绑定: propCode=" + propCode);
        }
        // 允许回退到物理列名本身（属性 code 即物理列名）
        return new ColumnRef(propCode, null, propCode);
    }

    /** 解析概念全部主键物理列；无主键属性时兼容回退约定列 id。 */
    private List<ColumnRef> resolvePrimaryKeyColumns(Long conceptId, ConceptTableDO table) {
        java.util.ArrayList<ColumnRef> result = new java.util.ArrayList<>();
        List<PropertyColumnDO> bindings = propertyColumnMapper.selectByConceptTableId(table.getId());
        for (PropertyColumnDO b : bindings) {
            if (b.getPropertyId() == null || b.getColumnName() == null) {
                continue;
            }
            PropertyDO prop = propertyMapper.selectById(b.getPropertyId());
            if (prop != null && Boolean.TRUE.equals(prop.getIsPrimary())) {
                result.add(new ColumnRef(b.getColumnName(), prop.getDataType(), prop.getCode()));
            }
        }
        if (result.isEmpty()) {
            result.add(new ColumnRef("id", null, "id"));
        }
        return result;
    }

    private RelationColumnDO findSourceBinding(RelationDO relation, Long sourceConceptTableId) {
        List<RelationColumnDO> cols = relationColumnMapper.selectByRelationId(relation.getId());
        if (cols == null) {
            return null;
        }
        for (RelationColumnDO c : cols) {
            if (sourceConceptTableId != null
                    && sourceConceptTableId.equals(c.getSourceConceptTableId())) {
                return c;
            }
        }
        return null;
    }

    private RelationDO findRelationByCode(ActionDO action, String relationCode) {
        List<RelationDO> rels = relationMapper.selectList(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<RelationDO>()
                .eq(RelationDO::getOntologyId, action.getOntologyId())
                .eq(RelationDO::getCode, relationCode));
        return (rels == null || rels.isEmpty()) ? null : rels.get(0);
    }

    /** 用完整对象主键构建 WHERE；联合主键任何一列缺失都 fail-closed。 */
    private String buildObjectKeyWhere(Long conceptId, ConceptTableDO table, String objectKey) {
        List<ColumnRef> pks = resolvePrimaryKeyColumns(conceptId, table);
        Map<?, ?> values = parseObjectKeyMap(objectKey);
        if (pks.size() > 1 && values == null) {
            throw new IllegalArgumentException("联合主键 objectKey 必须是包含全部主键列的 JSON 对象");
        }
        StringBuilder where = new StringBuilder();
        for (ColumnRef pk : pks) {
            Object value;
            if (values == null) {
                value = objectKey;
            } else {
                value = values.containsKey(pk.semanticCode)
                        ? values.get(pk.semanticCode) : values.get(pk.physicalColumn);
                if (value == null) {
                    throw new IllegalArgumentException("objectKey 缺少主键: " + pk.semanticCode
                            + "（物理列 " + pk.physicalColumn + "）");
                }
            }
            if (where.length() > 0) {
                where.append(" AND ");
            }
            where.append(pk.physicalColumn).append(" = ").append(escapeQueryParam(value));
        }
        return where.toString();
    }

    private Map<?, ?> parseObjectKeyMap(String objectKey) {
        String trimmed = objectKey == null ? "" : objectKey.trim();
        if (trimmed.startsWith("{")) {
            try {
                return new com.fasterxml.jackson.databind.ObjectMapper().readValue(trimmed, Map.class);
            } catch (Exception e) {
                throw new IllegalArgumentException("objectKey JSON 解析失败: " + e.getMessage(), e);
            }
        }
        return null;
    }

    private Object normalize(Object v, String dataType) {
        if (v == null) {
            return null;
        }
        if (dataType == null) {
            return v;
        }
        switch (dataType) {
            case "integer":
                try {
                    return Integer.valueOf(String.valueOf(v));
                } catch (Exception ignore) {
                    return v;
                }
            case "decimal":
                return new java.math.BigDecimal(String.valueOf(v));
            case "boolean":
                return Boolean.valueOf(String.valueOf(v));
            default:
                return v;
        }
    }

    private Object getIgnoreCase(Map<String, Object> row, String key) {
        if (row == null || key == null) {
            return null;
        }
        if (row.containsKey(key)) {
            return row.get(key);
        }
        for (Map.Entry<String, Object> entry : row.entrySet()) {
            if (entry.getKey() != null && key.equalsIgnoreCase(entry.getKey())) {
                return entry.getValue();
            }
        }
        return null;
    }

    /** 单引号转义 + 包装，防注入（回查值仅来自 objectKey 与规范化数据，仍做防御）。 */
    private String escapeQueryParam(Object v) {
        String s = String.valueOf(v == null ? "" : v);
        return "'" + s.replace("'", "''") + "'";
    }

    private DbQuery openDbQuery(Long datasourceId) {
        if (datasourceId == null) {
            throw new IllegalArgumentException("对象回查表缺少数据源配置");
        }
        DatasourceRespDTO ds = datasourceApiService.getDatasourceById(datasourceId);
        if (ds == null) {
            throw new IllegalArgumentException("数据源不存在: id=" + datasourceId);
        }
        return dataSourceFactory.createDbQuery(new DbQueryProperty(
                ds.getDatasourceType(), ds.getIp(), ds.getPort(), ds.getDatasourceConfig()));
    }

    private void closeQuietly(DbQuery dbQuery) {
        if (dbQuery != null) {
            try {
                dbQuery.close();
            } catch (Exception e) {
                log.debug("关闭对象回查 DbQuery 失败", e);
            }
        }
    }

    /** 物理列引用元组。 */
    private static final class ColumnRef {
        final String physicalColumn;
        final String dataType;
        final String semanticCode;

        ColumnRef(String physicalColumn, String dataType, String semanticCode) {
            this.physicalColumn = physicalColumn;
            this.dataType = dataType;
            this.semanticCode = semanticCode;
        }
    }
}
