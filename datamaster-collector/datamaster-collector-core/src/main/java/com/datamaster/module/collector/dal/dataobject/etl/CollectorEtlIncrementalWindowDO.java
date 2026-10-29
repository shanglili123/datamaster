package com.datamaster.module.collector.dal.dataobject.etl;

import lombok.Data;

/** 每个运行实例、每个 FlinkX 小任务（stage）的增量标志与窗口边界。 */
@Data
public class CollectorEtlIncrementalWindowDO {

    public static final String PREPARED = "PREPARED";
    public static final String SUCCESS = "SUCCESS";
    public static final String FAILURE = "FAILURE";

    private Long taskId;
    private String stageKey;
    private Long instanceId;
    /** JSON：{"<源表标识>": "<游标起点>"} */
    private String startValue;
    /** JSON：{"<源表标识>": "<游标终点>"}；空窗口源值为 null */
    private String endValue;
    private Boolean empty;
    private String status;
    private String sourcesJson;
    private Long replayFromId;
}
