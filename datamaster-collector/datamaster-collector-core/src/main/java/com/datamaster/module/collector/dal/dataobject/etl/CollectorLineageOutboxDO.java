package com.datamaster.module.collector.dal.dataobject.etl;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.util.Date;

/** Immutable minimal published lineage payload; never stores connection credentials. */
@Data
@TableName("col_lineage_outbox")
public class CollectorLineageOutboxDO {
    private Long id;
    private Long taskId;
    private Long sequence;
    private String eventType;
    private String version;
    private String payload;
    private String status;
    private Integer attempts;
    private Date nextAttemptAt;
    private Date leaseUntil;
    private String leaseToken;
    private String errorCode;
    private Date createTime;
}
