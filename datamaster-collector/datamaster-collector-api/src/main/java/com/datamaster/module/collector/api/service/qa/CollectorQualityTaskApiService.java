

package com.datamaster.module.collector.api.service.qa;

import com.datamaster.module.collector.api.qa.dto.CollectorQualitySummaryRespDTO;

public interface CollectorQualityTaskApiService {

    Long getCountByCatCode(String catCode);

    CollectorQualitySummaryRespDTO getLatestQualitySummary(Long datasourceId, String tableName);

}
