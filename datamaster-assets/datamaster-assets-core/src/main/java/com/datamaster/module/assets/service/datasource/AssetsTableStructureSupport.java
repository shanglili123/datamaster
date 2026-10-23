package com.datamaster.module.assets.service.datasource;

import com.datamaster.common.database.DbQuery;
import com.datamaster.common.database.constants.DbQueryProperty;
import com.datamaster.common.database.core.DbColumn;
import com.datamaster.common.database.core.DbTable;
import com.datamaster.common.exception.ServiceException;
import com.datamaster.common.utils.StringUtils;
import com.datamaster.module.assets.controller.admin.asset.vo.AssetsAssetSaveReqVO;
import com.datamaster.module.assets.controller.admin.assetColumn.vo.AssetsAssetColumnSaveReqVO;

import java.math.BigInteger;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/** 只做保守的结构证明，不修改外部表，也不推断未知类型等价。 */
public final class AssetsTableStructureSupport {
    private static final Pattern IDENTIFIER = Pattern.compile("[A-Za-z][A-Za-z0-9_]*");
    private static final Pattern TYPE = Pattern.compile("(?i)(?:VARCHAR2?|NVARCHAR2?|CHAR|NCHAR|TEXT|TINYTEXT|MEDIUMTEXT|LONGTEXT|STRING|INT|INTEGER|SMALLINT|TINYINT|BIGINT|NUMBER|NUMERIC|DECIMAL|FLOAT|REAL|DOUBLE(?: PRECISION)?|DATE|DATETIME|TIMESTAMP(?: WITHOUT TIME ZONE| WITH TIME ZONE)?|TIME|YEAR|BOOL|BOOLEAN|BINARY|VARBINARY|BLOB|CLOB|RAW|BYTEA|JSON|JSONB|CHARACTER VARYING)");
    private static final Pattern COLUMN_DECLARATION = Pattern.compile(
            "(?is)^\\s*(?:`[^`]+`|\"[^\"]+\"|\\[[^]]+]|[A-Za-z][A-Za-z0-9_]*)\\s+([A-Za-z][A-Za-z0-9_]*(?:\\s+(?:VARYING|PRECISION|WITHOUT TIME ZONE|WITH TIME ZONE))?)(?:\\s*\\(\\s*(\\d+)(?:\\s*,\\s*(\\d+))?\\s*\\))?.*$");

    private AssetsTableStructureSupport() { }

    public static void validateIdentifier(String value, String label) {
        if (value == null || value.length() > 128 || !IDENTIFIER.matcher(value).matches()) {
            throw new ServiceException(label + "只能包含字母、数字和下划线，且必须以字母开头");
        }
    }

    public static void validateColumns(List<DbColumn> columns) {
        if (columns == null || columns.isEmpty()) { throw new ServiceException("建表字段不能为空"); }
        Set<String> names = new HashSet<>();
        for (DbColumn column : columns) {
            if (column == null) { throw new ServiceException("字段不能为空"); }
            validateIdentifier(column.getColName(), "字段名");
            if (!names.add(column.getColName().toLowerCase(Locale.ROOT))) { throw new ServiceException("字段名重复：" + column.getColName()); }
            if (column.getDataType() == null || !TYPE.matcher(column.getDataType()).matches()) {
                throw new ServiceException("字段类型必须是单独的类型名，不得包含SQL片段");
            }
            validateNumber(column.getDataLength(), "字段长度", true);
            validateNumber(column.getDataScale(), "字段小数位", false);
            if (StringUtils.isNotBlank(column.getDataLength()) && StringUtils.isNotBlank(column.getDataScale())
                    && new BigInteger(column.getDataScale()).compareTo(new BigInteger(column.getDataLength())) > 0) {
                throw new ServiceException("字段小数位不能超过长度");
            }
            if (column.getColKey() == null || column.getNullable() == null) { throw new ServiceException("字段主键及可空标志不能为空"); }
            // 现有方言把默认值直接嵌入SQL，只允许无表达式的安全字面量。
            String value = column.getDataDefault();
            if (StringUtils.isNotBlank(value) && !value.matches("[-+]?[0-9]+(?:\\.[0-9]+)?|[A-Za-z0-9_]+|'[A-Za-z0-9_ .:/+-]*'")) {
                throw new ServiceException("默认值包含不支持的表达式，请使用安全字面量");
            }
        }
    }

    private static void validateNumber(String value, String label, boolean positive) {
        if (StringUtils.isBlank(value)) { return; }
        if (!value.matches("[0-9]{1,9}") || (positive && new BigInteger(value).signum() == 0)) { throw new ServiceException(label + "不合法"); }
    }

    /** 平台方言生成的表标识符不加引号；只处理已确认的折叠规则。 */
    public static String physicalIdentifier(String name, String datasourceType) {
        if (Arrays.asList("Oracle", "Oracle11", "DM8", "DB2", "OSCAR").contains(datasourceType)) { return name.toUpperCase(Locale.ROOT); }
        if (Arrays.asList("PostgreSQL", "Kingbase8").contains(datasourceType)) { return name.toLowerCase(Locale.ROOT); }
        return name;
    }

    /** 返回元数据中的真实名称；大小写同名多表不能猜测。 */
    public static String findPhysicalTable(DbQuery query, DbQueryProperty property, String tableName) {
        List<DbTable> tables = query.getTables(property);
        if (tables == null) { throw new ServiceException("无法读取实际表清单"); }
        List<String> matches = tables.stream().map(DbTable::getTableName).filter(Objects::nonNull)
                .filter(name -> name.equalsIgnoreCase(tableName)).collect(Collectors.toList());
        if (matches.size() > 1) { throw new ServiceException("存在多个仅大小写不同的同名表，禁止自动选择"); }
        return matches.isEmpty() ? null : matches.get(0);
    }

    public static List<DbColumn> proveStructure(DbQuery query, DbQueryProperty property, String tableName, List<DbColumn> expected) {
        List<DbColumn> actual = query.getTableColumns(property, tableName);
        if (actual == null || actual.isEmpty() || actual.size() != expected.size()) { throw new ServiceException("已有表字段数量不匹配或无法读取实际结构"); }
        Map<String, DbColumn> actualByName = new HashMap<>();
        for (DbColumn column : actual) {
            if (column == null || column.getColName() == null || actualByName.put(column.getColName(), column) != null) { throw new ServiceException("实际字段信息不完整或不唯一"); }
        }
        for (DbColumn column : expected) {
            // 用目标方言生成单列DDL，读取真正的目标类型，而不是拿模型的源方言类型粗比。
            List<String> ddl = query.generateCreateTableSQL(property, tableName, "", Collections.singletonList(column));
            if (ddl == null || ddl.isEmpty()) { throw new ServiceException("无法证明目标方言字段类型"); }
            String create = ddl.get(0);
            int start = create.indexOf('(');
            if (start < 0) { throw new ServiceException("目标方言未返回可核对的建表声明"); }
            String declaration = create.substring(start + 1).trim();
            Matcher matcher = COLUMN_DECLARATION.matcher(declaration);
            if (!matcher.matches()) { throw new ServiceException("无法解析目标字段声明，需人工核对"); }
            String expectedName = physicalIdentifier(column.getColName(), property.getDbType());
            if (declaration.startsWith("\"") || declaration.startsWith("`") || declaration.startsWith("[")) { expectedName = column.getColName(); }
            DbColumn found = actualByName.get(expectedName);
            if (found == null) { throw new ServiceException("实际字段名称或大小写不匹配：" + column.getColName()); }
            String targetType = canonicalType(matcher.group(1));
            if (!targetType.equals(canonicalType(found.getDataType()))) { throw new ServiceException("实际字段目标类型不匹配：" + found.getColName()); }
            if (Arrays.asList("VARCHAR", "NVARCHAR", "CHAR", "NCHAR", "BINARY", "VARBINARY", "NUMBER").contains(targetType)) {
                String length = "NUMBER".equals(targetType) && StringUtils.isNotBlank(found.getDataPrecision()) ? found.getDataPrecision() : found.getDataLength();
                if (matcher.group(2) == null || !sameNumber(matcher.group(2), length)) { throw new ServiceException("实际字段长度/精度无法证明匹配：" + found.getColName()); }
                if ("NUMBER".equals(targetType) && !sameNumber(matcher.group(3) == null ? "0" : matcher.group(3), found.getDataScale())) { throw new ServiceException("实际字段小数位不匹配：" + found.getColName()); }
            } else if (!Arrays.asList("INT", "SMALLINT", "TINYINT", "BIGINT", "DATE", "DATETIME", "TIMESTAMP", "TIME", "TEXT", "CLOB", "BLOB", "FLOAT", "DOUBLE", "BOOLEAN").contains(targetType)) {
                throw new ServiceException("该字段类型暂不能安全证明结构匹配：" + targetType);
            }
            if (matcher.group(2) != null && !Arrays.asList("VARCHAR", "NVARCHAR", "CHAR", "NCHAR", "BINARY", "VARBINARY", "NUMBER").contains(targetType)) {
                throw new ServiceException("该类型参数暂不能安全证明匹配：" + found.getColName());
            }
            boolean expectedNullable = !Boolean.TRUE.equals(column.getColKey()) && Boolean.TRUE.equals(column.getNullable());
            if (found.getNullable() == null || found.getColKey() == null || found.getNullable() != expectedNullable
                    || !Objects.equals(found.getColKey(), column.getColKey())) { throw new ServiceException("实际主键/可空约束不匹配：" + found.getColName()); }
            if (!Objects.equals(normalizeDefault(column.getDataDefault()), normalizeDefault(found.getDataDefault()))) { throw new ServiceException("实际字段默认值无法证明匹配：" + found.getColName()); }
        }
        return actual;
    }

    private static boolean sameNumber(String first, String second) {
        try { return first != null && second != null && new BigInteger(first).equals(new BigInteger(second)); }
        catch (NumberFormatException ex) { return false; }
    }

    private static String canonicalType(String value) {
        if (value == null) { return ""; }
        String type = value.trim().toUpperCase(Locale.ROOT).replaceAll("\\s+", " ");
        switch (type) {
            case "VARCHAR2": case "CHARACTER VARYING": return "VARCHAR";
            case "NVARCHAR2": case "NATIONAL CHARACTER VARYING": return "NVARCHAR";
            case "CHARACTER": return "CHAR";
            case "INTEGER": case "INT4": return "INT";
            case "INT2": return "SMALLINT";
            case "INT8": return "BIGINT";
            case "DECIMAL": case "NUMERIC": return "NUMBER";
            case "DOUBLE PRECISION": return "DOUBLE";
            case "BOOL": return "BOOLEAN";
            case "TIMESTAMP WITHOUT TIME ZONE": return "TIMESTAMP";
            default: return type;
        }
    }

    private static String normalizeDefault(String value) { return StringUtils.isBlank(value) ? null : value.trim(); }

    public static void setAssetColumns(AssetsAssetSaveReqVO asset, List<DbColumn> columns) {
        List<AssetsAssetColumnSaveReqVO> result = new ArrayList<>();
        for (DbColumn column : columns) {
            AssetsAssetColumnSaveReqVO item = new AssetsAssetColumnSaveReqVO();
            item.setColumnName(column.getColName());
            item.setColumnType(column.getDataType());
            item.setColumnLength(toLong(column.getDataLength()));
            item.setColumnScale(toLong(column.getDataScale()));
            item.setColumnComment(column.getColComment());
            item.setDefaultValue(column.getDataDefault());
            item.setPkFlag(Boolean.TRUE.equals(column.getColKey()) ? "1" : "0");
            item.setNullableFlag(Boolean.TRUE.equals(column.getNullable()) ? "0" : "1");
            result.add(item);
        }
        asset.setAssetColumnList(result);
    }

    private static Long toLong(String value) {
        if (StringUtils.isBlank(value)) { return null; }
        try { return Long.valueOf(value); } catch (NumberFormatException ex) { return null; }
    }
}
