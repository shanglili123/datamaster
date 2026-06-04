package com.datamaster.quality.utils.qualityDB.dialect;

import org.apache.commons.collections4.MapUtils;
import com.datamaster.quality.dal.dataobject.quality.QualityRuleEntity;
import com.datamaster.quality.utils.SqlBuilderUtils;
import com.datamaster.quality.utils.qualityDB.ComponentItem;

import java.util.Map;

public class PostgreSqlQuality implements ComponentItem {

    @Override
    public String regex(String column, String regex) {
        return String.format("CAST(%s AS TEXT) ~ '%s'", column, regex);
    }

    @Override
    public String fragDecimalPrecision(QualityRuleEntity rule) {
        String column = rule.getRuleColumn();
        Map<String, Object> ruleConfig = rule.getConfig();
        boolean skipInteger = SqlBuilderUtils.parseBoolean(ruleConfig.get("skipInteger"));
        int scale = Integer.parseInt(ruleConfig.get("scale").toString());
        String value = String.format("CAST(%s AS TEXT)", column);
        String frag = String.format("(LENGTH(%s)-POSITION('.' IN %s))=%d AND POSITION('.' IN %s)>0", value, value, scale, value);
        if (skipInteger) {
            frag = String.format("POSITION('.' IN %s)=0 OR %s", value, frag);
        }
        return frag;
    }

    @Override
    public String fragLength(QualityRuleEntity rule) {
        String column = rule.getRuleColumn();
        Map<String, Object> ruleConfig = rule.getConfig();
        Integer min = MapUtils.getInteger(ruleConfig, "minLength");
        Integer max = MapUtils.getInteger(ruleConfig, "maxLength");
        String value = String.format("CAST(%s AS TEXT)", column);
        java.util.List<String> conditions = new java.util.ArrayList<>();
        if (min != null) {
            conditions.add(String.format("LENGTH(%s)>=%d", value, min));
        }
        if (max != null) {
            conditions.add(String.format("LENGTH(%s)<=%d", value, max));
        }
        return String.join(" and ", conditions);
    }
}
