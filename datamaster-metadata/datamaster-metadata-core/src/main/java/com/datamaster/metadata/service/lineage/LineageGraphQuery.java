package com.datamaster.metadata.service.lineage;

import com.datamaster.metadata.api.lineage.dto.*;
import com.datamaster.metadata.dal.dataobject.lineage.LineageRows.*;
import com.datamaster.metadata.dal.dataobject.metadata.CatalogTableDO;
import com.datamaster.metadata.dal.mapper.lineage.LineageMapper;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import javax.annotation.Resource;
import java.util.*;

/** Bounded breadth-first traversal over table facts; table-task-table is one hop. */
@Component
public class LineageGraphQuery {
    private static final int MAX_NODES = 200, MAX_EDGES = 500, MAX_CANDIDATES = 2000;
    private static final long MAX_NANOS = 1500000000L;
    @Resource private LineageMapper mapper;
    @Resource private LineageGuard guard;
    @Resource private LineageIdentity identity;

    public LineageGraphDTO empty(String status, Integer depth) {
        LineageGraphDTO graph = new LineageGraphDTO();
        graph.setStatus(status); graph.setDepth(depth == null ? 1 : depth);
        graph.setNodes(new ArrayList<>()); graph.setEdges(new ArrayList<>());
        graph.setDiagnostics(new ArrayList<>()); graph.setTruncated(false);
        return graph;
    }

    public LineageGraphDTO query(LineageQueryReqDTO request) { return query(request, false); }

    public LineageGraphDTO query(LineageQueryReqDTO request, boolean assetMode) {
        int depth = request.getDepth() == null ? 1 : request.getDepth();
        if (depth < 1 || depth > 5) throw new IllegalArgumentException("INVALID_LINEAGE_DEPTH");
        String direction = request.getDirection() == null ? "BOTH" : request.getDirection().toUpperCase(Locale.ROOT);
        String granularity = request.getGranularity() == null ? "TABLE" : request.getGranularity().toUpperCase(Locale.ROOT);
        if (!Arrays.asList("BOTH", "UPSTREAM", "DOWNSTREAM").contains(direction)
                || !Arrays.asList("TABLE", "FIELD").contains(granularity)) throw new IllegalArgumentException("INVALID_LINEAGE_QUERY");
        LineageGraphDTO graph = empty("EMPTY", depth);
        if (request.getObjectId() == null || !request.getObjectId().matches("obj:[a-f0-9]{64}")) throw new IllegalArgumentException("INVALID_LINEAGE_CENTER");
        ObjectRow center = mapper.object(request.getObjectId());
        LineageGuard.Visible centerVisible = guard.visible(center, request, assetMode);
        if (centerVisible == null) throw new AccessDeniedException("LINEAGE_ACCESS_DENIED");
        graph.setCenterId(center.getId());
        LinkedHashMap<String, LineageNodeDTO> nodes = new LinkedHashMap<>();
        LinkedHashMap<String, LineageEdgeDTO> edges = new LinkedHashMap<>();
        Map<String, LineageGuard.Visible> allowed = new HashMap<>();
        allowed.put(center.getId(), centerVisible);
        nodes.put(center.getId(), tableNode(center, centerVisible));
        LinkedHashSet<String> frontier = new LinkedHashSet<>(); frontier.add(center.getId());
        Set<String> visited = new HashSet<>(); visited.add(center.getId());
        Set<String> seenFacts = new HashSet<>();
        LinkedHashSet<String> diagnostics = new LinkedHashSet<>();
        long started = System.nanoTime();
        int scanned = 0;
        boolean truncated = false;
        outer: for (int hop = 0; hop < depth && !frontier.isEmpty(); hop++) {
            if (System.nanoTime() - started > MAX_NANOS) { truncated = true; break; }
            int remaining = MAX_CANDIDATES - scanned;
            if (remaining <= 0) { truncated = true; break; }
            List<EdgeRow> facts = mapper.adjacent(frontier, request.getSpaceId(), request.getSpaceCode(), direction,
                    Boolean.TRUE.equals(request.getIncludeInactive()), remaining + 1);
            if (facts.size() > remaining) { facts = facts.subList(0, remaining); truncated = true; }
            scanned += facts.size();
            LinkedHashSet<String> objectIds = new LinkedHashSet<>();
            for (EdgeRow fact : facts) {
                objectIds.add(fact.getSourceId()); objectIds.add(fact.getTargetId());
                if ("FIELD".equals(granularity)) {
                    if (fact.getSourceColumnId() != null) objectIds.add(fact.getSourceColumnId());
                    if (fact.getTargetColumnId() != null) objectIds.add(fact.getTargetColumnId());
                }
            }
            Map<String, ObjectRow> objects = new HashMap<>();
            if (!objectIds.isEmpty()) for (ObjectRow object : mapper.objects(objectIds)) objects.put(object.getId(), object);
            LinkedHashSet<String> next = new LinkedHashSet<>();
            for (EdgeRow fact : facts) {
                if (System.nanoTime() - started > MAX_NANOS) { truncated = true; break outer; }
                if (!seenFacts.add(fact.getId())) continue;
                ObjectRow source = objects.get(fact.getSourceId()), target = objects.get(fact.getTargetId());
                LineageGuard.Visible sourceMetadata = authorize(source, request, allowed, assetMode);
                LineageGuard.Visible targetMetadata = authorize(target, request, allowed, assetMode);
                // Never add or enqueue either endpoint through an unauthorized bridge, even at a later hop.
                if (sourceMetadata == null || targetMetadata == null) continue;
                LinkedHashMap<String, LineageNodeDTO> addNodes = new LinkedHashMap<>();
                LinkedHashMap<String, LineageEdgeDTO> addEdges = new LinkedHashMap<>();
                addNodes.put(source.getId(), tableNode(source, sourceMetadata));
                addNodes.put(target.getId(), tableNode(target, targetMetadata));
                String route = identity.routeId(fact.getTaskId(), fact.getUnitKey(), fact.getRouteKey() + "|" + target.getId());
                LineageNodeDTO task = new LineageNodeDTO();
                task.setId(route); task.setType("TASK"); task.setName("配置任务 " + fact.getTaskId());
                task.setTaskId(fact.getTaskId()); task.setSpaceId(request.getSpaceId());
                task.setStatus(fact.getStatus()); task.setStale(Boolean.TRUE.equals(fact.getStale()));
                addNodes.put(route, task);
                String readId = "edge:" + LineageIdentity.hash(source.getId() + "|" + route + "|READ");
                String writeId = "edge:" + LineageIdentity.hash(route + "|" + target.getId() + "|WRITE");
                addEdges.put(readId, edge(readId, source.getId(), route, "READ", fact));
                addEdges.put(writeId, edge(writeId, route, target.getId(), "WRITE", fact));
                if ("FIELD".equals(granularity) && fact.getSourceColumnId() != null && fact.getTargetColumnId() != null) {
                    ObjectRow sourceColumn = objects.get(fact.getSourceColumnId()), targetColumn = objects.get(fact.getTargetColumnId());
                    if (sourceColumn != null && targetColumn != null
                            && source.getId().equals(sourceColumn.getParentId()) && target.getId().equals(targetColumn.getParentId())
                            && guard.columnVisible(sourceColumn, sourceMetadata, request, assetMode)
                            && guard.columnVisible(targetColumn, targetMetadata, request, assetMode)) {
                        addNodes.put(sourceColumn.getId(), columnNode(sourceColumn, sourceMetadata));
                        addNodes.put(targetColumn.getId(), columnNode(targetColumn, targetMetadata));
                        addEdges.put(fact.getId(), edge(fact.getId(), sourceColumn.getId(), targetColumn.getId(), "FIELD", fact));
                    } else {
                        diagnostics.add("FIELD_VISIBILITY_UNRESOLVED");
                    }
                }
                if (nodes.size() + newKeys(nodes, addNodes) > MAX_NODES || edges.size() + newKeys(edges, addEdges) > MAX_EDGES) {
                    truncated = true; break outer;
                }
                for (LineageNodeDTO node : addNodes.values()) {
                    LineageNodeDTO old = nodes.get(node.getId());
                    if (old == null) nodes.put(node.getId(), node);
                    if (Boolean.TRUE.equals(fact.getStale())) nodes.get(node.getId()).setStale(true);
                }
                for (LineageEdgeDTO edge : addEdges.values()) {
                    LineageEdgeDTO old = edges.get(edge.getId());
                    if (old == null) edges.put(edge.getId(), edge);
                    else if (Boolean.TRUE.equals(edge.getStale())) old.setStale(true);
                }
                if (fact.getErrorCode() != null && !fact.getErrorCode().isEmpty()) {
                    for (String code : JdbcLineageService.safeCodes(fact.getErrorCode()).split(",")) if (!code.isEmpty()) diagnostics.add(code);
                }
                if (!"UPSTREAM".equals(direction) && frontier.contains(source.getId()) && visited.add(target.getId())) next.add(target.getId());
                if (!"DOWNSTREAM".equals(direction) && frontier.contains(target.getId()) && visited.add(source.getId())) next.add(source.getId());
            }
            frontier = next;
            if (truncated) break;
        }
        List<ProducerRow> centerStates = mapper.diagnostics(Collections.singleton(center.getId()), request.getSpaceId(),
                request.getSpaceCode(), Boolean.TRUE.equals(request.getIncludeInactive()));
        for (ProducerRow state : centerStates) {
            if (Boolean.TRUE.equals(state.getStale())) {
                nodes.get(center.getId()).setStale(true);
                nodes.get(center.getId()).setStatus(state.getStatus());
            }
            if (state.getErrorCode() != null) for (String code : JdbcLineageService.safeCodes(state.getErrorCode()).split(",")) {
                if (!code.isEmpty()) diagnostics.add(code);
            }
        }
        graph.setNodes(new ArrayList<>(nodes.values())); graph.setEdges(new ArrayList<>(edges.values()));
        graph.setDiagnostics(new ArrayList<>(diagnostics)); graph.setTruncated(truncated);
        graph.setStatus(truncated ? "TRUNCATED" : edges.isEmpty() ? "EMPTY" : "OK");
        if (truncated) graph.getDiagnostics().add("QUERY_LIMIT_REACHED");
        return graph;
    }

    private LineageGuard.Visible authorize(ObjectRow object, LineageQueryReqDTO request, Map<String, LineageGuard.Visible> allowed, boolean assetMode) {
        if (object == null) return null;
        if (!allowed.containsKey(object.getId())) allowed.put(object.getId(), guard.visible(object, request, assetMode));
        return allowed.get(object.getId());
    }

    private static LineageNodeDTO tableNode(ObjectRow object, LineageGuard.Visible visible) {
        CatalogTableDO table = visible.table;
        LineageNodeDTO node = new LineageNodeDTO();
        node.setId(object.getId()); node.setType("TABLE"); node.setName(table.getTableName());
        node.setMetadataId(table.getId()); node.setAssetId(visible.assetId); node.setDatasourceId(table.getDatasourceId()); node.setSpaceId(table.getSpaceId());
        node.setStatus("CURRENT"); node.setStale(false); return node;
    }

    private static LineageNodeDTO columnNode(ObjectRow object, LineageGuard.Visible visible) {
        CatalogTableDO table = visible.table;
        LineageNodeDTO node = new LineageNodeDTO();
        node.setId(object.getId()); node.setType("COLUMN"); node.setName(object.getColumnName()); node.setColumnName(object.getColumnName());
        node.setParentId(object.getParentId()); node.setMetadataId(table.getId()); node.setAssetId(visible.assetId); node.setDatasourceId(table.getDatasourceId());
        node.setSpaceId(table.getSpaceId()); node.setStatus("CURRENT"); node.setStale(false); return node;
    }

    private static LineageEdgeDTO edge(String id, String source, String target, String kind, EdgeRow fact) {
        LineageEdgeDTO edge = new LineageEdgeDTO();
        edge.setId(id); edge.setSource(source); edge.setTarget(target); edge.setKind(kind); edge.setConfidence(fact.getConfidence());
        edge.setUnitKey("unit:" + LineageIdentity.hash(fact.getUnitKey()));
        edge.setRouteKey("route:" + LineageIdentity.hash(fact.getRouteKey())); edge.setStatus(fact.getStatus());
        edge.setTaskId(fact.getTaskId()); edge.setStale(Boolean.TRUE.equals(fact.getStale())); return edge;
    }

    private static int newKeys(Map<String, ?> existing, Map<String, ?> additions) {
        int count = 0; for (String key : additions.keySet()) if (!existing.containsKey(key)) count++; return count;
    }
}
