package com.datamaster.module.governance.service.flow;

import com.datamaster.common.exception.ServiceException;
import com.datamaster.module.governance.api.flow.ISpaceFlowApi.*;
import java.util.*;

/** Fixed capability DAG; all configured prerequisites must dominate every consumer. */
public final class SpaceFlowRules {
    private SpaceFlowRules() { }
    private static final Set<String> ALLOWED = Set.of("METADATA", "ASSET", "QUALITY", "API", "ANALYSIS");
    public static Definition validate(Definition d) {
        if (d == null || d.getNodes() == null || d.getEdges() == null || d.getNodes().size() > 5 || d.getEdges().size() > 12)
            throw new ServiceException("流程定义无效或超限");
        Set<String> nodes = new HashSet<>(d.getNodes());
        if (nodes.contains(null) || nodes.size() != d.getNodes().size() || !ALLOWED.containsAll(nodes) || !nodes.containsAll(Set.of("METADATA", "API", "ANALYSIS")))
            throw new ServiceException("必须包含元数据、API和分析节点；只允许现有功能节点，不能使用数仓层或任意脚本");
        if (d.getLayout() != null) {
            if (d.getLayout().size() > 5 || !nodes.containsAll(d.getLayout().keySet())) throw new ServiceException("布局只能包含当前能力节点");
            for (Position position : d.getLayout().values()) {
                if (position == null || position.getX() == null || position.getY() == null || !Double.isFinite(position.getX()) || !Double.isFinite(position.getY())
                        || position.getX() < 0 || position.getX() > 695 || position.getY() < 0 || position.getY() > 232) throw new ServiceException("流程节点布局坐标无效或超限");
            }
        }
        Map<String, Set<String>> graph = new HashMap<>(); nodes.forEach(n -> graph.put(n, new HashSet<>()));
        for (Edge edge : d.getEdges()) {
            if (edge == null || !nodes.contains(edge.getFrom()) || !nodes.contains(edge.getTo()) || edge.getFrom().equals(edge.getTo())
                    || !graph.get(edge.getFrom()).add(edge.getTo()) || Set.of("API", "ANALYSIS").contains(edge.getFrom()))
                throw new ServiceException("连线重复、端点无效或消费节点不能有出边");
        }
        for (String node : nodes) if (reaches(graph, node, node, new HashSet<>(), true)) throw new ServiceException("流程不能循环");
        for (String consumer : List.of("API", "ANALYSIS")) {
            if (!reaches(graph, "METADATA", consumer, new HashSet<>(), false)) throw new ServiceException("API和分析必须连接元数据路径");
            for (String gate : List.of("ASSET", "QUALITY")) if (nodes.contains(gate)) {
                if (!reaches(graph, "METADATA", gate, new HashSet<>(), false) || !reaches(graph, gate, consumer, new HashSet<>(), false))
                    throw new ServiceException("已配置的资产和质量必须连接并约束API及分析");
                Map<String, Set<String>> without = new HashMap<>();
                graph.forEach((k,v) -> { Set<String> copy = new HashSet<>(v); copy.remove(gate); without.put(k, copy); });
                if (reaches(without, "METADATA", consumer, new HashSet<>(), false)) throw new ServiceException("存在绕过" + gate + "的路径");
            }
        }
        return d;
    }
    /** Deterministic, read-only publication diff, preserving gate order and detecting edge changes. */
    public static Map<String,Object> changes(Definition published, Definition draft) {
        validate(draft);
        if (published != null) validate(published);
        Set<String> before = published == null ? Set.of() : new LinkedHashSet<>(published.getNodes());
        Set<String> after = new LinkedHashSet<>(draft.getNodes());
        List<String> added = new ArrayList<>(), removed = new ArrayList<>();
        for (String gate : List.of("ASSET", "QUALITY")) {
            if (after.contains(gate) && !before.contains(gate)) added.add(gate);
            if (before.contains(gate) && !after.contains(gate)) removed.add(gate);
        }
        Set<String> oldEdges = edgeKeys(published), newEdges = edgeKeys(draft);
        Map<String,Object> result = new LinkedHashMap<>();
        result.put("firstPublication", published == null);
        result.put("addedRequirements", added); result.put("removedRequirements", removed);
        result.put("definitionChanged", published == null || !before.equals(after) || !oldEdges.equals(newEdges));
        result.put("addedEdges", newEdges.stream().filter(edge -> !oldEdges.contains(edge)).toList());
        result.put("removedEdges", oldEdges.stream().filter(edge -> !newEdges.contains(edge)).toList());
        return result;
    }
    private static Set<String> edgeKeys(Definition definition) {
        Set<String> edges = new TreeSet<>();
        if (definition != null) for (Edge edge : definition.getEdges()) edges.add(edge.getFrom() + " → " + edge.getTo());
        return edges;
    }

    private static boolean reaches(Map<String, Set<String>> graph, String from, String to, Set<String> seen, boolean nonzero) {
        if (!nonzero && from.equals(to)) return true;
        if (!seen.add(from)) return false;
        for (String child : graph.getOrDefault(from, Set.of())) if (child.equals(to) || reaches(graph, child, to, seen, false)) return true;
        return false;
    }
}
