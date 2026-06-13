package com.datamaster.flinkx.core;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.datamaster.common.database.constants.DbType;
import com.datamaster.common.database.exception.DataQueryException;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class FlinkxEtlTaskConverter {

    public static String convertToFlinkxJobJson(Map<String, Object> mainArgs) {
        Map<String, Object> readerMap = (Map<String, Object>) mainArgs.get("reader");
        Map<String, Object> writerMap = (Map<String, Object>) mainArgs.get("writer");
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
        if (readerMap != null || writerMap != null) {
            JSONObject contentItem = new JSONObject();
            if (readerMap != null) {
                if (isExcelOrCsvReader(readerMap)) {
                    contentItem.put("reader", buildFileReader(readerMap));
                } else {
                    contentItem.put("reader", buildReader(readerMap));
                }
            }
            if (writerMap != null) {
                contentItem.put("writer", buildWriter(writerMap));
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
            rp.put("column", param.get("column"));
            rp.put("splitPk", param.getOrDefault("splitPk", ""));

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

            Object targetColumns = param.get("target_column");
            wp.put("column", targetColumns != null ? targetColumns : param.get("column"));

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
        mp.put("column", firstPresent(param.get("target_column"), param.get("column")));
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
        ep.put("column", firstPresent(param.get("target_column"), param.get("column")));
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
            rp.put("column", param.get("column"));
            rp.put("fieldDelimiter", ",");
        }
        reader.put("parameter", rp);
        return reader;
    }

    // ========================================================================
    // transformSql generation
    // ========================================================================

    private static String buildTransformSql(List<Map<String, Object>> transitionList,
                                            Map<String, Object> readerMap) {
        List<String> originalCols = extractColumns(readerMap);
        Map<String, String> colExprs = new LinkedHashMap<>();
        if (originalCols.isEmpty()) {
            colExprs.put("*", "*");
        } else {
            for (String col : originalCols) {
                colExprs.put(col, quoteId(col));
            }
        }

        List<String> orderByClauses = new ArrayList<>();
        List<String> dedupPartitions = new ArrayList<>();
        List<String> whereClauses = new ArrayList<>();
        boolean hasDedup = false;

        for (Map<String, Object> transition : transitionList) {
            String componentType = (String) transition.get("componentType");
            Map<String, Object> param = (Map<String, Object>) transition.get("parameter");
            if (param == null) continue;

            switch (componentType) {
                case "VALUE_MAP":
                    applyValueMap(colExprs, param);
                    break;
                case "FIELD_DERIVATION":
                    applyFieldDerivation(colExprs, param);
                    break;
                case "DATA_DEDUPLICATION":
                    hasDedup = true;
                    applyDataDeduplication(colExprs, dedupPartitions, param);
                    break;
                case "SELECT_FIELDS":
                    applySelectFields(colExprs, param);
                    break;
                case "ADD_CONSTANT":
                    applyAddConstant(colExprs, param);
                    break;
                case "SORT_RECORD":
                    applySortRecord(orderByClauses, param);
                    break;
                case "SPARK_CLEAN":
                    applyClean(colExprs, whereClauses, param);
                    break;
            }
        }

        if (colExprs.isEmpty()) return null;

        String whereSql = whereClauses.isEmpty() ? "" :
                " WHERE " + whereClauses.stream().collect(Collectors.joining(" AND "));

        if (hasDedup && !dedupPartitions.isEmpty()) {
            StringBuilder sql = new StringBuilder();
            sql.append("SELECT ");
            List<String> outerItems = new ArrayList<>();
            for (Map.Entry<String, String> e : colExprs.entrySet()) {
                String k = e.getKey();
                outerItems.add(quoteId(k));
            }
            sql.append(String.join(", ", outerItems));
            sql.append(" FROM (SELECT ");
            List<String> innerItems = new ArrayList<>();
            for (Map.Entry<String, String> e : colExprs.entrySet()) {
                innerItems.add(e.getValue() + " AS " + quoteId(e.getKey()));
            }
            sql.append(String.join(", ", innerItems));
            sql.append(", ROW_NUMBER() OVER (PARTITION BY ");
            sql.append(String.join(", ", dedupPartitions));
            sql.append(") AS \"__rn\" FROM source");
            sql.append(whereSql);
            sql.append(") WHERE \"__rn\" = 1");
            if (!orderByClauses.isEmpty()) {
                sql.append(" ORDER BY ").append(String.join(", ", orderByClauses));
            }
            return sql.toString();
        }

        StringBuilder sql = new StringBuilder("SELECT ");
        List<String> selectItems = new ArrayList<>();
        for (Map.Entry<String, String> e : colExprs.entrySet()) {
            String k = e.getKey();
            String v = e.getValue();
            if ("*".equals(k) && "*".equals(v)) {
                selectItems.add("*");
            } else if (v.equals(quoteId(k)) || v.equals(k)) {
                selectItems.add(quoteId(k));
            } else {
                selectItems.add(v + " AS " + quoteId(k));
            }
        }
        sql.append(String.join(", ", selectItems));
        sql.append(" FROM source");
        sql.append(whereSql);
        if (!orderByClauses.isEmpty()) {
            sql.append(" ORDER BY ").append(String.join(", ", orderByClauses));
        }
        return sql.toString();
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

    private static String quoteId(String name) {
        if (name == null || name.isEmpty() || "*".equals(name)) return name;
        if (name.startsWith("\"") && name.endsWith("\"")) return name;
        if (name.startsWith("`") && name.endsWith("`")) return name;
        return "\"" + name.replace("\"", "\"\"") + "\"";
    }

    // ========================================================================
    // VALUE_MAP
    // ========================================================================

    @SuppressWarnings("unchecked")
    private static void applyValueMap(Map<String, String> colExprs, Map<String, Object> param) {
        String inputField = (String) param.get("inputField");
        String outputField = (String) param.get("outputField");
        String defaultValue = (String) param.get("defaultValue");
        List<Map<String, Object>> tableFields = (List<Map<String, Object>>) param.get("tableFields");
        if (inputField == null || outputField == null || tableFields == null || tableFields.isEmpty()) return;

        StringBuilder caseExpr = new StringBuilder("CASE ");
        appendQuoted(caseExpr, inputField);
        for (Map<String, Object> mapping : tableFields) {
            String source = (String) mapping.get("source");
            String target = (String) mapping.get("target");
            if (source != null && target != null) {
                caseExpr.append(" WHEN '").append(escapeSql(source)).append("' THEN '").append(escapeSql(target)).append("'");
            }
        }
        if (defaultValue != null) {
            caseExpr.append(" ELSE '").append(escapeSql(defaultValue)).append("'");
        } else {
            caseExpr.append(" ELSE ").append(quoteId(inputField));
        }
        caseExpr.append(" END");
        colExprs.put(outputField, caseExpr.toString());
    }

    // ========================================================================
    // FIELD_DERIVATION
    // ========================================================================

    @SuppressWarnings("unchecked")
    private static void applyFieldDerivation(Map<String, String> colExprs, Map<String, Object> param) {
        String type = (String) param.get("fieldDerivationType");
        if (type == null) return;
        switch (type) {
            case "FIELD_DERIVE_CONCAT":
                applyFieldConcat(colExprs, param);
                break;
            case "FIELD_DERIVE_SUBSTRING":
                applyFieldSubstring(colExprs, param);
                break;
        }
    }

    @SuppressWarnings("unchecked")
    private static void applyFieldConcat(Map<String, String> colExprs, Map<String, Object> param) {
        String fieldDerivationName = (String) param.get("fieldDerivationName");
        String delimiter = (String) param.get("delimiter");
        if (delimiter == null) delimiter = "";
        String prefix = (String) param.get("fieldDerivationPrefix");
        if (prefix == null) prefix = "";
        String suffix = (String) param.get("fieldDerivationSuffix");
        if (suffix == null) suffix = "";
        List<Map<String, Object>> tableFields = (List<Map<String, Object>>) param.get("tableFields");
        if (fieldDerivationName == null || tableFields == null || tableFields.isEmpty()) return;

        List<String> colParts = new ArrayList<>();
        for (Map<String, Object> field : tableFields) {
            String colName = (String) field.get("columnName");
            if (colName != null) {
                colParts.add("COALESCE(CAST(" + quoteId(colName) + " AS VARCHAR), 'null')");
            }
        }
        if (colParts.isEmpty()) return;

        StringBuilder expr = new StringBuilder();
        expr.append("CONCAT(");
        List<String> concatParts = new ArrayList<>();
        if (!prefix.isEmpty()) {
            concatParts.add("'" + escapeSql(prefix) + "'");
        }
        if (delimiter.isEmpty()) {
            concatParts.addAll(colParts);
        } else {
            StringBuilder ws = new StringBuilder("CONCAT_WS('");
            ws.append(escapeSql(delimiter)).append("'");
            for (String p : colParts) {
                ws.append(", ").append(p);
            }
            ws.append(")");
            concatParts.add(ws.toString());
        }
        if (!suffix.isEmpty()) {
            concatParts.add("'" + escapeSql(suffix) + "'");
        }
        expr.append(String.join(", ", concatParts));
        expr.append(")");
        colExprs.put(fieldDerivationName, expr.toString());
    }

    @SuppressWarnings("unchecked")
    private static void applyFieldSubstring(Map<String, String> colExprs, Map<String, Object> param) {
        String fieldDerivationName = (String) param.get("fieldDerivationName");
        String direction = (String) param.get("direction");
        Integer startIndex = param.get("startIndex") instanceof Number
                ? ((Number) param.get("startIndex")).intValue() : null;
        Integer endIndex = param.get("endIndex") instanceof Number
                ? ((Number) param.get("endIndex")).intValue() : null;
        List<Map<String, Object>> tableFields = (List<Map<String, Object>>) param.get("tableFields");
        if (fieldDerivationName == null || tableFields == null || tableFields.isEmpty()) return;
        String columnName = (String) tableFields.get(0).get("columnName");
        if (columnName == null || startIndex == null) return;

        StringBuilder expr = new StringBuilder();
        if ("2".equalsIgnoreCase(direction)) {
            if (endIndex != null) {
                int length = endIndex - startIndex;
                expr.append("SUBSTRING(").append(quoteId(columnName))
                        .append(", LENGTH(").append(quoteId(columnName)).append(") - ").append(startIndex).append(" + 1, ")
                        .append(length).append(")");
            } else {
                expr.append("SUBSTRING(").append(quoteId(columnName))
                        .append(", LENGTH(").append(quoteId(columnName)).append(") - ").append(startIndex).append(" + 1)");
            }
        } else {
            if (endIndex != null) {
                int length = endIndex - startIndex;
                expr.append("SUBSTRING(").append(quoteId(columnName))
                        .append(", ").append(startIndex + 1).append(", ").append(length).append(")");
            } else {
                expr.append("SUBSTRING(").append(quoteId(columnName))
                        .append(", ").append(startIndex + 1).append(")");
            }
        }
        colExprs.put(fieldDerivationName, expr.toString());
    }

    // ========================================================================
    // DATA_DEDUPLICATION
    // ========================================================================

    @SuppressWarnings("unchecked")
    private static void applyDataDeduplication(Map<String, String> colExprs,
                                                List<String> dedupPartitions,
                                                Map<String, Object> param) {
        List<Map<String, Object>> tableFields = (List<Map<String, Object>>) param.get("tableFields");
        if (tableFields == null || tableFields.isEmpty()) return;
        for (Map<String, Object> field : tableFields) {
            String columnName = (String) field.get("columnName");
            String ignoreCase = (String) field.get("ignoreCase");
            if (columnName == null) continue;
            if ("2".equals(ignoreCase)) {
                dedupPartitions.add("LOWER(" + quoteId(columnName) + ")");
            } else {
                dedupPartitions.add(quoteId(columnName));
            }
        }
    }

    // ========================================================================
    // SELECT_FIELDS
    // ========================================================================

    @SuppressWarnings("unchecked")
    private static void applySelectFields(Map<String, String> colExprs, Map<String, Object> param) {
        List<Map<String, Object>> tableFields = (List<Map<String, Object>>) param.get("tableFields");
        List<Map<String, Object>> removeFields = (List<Map<String, Object>>) param.get("removeFields");
        if (tableFields != null) {
            for (Map<String, Object> field : tableFields) {
                String inputField = (String) field.get("inputField");
                String outputField = (String) field.get("outputField");
                String type = (String) field.get("type");
                Integer length = field.get("length") instanceof Number
                        ? ((Number) field.get("length")).intValue() : null;
                Integer precision = field.get("precision") instanceof Number
                        ? ((Number) field.get("precision")).intValue() : null;
                if (outputField == null) continue;
                if (inputField == null) continue;

                String expr = quoteId(inputField);
                if (type != null) {
                    switch (type.toLowerCase()) {
                        case "string":
                            expr = "CAST(" + expr + " AS VARCHAR)";
                            if (length != null && length > 0) {
                                expr = "SUBSTRING(" + expr + ", 1, " + length + ")";
                            }
                            break;
                        case "int":
                        case "integer":
                            expr = "CAST(" + expr + " AS INTEGER)";
                            if (length != null && length > 0) {
                                int maxVal = (int) Math.pow(10, length) - 1;
                                expr = "CASE WHEN " + expr + " > " + maxVal + " THEN " + maxVal
                                        + " WHEN " + expr + " < " + (-maxVal) + " THEN " + (-maxVal)
                                        + " ELSE " + expr + " END";
                            }
                            break;
                        case "long":
                            expr = "CAST(" + expr + " AS BIGINT)";
                            if (length != null && length > 0) {
                                long maxVal = (long) Math.pow(10, length) - 1;
                                expr = "CASE WHEN " + expr + " > " + maxVal + " THEN " + maxVal
                                        + " WHEN " + expr + " < " + (-maxVal) + " THEN " + (-maxVal)
                                        + " ELSE " + expr + " END";
                            }
                            break;
                        case "double":
                            expr = "CAST(" + expr + " AS DOUBLE)";
                            if (precision != null && precision >= 0) {
                                expr = "ROUND(" + expr + ", " + precision + ")";
                            }
                            break;
                        case "boolean":
                            expr = "CAST(" + expr + " AS BOOLEAN)";
                            break;
                        case "date":
                            expr = "CAST(" + expr + " AS DATE)";
                            break;
                        case "timestamp":
                            expr = "CAST(" + expr + " AS TIMESTAMP)";
                            break;
                    }
                }
                if (precision != null && precision >= 0 && type != null && type.equalsIgnoreCase("double")) {
                    expr = "ROUND(" + expr + ", " + precision + ")";
                }
                if (length != null && length > 0 && type != null && type.equalsIgnoreCase("string")) {
                    expr = "SUBSTRING(" + expr + ", 1, " + length + ")";
                }
                colExprs.put(outputField, expr);
            }
        }
        if (removeFields != null) {
            for (Map<String, Object> rf : removeFields) {
                String fn = (String) rf.get("inputField");
                if (fn != null) {
                    colExprs.remove(fn);
                }
            }
        }
    }

    // ========================================================================
    // ADD_CONSTANT
    // ========================================================================

    @SuppressWarnings("unchecked")
    private static void applyAddConstant(Map<String, String> colExprs, Map<String, Object> param) {
        List<Map<String, Object>> tableFields = (List<Map<String, Object>>) param.get("tableFields");
        if (tableFields == null) return;
        for (Map<String, Object> field : tableFields) {
            String name = (String) field.get("name");
            String type = (String) field.get("type");
            String defaultValue = (String) field.get("defaultValue");
            Boolean emptyString = field.get("emptyString") instanceof Boolean
                    ? (Boolean) field.get("emptyString") : false;
            if (name == null || colExprs.containsKey(name)) continue;

            if (Boolean.TRUE.equals(emptyString)) {
                colExprs.put(name, "''");
            } else if (defaultValue == null) {
                colExprs.put(name, "NULL");
            } else {
                switch (type != null ? type.toLowerCase() : "string") {
                    case "int":
                    case "integer":
                    case "long":
                        colExprs.put(name, defaultValue);
                        break;
                    case "double":
                        colExprs.put(name, defaultValue);
                        break;
                    case "boolean":
                        colExprs.put(name, defaultValue);
                        break;
                    case "date":
                        colExprs.put(name, "CAST('" + escapeSql(defaultValue) + "' AS TIMESTAMP)");
                        break;
                    default:
                        colExprs.put(name, "'" + escapeSql(defaultValue) + "'");
                        break;
                }
            }
        }
    }

    // ========================================================================
    // SORT_RECORD
    // ========================================================================

    @SuppressWarnings("unchecked")
    private static void applySortRecord(List<String> orderByClauses, Map<String, Object> param) {
        List<Map<String, Object>> tableFields = (List<Map<String, Object>>) param.get("tableFields");
        if (tableFields == null) return;
        for (Map<String, Object> field : tableFields) {
            String columnName = (String) field.get("columnName");
            String order = (String) field.get("order");
            Boolean caseSensitive = field.get("caseSensitive") instanceof Boolean
                    ? (Boolean) field.get("caseSensitive") : false;
            if (columnName == null) continue;

            StringBuilder sortExpr = new StringBuilder();
            if (!Boolean.TRUE.equals(caseSensitive)) {
                sortExpr.append("LOWER(").append(quoteId(columnName)).append(")");
            } else {
                sortExpr.append(quoteId(columnName));
            }
            if ("desc".equalsIgnoreCase(order)) {
                sortExpr.append(" DESC");
            } else {
                sortExpr.append(" ASC");
            }
            orderByClauses.add(sortExpr.toString());
        }
    }

    // ========================================================================
    // SPARK_CLEAN
    // ========================================================================

    @SuppressWarnings("unchecked")
    private static void applyClean(Map<String, String> colExprs,
                                   List<String> whereClauses,
                                   Map<String, Object> param) {
        String where = (String) param.get("where");
        if (StringUtils.isNotBlank(where)) {
            whereClauses.add(where);
        }
        List<Map<String, Object>> tableFieldList = (List<Map<String, Object>>) param.get("tableFields");
        if (tableFieldList == null) return;
        for (Map<String, Object> rule : tableFieldList) {
            String ruleType = (String) rule.get("ruleType");
            String ruleConfigStr = (String) rule.get("ruleConfig");
            if (ruleConfigStr == null) continue;
            JSONObject cfg;
            try {
                cfg = JSONObject.parseObject(ruleConfigStr);
            } catch (Exception e) {
                continue;
            }
            String whereClause = (String) rule.get("whereClause");
            if (StringUtils.isNotBlank(whereClause)) {
                whereClauses.add(whereClause);
            }
            switch (ruleType) {
                case "WITHIN_BOUNDARY":
                    applyCleanBoundary(colExprs, cfg);
                    break;
                case "REMOVE_WHITESPACE":
                    applyCleanTrim(colExprs, cfg);
                    break;
                case "SIMPLE_REPLACE":
                    applyCleanRegexReplace(colExprs, cfg);
                    break;
                case "REMOVE_EMPTY_COMBINATION":
                    applyCleanDeleteNullCombination(colExprs, whereClauses, cfg);
                    break;
                case "TO_UPPERCASE":
                    applyCleanCase(colExprs, cfg, true);
                    break;
                case "TO_LOWERCASE":
                    applyCleanCase(colExprs, cfg, false);
                    break;
                case "ADD_PREFIX_SUFFIX":
                    applyCleanPrefixSuffix(colExprs, cfg);
                    break;
                case "MENU_CUSTOM":
                    applyCleanEnumMap(colExprs, cfg);
                    break;
                case "KEEP_LATEST_OR_FIRST":
                    applyCleanDedupKeepFirst(colExprs, cfg);
                    break;
                case "CHECK_EXPIRATION":
                    applyCleanExpiration(colExprs, whereClauses, cfg);
                    break;
                case "FIX_TO_PRECISION":
                    applyCleanRound(colExprs, cfg);
                    break;
                case "DATE_FORMAT_STD":
                    applyCleanDateFormat(colExprs, cfg);
                    break;
                case "STRING_SUBSTR":
                    applyCleanSubstr(colExprs, cfg);
                    break;
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static List<String> cleanColumns(JSONObject cfg) {
        List<String> cols = new ArrayList<>();
        if (cfg.containsKey("columns")) {
            JSONArray arr = cfg.getJSONArray("columns");
            if (arr != null) {
                for (int i = 0; i < arr.size(); i++) {
                    cols.add(arr.getString(i));
                }
            }
        } else if (cfg.containsKey("columnName")) {
            cols.add(cfg.getString("columnName"));
        }
        return cols;
    }

    private static void applyCleanBoundary(Map<String, String> colExprs, JSONObject cfg) {
        List<String> cols = cleanColumns(cfg);
        if (cols.isEmpty()) return;
        String col = cols.get(0);
        double min = cfg.getDoubleValue("min");
        double max = cfg.getDoubleValue("max");
        int handleType = cfg.getIntValue("handleType");
        String colExpr = colExprs.getOrDefault(col, quoteId(col));
        String expr;
        switch (handleType) {
            case 1:
                expr = "CASE WHEN " + colExpr + " < " + min + " THEN " + min
                        + " WHEN " + colExpr + " > " + max + " THEN " + max
                        + " ELSE " + colExpr + " END";
                break;
            case 2:
                expr = "CASE WHEN " + colExpr + " < " + min + " OR " + colExpr + " > " + max
                        + " THEN " + min + " ELSE " + colExpr + " END";
                break;
            case 3:
                expr = "CASE WHEN " + colExpr + " < " + min + " OR " + colExpr + " > " + max
                        + " THEN " + max + " ELSE " + colExpr + " END";
                break;
            default:
                return;
        }
        colExprs.put(col, expr);
    }

    private static void applyCleanTrim(Map<String, String> colExprs, JSONObject cfg) {
        List<String> cols = cleanColumns(cfg);
        if (cols.isEmpty()) return;
        String col = cols.get(0);
        int handleType = cfg.getIntValue("handleType");
        String colExpr = colExprs.getOrDefault(col, quoteId(col));
        if (handleType == 1) {
            colExprs.put(col, "TRIM(" + colExpr + ")");
        } else if (handleType == 2) {
            colExprs.put(col, "REGEXP_REPLACE(" + colExpr + ", '\\\\s+', '')");
        }
    }

    private static void applyCleanRegexReplace(Map<String, String> colExprs, JSONObject cfg) {
        List<String> cols = cleanColumns(cfg);
        if (cols.isEmpty()) return;
        String col = cols.get(0);
        String regex = cfg.getString("regex");
        String replacement = cfg.getString("replacement");
        if (regex == null) return;
        if (replacement == null) replacement = "";
        String colExpr = colExprs.getOrDefault(col, quoteId(col));
        colExprs.put(col, "REGEXP_REPLACE(" + colExpr + ", '" + escapeSql(regex) + "', '" + escapeSql(replacement) + "')");
    }

    private static void applyCleanDeleteNullCombination(Map<String, String> colExprs,
                                                        List<String> whereClauses,
                                                        JSONObject cfg) {
        List<String> cols = cleanColumns(cfg);
        if (cols.isEmpty()) return;
        List<String> nullChecks = new ArrayList<>();
        for (String c : cols) {
            String colExpr = colExprs.getOrDefault(c, quoteId(c));
            nullChecks.add("(" + colExpr + " IS NULL OR TRIM(CAST(" + colExpr + " AS VARCHAR)) = '')");
        }
        whereClauses.add("NOT (" + String.join(" AND ", nullChecks) + ")");
    }

    private static void applyCleanCase(Map<String, String> colExprs, JSONObject cfg, boolean upper) {
        List<String> cols = cleanColumns(cfg);
        if (cols.isEmpty()) return;
        String col = cols.get(0);
        String colExpr = colExprs.getOrDefault(col, quoteId(col));
        colExprs.put(col, (upper ? "UPPER" : "LOWER") + "(" + colExpr + ")");
    }

    @SuppressWarnings("unchecked")
    private static void applyCleanPrefixSuffix(Map<String, String> colExprs, JSONObject cfg) {
        List<String> cols = cleanColumns(cfg);
        if (cols.isEmpty()) return;
        String col = cols.get(0);
        String stringValue = cfg.getString("stringValue");
        String handleType = cfg.getString("handleType");
        if (stringValue == null || handleType == null) return;
        String colExpr = colExprs.getOrDefault(col, quoteId(col));
        String sv = escapeSql(stringValue);

        switch (handleType) {
            case "1":
                colExprs.put(col, "CASE WHEN " + colExpr + " IS NULL THEN NULL"
                        + " WHEN " + colExpr + " LIKE '" + sv + "%' THEN " + colExpr
                        + " ELSE CONCAT('" + sv + "', " + colExpr + ") END");
                break;
            case "2":
                colExprs.put(col, "CASE WHEN " + colExpr + " IS NULL THEN NULL"
                        + " WHEN " + colExpr + " LIKE '%" + sv + "' THEN " + colExpr
                        + " ELSE CONCAT(" + colExpr + ", '" + sv + "') END");
                break;
            case "3":
                colExprs.put(col, "CASE WHEN " + colExpr + " IS NULL THEN NULL"
                        + " WHEN " + colExpr + " LIKE '" + sv + "%'"
                        + " THEN SUBSTRING(" + colExpr + ", " + (stringValue.length() + 1) + ")"
                        + " ELSE " + colExpr + " END");
                break;
            case "4":
                colExprs.put(col, "CASE WHEN " + colExpr + " IS NULL THEN NULL"
                        + " WHEN " + colExpr + " LIKE '%" + sv + "'"
                        + " THEN SUBSTRING(" + colExpr + ", 1, LENGTH(" + colExpr + ") - " + stringValue.length() + ")"
                        + " ELSE " + colExpr + " END");
                break;
        }
    }

    @SuppressWarnings("unchecked")
    private static void applyCleanEnumMap(Map<String, String> colExprs, JSONObject cfg) {
        List<String> cols = cleanColumns(cfg);
        if (cols.isEmpty()) return;
        String col = cols.get(0);
        List<Map<String, Object>> mappings = (List<Map<String, Object>>) cfg.get("stringValue");
        if (mappings == null || mappings.isEmpty()) return;
        String colExpr = colExprs.getOrDefault(col, quoteId(col));
        StringBuilder expr = new StringBuilder("CASE");
        for (Map<String, Object> m : mappings) {
            String val = (String) m.get("value");
            String name = (String) m.get("name");
            if (val != null && name != null) {
                expr.append(" WHEN ").append(colExpr).append(" = '").append(escapeSql(val)).append("' THEN '").append(escapeSql(name)).append("'");
            }
        }
        expr.append(" ELSE ").append(colExpr).append(" END");
        colExprs.put(col, expr.toString());
    }

    @SuppressWarnings("unchecked")
    private static void applyCleanDedupKeepFirst(Map<String, String> colExprs, JSONObject cfg) {
        // KEEP_LATEST_OR_FIRST is a dedup operation within CleanTransition
        // We'll use ROW_NUMBER for this as well, but since it's configured differently,
        // we don't modify colExprs here - this would need to be handled at the SQL level
        // which is complex. For now, we skip it as it overlaps with DATA_DEDUPLICATION.
    }

    @SuppressWarnings("unchecked")
    private static void applyCleanExpiration(Map<String, String> colExprs,
                                             List<String> whereClauses,
                                             JSONObject cfg) {
        List<String> cols = cleanColumns(cfg);
        if (cols.isEmpty()) return;
        String col = cols.get(0);
        Integer handleType = cfg.getInteger("handleType");
        String handleColumns = cfg.getString("handleColumns");
        String handleValue = cfg.getString("handleValue");
        Integer dataRange = cfg.getInteger("dataRange");
        Integer dataRangeType = cfg.getInteger("dataRangeType");
        Integer dataRangeValue = cfg.getInteger("dataRangeValue");

        String colExpr = colExprs.getOrDefault(col, quoteId(col));
        String dateExpr = "DATE_FORMAT(" + colExpr + ", 'yyyy-MM-dd')";

        String condition;
        if (dataRange != null && dataRange == 1) {
            if (dataRangeType != null && dataRangeType == 1) {
                condition = dateExpr + " > CAST('" + cfg.getString("dataRangeValue") + "' AS DATE)";
            } else {
                condition = dateExpr + " < CAST('" + cfg.getString("dataRangeValue") + "' AS DATE)";
            }
        } else {
            if (dataRangeType != null) {
                switch (dataRangeType) {
                    case 1:
                        condition = dateExpr + " < DATEADD(DAY, -" + (dataRangeValue != null ? dataRangeValue : 30) + ", CURRENT_DATE)";
                        break;
                    case 2:
                        condition = dateExpr + " < DATEADD(MONTH, -" + (dataRangeValue != null ? dataRangeValue : 1) + ", CURRENT_DATE)";
                        break;
                    case 3:
                        condition = dateExpr + " < DATEADD(YEAR, -" + (dataRangeValue != null ? dataRangeValue : 1) + ", CURRENT_DATE)";
                        break;
                    default:
                        return;
                }
            } else {
                return;
            }
        }

        if (handleType != null && handleType == 0 && handleColumns != null) {
            String hcExpr = colExprs.getOrDefault(handleColumns, quoteId(handleColumns));
            String setVal = handleValue != null ? "'" + escapeSql(handleValue) + "'" : "NULL";
            colExprs.put(handleColumns, "CASE WHEN " + condition + " THEN " + setVal + " ELSE " + hcExpr + " END");
        } else {
            whereClauses.add("NOT (" + condition + ")");
        }
    }

    private static void applyCleanRound(Map<String, String> colExprs, JSONObject cfg) {
        List<String> cols = cleanColumns(cfg);
        if (cols.isEmpty()) return;
        String col = cols.get(0);
        Integer precision = cfg.getInteger("stringValue");
        if (precision == null) return;
        String colExpr = colExprs.getOrDefault(col, quoteId(col));
        String expr = "ROUND(" + colExpr + ", " + precision + ")";
        if (precision == 0) {
            expr = "CAST(" + expr + " AS INTEGER)";
        }
        colExprs.put(col, expr);
    }

    @SuppressWarnings("unchecked")
    private static void applyCleanDateFormat(Map<String, String> colExprs, JSONObject cfg) {
        List<String> cols = cleanColumns(cfg);
        if (cols.isEmpty()) return;
        String col = cols.get(0);
        String targetFormat = cfg.getString("targetFormat");
        JSONArray inputFormats = cfg.getJSONArray("inputFormats");
        if (targetFormat == null) return;

        String colExpr = colExprs.getOrDefault(col, quoteId(col));
        // Build a COALESCE chain of TO_TIMESTAMP attempts
        StringBuilder tsExpr = new StringBuilder();
        if (inputFormats != null && !inputFormats.isEmpty()) {
            for (int i = 0; i < inputFormats.size(); i++) {
                String fmt = inputFormats.getString(i);
                String attempt;
                if ("timestamp".equalsIgnoreCase(fmt)) {
                    attempt = "TO_TIMESTAMP(FROM_UNIXTIME(CAST(REGEXP_REPLACE(CAST(" + colExpr + " AS VARCHAR), '[^0-9]', '') AS DOUBLE) / 1000.0))";
                } else {
                    attempt = "TO_TIMESTAMP(CAST(" + colExpr + " AS VARCHAR), '" + escapeSql(fmt) + "')";
                }
                if (tsExpr.length() == 0) {
                    tsExpr.append(attempt);
                } else {
                    tsExpr.insert(0, "COALESCE(");
                    tsExpr.append(", ").append(attempt).append(")");
                }
            }
        }
        if (tsExpr.length() == 0) return;

        String resultExpr;
        String tl = targetFormat.toLowerCase();
        if ("date".equals(tl)) {
            resultExpr = "CAST(" + tsExpr.toString() + " AS DATE)";
        } else if ("timestamp".equals(tl)) {
            resultExpr = tsExpr.toString();
        } else {
            resultExpr = "DATE_FORMAT(" + tsExpr.toString() + ", '" + escapeSql(targetFormat) + "')";
        }
        colExprs.put(col, "CASE WHEN " + tsExpr.toString() + " IS NOT NULL THEN " + resultExpr
                + " ELSE CAST(" + colExpr + " AS VARCHAR) END");
    }

    private static void applyCleanSubstr(Map<String, String> colExprs, JSONObject cfg) {
        List<String> cols = cleanColumns(cfg);
        if (cols.isEmpty()) return;
        String col = cols.get(0);
        Integer maxLength = cfg.getInteger("maxLength");
        if (maxLength == null) return;
        String direction = cfg.getString("direction");
        if (direction == null) direction = "1";
        String colExpr = colExprs.getOrDefault(col, quoteId(col));
        String expr;
        if ("2".equals(direction)) {
            expr = "CASE WHEN LENGTH(" + colExpr + ") > " + maxLength
                    + " THEN SUBSTRING(" + colExpr + ", LENGTH(" + colExpr + ") - " + (maxLength - 1) + ", " + maxLength + ")"
                    + " ELSE " + colExpr + " END";
        } else {
            expr = "CASE WHEN LENGTH(" + colExpr + ") > " + maxLength
                    + " THEN SUBSTRING(" + colExpr + ", 1, " + maxLength + ")"
                    + " ELSE " + colExpr + " END";
        }
        colExprs.put(col, expr);
    }

    // ========================================================================
    // SQL helpers
    // ========================================================================

    private static void appendQuoted(StringBuilder sb, String name) {
        sb.append(quoteId(name));
    }

    private static String escapeSql(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("'", "''");
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
