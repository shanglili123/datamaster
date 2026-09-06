package com.datamaster.flinkx.core;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.datamaster.common.database.constants.DbType;
import com.datamaster.common.database.exception.DataQueryException;
import com.datamaster.flinkx.core.transform.TransformSqlBuilder;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class FlinkxEtlTaskConverter {

    /**
     * 自定义 SQL 模式（connection.querySql 非空）下 reader 的虚拟表别名。
     * ChunJun 在存在 transformer 时通过 reader.table.tableName 创建临时视图，
     * transformSql 的 FROM 必须引用同一名称，故自定义 SQL 模式下使用固定别名。
     */
    private static final String CUSTOM_SQL_TABLE_ALIAS = "sourceTable";

    public static String convertToFlinkxJobJson(Map<String, Object> mainArgs) {
        Map<String, Object> readerMap = (Map<String, Object>) mainArgs.get("reader");
        List<Map<String, Object>> writerList = resolveWriterList(mainArgs);
        List<Map<String, Object>> transitionList = (List<Map<String, Object>>) mainArgs.get("transition");
        JSONObject config = mainArgs.get("config") instanceof Map
                ? jsonObjectFromMap((Map<String, Object>) mainArgs.get("config"))
                : new JSONObject();
        Object taskInfo = config.get("taskInfo");
        String name = taskInfo instanceof Map
                ? (String) ((Map<String, Object>) taskInfo).get("name")
                : "flinkx-etl";

        JSONObject root = new JSONObject();
        JSONObject job = new JSONObject();

        JSONArray content = new JSONArray();
        if (readerMap != null && !writerList.isEmpty()) {
            for (Map<String, Object> writerMap : writerList) {
                JSONObject contentItem = new JSONObject();
                if (isExcelOrCsvReader(readerMap)) {
                    contentItem.put("reader", buildFileReader(readerMap));
                } else {
                    contentItem.put("reader", buildReader(readerMap));
                }
                contentItem.put("writer", buildWriter(writerMap));
                if (transitionList != null && !transitionList.isEmpty()) {
                    String transformSql = buildTransformSql(transitionList, readerMap);
                    if (transformSql != null) {
                        JSONObject transformer = new JSONObject();
                        transformer.put("transformSql", transformSql);
                        contentItem.put("transformer", transformer);
                    }
                }
                content.add(contentItem);
            }
        } else if (readerMap != null) {
            JSONObject contentItem = new JSONObject();
            if (isExcelOrCsvReader(readerMap)) {
                contentItem.put("reader", buildFileReader(readerMap));
            } else {
                contentItem.put("reader", buildReader(readerMap));
            }
            if (transitionList != null && !transitionList.isEmpty()) {
                String transformSql = buildTransformSql(transitionList, readerMap);
                if (transformSql != null) {
                    JSONObject transformer = new JSONObject();
                    transformer.put("transformSql", transformSql);
                    contentItem.put("transformer", transformer);
                }
            }
            content.add(contentItem);
        }
        job.put("content", content);

        job.put("setting", buildSetting(config));
        root.put("job", job);
        return root.toJSONString();
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> resolveWriterList(Map<String, Object> mainArgs) {
        Object writerList = mainArgs.get("writerList");
        if (writerList instanceof List && !((List<?>) writerList).isEmpty()) {
            List<Map<String, Object>> writers = new ArrayList<>();
            for (Object item : (List<?>) writerList) {
                if (item instanceof Map) {
                    writers.add((Map<String, Object>) item);
                }
            }
            if (!writers.isEmpty()) {
                return writers;
            }
        }
        Map<String, Object> singleWriter = (Map<String, Object>) mainArgs.get("writer");
        if (singleWriter != null) {
            List<Map<String, Object>> writers = new ArrayList<>();
            writers.add(singleWriter);
            return writers;
        }
        return new ArrayList<>();
    }

    private static boolean isExcelOrCsvReader(Map<String, Object> readerMap) {
        String componentType = (String) readerMap.get("componentType");
        return "EXCEL_READER".equals(componentType) || "CSV_READER".equals(componentType);
    }

    @SuppressWarnings("unchecked")
    private static JSONObject buildReader(Map<String, Object> readerMap) {
        Map<String, Object> param = (Map<String, Object>) readerMap.get("parameter");
        DbType dbType = parseDbType(param);

        JSONObject reader = new JSONObject();
        boolean kafkaReader = isKafka(dbType);
        boolean streamingMqReader = !kafkaReader && isStreamingMqReader(dbType);
        boolean cdcReader = param != null && !kafkaReader && !streamingMqReader && isCdcReadMode(param);
        reader.put("name", resolveReaderName(dbType, cdcReader));

        JSONObject rp = new JSONObject();
        if (param != null) {
            if (kafkaReader) {
                reader.put("parameter", buildKafkaParameter(param));
                return reader;
            }
            if (streamingMqReader) {
                reader.put("parameter", buildStreamingMqParameter(param, dbType));
                return reader;
            }
            if (cdcReader) {
                reader.put("parameter", buildCdcParameter(param, dbType));
                return reader;
            }
            if (isMongo(dbType)) {
                reader.put("parameter", buildMongoParameter(param, true));
                return reader;
            }
            if (isElasticsearch(dbType)) {
                reader.put("parameter", buildElasticsearchParameter(param, true));
                return reader;
            }
            rp.put("username", param.get("username"));
            rp.put("password", param.get("password"));
            rp.put("where", param.getOrDefault("where", ""));
            rp.put("column", normalizeFieldColumns(param.get("column")));
            rp.put("splitPk", param.getOrDefault("splitPk", ""));

            Object querySql = connectionQuery(param);
            if (querySql != null && StringUtils.isNotBlank(String.valueOf(querySql))) {
                // 自定义 SQL 模式：ChunJun 顶层 customSql 优先于 connection.table / where / splitPk。
                // 注意：不能使用 querySql 键——JdbcInputFormat.open() 会用内置 SQL 覆盖 querySql，
                // 且空 table 数组会让 getTable() 抛 IndexOutOfBoundsException；customSql 才是输入键。
                rp.put("customSql", String.valueOf(querySql));
                rp.put("where", "");
                rp.put("splitPk", "");
            }

            Map<String, Object> rawConn = (Map<String, Object>) param.get("connection");
            if (rawConn != null) {
                JSONArray conns = new JSONArray();
                JSONObject conn = new JSONObject();
                Object jdbcUrl = rawConn.get("jdbcUrl");
                conn.put("jdbcUrl", jdbcUrl instanceof List ? jdbcUrl : new String[]{String.valueOf(jdbcUrl)});
                Object table = rawConn.get("table");
                if (table instanceof String) {
                    conn.put("table", new String[]{(String) table});
                } else if (table instanceof List) {
                    conn.put("table", table);
                } else {
                    conn.put("table", new String[0]);
                }
                conns.add(conn);
                rp.put("connection", conns);
            }

            rp.put("batchSize", param.getOrDefault("batchSize", 1024));
        }
        addTableField(reader, param);
        reader.put("parameter", rp);
        return reader;
    }

    @SuppressWarnings("unchecked")
    private static JSONObject buildWriter(Map<String, Object> writerMap) {
        Map<String, Object> param = (Map<String, Object>) writerMap.get("parameter");
        DbType dbType = parseDbType(param);

        JSONObject writer = new JSONObject();
        writer.put("name", resolveWriterName(dbType));

        JSONObject wp = new JSONObject();
        if (param != null) {
            if (isKafka(dbType)) {
                writer.put("parameter", buildKafkaWriterParameter(param));
                return writer;
            }
            if (isMongo(dbType)) {
                writer.put("parameter", buildMongoParameter(param, false));
                return writer;
            }
            if (isElasticsearch(dbType)) {
                writer.put("parameter", buildElasticsearchParameter(param, false));
                return writer;
            }
            wp.put("username", param.get("username"));
            wp.put("password", param.get("password"));
            wp.put("batchSize", param.getOrDefault("batchSize", 1024));

            wp.put("column", normalizeFieldColumns(firstPresent(param.get("target_column"), param.get("column"))));

            String writeModeType = param.get("writeModeType") == null ? null : String.valueOf(param.get("writeModeType"));
            wp.put("writeMode", resolveWriteMode(writeModeType, param.get("selectedColumns"), dbType));

            Object preSql = param.get("preSql");
            if (preSql != null) {
                wp.put("preSql", preSql instanceof List ? preSql : String.valueOf(preSql).split(","));
            }
            Object postSql = param.get("postSql");
            if (postSql != null) {
                wp.put("postSql", postSql instanceof List ? postSql : String.valueOf(postSql).split(","));
            }

            Map<String, Object> rawConn = (Map<String, Object>) param.get("connection");
            if (rawConn != null) {
                JSONArray conns = new JSONArray();
                JSONObject conn = new JSONObject();
                conn.put("jdbcUrl", String.valueOf(rawConn.get("jdbcUrl")));
                Object table = rawConn.get("table");
                if (table instanceof String) {
                    conn.put("table", new String[]{(String) table});
                } else if (table instanceof List) {
                    conn.put("table", table);
                } else {
                    conn.put("table", new String[0]);
                }
                conns.add(conn);
                wp.put("connection", conns);
            }
        }
        addTableField(writer, param);
        writer.put("parameter", wp);
        return writer;
    }

    @SuppressWarnings("unchecked")
    private static JSONObject buildMongoParameter(Map<String, Object> param, boolean reader) {
        JSONObject config = datasourceConfig(param);
        JSONObject mp = new JSONObject();
        mp.put("address", param.get("host") + ":" + param.get("port"));
        putIfPresent(mp, "username", param.get("username"));
        putIfPresent(mp, "password", param.get("password"));
        mp.put("database", firstPresent(param.get("dbName"), config.get("database"), config.get("dbname"), config.get("dbName")));
        mp.put("collectionName", firstPresent(connectionTable(param), config.get("collection"), config.get("collectionName")));
        mp.put("column", normalizeFieldColumns(firstPresent(param.get("target_column"), param.get("column"))));
        mp.put("batchSize", param.getOrDefault("batchSize", 1024));
        if (!reader) {
            String writeModeType = param.get("writeModeType") == null ? null : String.valueOf(param.get("writeModeType"));
            mp.put("writeMode", resolveWriteMode(writeModeType, param.get("selectedColumns"), parseDbType(param)));
        }
        putIfPresent(mp, "query", connectionQuery(param));
        return mp;
    }

    private static JSONObject buildElasticsearchParameter(Map<String, Object> param, boolean reader) {
        JSONObject config = datasourceConfig(param);
        JSONObject ep = new JSONObject();
        ep.put("address", firstPresent(config.get("address"), "http://" + param.get("host") + ":" + param.get("port")));
        putIfPresent(ep, "username", param.get("username"));
        putIfPresent(ep, "password", param.get("password"));
        ep.put("index", firstPresent(connectionTable(param), config.get("index"), config.get("indexName")));
        putIfPresent(ep, "type", firstPresent(config.get("type"), config.get("docType"), "_doc"));
        ep.put("column", normalizeFieldColumns(firstPresent(param.get("target_column"), param.get("column"))));
        ep.put("batchSize", param.getOrDefault("batchSize", 1024));
        if (!reader) {
            String writeModeType = param.get("writeModeType") == null ? null : String.valueOf(param.get("writeModeType"));
            ep.put("writeMode", resolveWriteMode(writeModeType, param.get("selectedColumns"), parseDbType(param)));
        }
        putIfPresent(ep, "query", connectionQuery(param));
        return ep;
    }

    private static JSONObject buildCdcParameter(Map<String, Object> param, DbType dbType) {
        JSONObject cdcConfig = nestedConfig(param, "cdcConfig");
        JSONObject cp = new JSONObject();
        putIfPresent(cp, "username", param.get("username"));
        putIfPresent(cp, "password", param.get("password"));
        putIfPresent(cp, "column", normalizeFieldColumns(firstPresent(param.get("tableFields"), param.get("column"))));
        Object tableName = firstPresent(connectionTable(param), cdcConfig.get("table"), cdcConfig.get("tableName"));
        Object dbName = firstPresent(param.get("dbName"), cdcConfig.get("database"), cdcConfig.get("databaseName"));

        if (isMysqlLike(dbType)) {
            putIfPresent(cp, "host", firstPresent(cdcConfig.get("host"), param.get("host")));
            cp.put("port", toInt(firstPresent(cdcConfig.get("port"), param.get("port")), 3306));
            putListIfPresent(cp, "databaseList", firstPresent(cdcConfig.get("databaseList"), cdcConfig.get("database"), dbName));
            putListIfPresent(cp, "tableList", firstPresent(cdcConfig.get("tableList"), cdcConfig.get("table"), qualifyMySqlCdcTable(dbName, tableName)));
            cp.put("serverId", toInt(firstPresent(cdcConfig.get("serverId"), cdcConfig.get("server-id")), defaultServerId(param)));
            requireCdcParameter(cp, dbType, "host", "databaseList", "tableList", "username", "password");
        } else if (isSqlServer(dbType)) {
            putIfPresent(cp, "url", firstPresent(cdcConfig.get("url"), connectionJdbcUrl(param)));
            putIfPresent(cp, "databaseName", firstPresent(cdcConfig.get("databaseName"), cdcConfig.get("database"), dbName));
            putListIfPresent(cp, "tableList", firstPresent(cdcConfig.get("tableList"), cdcConfig.get("table"), tableName));
            cp.put("pavingData", toBoolean(firstPresent(cdcConfig.get("pavingData"), true)));
            cp.put("splitUpdate", toBoolean(firstPresent(cdcConfig.get("splitUpdate"), false)));
            cp.put("timestampFormat", firstPresent(cdcConfig.get("timestampFormat"), "yyyy-MM-dd HH:mm:ss"));
            putIfPresent(cp, "pollInterval", cdcConfig.get("pollInterval"));
            putIfPresent(cp, "lsn", cdcConfig.get("lsn"));
            requireCdcParameter(cp, dbType, "url", "databaseName", "tableList", "username", "password");
        } else if (isOracle(dbType)) {
            Object oracleTables = firstPresent(cdcConfig.get("table"), cdcConfig.get("tableList"), tableName);
            putIfPresent(cp, "jdbcUrl", firstPresent(cdcConfig.get("jdbcUrl"), connectionJdbcUrl(param)));
            putIfPresent(cp, "username", firstPresent(cdcConfig.get("username"), param.get("username")));
            putIfPresent(cp, "password", firstPresent(cdcConfig.get("password"), param.get("password")));
            putListIfPresent(cp, "table", oracleTables);
            putIfPresent(cp, "listenerTables", joinListValue(firstPresent(cdcConfig.get("listenerTables"), oracleTables)));
            cp.put("readPosition", firstPresent(cdcConfig.get("readPosition"), "current"));
            cp.put("timestampFormat", firstPresent(cdcConfig.get("timestampFormat"), "yyyy-MM-dd HH:mm:ss"));
            cp.put("pavingData", toBoolean(firstPresent(cdcConfig.get("pavingData"), true)));
            cp.put("split", toBoolean(firstPresent(cdcConfig.get("split"), false)));
            putIfPresent(cp, "startScn", cdcConfig.get("startScn"));
            putIfPresent(cp, "startTime", cdcConfig.get("startTime"));
            requireCdcParameter(cp, dbType, "jdbcUrl", "table", "listenerTables", "username", "password");
        } else {
            throw new DataQueryException("FlinkX CDC 暂不支持的 reader 数据库类型: " + dbType.getDb());
        }
        mergeExtraConfig(cp, cdcConfig);
        return cp;
    }

    private static JSONObject buildKafkaParameter(Map<String, Object> param) {
        JSONObject kafkaConfig = nestedConfig(param, "kafkaConfig");
        JSONObject kp = new JSONObject();
        Object configuredTopics = kafkaConfig.get("topics");
        Object rawTopic = firstPresent(
                connectionTopic(param), param.get("topic"), kafkaConfig.get("topic"), firstTopic(configuredTopics));
        Object topics = firstPresent(configuredTopics, rawTopic);
        Object topic = firstTopic(topics);
        putIfPresent(kp, "topic", topic);
        putListIfPresent(kp, "topics", topics);
        kp.put("groupId", firstPresent(kafkaConfig.get("groupId"), kafkaConfig.get("group-id"), "datamaster-chunjun"));
        kp.put("mode", firstPresent(kafkaConfig.get("mode"), kafkaConfig.get("startupMode"), "LATEST"));
        Object codec = firstPresent(kafkaConfig.get("codec"), "json");
        kp.put("codec", codec);
        Object tableFields = firstPresent(kafkaConfig.get("tableFields"), param.get("tableFields"));
        Object columns = firstPresent(kafkaConfig.get("column"), param.get("column"), tableFields);
        Object normalizedColumns = normalizeFieldColumns(columns);
        putIfPresent(kp, "column", normalizedColumns);
        putListIfPresent(kp, "tableFields", normalizeKafkaFieldNames(columns));
        putIfPresent(kp, "tableSchema", firstPresent(
                normalizeKafkaTableSchema(kafkaConfig.get("tableSchema")),
                buildKafkaTableSchema(firstPresent(topic, firstTopic(topics)), normalizedColumns)));
        kp.put("pavingData", toBoolean(firstPresent(kafkaConfig.get("pavingData"), true)));
        kp.put("split", toBoolean(firstPresent(kafkaConfig.get("split"), false)));
        List<String> keyFields = toStringList(firstPresent(kafkaConfig.get("keyFields"), param.get("keyFields")));
        if (keyFields.isEmpty()) {
            keyFields = extractKeyFields(columns);
        }
        putListIfPresent(kp, "keyFields", keyFields);
        JSONObject datasourceConfig = datasourceConfig(param);
        JSONObject consumerSettings = new JSONObject();
        mergeConsumerSettings(consumerSettings, datasourceConfig.get("config"));
        mergeConsumerSettings(consumerSettings, param.get("config"));
        mergeKafkaClientProperties(consumerSettings, kafkaConfig);
        mergeConsumerSettings(consumerSettings, kafkaConfig.get("consumerSettings"));
        normalizeBootstrapServers(consumerSettings, param);
        kp.put("consumerSettings", stringifyJsonValues(consumerSettings));
        if (!hasValue(kp.get("topic")) && !hasValue(kp.get("topics"))) {
            throw new DataQueryException("Kafka reader 缺少 Topic");
        }
        if (!hasValue(consumerSettings.get("bootstrap.servers"))) {
            throw new DataQueryException("Kafka reader 缺少 bootstrap.servers");
        }
        putIfPresent(kp, "timestamp", kafkaConfig.get("timestamp"));
        putIfPresent(kp, "offset", kafkaConfig.get("offset"));
        putIfPresent(kp, "deserialization", kafkaConfig.get("deserialization"));
        putIfPresent(kp, "deserializationProperties", kafkaConfig.get("deserializationProperties"));
        mergeKafkaExtraConfig(kp, kafkaConfig,
                "topic", "topics", "groupId", "group-id", "mode", "startupMode", "codec",
                "column", "columns", "fields", "tableFields", "tableSchema",
                "pavingData", "split", "keyFields", "consumerSettings",
                "timestamp", "offset", "deserialization", "deserializationProperties");
        return kp;
    }

    /**
     * 构建 Kafka 输出（kafkawriter）参数。
     * 依据 ChunJun chunjun-connector-kafka 的 KafkaConfig 契约：
     * - topic / topics             输出目标
     * - codec                      序列化格式（默认 json）
     * - column / tableFields       字段定义
     * - partitionAssignColumns     分区键字段（KafkaSinkFactory 校验其必须包含在 tableFields 中，
     *                              并通过 KafkaSyncKeyConverter 将分区键序列化为消息 key，
     *                              配合 FlinkKafkaProducer 默认分区器实现"同 key 同分区"的主键/键分区）
     * - producerSettings           Kafka 生产端原生参数（bootstrap.servers、序列化器等）
     */
    private static JSONObject buildKafkaWriterParameter(Map<String, Object> param) {
        JSONObject kafkaConfig = nestedConfig(param, "kafkaWriterConfig");
        JSONObject kp = new JSONObject();

        // topic / topics：优先取配置，其次兜底 connection.table
        Object configuredTopics = kafkaConfig.get("topics");
        Object rawTopic = firstPresent(
                param.get("topic"), kafkaConfig.get("topic"), connectionTable(param), firstTopic(configuredTopics));
        Object topics = firstPresent(configuredTopics, rawTopic);
        Object topic = firstTopic(topics);
        putIfPresent(kp, "topic", topic);
        putListIfPresent(kp, "topics", topics);
        if (!hasValue(kp.get("topic")) && !hasValue(kp.get("topics"))) {
            throw new DataQueryException("Kafka writer 缺少 Topic");
        }

        // 序列化格式
        kp.put("codec", firstPresent(kafkaConfig.get("codec"), "json"));

        // 字段
        Object tableFields = firstPresent(kafkaConfig.get("tableFields"), param.get("tableFields"));
        Object columns = firstPresent(kafkaConfig.get("column"), param.get("target_column"), param.get("column"));
        Object normalizedColumns = normalizeFieldColumns(columns);
        putIfPresent(kp, "column", normalizedColumns);
        putListIfPresent(kp, "tableFields", firstPresent(kafkaConfig.get("fieldList"), normalizeKafkaFieldNames(columns)));
        putIfPresent(kp, "tableSchema", firstPresent(
                normalizeKafkaTableSchema(kafkaConfig.get("tableSchema")),
                buildKafkaTableSchema(firstPresent(topic, firstTopic(topics)), normalizedColumns)));

        // 主键/键分区：partitionAssignColumns 必须包含在 tableFields 中
        List<String> partitionAssignColumns = toStringList(
                firstPresent(kafkaConfig.get("partitionAssignColumns"), param.get("partitionAssignColumns")));
        if (partitionAssignColumns.isEmpty()) {
            List<String> selected = toStringList(kafkaConfig.get("selectedColumns"));
            partitionAssignColumns = selected;
        }
        putListIfPresent(kp, "partitionAssignColumns", partitionAssignColumns);

        kp.put("batchSize", toInt(firstPresent(kafkaConfig.get("batchSize"), param.getOrDefault("batchSize", 1024)), 1024));
        putIfPresent(kp, "dataCompelOrder", kafkaConfig.get("dataCompelOrder"));
        putIfPresent(kp, "pavingData", kafkaConfig.get("pavingData"));
        putIfPresent(kp, "split", kafkaConfig.get("split"));

        // producerSettings：Kafka 生产端原生参数
        JSONObject producerSettings = new JSONObject();
        mergeProducerSettings(producerSettings, datasourceConfig(param).get("config"));
        mergeProducerSettings(producerSettings, param.get("config"));
        mergeKafkaClientProperties(producerSettings, kafkaConfig);
        normalizeBootstrapServers(producerSettings, param);
        if (!hasValue(producerSettings.get("bootstrap.servers"))) {
            throw new DataQueryException("Kafka writer 缺少 bootstrap.servers");
        }
        putIfPresent(kp, "producerSettings", stringifyJsonValues(producerSettings));

        mergeKafkaExtraConfig(kp, kafkaConfig,
                "topic", "topics", "codec", "column", "columns", "fields",
                "tableFields", "fieldList", "tableSchema", "partitionAssignColumns",
                "selectedColumns", "batchSize", "dataCompelOrder", "pavingData", "split",
                "producerSettings", "timestamp", "offset",
                "deserialization", "deserializationProperties");
        return kp;
    }

    private static void mergeProducerSettings(JSONObject target, Object raw) {
        JSONObject config = toJsonObject(raw);
        for (Map.Entry<String, Object> entry : config.entrySet()) {
            String key = entry.getKey() == null ? null : entry.getKey().trim();
            if (StringUtils.isBlank(key) || entry.getValue() == null) {
                continue;
            }
            // 只合并 Kafka 生产端相关属性，其余配置由 mergeKafkaExtraConfig 兜底透传
            if ("bootstrap.servers".equalsIgnoreCase(key)
                    || "bootstrapServers".equalsIgnoreCase(key)
                    || "bootstrap_servers".equalsIgnoreCase(key)
                    || key.endsWith(".serializer")
                    || key.endsWith("serializer")
                    || key.startsWith("producer.")
                    || key.toLowerCase(java.util.Locale.ROOT).contains("acks")
                    || key.toLowerCase(java.util.Locale.ROOT).contains("linger")
                    || key.toLowerCase(java.util.Locale.ROOT).contains("retries")
                    || key.toLowerCase(java.util.Locale.ROOT).contains("batch")
                    || key.toLowerCase(java.util.Locale.ROOT).contains("buffer.memory")
                    || key.toLowerCase(java.util.Locale.ROOT).contains("request.timeout")) {
                target.put(key, entry.getValue());
            }
        }
    }

    private static JSONObject buildStreamingMqParameter(Map<String, Object> param, DbType dbType) {
        switch (dbType) {
            case RABBITMQ: return buildRabbitmqParameter(param);
            case REDIS: return buildRedisParameter(param);
            case ROCKETMQ: return buildRocketmqParameter(param);
            case SOCKET: return buildSocketParameter(param);
            case STREAM: return buildStreamParameter(param);
            default: throw new DataQueryException("不支持的流式消息中间件: " + dbType.getDb());
        }
    }

    private static JSONObject buildRabbitmqParameter(Map<String, Object> param) {
        JSONObject rabbitConfig = nestedConfig(param, "rabbitmqConfig");
        JSONObject rp = new JSONObject();
        putIfPresent(rp, "host", firstPresent(rabbitConfig.get("host"), param.get("host")));
        rp.put("port", toInt(firstPresent(rabbitConfig.get("port"), param.get("port")), 5672));
        putIfPresent(rp, "username", firstPresent(rabbitConfig.get("username"), param.get("username")));
        putIfPresent(rp, "password", firstPresent(rabbitConfig.get("password"), param.get("password")));
        putIfPresent(rp, "virtualHost", firstPresent(rabbitConfig.get("virtualHost"), rabbitConfig.get("vhost"), "/"));
        putIfPresent(rp, "queue", firstPresent(
                connectionTopic(param), rabbitConfig.get("queue"), rabbitConfig.get("queueName"),
                param.get("topic"), param.get("queue")));
        putIfPresent(rp, "exchange", rabbitConfig.get("exchange"));
        putIfPresent(rp, "routingKey", firstPresent(rabbitConfig.get("routingKey"), param.get("routingKey")));
        putIfPresent(rp, "tableFields", firstPresent(param.get("tableFields"), normalizeFieldColumns(param.get("column"))));
        mergeExtraConfig(rp, rabbitConfig,
                "host", "port", "username", "password", "virtualHost", "vhost",
                "queue", "queueName", "exchange", "routingKey", "tableFields");
        return rp;
    }

    private static JSONObject buildRedisParameter(Map<String, Object> param) {
        JSONObject redisConfig = nestedConfig(param, "redisConfig");
        JSONObject rp = new JSONObject();
        putIfPresent(rp, "host", firstPresent(redisConfig.get("host"), param.get("host")));
        rp.put("port", toInt(firstPresent(redisConfig.get("port"), param.get("port")), 6379));
        putIfPresent(rp, "username", firstPresent(redisConfig.get("username"), param.get("username")));
        putIfPresent(rp, "password", firstPresent(redisConfig.get("password"), param.get("password")));
        putIfPresent(rp, "database", firstPresent(redisConfig.get("database"), redisConfig.get("db"), 0));
        putIfPresent(rp, "key", firstPresent(redisConfig.get("key"), param.get("key"), param.get("topic")));
        putIfPresent(rp, "keyPattern", redisConfig.get("keyPattern"));
        putListIfPresent(rp, "keys", firstPresent(redisConfig.get("keys"), param.get("keys")));
        putIfPresent(rp, "tableFields", firstPresent(param.get("tableFields"), normalizeFieldColumns(param.get("column"))));
        mergeExtraConfig(rp, redisConfig,
                "host", "port", "username", "password", "database", "db",
                "key", "keyPattern", "keys", "tableFields");
        return rp;
    }

    private static JSONObject buildRocketmqParameter(Map<String, Object> param) {
        JSONObject rocketConfig = nestedConfig(param, "rocketmqConfig");
        JSONObject rp = new JSONObject();
        putIfPresent(rp, "nameserver", firstPresent(
                rocketConfig.get("nameserver"), rocketConfig.get("nameServer"),
                param.get("host") + ":" + firstPresent(rocketConfig.get("port"), param.get("port"), 9876)));
        putIfPresent(rp, "topic", firstPresent(
                connectionTopic(param), rocketConfig.get("topic"), param.get("topic")));
        putIfPresent(rp, "consumerGroup", firstPresent(
                rocketConfig.get("consumerGroup"), rocketConfig.get("consumer-group"), "ChunjunConsumer"));
        putIfPresent(rp, "tag", firstPresent(rocketConfig.get("tag"), "*"));
        putIfPresent(rp, "codec", firstPresent(rocketConfig.get("codec"), "json"));
        putIfPresent(rp, "tableFields", firstPresent(param.get("tableFields"), normalizeFieldColumns(param.get("column"))));
        mergeExtraConfig(rp, rocketConfig,
                "nameserver", "nameServer", "topic", "consumerGroup", "consumer-group",
                "tag", "codec", "tableFields");
        return rp;
    }

    private static JSONObject buildSocketParameter(Map<String, Object> param) {
        JSONObject socketConfig = nestedConfig(param, "socketConfig");
        JSONObject rp = new JSONObject();
        rp.put("host", firstPresent(socketConfig.get("host"), param.get("host"), "0.0.0.0"));
        rp.put("port", toInt(firstPresent(socketConfig.get("port"), param.get("port")), 8888));
        putIfPresent(rp, "tableFields", firstPresent(param.get("tableFields"), normalizeFieldColumns(param.get("column"))));
        mergeExtraConfig(rp, socketConfig, "host", "port", "tableFields");
        return rp;
    }

    private static JSONObject buildStreamParameter(Map<String, Object> param) {
        JSONObject streamConfig = nestedConfig(param, "streamConfig");
        JSONObject rp = new JSONObject();
        putIfPresent(rp, "content", streamConfig.get("content"));
        putIfPresent(rp, "tableFields", firstPresent(param.get("tableFields"), normalizeFieldColumns(param.get("column"))));
        mergeExtraConfig(rp, streamConfig, "content", "tableFields");
        return rp;
    }

    private static void mergeConsumerSettings(JSONObject target, Object raw) {
        JSONObject settings = toJsonObject(raw);
        for (Map.Entry<String, Object> entry : settings.entrySet()) {
            target.put(entry.getKey(), entry.getValue());
        }
    }

    private static void mergeKafkaClientProperties(JSONObject target, JSONObject kafkaConfig) {
        for (Map.Entry<String, Object> entry : kafkaConfig.entrySet()) {
            if (isKafkaClientPropertyKey(entry.getKey())) {
                target.put(entry.getKey(), entry.getValue());
            }
        }
    }

    private static void normalizeBootstrapServers(JSONObject consumerSettings, Map<String, Object> param) {
        Object bootstrapServers = firstPresent(
                consumerSettings.get("bootstrap.servers"),
                consumerSettings.get("bootstrapServers"),
                consumerSettings.get("bootstrap_servers"));
        if (!hasValue(bootstrapServers)) {
            Object host = param.get("host");
            Object port = param.get("port");
            if (hasValue(host) && hasValue(port)) {
                bootstrapServers = host + ":" + port;
            }
        }
        if (hasValue(bootstrapServers)) {
            consumerSettings.put("bootstrap.servers", bootstrapServers);
            consumerSettings.remove("bootstrapServers");
            consumerSettings.remove("bootstrap_servers");
        }
    }

    private static JSONObject datasourceConfig(Map<String, Object> param) {
        return toJsonObject(param.get("datasourceConfig"));
    }

    private static JSONObject nestedConfig(Map<String, Object> param, String key) {
        Object raw = param.get(key);
        return toJsonObject(raw);
    }

    @SuppressWarnings("unchecked")
    private static JSONObject toJsonObject(Object raw) {
        if (raw instanceof Map) {
            return jsonObjectFromMap((Map<String, Object>) raw);
        }
        if (raw instanceof String && StringUtils.isNotBlank((String) raw)) {
            try {
                return JSONObject.parseObject((String) raw);
            } catch (Exception ignored) {
            }
        }
        return new JSONObject();
    }

    private static JSONObject jsonObjectFromMap(Map<String, Object> raw) {
        JSONObject object = new JSONObject();
        object.putAll(raw);
        return object;
    }

    private static JSONObject stringifyJsonValues(JSONObject raw) {
        JSONObject result = new JSONObject();
        for (Map.Entry<String, Object> entry : raw.entrySet()) {
            if (entry.getValue() != null) {
                result.put(entry.getKey(), String.valueOf(entry.getValue()));
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private static Object connectionTable(Map<String, Object> param) {
        Object rawConn = param.get("connection");
        if (!(rawConn instanceof Map)) {
            return null;
        }
        return ((Map<String, Object>) rawConn).get("table");
    }

    @SuppressWarnings("unchecked")
    private static Object connectionQuery(Map<String, Object> param) {
        Object rawConn = param.get("connection");
        if (!(rawConn instanceof Map)) {
            return null;
        }
        return ((Map<String, Object>) rawConn).get("querySql");
    }

    @SuppressWarnings("unchecked")
    private static Object connectionJdbcUrl(Map<String, Object> param) {
        Object rawConn = param.get("connection");
        if (!(rawConn instanceof Map)) {
            return null;
        }
        return ((Map<String, Object>) rawConn).get("jdbcUrl");
    }

    private static void addTableField(JSONObject target, Map<String, Object> param) {
        if (param == null) {
            return;
        }
        Object rawConn = param.get("connection");
        if (!(rawConn instanceof Map)) {
            return;
        }
        Object table = ((Map<String, Object>) rawConn).get("table");
        String tableName = null;
        if (table instanceof String) {
            tableName = (String) table;
        } else if (table instanceof List && !((List<?>) table).isEmpty()) {
            Object first = ((List<?>) table).get(0);
            if (first != null) {
                tableName = first.toString();
            }
        }
        // 自定义 SQL 模式：connection.table 为空，但 transformer 路径要求
        // reader.table.tableName 存在（checkTableConfig 校验），使用固定虚拟别名。
        Object querySql = connectionQuery(param);
        if ((tableName == null || tableName.isEmpty()) && querySql != null
                && StringUtils.isNotBlank(String.valueOf(querySql))) {
            tableName = CUSTOM_SQL_TABLE_ALIAS;
        }
        if (tableName != null && !tableName.isEmpty()) {
            JSONObject tableObj = new JSONObject();
            tableObj.put("tableName", tableName);
            target.put("table", tableObj);
        }
    }

    @SuppressWarnings("unchecked")
    private static Object connectionTopic(Map<String, Object> param) {
        Object rawConn = param.get("connection");
        if (!(rawConn instanceof Map)) {
            return null;
        }
        return ((Map<String, Object>) rawConn).get("topic");
    }

    private static Object firstPresent(Object... values) {
        for (Object value : values) {
            if (hasValue(value)) {
                return value;
            }
        }
        return null;
    }

    private static void putIfPresent(JSONObject object, String key, Object value) {
        if (hasValue(value)) {
            object.put(key, value);
        }
    }

    private static boolean hasValue(Object value) {
        if (value == null) {
            return false;
        }
        if (value instanceof Map) {
            return !((Map<?, ?>) value).isEmpty();
        }
        if (value instanceof Collection) {
            return !((Collection<?>) value).isEmpty();
        }
        String text = String.valueOf(value);
        return StringUtils.isNotBlank(text) && !"null".equalsIgnoreCase(text);
    }

    private static void requireCdcParameter(JSONObject config, DbType dbType, String... keys) {
        for (String key : keys) {
            if (!hasValue(config.get(key))) {
                throw new DataQueryException("FlinkX CDC " + dbType.getDb() + " 缺少参数: " + key);
            }
        }
    }

    private static boolean isCdcReadMode(Map<String, Object> param) {
        Object mode = param.get("readModeType");
        return "4".equals(String.valueOf(mode)) || "CDC".equalsIgnoreCase(String.valueOf(mode));
    }

    private static void mergeExtraConfig(JSONObject target, JSONObject extra) {
        mergeExtraConfig(target, extra, new String[0]);
    }

    private static void mergeExtraConfig(JSONObject target, JSONObject extra, String... ignoredKeys) {
        for (Map.Entry<String, Object> entry : extra.entrySet()) {
            if (containsIgnoreCase(ignoredKeys, entry.getKey())) {
                continue;
            }
            if (!target.containsKey(entry.getKey())) {
                target.put(entry.getKey(), entry.getValue());
            }
        }
    }

    private static void mergeKafkaExtraConfig(JSONObject target, JSONObject extra, String... ignoredKeys) {
        for (Map.Entry<String, Object> entry : extra.entrySet()) {
            String key = entry.getKey();
            if (containsIgnoreCase(ignoredKeys, key) || isKafkaClientPropertyKey(key)) {
                continue;
            }
            if (!target.containsKey(key)) {
                target.put(key, entry.getValue());
            }
        }
    }

    private static boolean isKafkaClientPropertyKey(String key) {
        return key != null && key.contains(".");
    }

    private static boolean containsIgnoreCase(String[] values, String target) {
        if (values == null || target == null) {
            return false;
        }
        for (String value : values) {
            if (StringUtils.equalsIgnoreCase(value, target)) {
                return true;
            }
        }
        return false;
    }

    private static void putListIfPresent(JSONObject target, String key, Object value) {
        List<String> values = toStringList(value);
        if (!values.isEmpty()) {
            target.put(key, values);
        }
    }

    @SuppressWarnings("unchecked")
    private static Object normalizeFieldColumns(Object value) {
        if (!(value instanceof List)) {
            return value;
        }
        List<JSONObject> columns = new ArrayList<>();
        for (Object item : (List<?>) value) {
            if (item instanceof Map) {
                Map<String, Object> field = (Map<String, Object>) item;
                Object name = firstPresent(field.get("name"), field.get("columnName"), field.get("key"));
                String nameText = name == null ? null : String.valueOf(name).trim();
                if (StringUtils.isBlank(nameText)) {
                    continue;
                }
                JSONObject column = new JSONObject();
                column.put("name", nameText);
                column.put("type", firstPresent(field.get("type"), field.get("columnType"), "STRING"));
                putIfPresent(column, "isKey", toBooleanObject(firstPresent(
                        field.get("isKey"), field.get("keyFlag"), field.get("primaryKey"), "1".equals(String.valueOf(field.get("pkFlag"))))));
                putIfPresent(column, "index", toInteger(field.get("index")));
                putIfPresent(column, "value", field.get("value"));
                putIfPresent(column, "format", field.get("format"));
                putIfPresent(column, "parseFormat", field.get("parseFormat"));
                putIfPresent(column, "splitter", field.get("splitter"));
                putIfPresent(column, "isPart", toBooleanObject(field.get("isPart")));
                putIfPresent(column, "notNull", toBooleanObject(field.get("notNull")));
                columns.add(column);
            } else if (item != null && StringUtils.isNotBlank(String.valueOf(item).trim())) {
                JSONObject column = new JSONObject();
                column.put("name", String.valueOf(item).trim());
                column.put("type", "STRING");
                columns.add(column);
            }
        }
        return columns;
    }

    @SuppressWarnings("unchecked")
    private static List<String> extractKeyFields(Object value) {
        List<String> names = new ArrayList<>();
        if (!(value instanceof List)) {
            return names;
        }
        for (Object item : (List<?>) value) {
            if (!(item instanceof Map)) {
                continue;
            }
            Map<String, Object> field = (Map<String, Object>) item;
            Boolean key = toBooleanObject(firstPresent(
                    field.get("isKey"), field.get("keyFlag"), field.get("primaryKey"), "1".equals(String.valueOf(field.get("pkFlag")))));
            if (!Boolean.TRUE.equals(key)) {
                continue;
            }
            Object name = firstPresent(field.get("name"), field.get("columnName"));
            String nameText = name == null ? null : String.valueOf(name).trim();
            if (StringUtils.isNotBlank(nameText)) {
                names.add(nameText);
            }
        }
        return names;
    }

    @SuppressWarnings("unchecked")
    private static List<String> normalizeKafkaFieldNames(Object value) {
        List<String> names = new ArrayList<>();
        if (!(value instanceof List)) {
            return names;
        }
        for (Object item : (List<?>) value) {
            Object name = null;
            if (item instanceof Map) {
                Map<String, Object> field = (Map<String, Object>) item;
                name = firstPresent(field.get("name"), field.get("columnName"), field.get("key"));
            } else if (item != null) {
                name = item;
            }
            String nameText = name == null ? null : String.valueOf(name).trim();
            if (StringUtils.isNotBlank(nameText)) {
                names.add(nameText);
            }
        }
        return names;
    }

    private static JSONObject buildKafkaTableSchema(Object topic, Object columns) {
        if (topic == null || StringUtils.isBlank(String.valueOf(topic)) || !(columns instanceof List) || ((List<?>) columns).isEmpty()) {
            return new JSONObject();
        }
        JSONObject tableSchema = new JSONObject();
        tableSchema.put(String.valueOf(topic), columns);
        return tableSchema;
    }

    @SuppressWarnings("unchecked")
    private static JSONObject normalizeKafkaTableSchema(Object rawSchema) {
        JSONObject tableSchema = new JSONObject();
        if (!(rawSchema instanceof Map)) {
            return tableSchema;
        }
        Map<String, Object> schemaMap = (Map<String, Object>) rawSchema;
        for (Map.Entry<String, Object> entry : schemaMap.entrySet()) {
            String topic = entry.getKey() == null ? null : entry.getKey().trim();
            if (StringUtils.isBlank(topic)) {
                continue;
            }
            Object normalizedColumns = normalizeFieldColumns(entry.getValue());
            if (normalizedColumns instanceof List && !((List<?>) normalizedColumns).isEmpty()) {
                tableSchema.put(topic, normalizedColumns);
            }
        }
        return tableSchema;
    }

    private static Object firstTopic(Object topics) {
        List<String> values = toStringList(topics);
        return values.isEmpty() ? null : values.get(0);
    }

    private static Object joinListValue(Object value) {
        List<String> values = toStringList(value);
        return values.isEmpty() ? value : String.join(",", values);
    }

    private static Object qualifyMySqlCdcTable(Object database, Object table) {
        if (!hasValue(database) || !hasValue(table)) {
            return table;
        }
        List<String> databases = toStringList(database);
        if (databases.isEmpty()) {
            return table;
        }
        String dbName = databases.get(0);
        List<String> tables = toStringList(table);
        if (tables.isEmpty()) {
            return table;
        }
        List<String> qualified = new ArrayList<>();
        for (String tableName : tables) {
            qualified.add(tableName.contains(".") ? tableName : dbName + "." + tableName);
        }
        return qualified;
    }

    private static List<String> toStringList(Object value) {
        List<String> result = new ArrayList<>();
        if (value instanceof List) {
            for (Object item : (List<?>) value) {
                String text = item == null ? null : String.valueOf(item).trim();
                if (StringUtils.isNotBlank(text)) {
                    result.add(text);
                }
            }
        } else if (value != null && StringUtils.isNotBlank(String.valueOf(value))) {
            String raw = String.valueOf(value);
            for (String item : raw.split(",")) {
                if (StringUtils.isNotBlank(item)) {
                    result.add(item.trim());
                }
            }
        }
        return result;
    }

    private static Integer toInteger(Object value) {
        if (!hasValue(value)) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return Integer.valueOf(String.valueOf(value));
        } catch (NumberFormatException e) {
            throw new DataQueryException("FlinkX 字段 index 必须是整数: " + value);
        }
    }

    private static int toInt(Object value, int defaultValue) {
        if (value == null || StringUtils.isBlank(String.valueOf(value))) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private static double toDouble(Object value, double defaultValue) {
        if (value == null || StringUtils.isBlank(String.valueOf(value))) {
            return defaultValue;
        }
        try {
            return Double.parseDouble(String.valueOf(value));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private static String stringValue(Object value, String defaultValue) {
        if (value == null || StringUtils.isBlank(String.valueOf(value))) {
            return defaultValue;
        }
        return String.valueOf(value);
    }

    private static boolean toBoolean(Object value) {
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        String text = String.valueOf(value).trim();
        return "1".equals(text) || "yes".equalsIgnoreCase(text) || Boolean.parseBoolean(text);
    }

    private static Boolean toBooleanObject(Object value) {
        if (!hasValue(value)) {
            return null;
        }
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        String text = String.valueOf(value).trim();
        return "1".equals(text) || "yes".equalsIgnoreCase(text) || Boolean.valueOf(text);
    }

    private static int defaultServerId(Map<String, Object> param) {
        Object datasourceId = param.get("datasourceId");
        return 5400 + Math.abs(String.valueOf(datasourceId == null ? "0" : datasourceId).hashCode() % 1000);
    }

    @SuppressWarnings("unchecked")
    private static JSONObject buildFileReader(Map<String, Object> readerMap) {
        Map<String, Object> param = (Map<String, Object>) readerMap.get("parameter");

        JSONObject reader = new JSONObject();
        reader.put("name", "txtfilereader");

        JSONObject rp = new JSONObject();
        if (param != null) {
            Object path = param.get("path");
            if (path instanceof String) {
                rp.put("path", new String[]{((String) path)});
            } else if (path instanceof List) {
                rp.put("path", path);
            } else {
                rp.put("path", new String[0]);
            }
            rp.put("encoding", "UTF-8");
            rp.put("column", normalizeFieldColumns(param.get("column")));
            rp.put("fieldDelimiter", ",");
        }
        reader.put("parameter", rp);
        return reader;
    }

    // ========================================================================
    // transformSql generation — delegates to shared TransformSqlBuilder
    // ========================================================================

    private static String buildTransformSql(List<Map<String, Object>> transitionList,
                                            Map<String, Object> readerMap) {
        if (transitionList != null) {
            for (Map<String, Object> t : transitionList) {
                String componentType = (String) t.get("componentType");
                if ("32".equals(componentType)) {
                    Map<String, Object> param = (Map<String, Object>) t.get("parameter");
                    if (param != null) {
                        String customSql = (String) param.get("sql");
                        if (StringUtils.isNotBlank(customSql)) {
                            return customSql;
                        }
                    }
                }
            }
        }
        JSONArray transitions = new JSONArray();
        if (transitionList != null) {
            for (Map<String, Object> t : transitionList) {
                transitions.add(new JSONObject(t));
            }
        }
        List<String> sourceColumns = extractColumns(readerMap);
        String sourceTableName = extractSourceTableName(readerMap);
        return TransformSqlBuilder.build(transitions, sourceColumns, sourceTableName);
    }

    @SuppressWarnings("unchecked")
    private static List<String> extractColumns(Map<String, Object> readerMap) {
        List<String> cols = new ArrayList<>();
        if (readerMap == null) return cols;
        Map<String, Object> param = (Map<String, Object>) readerMap.get("parameter");
        if (param == null) return cols;
        Object column = param.get("column");
        if (column instanceof List) {
            for (Object item : (List<Object>) column) {
                if (item instanceof String) {
                    cols.add((String) item);
                } else if (item instanceof Map) {
                    Object name = ((Map<String, Object>) item).get("name");
                    if (name instanceof String) {
                        cols.add((String) name);
                    }
                }
            }
        } else if (column instanceof String && StringUtils.isNotBlank((String) column)) {
            cols.add((String) column);
        }
        return cols;
    }

    @SuppressWarnings("unchecked")
    private static String extractSourceTableName(Map<String, Object> readerMap) {
        if (readerMap == null) return CUSTOM_SQL_TABLE_ALIAS;
        Map<String, Object> param = (Map<String, Object>) readerMap.get("parameter");
        if (param == null) return CUSTOM_SQL_TABLE_ALIAS;
        Object rawConn = param.get("connection");
        if (!(rawConn instanceof Map)) return CUSTOM_SQL_TABLE_ALIAS;
        Object table = ((Map<String, Object>) rawConn).get("table");
        String tableName = null;
        if (table instanceof String) {
            tableName = (String) table;
        } else if (table instanceof List && !((List<?>) table).isEmpty()) {
            Object first = ((List<?>) table).get(0);
            if (first != null) {
                tableName = first.toString();
            }
        }
        // 自定义 SQL 模式（connection.table 为空）：transformSql FROM 必须引用
        // reader.table.tableName 相同的虚拟别名，统一使用 CUSTOM_SQL_TABLE_ALIAS。
        return tableName != null && !tableName.isEmpty() ? tableName : CUSTOM_SQL_TABLE_ALIAS;
    }

    private static JSONObject buildSetting(JSONObject config) {
        JSONObject taskInfoConfig = toJsonObject(config.get("taskInfo"));
        JSONObject settingConfig = toJsonObject(firstPresent(config.get("setting"), taskInfoConfig.get("setting")));
        JSONObject errorLimitConfig = toJsonObject(settingConfig.get("errorLimit"));
        JSONObject restoreConfig = toJsonObject(settingConfig.get("restore"));
        JSONObject logConfig = toJsonObject(settingConfig.get("log"));
        JSONObject setting = new JSONObject();

        JSONObject speed = new JSONObject();
        speed.put("channel", 1);
        speed.put("bytes", 0);
        setting.put("speed", speed);

        JSONObject errorLimit = new JSONObject();
        errorLimit.put("record", toInt(firstPresent(errorLimitConfig.get("record"), config.get("errorLimitRecord"), taskInfoConfig.get("errorLimitRecord")), 100));
        errorLimit.put("percentage", toDouble(firstPresent(errorLimitConfig.get("percentage"), config.get("errorLimitPercentage"), taskInfoConfig.get("errorLimitPercentage")), 0.1));
        setting.put("errorLimit", errorLimit);

        JSONObject restore = new JSONObject();
        restore.put("maxRowNumForCheckpoint", toInt(firstPresent(restoreConfig.get("maxRowNumForCheckpoint"), config.get("maxRowNumForCheckpoint"), taskInfoConfig.get("maxRowNumForCheckpoint")), 10000));
        restore.put("isRestore", toBoolean(firstPresent(restoreConfig.get("isRestore"), config.get("isRestore"), taskInfoConfig.get("isRestore"), true)));
        restore.put("restoreColumnName", stringValue(firstPresent(restoreConfig.get("restoreColumnName"), config.get("restoreColumnName"), taskInfoConfig.get("restoreColumnName")), ""));
        restore.put("restoreColumnIndex", toInt(firstPresent(restoreConfig.get("restoreColumnIndex"), config.get("restoreColumnIndex"), taskInfoConfig.get("restoreColumnIndex")), 0));
        setting.put("restore", restore);

        JSONObject log = new JSONObject();
        log.put("isLogger", toBoolean(firstPresent(logConfig.get("isLogger"), config.get("isLogger"), taskInfoConfig.get("isLogger"), true)));
        log.put("level", stringValue(firstPresent(logConfig.get("level"), config.get("logLevel"), taskInfoConfig.get("logLevel")), "info"));
        log.put("path", stringValue(firstPresent(logConfig.get("path"), config.get("logPath"), taskInfoConfig.get("logPath")), ""));
        log.put("pattern", stringValue(firstPresent(logConfig.get("pattern"), config.get("logPattern"), taskInfoConfig.get("logPattern")), ""));
        setting.put("log", log);

        return setting;
    }

    private static String resolveReaderName(DbType dbType) {
        return resolveReaderName(dbType, false);
    }

    private static String resolveReaderName(DbType dbType, boolean cdcReader) {
        if (dbType == null) {
            throw new DataQueryException("FlinkX reader 缺少数据库类型");
        }
        if (cdcReader) {
            switch (dbType) {
                case MYSQL:
                case MARIADB: return "mysqlcdcreader";
                case ORACLE:
                case ORACLE_12C: return "oraclelogminerreader";
                case SQL_SERVER:
                case SQL_SERVER2008: return "sqlservercdcreader";
                default: throw new DataQueryException("FlinkX CDC 暂不支持的 reader 数据库类型: " + dbType.getDb());
            }
        }
        switch (dbType) {
            case MYSQL:
            case MARIADB: return "mysqlreader";
            case ORACLE:
            case ORACLE_12C: return "oraclereader";
            case POSTGRE_SQL: return "postgresqlreader";
            case DORIS: return "dorisreader";
            case CLICK_HOUSE: return "clickhousereader";
            case HIVE: return "hivereader";
            case MONGODB: return "mongodbreader";
            case ELASTICSEARCH: return "elasticsearch7reader";
            case SQL_SERVER:
            case SQL_SERVER2008: return "sqlserverreader";
            case DM8: return "dmreader";
            case KINGBASE8: return "kingbasereader";
            case DB2: return "db2reader";
            case KAFKA: return "kafkareader";
            case RABBITMQ: return "rabbitmqreader";
            case REDIS: return "redisreader";
            case ROCKETMQ: return "rocketmqreader";
            case SOCKET: return "socketreader";
            case STREAM: return "streamreader";
            case HDFS: return "hdfsreader";
            case FTP: return "ftpreader";
            case OSS_ALIYUN: return "s3reader";
            default: throw new DataQueryException("FlinkX 不支持的 reader 数据库类型: " + dbType.getDb());
        }
    }

    private static String resolveWriterName(DbType dbType) {
        if (dbType == null) {
            throw new DataQueryException("FlinkX writer 缺少数据库类型");
        }
        switch (dbType) {
            case MYSQL:
            case MARIADB: return "mysqlwriter";
            case ORACLE:
            case ORACLE_12C: return "oraclewriter";
            case POSTGRE_SQL: return "postgresqlwriter";
            case DORIS: return "doriswriter";
            case CLICK_HOUSE: return "clickhousewriter";
            case HIVE: return "hivewriter";
            case MONGODB: return "mongodbwriter";
            case ELASTICSEARCH: return "elasticsearch7writer";
            case SQL_SERVER:
            case SQL_SERVER2008: return "sqlserverwriter";
            case DM8: return "dmwriter";
            case KINGBASE8: return "kingbasewriter";
            case DB2: return "db2writer";
            case KAFKA: return "kafkawriter";
            case RABBITMQ: return "rabbitmqwriter";
            case REDIS: return "rediswriter";
            case ROCKETMQ: return "rocketmqwriter";
            case SOCKET: return "socketwriter";
            case STREAM: return "streamwriter";
            case HDFS: return "hdfswriter";
            case FTP: return "ftpwriter";
            case OSS_ALIYUN: return "s3writer";
            default: throw new DataQueryException("FlinkX 不支持的 writer 数据库类型: " + dbType.getDb());
        }
    }

    private static DbType parseDbType(Map<String, Object> param) {
        String dbTypeStr = param != null ? (String) param.get("dbType") : null;
        if (dbTypeStr == null) {
            return null;
        }
        try {
            return DbType.getDbType(dbTypeStr);
        } catch (Exception e) {
            return null;
        }
    }

    private static boolean isMongo(DbType dbType) {
        return dbType == DbType.MONGODB;
    }

    private static boolean isElasticsearch(DbType dbType) {
        return dbType == DbType.ELASTICSEARCH;
    }

    private static boolean isKafka(DbType dbType) {
        return dbType == DbType.KAFKA;
    }

    private static boolean isMysqlLike(DbType dbType) {
        return dbType == DbType.MYSQL || dbType == DbType.MARIADB;
    }

    private static boolean isOracle(DbType dbType) {
        return dbType == DbType.ORACLE || dbType == DbType.ORACLE_12C;
    }

    private static boolean isSqlServer(DbType dbType) {
        return dbType == DbType.SQL_SERVER || dbType == DbType.SQL_SERVER2008;
    }

    private static boolean isStreamingMqReader(DbType dbType) {
        return dbType == DbType.RABBITMQ
                || dbType == DbType.REDIS
                || dbType == DbType.ROCKETMQ
                || dbType == DbType.SOCKET
                || dbType == DbType.STREAM;
    }

    private static String resolveWriteMode(String writeModeType, Object selectedColumns, DbType dbType) {
        if ("3".equals(writeModeType)) {
            if (selectedColumns instanceof List && !((List<?>) selectedColumns).isEmpty()) {
                String cols = String.join(",", (List<CharSequence>) (List<?>) selectedColumns);
                if (dbType == DbType.DM8) {
                    return "update-dm (" + cols + ")";
                }
                return "update (" + cols + ")";
            }
            return "update";
        }
        return "insert";
    }
}
