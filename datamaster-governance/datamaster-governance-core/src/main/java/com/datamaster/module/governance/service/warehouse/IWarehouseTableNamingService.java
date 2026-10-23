package com.datamaster.module.governance.service.warehouse;

import com.datamaster.module.governance.api.service.warehouse.IWarehouseTableNamingApiService;
import com.datamaster.module.governance.api.warehouse.dto.WarehouseTableNamingReqDTO;
import com.datamaster.module.governance.api.warehouse.dto.WarehouseTableNamingRespDTO;

public interface IWarehouseTableNamingService extends IWarehouseTableNamingApiService {
    /** Display current metadata without recalculating or validating a historical physical target. */
    WarehouseTableNamingRespDTO describe(WarehouseTableNamingReqDTO req);

    void validateModelSpace(Long dataLayerId, Long spaceId, String spaceCode);
}
