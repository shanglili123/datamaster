package com.datamaster.flinkx.core.transform;

import com.alibaba.fastjson2.JSONObject;

import java.util.List;
import java.util.Map;

public interface TransitionHandler {

    String componentType();

    void apply(Map<String, String> colExprs, List<String> orderByClauses,
               List<String> dedupPartitions, List<String> whereClauses,
               JSONObject param);
}
