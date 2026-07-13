package com.datamaster.api.ds.api.etl;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * DolphinScheduler task instance log slice.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DsLogResultDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Integer fromLineNum;
    private Integer toLineNum;
    private String logContent;
    private Boolean end;
}
