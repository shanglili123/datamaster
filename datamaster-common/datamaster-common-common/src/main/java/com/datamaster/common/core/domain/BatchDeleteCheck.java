

package com.datamaster.common.core.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class BatchDeleteCheck<T> {

    private Integer cannotDeleteCount;
    private List<T> canDeleteIds;

    public Integer getCanDeleteCount() {
        return canDeleteIds == null ? 0 : canDeleteIds.size();
    }

}
