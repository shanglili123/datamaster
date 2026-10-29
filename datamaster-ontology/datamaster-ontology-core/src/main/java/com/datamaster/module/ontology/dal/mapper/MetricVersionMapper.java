package com.datamaster.module.ontology.dal.mapper;

import com.datamaster.module.ontology.dal.dataobject.MetricVersionDO;
import com.datamaster.mybatis.core.mapper.BaseMapperX;

/** Append only. PostgreSQL migration additionally rejects UPDATE/DELETE. */
public interface MetricVersionMapper extends BaseMapperX<MetricVersionDO> {
}
