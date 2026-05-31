

package com.datamaster.quality.utils.quality.enums;

import org.springframework.stereotype.Component;
import com.datamaster.quality.dal.dataobject.quality.QualityRuleEntity;
import com.datamaster.quality.utils.quality.QualitySqlGenerator;
import com.datamaster.quality.utils.qualityDB.ComponentItem;
import com.datamaster.quality.utils.qualityDB.ComponentRegistry;

// 时间字段先后顺序校验
@Component("TIME_ORDER_VALIDATION")
public class TimeOrderValidationGenerator implements QualitySqlGenerator {
    @Override
    public String generateSql(QualityRuleEntity rule) {
        ComponentRegistry registry = new ComponentRegistry();
        ComponentItem componentItem = registry.getComponentItem(rule.getDaDatasourceById().getDatasourceType());
        return componentItem.generateLogicCompareValidationSql(rule);
    }

    @Override
    public String generateErrorSql(QualityRuleEntity rule) {
        ComponentRegistry registry = new ComponentRegistry();
        ComponentItem componentItem = registry.getComponentItem(rule.getDaDatasourceById().getDatasourceType());
        return componentItem.generateLogicCompareValidationErrorSql(rule);
    }

    @Override
    public String generateValidDataSql(QualityRuleEntity rule, int limit, int offset) {
        ComponentRegistry registry = new ComponentRegistry();
        ComponentItem componentItem = registry.getComponentItem(rule.getDaDatasourceById().getDatasourceType());
        return componentItem.generateLogicCompareValidationValidDataSql(rule,limit,offset);
    }
}
