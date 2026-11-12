package com.datamaster.module.ontology.service.analysis;

import com.alibaba.fastjson2.JSON;
import com.datamaster.module.ontology.controller.admin.analysis.vo.MetricQueryColumnVO;
import com.datamaster.module.ontology.service.metric.MetricRules;
import java.math.BigDecimal;
import java.util.*;
import static com.datamaster.module.ontology.service.metric.MetricRules.require;

/** Typed grouping identity, not display text; decimal scale is not a distinct business dimension. */
public final class MetricGroupIdentity {
    private MetricGroupIdentity() { }
    public static String of(List<MetricQueryColumnVO> dimensions, Map<String,Object> row) {
        require(row != null, "分组结果行不能为空");
        List<Object> values = new ArrayList<>();
        for (MetricQueryColumnVO dimension : dimensions) {
            require(dimension != null && dimension.getKey() != null && row.containsKey(dimension.getKey()), "分组结果缺少维度键");
            Object value = row.get(dimension.getKey());
            if (value != null && MetricRules.numeric(dimension.getDataType())) {
                try {
                    BigDecimal number = new BigDecimal(String.valueOf(value));
                    require(number.precision() <= 1000 && Math.abs((long) number.scale()) <= 1000, "分组数值超出安全范围");
                    value = number.signum() == 0 ? "0" : number.stripTrailingZeros().toPlainString();
                } catch (NumberFormatException invalid) { throw new com.datamaster.common.exception.ServiceException("数值维度结果格式无效"); }
            }
            // Keep SQL null separate from strings such as "null" and from the empty string.
            values.add(Arrays.asList(dimension.getDataType(), value));
        }
        return JSON.toJSONString(values, com.alibaba.fastjson2.JSONWriter.Feature.WriteMapNullValue);
    }
}
