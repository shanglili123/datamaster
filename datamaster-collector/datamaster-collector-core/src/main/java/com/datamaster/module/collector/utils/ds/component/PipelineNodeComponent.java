package com.datamaster.module.collector.utils.ds.component;

import com.datamaster.common.enums.TaskComponentTypeEnum;
import com.datamaster.module.collector.utils.model.DsResource;

import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 前端逻辑集成节点的兼容载体。
 *
 * <p>逻辑节点不是一个可直接执行的 FlinkX reader/writer，先完整透传其配置，
 * 后续发布展开器再按 pipelineNodes 生成多个 DolphinScheduler 节点。</p>
 */
public class PipelineNodeComponent implements ComponentItem {

    @Override
    public Map<String, Object> parse2(String nodeCode, Integer nodeVersion,
                                      TaskComponentTypeEnum componentType,
                                      Map<String, Object> taskParams,
                                      String resourceUrl,
                                      List<DsResource> resourceList) {
        Map<String, Object> result = mapNode(nodeCode, nodeVersion, componentType);
        // 前端保存结构是 taskParams.pipelineNode；兼容早期草稿直接把 pipeline
        // 放在 taskParams 根节点的格式。发布阶段后续逻辑只消费 pipeline 配置本身。
        Map<String, Object> pipeline;
        if (taskParams != null && taskParams.get("pipelineNode") instanceof Map) {
            pipeline = new LinkedHashMap<>((Map<String, Object>) taskParams.get("pipelineNode"));
        } else {
            pipeline = taskParams == null
                    ? new LinkedHashMap<>() : new LinkedHashMap<>(taskParams);
        }
        // 逻辑集成节点的治理契约要求 source_table、write_time 必须落库。
        // 即使旧草稿漏了 enabled，也按新节点语义强制开启来源元数据。
        Map<String, Object> sourceMetadata = mapValue(pipeline.get("sourceMetadata"));
        if (sourceMetadata.isEmpty() && pipeline.get("input") instanceof Map
                && ((Map<?, ?>) pipeline.get("input")).get("sourceMetadata") instanceof Map) {
            sourceMetadata = new LinkedHashMap<>((Map<String, Object>)
                    ((Map<?, ?>) pipeline.get("input")).get("sourceMetadata"));
        }
        sourceMetadata.put("enabled", true);
        pipeline.put("sourceMetadata", sourceMetadata);
        result.put("parameter", pipeline);
        result.put("pipelineNode", pipeline);
        String mode = String.valueOf(pipeline.getOrDefault("mode", "INTEGRATION"));
        Map<String, Object> input = mapValue(pipeline.get("input"));
        Map<String, Object> output = mapValue(pipeline.get("output"));
        if (input.isEmpty()) input = pipeline;
        if (output.isEmpty()) output = pipeline;

        Map<String, Object> readerParams = new LinkedHashMap<>(input);
        readerParams.putIfAbsent("readerDatasource", pipeline.get("readerDatasource"));
        readerParams.put("sourceMetadata", pipeline.get("sourceMetadata"));
        readerParams.putIfAbsent("schemaMonitoring", pipeline.get("schemaMonitoring"));
        readerParams.putIfAbsent("table_name", input.get("table"));
        readerParams.putIfAbsent("table_name", pipeline.get("sourceTable"));
        readerParams.putIfAbsent("columns", pipeline.get("columns"));
        if ("MERGE".equalsIgnoreCase(mode)) {
            readerParams.put("columns", pipeline.get("columns"));
            List<Map<String, Object>> sourceTables = new ArrayList<>();
            Object inputs = pipeline.get("inputs");
            if (inputs instanceof List) {
                for (Object item : (List<?>) inputs) {
                    if (item instanceof Map) {
                        Map<String, Object> source = new LinkedHashMap<>((Map<String, Object>) item);
                        Object table = source.get("table");
                        if (table != null) source.put("table_name", table);
                        sourceTables.add(source);
                    }
                }
            }
            readerParams.put("sourceTables", sourceTables);
            readerParams.put("mergeConfig", pipeline.get("mergeConfig"));
            readerParams.put("joinConfig", pipeline.get("joinConfig"));
            readerParams.put("tableFields", pipeline.get("tableFields"));
            if (readerParams.get("readerDatasource") instanceof Map) {
                result.put("reader", new TableMergeComponent().parse2(nodeCode, nodeVersion,
                        TaskComponentTypeEnum.TABLE_MERGE, readerParams, resourceUrl, resourceList));
            }
        } else {
            if (readerParams.get("readerDatasource") instanceof Map) {
                result.put("reader", new DBReaderComponent().parse2(nodeCode, nodeVersion,
                        TaskComponentTypeEnum.DB_READER, readerParams, resourceUrl, resourceList));
            }
        }
        Map<String, Object> writerParams = new LinkedHashMap<>(output);
        writerParams.putIfAbsent("writerDatasource", pipeline.get("writerDatasource"));
        writerParams.put("sourceMetadata", pipeline.get("sourceMetadata"));
        writerParams.putIfAbsent("target_table_name", output.get("table"));
        writerParams.putIfAbsent("target_table_name", pipeline.get("targetTable"));
        writerParams.putIfAbsent("target_columns", output.get("target_columns"));
        writerParams.putIfAbsent("target_columns", pipeline.get("targetColumns"));
        writerParams.putIfAbsent("columns", pipeline.get("columns"));
        if (writerParams.get("writerDatasource") instanceof Map) {
            result.put("writer", new DBWriterComponent().parse2(nodeCode, nodeVersion,
                    TaskComponentTypeEnum.DB_WRITER, writerParams, resourceUrl, resourceList));
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> mapValue(Object value) {
        return value instanceof Map ? (Map<String, Object>) value : new LinkedHashMap<>();
    }

    @Override
    public String code() {
        return TaskComponentTypeEnum.PIPELINE_NODE.getCode();
    }
}
