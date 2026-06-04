package com.datamaster.quality.utils.qualityDB.dialect;

import org.apache.commons.collections4.MapUtils;
import com.datamaster.quality.dal.dataobject.quality.QualityRuleEntity;
import com.datamaster.quality.utils.SqlBuilderUtils;
import com.datamaster.quality.utils.qualityDB.ComponentItem;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ClickHouseQuality implements ComponentItem {

    @Override
    public String regex(String column, String regex) {
        return String.format("match(toString(%s), '%s')", column, regex);
    }

    @Override
    public String fragLength(QualityRuleEntity rule) {
        String column = rule.getRuleColumn();
        Map<String, Object> ruleConfig = rule.getConfig();
        Integer min = MapUtils.getInteger(ruleConfig, "minLength");
        Integer max = MapUtils.getInteger(ruleConfig, "maxLength");
        String value = String.format("toString(%s)", column);
        List<String> conditions = new ArrayList<>();
        if (min != null) {
            conditions.add(String.format("length(%s)>=%d", value, min));
        }
        if (max != null) {
            conditions.add(String.format("length(%s)<=%d", value, max));
        }
        return String.join(" and ", conditions);
    }

    @Override
    public String fragDecimalPrecision(QualityRuleEntity rule) {
        String column = rule.getRuleColumn();
        Map<String, Object> ruleConfig = rule.getConfig();
        boolean skipInteger = SqlBuilderUtils.parseBoolean(ruleConfig.get("skipInteger"));
        int scale = Integer.parseInt(ruleConfig.get("scale").toString());
        String value = String.format("toString(%s)", column);
        String frag = String.format("(length(%s)-position(%s, '.'))=%d AND position(%s, '.')>0", value, value, scale, value);
        if (skipInteger) {
            frag = String.format("position(%s, '.')=0 OR %s", value, frag);
        }
        return frag;
    }
}
