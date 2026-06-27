package com.datamaster.module.assets.service.dbgpt;

import com.datamaster.common.core.domain.AjaxResult;

public interface IDbGptDatasourceSyncService {

    AjaxResult syncAll();

    AjaxResult syncById(Long datasourceId);

    AjaxResult removeFromDbGpt(Long datasourceId);
}
