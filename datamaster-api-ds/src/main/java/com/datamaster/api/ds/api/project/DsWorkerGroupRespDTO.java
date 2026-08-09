package com.datamaster.api.ds.api.project;

import lombok.Data;
import com.datamaster.api.ds.api.base.DsResultDTO;

/**
 * DS worker group save response.
 */
@Data
public class DsWorkerGroupRespDTO extends DsResultDTO {

    private WorkerGroup data;

    @Data
    public static class WorkerGroup {
        private Integer id;
        private String name;
        private String addrList;
        private String description;
    }
}
