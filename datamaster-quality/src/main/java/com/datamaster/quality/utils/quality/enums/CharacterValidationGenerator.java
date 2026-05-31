

package com.datamaster.quality.utils.quality.enums;


import org.springframework.stereotype.Component;
import com.datamaster.quality.dal.dataobject.quality.QualityRuleEntity;
import com.datamaster.quality.utils.quality.QualitySqlGenerator;
import com.datamaster.quality.utils.qualityDB.ComponentItem;
import com.datamaster.quality.utils.qualityDB.ComponentRegistry;


// 字符串类型校验（如身份证只允许数字和X）
@Component("CHARACTER_VALIDATION")
public class CharacterValidationGenerator implements QualitySqlGenerator {
    @Override
    public String generateSql(QualityRuleEntity rule) {
        ComponentRegistry registry = new ComponentRegistry();
        ComponentItem componentItem = registry.getComponentItem(rule.getDaDatasourceById().getDatasourceType());
        return componentItem.generateCharacterValidationSql(rule);
    }

    @Override
    public String generateErrorSql(QualityRuleEntity rule) {
        ComponentRegistry registry = new ComponentRegistry();
        ComponentItem componentItem = registry.getComponentItem(rule.getDaDatasourceById().getDatasourceType());
        return componentItem.generateCharacterValidationErrorSql(rule);
    }

    @Override
    public String generateValidDataSql(QualityRuleEntity rule, int limit, int offset) {
        ComponentRegistry registry = new ComponentRegistry();
        ComponentItem componentItem = registry.getComponentItem(rule.getDaDatasourceById().getDatasourceType());
        return componentItem.generateCharacterValidationValidDataSql(rule,limit,offset);
    }


    public String generateDataCheckSql(QualityRuleEntity rule,String inputValue) {
        ComponentRegistry registry = new ComponentRegistry();
        ComponentItem componentItem = registry.getComponentItem(rule.getDaDatasourceById().getDatasourceType());
        return componentItem.generateValidDataCheckSql(rule,inputValue);
    }
}
