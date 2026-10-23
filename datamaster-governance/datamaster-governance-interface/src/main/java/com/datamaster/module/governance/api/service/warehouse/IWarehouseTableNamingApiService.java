package com.datamaster.module.governance.api.service.warehouse;

import com.datamaster.module.governance.api.warehouse.dto.WarehouseTableNamingReqDTO;
import com.datamaster.module.governance.api.warehouse.dto.WarehouseTableNamingRespDTO;

public interface IWarehouseTableNamingApiService {
    WarehouseTableNamingRespDTO resolveAndValidate(WarehouseTableNamingReqDTO req);
}
