package com.datamaster.metadata.service.lineage;

import com.datamaster.metadata.api.lineage.dto.*;
import com.datamaster.metadata.api.service.lineage.LineageApiService;
import com.datamaster.metadata.dal.dataobject.lineage.LineageRows.*;
import com.datamaster.metadata.dal.dataobject.metadata.CatalogTableDO;
import com.datamaster.metadata.dal.mapper.lineage.LineageMapper;
import com.datamaster.module.assets.dal.dataobject.asset.AssetsAssetDO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import javax.annotation.Resource;
import java.util.*;

@Service
public class JdbcLineageService implements LineageApiService {
    private static final int MAX_SNAPSHOT_EDGES = 10000;
    @Value("${datamaster.lineage.jdbc-enabled:false}") private boolean enabled;
    @Resource private LineageMapper mapper;
    @Resource private LineageExtractor extractor;
    @Resource private LineageIdentity identity;
    @Resource private LineageGuard guard;
    @Resource private LineageGraphQuery graphQuery;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LineageSyncResultDTO sync(LineageSnapshotDTO snapshot) {
        if (!enabled) return result("DISABLED", null, snapshot == null ? null : snapshot.getSequence());
        validate(snapshot);
        ProducerRow seed = new ProducerRow();
        seed.setId(identity.taskId(snapshot.getTaskId()));
        seed.setTaskId(snapshot.getTaskId());
        seed.setSpaceId(snapshot.getSpaceId());
        seed.setSpaceCode(snapshot.getSpaceCode());
        mapper.ensureProducer(seed);
        // Row lock serializes comparison, history, edge replacement and tombstone atomically.
        ProducerRow producer = mapper.lockProducer(seed.getId());
        if (!Objects.equals(producer.getSpaceId(), snapshot.getSpaceId()) || !Objects.equals(producer.getSpaceCode(), snapshot.getSpaceCode())) {
            throw new IllegalArgumentException("PRODUCER_SPACE_MISMATCH");
        }
        if (Boolean.TRUE.equals(producer.getDeleted()) || snapshot.getSequence() <= producer.getSequence()) {
            return result("IGNORED", null, producer.getSequence());
        }
        String event = snapshot.getEventType();
        producer.setSequence(snapshot.getSequence());
        producer.setVersion(safeText(snapshot.getVersion(), 120));
        producer.setTaskName(safeText(snapshot.getTaskName(), 200));
        producer.setTaskType(safeText(snapshot.getTaskType(), 80));
        producer.setTaskCode(safeText(snapshot.getTaskCode(), 120));
        if (!"INACTIVE".equals(event)) producer.setErrorCode(null);
        rememberObjects(producer, snapshot);
        if ("DELETE".equals(event)) {
            mapper.clearEdges(producer.getId());
            producer.setDeleted(true);
            producer.setActive(false);
            producer.setStale(false);
            producer.setStatus("DELETE");
        } else if ("INACTIVE".equals(event)) {
            // Keep lastgood facts for explicitly requested inactive inspection; default reads exclude them.
            producer.setActive(false);
            producer.setStatus("INACTIVE");
        } else {
            LineageExtractionDTO extraction = extract(snapshot);
            producer.setActive(true);
            producer.setStatus(extraction.getStatus());
            producer.setStale("FAILED".equals(extraction.getStatus()) || "PARTIAL".equals(extraction.getStatus()));
            producer.setErrorCode(safeCodes(extraction.getErrorCode()));
            if (!"FAILED".equals(extraction.getStatus())) {
                List<LineageRelationDTO> relations = extraction.getRelations() == null ? Collections.emptyList() : extraction.getRelations();
                if (relations.size() > MAX_SNAPSHOT_EDGES) throw new IllegalArgumentException("SNAPSHOT_TOO_LARGE");
                // COMPLETE empty clears old facts. PARTIAL also replaces with only newly proved facts.
                mapper.clearEdges(producer.getId());
                for (LineageRelationDTO relation : relations) persist(producer, relation);
                producer.setLastgoodSequence(snapshot.getSequence());
            }
            // FAILED advances observed version but preserves lastgood edges, shown stale by the read join.
        }
        mapper.updateProducer(producer);
        mapper.insertVersion(producer, event);
        return result("APPLIED", producer.getErrorCode(), producer.getSequence());
    }

    private void rememberObjects(ProducerRow producer, LineageSnapshotDTO snapshot) {
        Set<String> ids = new TreeSet<>();
        if (snapshot.getUnits() != null) for (LineageUnitDTO unit : snapshot.getUnits()) {
            if (unit == null) continue;
            if (unit.getSources() != null) for (LineageEndpointDTO endpoint : unit.getSources()) rememberEndpoint(endpoint, ids);
            rememberEndpoint(unit.getTarget(), ids);
        }
        if (!ids.isEmpty() || ("COMPLETE".equals(snapshot.getCaptureStatus()) && snapshot.getUnits() != null && snapshot.getUnits().isEmpty())) {
            StringBuilder json = new StringBuilder("[");
            for (String id : ids) { if (json.length() > 1) json.append(','); json.append('"').append(id).append('"'); }
            producer.setObjectIdsJson(json.append(']').toString());
        } else if (producer.getObjectIdsJson() == null) producer.setObjectIdsJson("[]");
    }

    private void rememberEndpoint(LineageEndpointDTO endpoint, Set<String> ids) {
        if (endpoint == null) return;
        try { ids.add(storeObject(endpoint, null)); }
        catch (IllegalArgumentException e) { /* Unresolved structured endpoint is not an identity. */ }
    }

    private LineageExtractionDTO extract(LineageSnapshotDTO snapshot) {
        LineageExtractionDTO result;
        if ("FAILED".equals(snapshot.getCaptureStatus())) {
            result = new LineageExtractionDTO();
            result.setStatus("FAILED");
            result.setErrorCode(snapshot.getErrorCode() == null ? "CAPTURE_FAILED" : snapshot.getErrorCode());
            return result;
        }
        try {
            result = extractor.extract(snapshot);
            if (result == null || !Arrays.asList("COMPLETE", "PARTIAL", "FAILED").contains(result.getStatus())
                    || (!"FAILED".equals(result.getStatus()) && result.getRelations() == null)) {
                throw new IllegalArgumentException("INVALID_EXTRACTION");
            }
        } catch (RuntimeException e) {
            result = new LineageExtractionDTO();
            result.setStatus("FAILED");
            result.setErrorCode("EXTRACTION_FAILED");
        }
        if ("PARTIAL".equals(snapshot.getCaptureStatus()) && "COMPLETE".equals(result.getStatus())) result.setStatus("PARTIAL");
        if (snapshot.getErrorCode() != null) result.setErrorCode(safeCodes(result.getErrorCode()) + "," + safeCodes(snapshot.getErrorCode()));
        return result;
    }

    private void persist(ProducerRow producer, LineageRelationDTO relation) {
        if (relation == null || relation.getUnitKey() == null || relation.getRouteKey() == null
                || relation.getUnitKey().length() > 160 || relation.getRouteKey().length() > 160
                || !Arrays.asList("TABLE", "COLUMN", "FIELD").contains(relation.getKind())
                || !"EXACT".equals(relation.getConfidence())) throw new IllegalArgumentException("INVALID_RELATION");
        boolean column = !"TABLE".equals(relation.getKind());
        if (column && (relation.getSourceColumn() == null || relation.getTargetColumn() == null)) throw new IllegalArgumentException("INVALID_COLUMN_RELATION");
        EdgeRow row = new EdgeRow();
        row.setId(identity.relationId(producer.getTaskId(), relation));
        row.setProducerId(producer.getId());
        row.setSequence(producer.getSequence());
        row.setSourceId(storeObject(relation.getSource(), null));
        row.setTargetId(storeObject(relation.getTarget(), null));
        if (column) {
            row.setSourceColumnId(storeObject(relation.getSource(), relation.getSourceColumn()));
            row.setTargetColumnId(storeObject(relation.getTarget(), relation.getTargetColumn()));
        }
        row.setUnitKey(relation.getUnitKey());
        row.setRouteKey(relation.getRouteKey());
        row.setKind(column ? "FIELD" : "TABLE");
        row.setConfidence("EXACT");
        mapper.insertEdge(row);
    }

    private String storeObject(LineageEndpointDTO endpoint, LineageColumnDTO column) {
        ObjectRow object = new ObjectRow();
        object.setId(column == null ? identity.objectId(endpoint) : identity.columnId(endpoint, column));
        object.setKind(column == null ? (endpoint.getKind() == null ? "TABLE" : endpoint.getKind().toUpperCase(Locale.ROOT)) : "COLUMN");
        object.setDatasourceId(endpoint.getDatasourceId());
        object.setCatalogName(LineageIdentity.normalize(endpoint.getCatalog(), endpoint.getCatalogQuoted(), endpoint.getCaseRule()));
        object.setSchemaName(LineageIdentity.normalize(endpoint.getSchema(), endpoint.getSchemaQuoted(), endpoint.getCaseRule()));
        object.setObjectName(LineageIdentity.normalize(endpoint.getTable(), endpoint.getTableQuoted(), endpoint.getCaseRule()));
        // External keys can carry paths/secrets: hash only; display remains a neutral kind label.
        if (endpoint.getExternalKey() != null) object.setExternalKey(LineageIdentity.hash(endpoint.getExternalKey()));
        if (column != null) {
            object.setParentId(identity.objectId(endpoint));
            object.setColumnName(LineageIdentity.normalize(column.getName(), column.getQuoted(), endpoint.getCaseRule()));
        }
        checkName(object.getCatalogName()); checkName(object.getSchemaName()); checkName(object.getObjectName()); checkName(object.getColumnName());
        mapper.insertObject(object);
        return object.getId();
    }

    @Override
    public LineageGraphDTO query(LineageQueryReqDTO request) {
        guard.context(request, "cat:table:query");
        if (!enabled) return graphQuery.empty("DISABLED", request.getDepth());
        return graphQuery.query(request);
    }

    @Override
    public LineageGraphDTO queryByMetadata(Long tableId, LineageQueryReqDTO request) {
        guard.context(request, "cat:table:query");
        CatalogTableDO table = guard.metadata(tableId, request);
        return queryResolved(table, null, request);
    }

    @Override
    public LineageGraphDTO queryByAsset(Long assetId, LineageQueryReqDTO request) {
        guard.context(request, "ast:asset:query");
        AssetsAssetDO asset = guard.asset(assetId, request);
        CatalogTableDO table = guard.assetMetadata(asset, request);
        if (table == null) {
            LineageGraphDTO graph = graphQuery.empty("UNRESOLVED", request.getDepth());
            graph.getDiagnostics().add("ASSET_IDENTITY_UNRESOLVED");
            return graph;
        }
        return queryResolved(table, assetId, request);
    }

    private LineageGraphDTO queryResolved(CatalogTableDO table, Long assetId, LineageQueryReqDTO request) {
        if (!enabled) return graphQuery.empty("DISABLED", request.getDepth());
        List<ObjectRow> candidates = mapper.resolve(table.getDatasourceId(), blankToNull(table.getDbName()), blankToNull(table.getSchemaName()), table.getTableName());
        if (candidates.size() != 1) {
            LineageGraphDTO graph = graphQuery.empty("UNRESOLVED", request.getDepth());
            graph.getDiagnostics().add(candidates.isEmpty() ? "NO_LINEAGE_IDENTITY" : "AMBIGUOUS_IDENTITY");
            return graph;
        }
        request.setObjectId(candidates.get(0).getId());
        LineageGraphDTO graph = graphQuery.query(request, assetId != null);
        for (LineageNodeDTO node : graph.getNodes()) if (node.getId().equals(graph.getCenterId())) {
            node.setMetadataId(table.getId());
            node.setAssetId(assetId);
        }
        return graph;
    }

    private static void validate(LineageSnapshotDTO snapshot) {
        if (snapshot == null || snapshot.getTaskId() == null || snapshot.getTaskId() <= 0 || snapshot.getSpaceId() == null
                || snapshot.getSequence() == null || snapshot.getSequence() < 0 || snapshot.getSpaceCode() == null
                || snapshot.getSpaceCode().trim().isEmpty() || snapshot.getSpaceCode().length() > 120
                || !Arrays.asList("ACTIVE", "UPSERT", "INACTIVE", "DELETE").contains(snapshot.getEventType())
                || ((!"INACTIVE".equals(snapshot.getEventType()) && !"DELETE".equals(snapshot.getEventType()))
                    && !Arrays.asList("COMPLETE", "PARTIAL", "FAILED").contains(snapshot.getCaptureStatus()))) {
            throw new IllegalArgumentException("INVALID_SNAPSHOT");
        }
        if (snapshot.getUnits() != null && snapshot.getUnits().size() > 1000) throw new IllegalArgumentException("SNAPSHOT_TOO_LARGE");
    }

    static String safeCodes(String codes) {
        if (codes == null || codes.isEmpty()) return "";
        if (codes.length() > 500 || !codes.matches("[A-Z0-9_,]+")) return "UNSAFE_DIAGNOSTIC_REDACTED";
        return codes;
    }
    private static String safeText(String value, int max) {
        if (value == null) return null;
        if (value.length() > max || value.indexOf('\n') >= 0 || value.indexOf('\r') >= 0) throw new IllegalArgumentException("INVALID_PRODUCER_LABEL");
        return value;
    }
    private static void checkName(String value) {
        if (value != null && (value.length() > 512 || value.indexOf(0) >= 0)) throw new IllegalArgumentException("INVALID_IDENTIFIER");
    }
    private static String blankToNull(String value) { return value == null || value.isEmpty() ? null : value; }
    private static LineageSyncResultDTO result(String status, String code, Long sequence) {
        LineageSyncResultDTO result = new LineageSyncResultDTO();
        result.setStatus(status); result.setErrorCode(code); result.setSequence(sequence); return result;
    }
}
