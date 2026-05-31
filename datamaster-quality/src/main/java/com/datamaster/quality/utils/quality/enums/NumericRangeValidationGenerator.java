package com.datamaster.quality.utils.quality.enums;

import org.springframework.stereotype.Component;
import com.datamaster.quality.dal.dataobject.quality.QualityRuleEntity;
import com.datamaster.quality.utils.quality.QualitySqlGenerator;
import com.datamaster.quality.utils.qualityDB.ComponentItem;
import com.datamaster.quality.utils.qualityDB.ComponentRegistry;

// 数值字段范围校验
@Component("NUMERIC_RANGE_VALIDATION")
public class NumericRangeValidationGenerator implements QualitySqlGenerator {
    @Override
    public String generateSql(QualityRuleEntity rule) {
        ComponentRegistry registry = new ComponentRegistry();
        ComponentItem componentItem = registry.getComponentItem(rule.getDaDatasourceById().getDatasourceType());
        return componentItem.generateNumericRangeValidationSql(rule);
    }

    @Override
    public String generateErrorSql(QualityRuleEntity rule) {
        ComponentRegistry registry = new ComponentRegistry();
        ComponentItem componentItem = registry.getComponentItem(rule.getDaDatasourceById().getDatasourceType());
        return componentItem.generateNumericRangeValidationErrorSql(rule);
    }

    @Override
    public String generateValidDataSql(QualityRuleEntity rule, int limit, int offset) {
        ComponentRegistry registry = new ComponentRegistry();
        ComponentItem componentItem = registry.getComponentItem(rule.getDaDatasourceById().getDatasourceType());
        return componentItem.generateNumericRangeValidationValidDataSql(rule,limit,offset);
    }
}
