package com.datamaster.module.collector.service.etl;

/**
 * Prepares a FLINKX incremental execution before DolphinScheduler dispatches
 * the CHUNJUN node.
 */
public interface ICollectorEtlIncrementalService {

    /**
     * Resolves the current incremental window and returns the CHUNJUN job JSON.
     */
    String prepareIncrementalTask(Long taskId, Long processInstanceId);

    /**
     * Writes the FLINKX completion state and releases the running slot.
     */
    void completeIncrementalTask(Long taskId, Long processInstanceId);

    /**
     * Releases the running slot after any terminal workflow state.
     */
    void releaseIncrementalTask(Long taskId, Long processInstanceId);

    /**
     * Force clears the running slot for the task, used by task unload/offline flows.
     */
    void forceReleaseIncrementalTask(Long taskId);
}
