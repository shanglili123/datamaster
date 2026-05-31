package com.datamaster.quality.utils.quality.enums;

import org.springframework.stereotype.Component;
import com.datamaster.quality.dal.dataobject.quality.QualityRuleEntity;
import com.datamaster.quality.utils.quality.QualitySqlGenerator;
import com.datamaster.quality.utils.qualityDB.ComponentItem;
import com.datamaster.quality.utils.qualityDB.ComponentRegistry;

// 字段长度范围校验
@Component("LENGTH_VALIDATION")
public class LengthValidationGenerator implements QualitySqlGenerator {
    @Override
    public String generateSql(QualityRuleEntity rule) {
        ComponentRegistry registry = new ComponentRegistry();
        ComponentItem componentItem = registry.getComponentItem(rule.getDaDatasourceById().getDatasourceType());
        return componentItem.generateLengthValidationSql(rule);
    }

    @Override
    public String generateErrorSql(QualityRuleEntity rule) {
        ComponentRegistry registry = new ComponentRegistry();
        ComponentItem componentItem = registry.getComponentItem(rule.getDaDatasourceById().getDatasourceType());
        return componentItem.generateLengthValidationErrorSql(rule);
    }

    @Override
    public String generateValidDataSql(QualityRuleEntity rule, int limit, int offset) {
        ComponentRegistry registry = new ComponentRegistry();
        ComponentItem componentItem = registry.getComponentItem(rule.getDaDatasourceById().getDatasourceType());
        return componentItem.generateLengthValidationValidDataSql(rule,limit,offset);
    }
}
