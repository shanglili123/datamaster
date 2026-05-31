

package com.datamaster.quality.utils.qualityDB.dialect;


import com.datamaster.quality.dal.dataobject.quality.QualityRuleEntity;
import com.datamaster.quality.utils.qualityDB.ComponentItem;

public class MySqlQuality implements ComponentItem {

    @Override
    public String fragCharacter(QualityRuleEntity rule) {
        String column = rule.getRuleColumn();
        String regex = (String) rule.getConfig().get("regex");
        return String.format("BINARY %s REGEXP '%s'", column, regex);
    }

}
