package com.datamaster.module.assets.api.service.governance;

import com.datamaster.module.assets.api.governance.dto.AssetsTableGovernanceReqDTO;
import com.datamaster.module.assets.api.governance.dto.AssetsTableGovernanceRespDTO;

public interface IAssetsTableGovernanceApiService {

    AssetsTableGovernanceRespDTO resolveTable(AssetsTableGovernanceReqDTO reqDTO);

    void checkTableAccess(AssetsTableGovernanceReqDTO reqDTO);
}
