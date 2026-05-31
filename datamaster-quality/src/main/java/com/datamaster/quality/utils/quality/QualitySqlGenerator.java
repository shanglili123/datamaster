

package com.datamaster.quality.utils.quality;


import com.datamaster.quality.dal.dataobject.quality.QualityRuleEntity;

public interface QualitySqlGenerator {
    String generateSql(QualityRuleEntity rule);
    String generateErrorSql(QualityRuleEntity rule);
    String generateValidDataSql(QualityRuleEntity rule, int limit, int offset);
}
