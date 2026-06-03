

package com.datamaster.server;

import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import com.datamaster.common.database.constants.DbType;
import com.datamaster.quality.dal.dataobject.datasource.DatasourceDO;
import com.datamaster.quality.dal.dataobject.quality.QualityRuleEntity;
import com.datamaster.quality.utils.quality.enums.*;

import java.util.HashMap;
import java.util.Map;

@Slf4j
public class QualitySqlGenerateMySqlTest {

    @Test
    public void generateCharacterSql() {
        QualityRuleEntity qualityRule = new QualityRuleEntity();
        qualityRule.setId("2");
        qualityRule.setRuleType("CHARACTER_VALIDATION");
        qualityRule.setDataId("56");
        qualityRule.setTableName("user8");
        qualityRule.setRuleColumn("name");
        qualityRule.setWhereClause("id<1000");

        Map<String, Object> map = new HashMap<>();
        map.put("regex", "^[a-z0-9_ ]+$");
        map.put("ignoreNullValue", true);
        qualityRule.setConfig(map);
        DatasourceDO DatasourceDO = new DatasourceDO();
        DatasourceDO.setDatasourceType(DbType.MYSQL.getDb());
        qualityRule.setDaDatasourceById(DatasourceDO);

        CharacterValidationGenerator generator = new CharacterValidationGenerator();
        String sql = generator.generateSql(qualityRule);
        System.out.println(sql + ";");
        sql = generator.generateErrorSql(qualityRule);
        System.out.println(sql + ";");
        sql = generator.generateValidDataSql(qualityRule, 100, 0);
        System.out.println(sql + ";");
    }

    @Test
    public void generateDecimalPrecisionSql() {
        QualityRuleEntity qualityRule = new QualityRuleEntity();
        qualityRule.setId("2");
        qualityRule.setRuleType("CHARACTER_VALIDATION");
        qualityRule.setDataId("56");
        qualityRule.setTableName("user8");
        qualityRule.setRuleColumn("fraction");

        Map<String, Object> map = new HashMap<>();
        map.put("scale", 2);
        map.put("ignoreNullValue", false);
        map.put("skipInteger", true);
        qualityRule.setConfig(map);
        DatasourceDO DatasourceDO = new DatasourceDO();
        DatasourceDO.setDatasourceType(DbType.MYSQL.getDb());
        qualityRule.setDaDatasourceById(DatasourceDO);

        DecimalPrecisionGenerator generator = new DecimalPrecisionGenerator();
        String sql = generator.generateSql(qualityRule);
        System.out.println(sql + ";");
        sql = generator.generateErrorSql(qualityRule);
        System.out.println(sql + ";");
        sql = generator.generateValidDataSql(qualityRule, 100, 0);
        System.out.println(sql + ";");
    }

    @Test
    public void generateCompositeUniquenessSql() {
        QualityRuleEntity qualityRule = new QualityRuleEntity();
        qualityRule.setId("2");
        qualityRule.setRuleType("CHARACTER_VALIDATION");
        qualityRule.setDataId("56");
        qualityRule.setTableName("user8");
        qualityRule.setRuleColumn("AGE");
        qualityRule.setWhereClause("id>100");

        Map<String, Object> map = new HashMap<>();
        qualityRule.setRuleColumns(Lists.newArrayList("age", "name"));
        qualityRule.setConfig(map);
        DatasourceDO DatasourceDO = new DatasourceDO();
        DatasourceDO.setDatasourceType(DbType.MYSQL.getDb());
        qualityRule.setDaDatasourceById(DatasourceDO);

        CompositeUniquenessGenerator generator = new CompositeUniquenessGenerator();
        String sql = generator.generateSql(qualityRule);
        System.out.println(sql + ";");
        sql = generator.generateErrorSql(qualityRule);
        System.out.println(sql + ";");
        sql = generator.generateValidDataSql(qualityRule, 100, 0);
        System.out.println(sql + ";");
    }

}
