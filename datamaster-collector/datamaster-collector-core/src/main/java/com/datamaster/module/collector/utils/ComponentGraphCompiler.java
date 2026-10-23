package com.datamaster.module.collector.utils;

import com.alibaba.fastjson2.JSON;
import com.datamaster.common.exception.ServiceException;
import com.datamaster.module.collector.controller.admin.etl.vo.CollectorEtlNewNodeSaveReqVO;
import com.datamaster.module.collector.controller.admin.etl.vo.CollectorEtlNodeSaveReqVO;

import java.util.*;

/** 将组件画布按物化输出边界编译成执行阶段，不修改保存的组件图。 */
public final class ComponentGraphCompiler {
    private final Map<String, CollectorEtlNodeSaveReqVO> nodes = new LinkedHashMap<>();
    private final Map<String, List<String>> parents = new HashMap<>();
    private final Map<String, Map<String, Object>> stages = new LinkedHashMap<>();
    private final Set<String> visiting = new HashSet<>();
    private final Map<String, Object> graph;

    private ComponentGraphCompiler(CollectorEtlNewNodeSaveReqVO request) {
        Map<String, Object> draft = JSON.parseObject(request.getDraftJson(), Map.class);
        graph = map(draft == null ? null : draft.get("componentGraph"));
        Map<String, String> aliases = new HashMap<>();
        for (Object item : list(graph.get("nodes"))) {
            Map<String, Object> node = map(item);
            aliases.put(text(node.get("nodeCode")), text(node.get("nodeId")));
        }
        for (CollectorEtlNodeSaveReqVO node : JSON.parseArray(request.getTaskDefinitionList(), CollectorEtlNodeSaveReqVO.class)) {
            if (node == null || node.getTaskParams() == null || text(node.getCode()).isEmpty()) fail("组件缺少标识或配置");
            String key = aliases.getOrDefault(node.getCode(), node.getCode());
            if (nodes.put(key, node) != null) fail("组件标识重复");
            parents.put(key, new ArrayList<>());
        }
        for (Object item : list(graph.get("edges"))) {
            Map<String, Object> edge = map(item);
            String source = text(edge.get("source")), target = text(edge.get("target"));
            if (!nodes.containsKey(source) || !nodes.containsKey(target) || source.equals(target)) fail("存在无效组件连线");
            if (parents.get(target).contains(source)) fail("组件连线重复");
            parents.get(target).add(source);
        }
        Set<String> done = new HashSet<>();
        for (String key : nodes.keySet()) checkCycle(key, new HashSet<>(), done);
    }

    public static boolean supports(CollectorEtlNewNodeSaveReqVO request) {
        if (request.getDraftJson() == null) return false;
        Map<String, Object> draft = JSON.parseObject(request.getDraftJson(), Map.class);
        return draft != null && draft.get("componentGraph") instanceof Map;
    }

    public static CollectorEtlNewNodeSaveReqVO compile(CollectorEtlNewNodeSaveReqVO request) {
        ComponentGraphCompiler compiler = new ComponentGraphCompiler(request);
        for (String key : compiler.nodes.keySet()) {
            if (compiler.isOutput(key)) compiler.stage(key);
        }
        for (String key : compiler.nodes.keySet()) {
            if (compiler.isLogic(key) && "CONDITION".equals(text(map(compiler.nodes.get(key).getTaskParams().get("logicNode")).get("mode")))) {
                compiler.control(key);
            }
        }
        if (compiler.stages.isEmpty()) fail("组件编排至少需要一个表输出");
        for (String key : compiler.nodes.keySet()) {
            boolean consumed = false;
            for (Map<String, Object> stage : compiler.stages.values()) {
                if (list(stage.get("componentIds")).contains(key)) consumed = true;
            }
            if (!consumed) fail("存在未连接到表输出的组件：" + compiler.nodes.get(key).getName());
        }
        CollectorEtlNewNodeSaveReqVO result = JSON.parseObject(JSON.toJSONString(request), CollectorEtlNewNodeSaveReqVO.class);
        List<CollectorEtlNodeSaveReqVO> definitions = new ArrayList<>();
        List<Map<String, Object>> graphNodes = new ArrayList<>(), edges = new ArrayList<>();
        for (Map.Entry<String, Map<String, Object>> entry : compiler.stages.entrySet()) {
            String key = entry.getKey();
            CollectorEtlNodeSaveReqVO node = new CollectorEtlNodeSaveReqVO();
            node.setName(compiler.nodes.get(key).getName()); node.setCode(key); node.setPipelineNodeId(key);
            node.setVersion(1); node.setComponentType("100"); node.setTaskType("PIPELINE_NODE");
            Map<String, Object> params = new LinkedHashMap<>(); params.put("pipelineNode", entry.getValue());
            node.setTaskParams(params); definitions.add(node);
            Map<String, Object> graphNode = new LinkedHashMap<>();
            graphNode.put("nodeId", key); graphNode.put("nodeCode", key); graphNode.put("config", entry.getValue());
            graphNodes.add(graphNode);
            for (Object raw : list(entry.getValue().get("inputs"))) {
                Map<String, Object> input = map(raw);
                if (input.containsKey("refNodeId")) {
                    Map<String, Object> edge = new LinkedHashMap<>(); edge.put("source", input.get("refNodeId")); edge.put("target", key); edges.add(edge);
                }
            }
            for (Object raw : list(entry.getValue().get("controlDependencies"))) {
                Map<String, Object> edge = new LinkedHashMap<>(); edge.put("source", raw); edge.put("target", key); edges.add(edge);
            }
        }
        Map<String, Object> draft = JSON.parseObject(request.getDraftJson(), Map.class);
        Map<String, Object> pipeline = new LinkedHashMap<>(); pipeline.put("nodes", graphNodes); pipeline.put("edges", edges);
        draft.put("pipelineGraph", pipeline);
        draft.put("componentSnapshot", JSON.parseObject(JSON.toJSONString(request), Map.class));
        result.setDraftJson(JSON.toJSONString(draft)); result.setTaskDefinitionList(JSON.toJSONString(definitions));
        return result;
    }

    private Map<String, Object> stage(String outputId) {
        if (stages.containsKey(outputId)) return stages.get(outputId);
        if (!visiting.add(outputId)) fail("阶段依赖存在环");
        CollectorEtlNodeSaveReqVO outputNode = nodes.get(outputId);
        List<String> path = new ArrayList<>(); path.add(outputId);
        List<CollectorEtlNodeSaveReqVO> transforms = new ArrayList<>();
        String current = onlyParent(outputId);
        while (!isInput(current) && !isLogic(current)) {
            if (!Arrays.asList("22", "34", "39", "40", "47", "48", "32", "35", "36").contains(nodes.get(current).getComponentType())) {
                fail("组件尚未支持阶段编译：" + nodes.get(current).getName());
            }
            if (isOutput(current)) fail("表输出之间必须通过逻辑节点引用");
            path.add(current); transforms.add(0, nodes.get(current)); current = onlyParent(current);
        }
        path.add(current);
        Map<String, Object> config = new LinkedHashMap<>();
        config.put("pipelineNodeId", outputId); config.put("mode", "INTEGRATION");
        config.put("output", new LinkedHashMap<>(outputNode.getTaskParams()));
        map(config.get("output")).put("table", first(outputNode.getTaskParams(), "target_table_name", "target_asset_id"));
        config.put("transforms", transforms); config.put("componentIds", path);
        Map<String, Object> reader;
        config.put("inputComponentCode", nodes.get(current).getCode());
        if (isInput(current)) {
            if (!"1".equals(nodes.get(current).getComponentType())) fail("多阶段编排目前只支持关系表输入，旧文件任务请保持原格式");
            for (String parent : parents.get(current)) {
                if (!isLogic(parent) || !"CONDITION".equals(text(map(nodes.get(parent).getTaskParams().get("logicNode")).get("mode")))) {
                    fail("表输入只允许接收条件节点的控制连线");
                }
            }
            config.put("controlDependencies", new ArrayList<>(parents.get(current)));
            reader = new LinkedHashMap<>(nodes.get(current).getTaskParams());
            if (text(reader.get("table_name")).isEmpty()) reader.put("table_name", reader.get("asset_id"));
            config.put("input", reader); config.put("readerDatasource", reader.get("readerDatasource"));
            config.put("inputs", Collections.singletonList(Collections.singletonMap("table", first(reader, "table_name", "asset_id"))));
            config.put("sourceMetadata", new LinkedHashMap<>(SourceMetadataSupport.config(reader)));
        } else {
            Map<String, Object> logic = map(nodes.get(current).getTaskParams().get("logicNode"));
            String mode = text(logic.get("mode"));
            List<String> upstreamIds = parents.get(current);
            if (upstreamIds.isEmpty()) fail("逻辑节点必须连接上游表输出");
            if (!"MERGE".equals(mode) && upstreamIds.size() != 1) fail("除合并外的逻辑节点只能有一个上游表输出");
            List<Map<String, Object>> inputs = new ArrayList<>();
            Map<String, Object> upstream = null;
            for (String upstreamId : upstreamIds) {
                if (!isOutput(upstreamId)) fail("逻辑节点的上游必须是表输出，不能直接连接输入或转换");
                upstream = stage(upstreamId);
                Map<String, Object> ref = new LinkedHashMap<>(); ref.put("kind", "NODE_OUTPUT"); ref.put("refNodeId", upstreamId); ref.put("refNodeCode", upstreamId);
                inputs.add(ref);
                Map<String, Object> sourceOutput = map(upstream.get("output"));
                if (inputs.size() == 1) {
                    config.put("upstreamBusinessColumns", sourceOutput.get("target_columns"));
                    config.put("upstreamFields", sourceOutput.get("outputFields"));
                }
            }
            if ("MERGE".equals(mode)) {
                if (inputs.size() < 2) fail("合并至少需要两个不同的上游输出");
                String mergeType = text(logic.getOrDefault("mergeType", "UNION_ALL"));
                if (!Arrays.asList("UNION_ALL", "JOIN").contains(mergeType)) fail("不支持的合并方式");
                config.put("mode", "MERGE"); config.put("mergeConfig", Collections.singletonMap("type", mergeType));
                if ("JOIN".equals(mergeType)) {
                    Map<String, Object> join = map(logic.get("joinConfig"));
                    List<Map<String, Object>> ordered = new ArrayList<>();
                    Set<String> selected = new HashSet<>();
                    for (Object raw : list(join.get("sources"))) {
                        Map<String, Object> source = map(raw);
                        String id = text(source.get("nodeId"));
                        if (!upstreamIds.contains(id) || !selected.add(id)) fail("JOIN 引用的上游已变更，请重新配置表顺序");
                        Map<String, Object> ref = new LinkedHashMap<>();
                        ref.put("kind", "NODE_OUTPUT"); ref.put("refNodeId", id); ref.put("refNodeCode", id);
                        ref.put("alias", source.get("alias")); ordered.add(ref);
                    }
                    if (selected.size() != upstreamIds.size()) fail("JOIN 必须配置全部上游表的别名和顺序");
                    inputs = ordered; config.put("joinConfig", join);
                    List<Object> names = new ArrayList<>(), joinFields = new ArrayList<>();
                    Set<String> uniqueNames = new HashSet<>();
                    for (Object raw : list(join.get("fields"))) {
                        Map<String, Object> projection = map(raw);
                        String id = text(projection.get("nodeId")), column = text(projection.get("column"));
                        String name = text(projection.get("outputName"));
                        if (!selected.contains(id) || name.isEmpty() || !uniqueNames.add(name.toLowerCase(Locale.ROOT))) fail("JOIN 输出字段名称为空、重复或来源无效");
                        Map<String, Object> sourceOutput = map(stages.get(id).get("output"));
                        if (!list(sourceOutput.get("target_columns")).contains(column)) fail("JOIN 引用了上游未写入字段：" + column);
                        Map<String, Object> field = new LinkedHashMap<>();
                        for (Object candidate : list(sourceOutput.get("outputFields"))) {
                            if (column.equals(text(map(candidate).get("columnName")))) field.putAll(map(candidate));
                        }
                        field.put("columnName", name); field.put("pkFlag", "0"); field.put("nullableFlag", "1");
                        names.add(name); joinFields.add(field);
                    }
                    if (names.isEmpty()) fail("JOIN 至少选择一个输出字段");
                    config.put("upstreamBusinessColumns", names); config.put("upstreamFields", joinFields);
                } else {
                    List<Object> expectedColumns = list(config.get("upstreamBusinessColumns"));
                    for (String upstreamId : upstreamIds) {
                        if (!expectedColumns.equals(list(map(stages.get(upstreamId).get("output")).get("target_columns")))) {
                            fail("UNION ALL 要求上游实际输出字段名称和顺序一致");
                        }
                    }
                }
            } else if (!Arrays.asList("REFERENCE", "SPLIT", "BROADCAST").contains(mode)) fail("不支持的逻辑节点模式：" + mode);
            config.put("inputs", inputs);
            Object sharedDatasource = null;
            for (String upstreamId : upstreamIds) {
                Map<String, Object> datasource = map(map(stages.get(upstreamId).get("output")).get("writerDatasource"));
                Object identity = first(datasource, "datasourceId", "id");
                if (text(identity).isEmpty()) fail("上游输出缺少数据源标识");
                if (sharedDatasource != null && !text(sharedDatasource).equals(text(identity))) fail("合并/JOIN 只支持相同数据源连接的上游落库表");
                sharedDatasource = identity;
            }
            config.put("readerDatasource", map(upstream.get("output")).get("writerDatasource"));
            reader = new LinkedHashMap<>(); config.put("input", reader);
            reader.put("readerDatasource", config.get("readerDatasource"));
            if (config.containsKey("joinConfig")) {
                // JOIN 不能沿用任一单表的自然键、行 Hash 或源端操作类型。
                Map<String, Object> metadata = new LinkedHashMap<>(); metadata.put("enabled", true);
                metadata.put("sourceTableColumn", "source_table"); metadata.put("writeTimeColumn", "write_time");
                metadata.put("includeNaturalKey", false);
                config.put("sourceMetadata", metadata);
            } else {
                config.put("sourceMetadata", new LinkedHashMap<>(map(upstream.get("sourceMetadata"))));
            }
            if ("SPLIT".equals(mode)) config.put("rowFilter", branchFilter(current, path, logic));
        }
        Object columns = reader.get("columns");
        if (columns == null || list(columns).isEmpty()) columns = config.get("upstreamBusinessColumns");
        if (columns == null || list(columns).isEmpty()) columns = outputNode.getTaskParams().get("columns");
        Object fields = reader.get("tableFields");
        if (fields == null || list(fields).isEmpty()) fields = config.get("upstreamFields");
        if ((fields == null || list(fields).isEmpty()) && config.containsKey("upstreamBusinessColumns")) {
            List<Map<String, Object>> materializedFields = new ArrayList<>();
            for (Object column : list(columns)) {
                Map<String, Object> field = new LinkedHashMap<>(); field.put("columnName", column);
                materializedFields.add(field);
            }
            fields = materializedFields;
        }
        if (fields == null || list(fields).isEmpty()) fields = outputNode.getTaskParams().get("tableFields");
        config.put("columns", columns); config.put("tableFields", fields);
        config.put("targetColumns", outputNode.getTaskParams().get("target_columns"));
        reader.put("columns", columns); reader.put("tableFields", fields);
        map(config.get("sourceMetadata")).put("enabled", true);
        if (isInput(current)) map(config.get("sourceMetadata")).putIfAbsent("sourceTaskValue", nodes.get(current).getCode());
        Map<String, Object> output = map(config.get("output"));
        if (list(output.get("columns")).isEmpty() || list(output.get("target_columns")).isEmpty()
                || list(output.get("columns")).size() != list(output.get("target_columns")).size()) {
            fail("表输出必须配置完整且等长的字段映射：" + outputNode.getName());
        }
        Object readMode = reader.get("readModeType");
        if (readMode != null && !"1".equals(text(readMode))) fail("组件阶段编排本次仅支持全量读取；增量/CDC任务请保留旧任务模式");
        Map<String, Object> schema = map(reader.get("schemaMonitoring"));
        if (Boolean.TRUE.equals(schema.get("enabled"))) fail("普通输入结构监控未接入执行，请关闭旧配置后发布");
        if (Boolean.TRUE.equals(map(map(config.get("output")).get("qualityConfig")).get("enabled"))
                && text(map(map(config.get("output")).get("qualityConfig")).get("taskId")).isEmpty()) {
            fail("阶段质量检查需绑定已有质量任务，不能只启用开关");
        }
        stages.put(outputId, config); visiting.remove(outputId);
        return config;
    }

    private void control(String id) {
        Map<String, Object> logic = map(nodes.get(id).getTaskParams().get("logicNode"));
        String relation = text(logic.getOrDefault("conditionRelation", "AND"));
        if (!Arrays.asList("AND", "OR").contains(relation) || parents.get(id).isEmpty()) fail("条件节点需要前置任务及 AND/OR 关系");
        List<String> dependencies = new ArrayList<>(), success = new ArrayList<>(), failed = new ArrayList<>();
        for (String parent : parents.get(id)) {
            if (!isOutput(parent)) fail("条件节点只能判断上游表输出任务的状态");
            dependencies.add(parent);
        }
        for (Object raw : list(graph.get("edges"))) {
            Map<String, Object> edge = map(raw);
            if (!id.equals(text(edge.get("source")))) continue;
            String target = text(edge.get("target"));
            if (!isInput(target)) fail("条件控制分支必须连接表输入，不传递数据字段");
            boolean found = false;
            for (Map.Entry<String, Map<String, Object>> stage : stages.entrySet()) {
                if (list(stage.getValue().get("componentIds")).contains(target)) {
                    String branch = text(edge.get("branchId"));
                    if ("SUCCESS".equals(branch)) success.add(stage.getKey());
                    else if ("FAILURE".equals(branch)) failed.add(stage.getKey());
                    else fail("条件连线必须选择成功或失败分支");
                    found = true;
                }
            }
            if (!found) fail("条件分支没有连接完整的输入输出阶段");
        }
        if (success.isEmpty() || failed.isEmpty()) fail("条件节点必须配置成功和失败两路，避免空分支的不确定行为");
        Set<String> overlap = new HashSet<>(success); overlap.retainAll(failed);
        if (!overlap.isEmpty()) fail("同一执行阶段不能同时属于成功和失败分支");
        Map<String, Object> config = new LinkedHashMap<>();
        config.put("mode", "CONDITION"); config.put("conditionRelation", relation);
        config.put("controlDependencies", dependencies); config.put("successStages", success); config.put("failedStages", failed);
        config.put("componentIds", Collections.singletonList(id)); config.put("pipelineNodeId", id);
        stages.put(id, config);
    }

    private String branchFilter(String logicId, List<String> path, Map<String, Object> config) {
        String next = path.get(path.size() - 2), branchId = "";
        for (Object raw : list(graph.get("edges"))) {
            Map<String, Object> edge = map(raw);
            if (logicId.equals(text(edge.get("source"))) && next.equals(text(edge.get("target")))) branchId = text(edge.get("branchId"));
        }
        String strategy = text(config.getOrDefault("matchStrategy", "ALL"));
        if (!Arrays.asList("ALL", "FIRST").contains(strategy)) fail("无效的分流匹配策略");
        List<String> previous = new ArrayList<>(), allConditions = new ArrayList<>();
        Map<String, Object> selected = null;
        Set<String> ids = new HashSet<>();
        int defaults = 0;
        String unmatched = text(config.getOrDefault("unmatchedPolicy", "DROP"));
        if (!Arrays.asList("DROP", "DEFAULT").contains(unmatched)) fail("无效的未匹配策略");
        boolean selectedSeen = false;
        for (Object raw : list(config.get("branches"))) {
            Map<String, Object> branch = map(raw);
            String id = text(branch.get("id")), condition = text(branch.get("condition"));
            boolean defaultBranch = Boolean.TRUE.equals(branch.get("isDefault"));
            if (id.isEmpty() || !ids.add(id)) fail("分支标识不能为空或重复");
            if (defaultBranch) defaults++;
            else {
                if (condition.isEmpty()) fail("每个条件分支必须填写 Flink SQL 条件");
                String expression = "COALESCE((" + condition + "), FALSE)";
                allConditions.add(expression);
                if (!selectedSeen && !id.equals(branchId)) previous.add(expression);
            }
            if (id.equals(branchId)) { selected = branch; selectedSeen = true; }
        }
        if (defaults > 1 || ("DEFAULT".equals(unmatched) && defaults != 1)) fail("默认分支配置不完整");
        Set<String> connected = new HashSet<>();
        for (Object raw : list(graph.get("edges"))) {
            Map<String, Object> edge = map(raw);
            if (logicId.equals(text(edge.get("source")))) connected.add(text(edge.get("branchId")));
        }
        for (Object raw : list(config.get("branches"))) {
            Map<String, Object> branch = map(raw);
            if (Boolean.TRUE.equals(branch.get("isDefault")) && "DROP".equals(unmatched)) continue;
            if (!connected.contains(text(branch.get("id")))) fail("分流存在未连接的分支，请连接输出或删除该分支");
        }
        if (selected == null) fail("分流连线未选择有效分支，请重新连接并选择分支");
        if (Boolean.TRUE.equals(selected.get("isDefault"))) {
            if (!"DEFAULT".equals(unmatched)) fail("DROP 策略不能连接默认分支");
            return allConditions.isEmpty() ? "TRUE" : "NOT (" + String.join(" OR ", allConditions) + ")";
        }
        String predicate = "COALESCE((" + text(selected.get("condition")) + "), FALSE)";
        if ("FIRST".equals(strategy) && !previous.isEmpty()) predicate += " AND NOT (" + String.join(" OR ", previous) + ")";
        return predicate;
    }

    private void checkCycle(String id, Set<String> stack, Set<String> done) {
        if (done.contains(id)) return;
        if (!stack.add(id)) fail("组件图不能形成环");
        for (String parent : parents.get(id)) checkCycle(parent, stack, done);
        stack.remove(id); done.add(id);
    }
    private String onlyParent(String key) {
        if (parents.get(key).size() != 1) fail("组件“" + nodes.get(key).getName() + "”必须且只能连接一个数据输入");
        return parents.get(key).get(0);
    }
    private boolean isOutput(String id) { return "91".equals(nodes.get(id).getComponentType()); }
    private boolean isInput(String id) { return "1".equals(text(nodes.get(id).getTaskParams().get("type"))); }
    private boolean isLogic(String id) { return "100".equals(nodes.get(id).getComponentType()) && nodes.get(id).getTaskParams().get("logicNode") instanceof Map; }
    @SuppressWarnings("unchecked") private static Map<String, Object> map(Object raw) { return raw instanceof Map ? (Map<String, Object>) raw : new LinkedHashMap<>(); }
    @SuppressWarnings("unchecked") private static List<Object> list(Object raw) { return raw instanceof List ? (List<Object>) raw : Collections.emptyList(); }
    private static String text(Object raw) { return raw == null ? "" : String.valueOf(raw).trim(); }
    private static Object first(Map<String, Object> params, String a, String b) { return text(params.get(a)).isEmpty() ? params.get(b) : params.get(a); }
    private static void fail(String message) { throw new ServiceException(message); }
}
