package com.datamaster.module.assets.service.datasource;

/** 确保数据源与空间关联后的结果。 */
public class DatasourceSpaceRelEnsureResult {

    private final Long relationId;
    private final boolean reused;

    public DatasourceSpaceRelEnsureResult(Long relationId, boolean reused) {
        this.relationId = relationId;
        this.reused = reused;
    }

    public Long getRelationId() {
        return relationId;
    }

    public boolean isReused() {
        return reused;
    }
}
