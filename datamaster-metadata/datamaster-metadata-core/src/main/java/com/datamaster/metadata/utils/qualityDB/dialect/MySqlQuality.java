

package com.datamaster.metadata.utils.qualityDB.dialect;


import com.datamaster.metadata.dal.dataobject.quality.QualityRuleEntity;
import com.datamaster.metadata.utils.qualityDB.ComponentItem;

public class MySqlQuality implements ComponentItem {

    @Override
    public String fragCharacter(QualityRuleEntity rule) {
        String column = rule.getRuleColumn();
        String regex = (String) rule.getConfig().get("regex");
        return String.format("BINARY %s REGEXP '%s'", column, regex);
    }

}
