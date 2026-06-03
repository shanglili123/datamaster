

package com.datamaster.quality.utils.quality.enums;

import org.springframework.stereotype.Component;
import com.datamaster.quality.dal.dataobject.quality.QualityRuleEntity;
import com.datamaster.quality.utils.quality.QualitySqlGenerator;
import com.datamaster.quality.utils.qualityDB.ComponentItem;
import com.datamaster.quality.utils.qualityDB.ComponentRegistry;

// 字段组完整性校验（字段成对非空）
@Component("GROUP_FIELD_COMPLETENESS")
public class GroupFieldCompletenessGenerator implements QualitySqlGenerator {
    @Override
    public String generateSql(QualityRuleEntity rule) {
        ComponentRegistry registry = new ComponentRegistry();
        ComponentItem componentItem = registry.getComponentItem(rule.getDatasourceById().getDatasourceType());
        return componentItem.generateGroupFieldCompletenessSql(rule);
    }

    @Override
    public String generateErrorSql(QualityRuleEntity rule) {
        ComponentRegistry registry = new ComponentRegistry();
        ComponentItem componentItem = registry.getComponentItem(rule.getDatasourceById().getDatasourceType());
        return componentItem.generateGroupFieldCompletenessErrorSql(rule);
    }

    @Override
    public String generateValidDataSql(QualityRuleEntity rule, int limit, int offset) {
        ComponentRegistry registry = new ComponentRegistry();
        ComponentItem componentItem = registry.getComponentItem(rule.getDatasourceById().getDatasourceType());
        return componentItem.generateGroupFieldCompletenessValidDataSql(rule,limit,offset);
    }
}
