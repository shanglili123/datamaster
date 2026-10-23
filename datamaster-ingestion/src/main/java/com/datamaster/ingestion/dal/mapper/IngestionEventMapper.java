package com.datamaster.ingestion.dal.mapper;

import com.datamaster.ingestion.dal.dataobject.IngestionEventDO;
import org.apache.ibatis.annotations.Param;

import java.util.Date;

/** Kafka 事件幂等领取 Mapper。 */
public interface IngestionEventMapper {

    int tryInsertClaim(@Param("eventKey") String eventKey,
                       @Param("topic") String topic,
                       @Param("partition") Integer partition,
                       @Param("offset") Long offset,
                       @Param("owner") String owner,
                       @Param("leaseUntil") Date leaseUntil);

    IngestionEventDO selectByEventKey(@Param("eventKey") String eventKey);

    int reclaimExpired(@Param("eventKey") String eventKey,
                       @Param("owner") String owner,
                       @Param("leaseUntil") Date leaseUntil);

    int markCompleted(@Param("eventKey") String eventKey, @Param("owner") String owner,
                      @Param("status") String status);

    int markFailed(@Param("eventKey") String eventKey, @Param("owner") String owner);

    int renewLease(@Param("eventKey") String eventKey, @Param("owner") String owner,
                   @Param("leaseUntil") Date leaseUntil);

    long countByStatus(@Param("status") String status);
}
