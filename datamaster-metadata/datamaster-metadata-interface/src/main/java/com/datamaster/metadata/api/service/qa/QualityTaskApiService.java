package com.datamaster.metadata.api.service.qa;

import com.datamaster.metadata.api.qa.dto.QualitySummaryRespDTO;

public interface QualityTaskApiService {

    Long getCountByCatCode(String catCode);

    QualitySummaryRespDTO getLatestQualitySummary(Long datasourceId, String tableName);

}
