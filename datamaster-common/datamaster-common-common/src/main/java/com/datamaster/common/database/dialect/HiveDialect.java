package com.datamaster.common.database.dialect;

import com.datamaster.common.database.constants.DbQueryProperty;
import com.datamaster.common.database.core.DbColumn;
import com.datamaster.common.database.core.DbTable;
import com.datamaster.common.database.core.DbTableMetadata;
import com.datamaster.common.database.exception.DataQueryException;
import com.datamaster.common.database.utils.DatabaseUtil;
import com.datamaster.common.utils.StringUtils;
import org.springframework.jdbc.core.RowMapper;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Hive is handled as a JDBC source for FlinkX and connection metadata.
 */
public class HiveDialect extends MySqlDialect {

    @Override
    public RowMapper<DbTable> tableMapper() {
        return (ResultSet rs, int rowNum) -> {
            DbTable table = new DbTable();
            table.setTableName(rs.getString(1));
            table.setTableComment("");
            return table;
        };
    }

    @Override
    public RowMapper<DbColumn> columnMapper() {
        return (ResultSet rs, int rowNum) -> {
            String colName = rs.getString(1);
            if (colName == null || colName.trim().isEmpty() || colName.startsWith("#")) {
                return null;
            }
            DbColumn column = new DbColumn();
            column.setColName(colName);
            column.setDataType(rs.getString(2));
            column.setColComment(rs.getMetaData().getColumnCount() >= 3 ? rs.getString(3) : "");
            column.setColPosition(rowNum + 1);
            column.setColKey(false);
            column.setNullable(true);
            return column;
        };
    }

    @Override
    public String tables(DbQueryProperty dbQueryProperty) {
        return "SHOW TABLES IN `" + dbQueryProperty.getDbName() + "`";
    }

    @Override
    public String generateCheckTableExistsSQL(DbQueryProperty dbQueryProperty, String tableName) {
        return "SHOW TABLES IN `" + dbQueryProperty.getDbName() + "` LIKE '" + tableName + "'";
    }

    @Override
    public String columns(DbQueryProperty dbQueryProperty, String tableName) {
        return "DESCRIBE `" + dbQueryProperty.getDbName() + "`.`" + tableName + "`";
    }

    @Override
    public String tablesComment(DbQueryProperty dbQueryProperty, String tableName) {
        return "DESCRIBE FORMATTED `" + dbQueryProperty.getDbName() + "`.`" + tableName + "`";
    }

    @Override
    public String trainToJdbcUrl(DbQueryProperty property) {
        return "jdbc:hive2://" + property.getHost() + ":" + property.getPort() + "/" + property.getDbName();
    }

    @Override
    public List<String> someInternalSqlGenerator(DbQueryProperty dbQueryProperty, String tableName, String tableComment, List<DbColumn> dbColumnList) {
        List<String> sqlList = new ArrayList<>();
        StringBuilder sql = new StringBuilder();
        sql.append("CREATE TABLE ").append(qualifiedTableName(dbQueryProperty, tableName)).append(" (\n");
        for (DbColumn column : dbColumnList) {
            sql.append("  `").append(column.getColName()).append("` ");
            sql.append(hiveColumnType(column));
            if (StringUtils.isNotEmpty(column.getColComment())) {
                sql.append(" COMMENT '").append(DatabaseUtil.escapeSingleQuotes(column.getColComment())).append("'");
            }
            sql.append(",\n");
        }
        sql.setLength(sql.length() - 2);
        sql.append("\n)");
        if (StringUtils.isNotEmpty(tableComment)) {
            sql.append("\nCOMMENT '").append(DatabaseUtil.escapeSingleQuotes(tableComment)).append("'");
        }
        sql.append("\nSTORED AS PARQUET");
        sqlList.add(sql.toString());
        return sqlList;
    }

    private String qualifiedTableName(DbQueryProperty property, String tableName) {
        if (StringUtils.isNotEmpty(property.getDbName())) {
            return "`" + property.getDbName() + "`.`" + tableName + "`";
        }
        return "`" + tableName + "`";
    }

    private String hiveColumnType(DbColumn column) {
        String dataType = column.getDataType() == null ? "STRING" : column.getDataType().toUpperCase();
        switch (dataType) {
            case "VARCHAR":
            case "VARCHAR2":
                if (StringUtils.isNotEmpty(column.getDataLength())) {
                    return "VARCHAR(" + column.getDataLength() + ")";
                }
                return "STRING";
            case "CHAR":
                if (StringUtils.isNotEmpty(column.getDataLength())) {
                    return "CHAR(" + column.getDataLength() + ")";
                }
                return "STRING";
            case "TEXT":
            case "STRING":
            case "JSON":
                return "STRING";
            case "INT":
            case "INTEGER":
                return "INT";
            case "BIGINT":
                return "BIGINT";
            case "TINYINT":
                return "TINYINT";
            case "SMALLINT":
                return "SMALLINT";
            case "FLOAT":
                return "FLOAT";
            case "DOUBLE":
                return "DOUBLE";
            case "DECIMAL":
            case "NUMERIC":
            case "NUMBER":
                return "DECIMAL(" + valueOrDefault(column.getDataLength(), "18") + "," + valueOrDefault(column.getDataScale(), "2") + ")";
            case "DATE":
                return "DATE";
            case "DATETIME":
            case "TIMESTAMP":
                return "TIMESTAMP";
            case "BOOLEAN":
            case "BOOL":
                return "BOOLEAN";
            default:
                return dataType;
        }
    }

    private String valueOrDefault(String value, String defaultValue) {
        return StringUtils.isNotEmpty(value) ? value : defaultValue;
    }

    /**
     * 采集 Hive 表的元数据（行数、分区、存储、注释、创建时间、存储引擎等）。
     * <p>
     * 连接由 {@code conn} 提供，库名由 {@code dbQueryProperty.getDbName()} 提供。
     *
     * @param dbQueryProperty 数据源连接属性
     * @param tableName       表名
     * @param conn            已建立的数据库连接
     * @return 表元数据信息
     */
    @Override
    public DbTableMetadata tableMetadata(DbQueryProperty dbQueryProperty, String tableName, Connection conn) {
        DbTableMetadata metadata = new DbTableMetadata();
        try {
            // 获取表行数
            try (Statement stmt = conn.createStatement()) {
                String sql = "SELECT COUNT(*) FROM " + tableName;
                ResultSet rs = stmt.executeQuery(sql);
                if (rs.next()) {
                    metadata.setRowCount(rs.getLong(1));
                }
            }

            // 获取表分区字段信息
            try (Statement stmt = conn.createStatement()) {
                String sql = "SHOW PARTITIONS " + tableName;
                ResultSet rs = stmt.executeQuery(sql);
                if (rs.next()) {
                    String partition = rs.getString(1);
                    if (partition != null && partition.contains("=")) {
                        String[] parts = partition.split("/");
                        StringBuilder partitionFields = new StringBuilder();
                        for (String part : parts) {
                            if (part.contains("=")) {
                                String field = part.split("=")[0];
                                if (partitionFields.length() > 0) {
                                    partitionFields.append(", ");
                                }
                                partitionFields.append(field);
                            }
                        }
                        metadata.setPartitionFields(partitionFields.toString());
                    }
                }
            } catch (SQLException e) {
                // 非分区表
                metadata.setPartitionFields("");
            }

            // 获取表存储大小
            try (Statement stmt = conn.createStatement()) {
                String sql = "SHOW TABLE EXTENDED LIKE '" + tableName + "'";
                ResultSet rs = stmt.executeQuery(sql);
                while (rs.next()) {
                    String line = rs.getString(1);
                    if (line != null && line.contains("totalFileSize:")) {
                        // 解析存储大小，直接获取字节数
                        int startIndex = line.indexOf("totalFileSize:") + "totalFileSize:".length();
                        String sizeStr = line.substring(startIndex).trim();
                        try {
                            metadata.setTableSize(Integer.valueOf(sizeStr));
                        } catch (NumberFormatException e) {
                            // 忽略解析失败的存储大小
                        }
                        break;
                    }
                }
            }
            metadata.setTableComment(getTableComment(conn, tableName));

            // Hive 不支持主键，设置为空
            metadata.setPrimaryKey("");

            // 使用 DESCRIBE FORMATTED 查询获取创建时间和 InputFormat
            try (Statement stmt = conn.createStatement()) {
                String sql = "DESCRIBE FORMATTED " + tableName;
                ResultSet rs = stmt.executeQuery(sql);
                String inputFormat = "";
                String createTime = "";
                while (rs.next()) {
                    String colName = rs.getString("col_name");
                    if (colName.contains("InputFormat")) {
                        inputFormat = rs.getString("data_type");
                    } else if (colName.contains("CreateTime")) {
                        createTime = rs.getString("data_type");
                        try {
                            // Hive 返回的时间格式通常为：Thu Apr 14 10:00:00 CST 2026
                            SimpleDateFormat dataFormat = new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy", Locale.ENGLISH);
                            SimpleDateFormat outputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                            Date date = dataFormat.parse(createTime);
                            createTime = outputFormat.format(date);
                        } catch (Exception e) {
                            // 解析失败，返回原始时间
                        }
                    }
                }
                // 设置创建时间（Hive 不支持修改时间，设置为创建时间）
                metadata.setCreateTime(createTime);
                metadata.setUpdateTime(null);

                // 根据 InputFormat 映射存储引擎类型
                if (inputFormat.contains("OrcInputFormat")) {
                    metadata.setStorageEngine("ORC");
                } else if (inputFormat.contains("ParquetInputFormat")) {
                    metadata.setStorageEngine("PARQUET");
                } else if (inputFormat.contains("TextInputFormat")) {
                    metadata.setStorageEngine("TEXTFILE");
                } else if (inputFormat.contains("AvroInputFormat")) {
                    metadata.setStorageEngine("AVRO");
                } else if (inputFormat.contains("RCFileInputFormat")) {
                    metadata.setStorageEngine("RCFILE");
                } else if (inputFormat.contains("SequenceFileInputFormat")) {
                    metadata.setStorageEngine("SEQUENCEFILE");
                } else if (!inputFormat.isEmpty()) {
                    // 处理其他存储格式
                    metadata.setStorageEngine(inputFormat);
                } else {
                    metadata.setStorageEngine("Hive");
                }
            } catch (SQLException e) {
                metadata.setStorageEngine("Hive");
            }
        } catch (Exception e) {
            throw new DataQueryException("采集Hive表元数据失败: " + e.getMessage());
        }
        return metadata;
    }

    // 查询表级注释
    private static String getTableComment(Connection conn, String tableName) throws SQLException {
        String sql = "DESCRIBE FORMATTED " + tableName;
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                // DESCRIBE FORMATTED 返回三列: col_name, data_type, comment
                String data_type = rs.getString("data_type");
                if (StringUtils.isNotBlank(data_type) && data_type.contains("comment")) {
                    String comment = rs.getString("comment");
                    return comment;
                }
            }
        }
        return "";
    }
}
