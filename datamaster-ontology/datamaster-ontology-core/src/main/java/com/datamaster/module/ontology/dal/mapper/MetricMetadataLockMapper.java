package com.datamaster.module.ontology.dal.mapper;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/** Coordinates with reference guards before logically deleting parent metadata. */
public interface MetricMetadataLockMapper {
    @Select("SELECT id FROM ont_ontology WHERE id=#{id} AND del_flag=0 FOR UPDATE")
    Long lockOntology(@Param("id") Long id);
}
