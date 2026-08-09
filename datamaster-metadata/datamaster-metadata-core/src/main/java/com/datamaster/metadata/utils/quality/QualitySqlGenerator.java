

package com.datamaster.metadata.utils.quality;


import com.datamaster.metadata.dal.dataobject.quality.QualityRuleEntity;

public interface QualitySqlGenerator {
    String generateSql(QualityRuleEntity rule);
    String generateErrorSql(QualityRuleEntity rule);
    String generateValidDataSql(QualityRuleEntity rule, int limit, int offset);
}
