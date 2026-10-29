package com.datamaster.module.collector.api.etl;

import com.datamaster.module.collector.api.etl.dto.CollectorOpsAnalysisOutput;

/** Optional runtime capability implemented by AI; collector never depends on AI implementation classes. */
public interface ICollectorOpsModelGateway {
    CollectorOpsAnalysisOutput analyzeFailure(String systemPrompt, String userPrompt, Long spaceId, String spaceCode);
}
