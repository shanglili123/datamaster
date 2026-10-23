package com.datamaster.module.collector.utils;

import com.alibaba.fastjson2.JSON;
import com.datamaster.module.collector.controller.admin.etl.vo.CollectorEtlNewNodeSaveReqVO;
import com.datamaster.module.collector.controller.admin.etl.vo.CollectorEtlNodeSaveReqVO;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

public class ComponentGraphCompilerTest {
    @Test
    public void compilesSixComponentsIntoThreeStagesWithoutChangingDraft() {
        CollectorEtlNewNodeSaveReqVO request = request();
        String original = request.getTaskDefinitionList();
        CollectorEtlNewNodeSaveReqVO compiled = ComponentGraphCompiler.compile(request);
        List<CollectorEtlNodeSaveReqVO> stages = JSON.parseArray(compiled.getTaskDefinitionList(), CollectorEtlNodeSaveReqVO.class);
        assertEquals(3, stages.size());
        assertEquals(original, request.getTaskDefinitionList());
        Map<String, Object> merge = pipeline(stages, "out-c");
        assertEquals("MERGE", merge.get("mode"));
        assertEquals(2, ((List<?>) merge.get("inputs")).size());
        Map<?, ?> draft = JSON.parseObject(compiled.getDraftJson(), Map.class);
        assertEquals(2, ((List<?>) ((Map<?, ?>) draft.get("pipelineGraph")).get("edges")).size());
    }

    @Test
    public void splitFirstUsesNullSafePreviousPredicates() {
        CollectorEtlNewNodeSaveReqVO request = request();
        List<CollectorEtlNodeSaveReqVO> nodes = new ArrayList<>(JSON.parseArray(request.getTaskDefinitionList(), CollectorEtlNodeSaveReqVO.class));
        CollectorEtlNodeSaveReqVO logic = nodes.get(4);
        Map<String, Object> config = map("mode", "SPLIT", "matchStrategy", "FIRST", "unmatchedPolicy", "DEFAULT",
                "branches", Arrays.asList(map("id", "high", "condition", "id > 10"), map("id", "fallback", "isDefault", true)));
        logic.getTaskParams().put("logicNode", config);
        nodes.remove(2); nodes.remove(2);
        nodes.add(node("out-high", "91", writer("high")));
        request.setTaskDefinitionList(JSON.toJSONString(nodes));
        Map<String, Object> graph = map("nodes", graphNodes(nodes), "edges", Arrays.asList(
                edge("in-a", "out-a"), edge("out-a", "logic"),
                map("source", "logic", "target", "out-high", "branchId", "high"),
                map("source", "logic", "target", "out-c", "branchId", "fallback")));
        request.setDraftJson(JSON.toJSONString(map("componentGraph", graph)));
        List<CollectorEtlNodeSaveReqVO> stages = JSON.parseArray(ComponentGraphCompiler.compile(request).getTaskDefinitionList(), CollectorEtlNodeSaveReqVO.class);
        assertEquals("NOT (COALESCE((id > 10), FALSE))", pipeline(stages, "out-c").get("rowFilter"));
    }

    @Test
    public void conditionCompilesAsControlStageWithBothBranches() {
        CollectorEtlNewNodeSaveReqVO request = request();
        List<CollectorEtlNodeSaveReqVO> nodes = new ArrayList<>(JSON.parseArray(request.getTaskDefinitionList(), CollectorEtlNodeSaveReqVO.class));
        nodes.get(4).getTaskParams().put("logicNode", map("mode", "CONDITION", "conditionRelation", "AND"));
        nodes.add(node("in-ok", "1", map("type", 1, "asset_id", "a1", "readerDatasource", map("datasourceId", "test"), "columns", Arrays.asList("id"))));
        nodes.add(node("in-fail", "1", map("type", 1, "asset_id", "b1", "readerDatasource", map("datasourceId", "test"), "columns", Arrays.asList("id"))));
        nodes.add(node("out-fail", "91", writer("failed")));
        request.setTaskDefinitionList(JSON.toJSONString(nodes));
        request.setDraftJson(JSON.toJSONString(map("componentGraph", map("nodes", graphNodes(nodes), "edges", Arrays.asList(
                edge("in-a", "out-a"), edge("in-b", "out-b"), edge("out-a", "logic"), edge("out-b", "logic"),
                map("source", "logic", "target", "in-ok", "branchId", "SUCCESS"),
                map("source", "logic", "target", "in-fail", "branchId", "FAILURE"), edge("in-ok", "out-c"), edge("in-fail", "out-fail")))));
        List<CollectorEtlNodeSaveReqVO> stages = JSON.parseArray(ComponentGraphCompiler.compile(request).getTaskDefinitionList(), CollectorEtlNodeSaveReqVO.class);
        assertEquals(5, stages.size());
        Map<String, Object> control = pipeline(stages, "logic");
        assertEquals("CONDITION", control.get("mode"));
        assertEquals(Arrays.asList("out-c"), control.get("successStages"));
        assertEquals(Arrays.asList("out-fail"), control.get("failedStages"));
    }

    @Test
    public void joinProjectsDifferentSchemasAndKeepsExplicitSourceOrder() {
        CollectorEtlNewNodeSaveReqVO request = request();
        List<CollectorEtlNodeSaveReqVO> nodes = JSON.parseArray(request.getTaskDefinitionList(), CollectorEtlNodeSaveReqVO.class);
        nodes.get(3).getTaskParams().put("target_columns", Arrays.asList("customer_id"));
        Map<String, Object> join = map("sources", Arrays.asList(map("nodeId", "out-b", "alias", "b"), map("nodeId", "out-a", "alias", "a")),
                "joins", Arrays.asList(map("rightNodeId", "out-a", "type", "LEFT", "relation", "AND", "conditions",
                        Arrays.asList(map("leftNodeId", "out-b", "leftColumn", "customer_id", "operator", "=", "rightColumn", "id")))),
                "fields", Arrays.asList(map("nodeId", "out-b", "column", "customer_id", "outputName", "joined_id")));
        nodes.get(4).getTaskParams().put("logicNode", map("mode", "MERGE", "mergeType", "JOIN", "joinConfig", join));
        nodes.get(5).getTaskParams().put("columns", Arrays.asList("joined_id"));
        request.setTaskDefinitionList(JSON.toJSONString(nodes));
        List<CollectorEtlNodeSaveReqVO> stages = JSON.parseArray(ComponentGraphCompiler.compile(request).getTaskDefinitionList(), CollectorEtlNodeSaveReqVO.class);
        Map<String, Object> pipeline = pipeline(stages, "out-c");
        assertEquals(Arrays.asList("joined_id"), pipeline.get("columns"));
        List<?> inputs = (List<?>) pipeline.get("inputs");
        assertEquals("out-b", ((Map<?, ?>) inputs.get(0)).get("refNodeId"));
        assertEquals("JOIN", ((Map<?, ?>) pipeline.get("mergeConfig")).get("type"));
        Map<?, ?> metadata = (Map<?, ?>) pipeline.get("sourceMetadata");
        assertEquals(false, metadata.get("includeNaturalKey"));
        assertFalse(metadata.containsKey("preservedMetadataColumns"));
    }

    @Test
    public void compilesFourSourceJoinIntoFiveDataStages() {
        CollectorEtlNewNodeSaveReqVO request = request();
        List<CollectorEtlNodeSaveReqVO> nodes = new ArrayList<>(JSON.parseArray(request.getTaskDefinitionList(), CollectorEtlNodeSaveReqVO.class));
        nodes.add(node("in-d", "1", map("type", 1, "asset_id", "d", "readerDatasource", map("datasourceId", "test"), "columns", Arrays.asList("id"))));
        nodes.add(node("out-d", "91", writer("d1")));
        nodes.add(node("in-e", "1", map("type", 1, "asset_id", "e", "readerDatasource", map("datasourceId", "test"), "columns", Arrays.asList("id"))));
        nodes.add(node("out-e", "91", writer("e1")));
        List<Map<String, Object>> sources = Arrays.asList(map("nodeId", "out-a", "alias", "a"), map("nodeId", "out-b", "alias", "b"),
                map("nodeId", "out-d", "alias", "d"), map("nodeId", "out-e", "alias", "e"));
        Map<String, Object> join = map("sources", sources,
                "joins", Arrays.asList(
                        map("rightNodeId", "out-b", "type", "LEFT", "relation", "AND", "conditions", Arrays.asList(map("leftNodeId", "out-a", "leftColumn", "id", "operator", "=", "rightColumn", "id"))),
                        map("rightNodeId", "out-d", "type", "INNER", "relation", "AND", "conditions", Arrays.asList(map("leftNodeId", "out-b", "leftColumn", "id", "operator", "=", "rightColumn", "id"))),
                        map("rightNodeId", "out-e", "type", "LEFT", "relation", "AND", "conditions", Arrays.asList(map("leftNodeId", "out-d", "leftColumn", "id", "operator", "=", "rightColumn", "id")))),
                "fields", Arrays.asList(map("nodeId", "out-e", "column", "id", "outputName", "id")));
        nodes.get(4).getTaskParams().put("logicNode", map("mode", "MERGE", "mergeType", "JOIN", "joinConfig", join));
        request.setTaskDefinitionList(JSON.toJSONString(nodes));
        request.setDraftJson(JSON.toJSONString(map("componentGraph", map("nodes", graphNodes(nodes), "edges", Arrays.asList(
                edge("in-a", "out-a"), edge("in-b", "out-b"), edge("in-d", "out-d"), edge("in-e", "out-e"),
                edge("out-a", "logic"), edge("out-b", "logic"), edge("out-d", "logic"), edge("out-e", "logic"), edge("logic", "out-c")))));
        List<CollectorEtlNodeSaveReqVO> stages = JSON.parseArray(ComponentGraphCompiler.compile(request).getTaskDefinitionList(), CollectorEtlNodeSaveReqVO.class);
        assertEquals(5, stages.size());
        assertEquals(4, ((List<?>) pipeline(stages, "out-c").get("inputs")).size());
    }

    private CollectorEtlNewNodeSaveReqVO request() {
        List<CollectorEtlNodeSaveReqVO> nodes = Arrays.asList(
                node("in-a", "1", map("type", 1, "asset_id", "a", "readerDatasource", map("datasourceId", "test"), "columns", Arrays.asList("id"))),
                node("out-a", "91", writer("a1")),
                node("in-b", "1", map("type", 1, "asset_id", "b", "readerDatasource", map("datasourceId", "test"), "columns", Arrays.asList("id"))),
                node("out-b", "91", writer("b1")),
                node("logic", "100", map("type", 3, "logicNode", map("mode", "MERGE", "mergeType", "UNION_ALL"))),
                node("out-c", "91", writer("c")));
        Map<String, Object> graph = map("nodes", graphNodes(nodes), "edges", Arrays.asList(
                edge("in-a", "out-a"), edge("in-b", "out-b"), edge("out-a", "logic"), edge("out-b", "logic"), edge("logic", "out-c")));
        CollectorEtlNewNodeSaveReqVO request = new CollectorEtlNewNodeSaveReqVO();
        request.setTaskDefinitionList(JSON.toJSONString(nodes)); request.setDraftJson(JSON.toJSONString(map("componentGraph", graph)));
        return request;
    }
    private Map<String, Object> writer(String table) {
        return map("type", 2, "target_table_name", table, "writerDatasource", map("datasourceId", "test"),
                "columns", Arrays.asList("id"), "target_columns", Arrays.asList("id"), "tableFields", Arrays.asList(map("columnName", "id")));
    }
    private CollectorEtlNodeSaveReqVO node(String code, String type, Map<String, Object> params) {
        CollectorEtlNodeSaveReqVO node = new CollectorEtlNodeSaveReqVO(); node.setCode(code); node.setName(code); node.setVersion(1);
        node.setComponentType(type); node.setTaskParams(params); return node;
    }
    private List<Map<String, Object>> graphNodes(List<CollectorEtlNodeSaveReqVO> nodes) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (CollectorEtlNodeSaveReqVO node : nodes) result.add(map("nodeId", node.getCode(), "nodeCode", node.getCode()));
        return result;
    }
    @SuppressWarnings("unchecked") private Map<String, Object> pipeline(List<CollectorEtlNodeSaveReqVO> stages, String id) {
        for (CollectorEtlNodeSaveReqVO node : stages) if (id.equals(node.getCode())) return (Map<String, Object>) node.getTaskParams().get("pipelineNode");
        throw new AssertionError("Missing stage " + id);
    }
    private Map<String, Object> edge(String a, String b) { return map("source", a, "target", b); }
    private Map<String, Object> map(Object... values) {
        Map<String, Object> result = new LinkedHashMap<>(); for (int i = 0; i < values.length; i += 2) result.put(String.valueOf(values[i]), values[i + 1]); return result;
    }
}
