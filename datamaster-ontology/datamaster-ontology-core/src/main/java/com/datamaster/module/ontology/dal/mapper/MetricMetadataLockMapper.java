package com.datamaster.module.ontology.dal.mapper;

import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/** Coordinates with reference guards before logically deleting parent metadata. */
public interface MetricMetadataLockMapper {
    // Invalidate the session's pre-lock metadata reads; post-lock checks must not reuse stale parents.
    @Options(flushCache = Options.FlushCachePolicy.TRUE, useCache = false)
    @Select("SELECT id FROM ont_ontology WHERE id=#{id} AND del_flag=0 FOR UPDATE")
    Long lockOntology(@Param("id") Long id);
}
