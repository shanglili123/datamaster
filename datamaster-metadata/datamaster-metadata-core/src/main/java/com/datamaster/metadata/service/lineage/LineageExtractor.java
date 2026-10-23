package com.datamaster.metadata.service.lineage;

import com.datamaster.metadata.api.lineage.dto.LineageExtractionDTO;
import com.datamaster.metadata.api.lineage.dto.LineageSnapshotDTO;

/** Pure extraction only. No SQL execution, database access or authorization side effects. */
public interface LineageExtractor {
    LineageExtractionDTO extract(LineageSnapshotDTO snapshot);
}
