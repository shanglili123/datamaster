package com.datamaster.module.assets.controller.admin.asset;

import com.alibaba.fastjson.JSONObject;
import com.datamaster.metadata.api.lineage.dto.LineageEdgeDTO;
import com.datamaster.metadata.api.lineage.dto.LineageGraphDTO;
import com.datamaster.metadata.api.lineage.dto.LineageNodeDTO;
import com.datamaster.neo4j.dto.LineageDTO;
import com.datamaster.neo4j.node.TableNode;
import com.datamaster.neo4j.node.TaskNode;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

/** Response-only compatibility. Never queries or writes the legacy Neo4j store. */
final class LegacyLineageAdapter {
    private LegacyLineageAdapter() { }

    static LineageDTO adapt(LineageGraphDTO graph) {
        LineageDTO result = new LineageDTO();
        result.setTables(new ArrayList<>());
        result.setTasks(new ArrayList<>());
        result.setRels(new ArrayList<>());
        Map<String, Long> ids = new LinkedHashMap<>();
        if (graph == null || graph.getNodes() == null) return result;
        for (LineageNodeDTO node : graph.getNodes()) {
            if (!"TABLE".equals(node.getType()) && !"TASK".equals(node.getType())) continue;
            Long id = (long) ids.size() + 1;
            ids.put(node.getId(), id);
            if ("TABLE".equals(node.getType())) {
                TableNode table = TableNode.builder().id(id).name(node.getName()).tableName(node.getName()).build();
                result.getTables().add(table);
                if (node.getId().equals(graph.getCenterId())) result.setCurrentTable(table);
            } else {
                result.getTasks().add(TaskNode.builder().id(id).taskId(node.getTaskId()).name(node.getName()).build());
            }
        }
        if (graph.getEdges() == null) return result;
        for (LineageEdgeDTO edge : graph.getEdges()) {
            if (!ids.containsKey(edge.getSource()) || !ids.containsKey(edge.getTarget())) continue;
            JSONObject relation = new JSONObject();
            relation.put("startNodeId", ids.get(edge.getSource()));
            relation.put("endNodeId", ids.get(edge.getTarget()));
            relation.put("type", "READ".equals(edge.getKind()) ? "TABLE_TO_TASK" : "TASK_TO_TABLE");
            JSONObject properties = new JSONObject();
            properties.put("taskId", edge.getTaskId());
            relation.put("properties", properties);
            result.getRels().add(relation);
        }
        return result;
    }
}
