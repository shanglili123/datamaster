package com.datamaster.metadata.api.service.lineage;

import com.datamaster.metadata.api.lineage.dto.*;

/** Trusted internal writer; every query authorizes the current principal. */
public interface LineageApiService {
    LineageSyncResultDTO sync(LineageSnapshotDTO snapshot);
    LineageGraphDTO query(LineageQueryReqDTO request);
    LineageGraphDTO queryByMetadata(Long tableId, LineageQueryReqDTO request);
    LineageGraphDTO queryByAsset(Long assetId, LineageQueryReqDTO request);
}
