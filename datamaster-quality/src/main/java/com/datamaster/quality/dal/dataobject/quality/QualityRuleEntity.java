

package com.datamaster.quality.dal.dataobject.quality;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.datamaster.common.utils.JSONUtils;
import com.datamaster.quality.controller.quality.vo.QualityRuleQueryReqDTO;
import com.datamaster.quality.dal.dataobject.datasource.DatasourceDO;
import com.datamaster.quality.dal.dataobject.qa.QualityTaskEvaluateDO;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// QualityRuleEntity 示例定义（用于策略类）
@Data
@AllArgsConstructor
@NoArgsConstructor
public class QualityRuleEntity {

    //表名称
    //字段名
    /** 稽查规则编号 */
    private String ruleCode;
    /** 稽查规则名称 */
    private String ruleName;
    /** 质量维度*/
    private String dimensionType;
    /** 规则描述 */
    private String ruleDescription;
    private Long evaluateId;

    private Long taskId;
    private Long taskLogId;

    private String id;
    private String tableName;
    private String whereClause;
    private String ruleColumn;

    private String dataId;
    private DatasourceDO datasourceById;

    private String ruleType;

    /**
     * {
     *   "conditions": [
     *     {
     *       "leftField": "time1",
     *       "operator": "<=",
     *       "rightField": "time2"
     *     },
     *     {
     *       "leftField": "time2",
     *       "operator": "<",
     *       "rightField": "time3"
     *     }
     *   ],
     *   "allowPartialNull": true
     * }
     *
         *      * 是否忽略空值
         *      * true 表示忽略；false 表示不忽略
        private Boolean ignoreNullValue;
         *
          *      * 是否忽略大小写
          *      * true 表示忽略；false 表示不忽略
        private Boolean ignoreCase;
         *
          *      * 字段填写策略：
          *      * 1 表示字段必须全部填写（部分为空为异常）
          *      * 2 表示字段要么全部为空，要么全部填写（部分填写为异常）
        private Integer fillStrategy;
         *
          *      * 是否包含最大最小值
          *      * true 表示包含（>=、<=）；false 表示不包含（>、<）
        private Boolean includeRangeBound;

        是否忽略整数值，* true 表示忽略；false 表示不忽略
        private Boolean skipInteger;
     *
     *
     */
    private Map<String, Object> config;

    // 可选：组合唯一性支持多个列
    private List<String> ruleColumns;
    private List<String> showErrorColumns;



    public QualityRuleEntity(Map<String, Object> stringObjectMap){

        // TODO
        //表字段需要兼容

    }

    public QualityRuleEntity(QualityRuleQueryReqDTO queryReqDTO) {
        this.tableName = queryReqDTO.getTableName();
        this.config = queryReqDTO.getConfig();
        this.whereClause = queryReqDTO.getWhereClause();
        this.ruleColumn = queryReqDTO.getEvaColumn(); // 若是单字段规则，取 evaColumn

        ruleColumns = new ArrayList<>();
        if (queryReqDTO.getEvaColumn() != null && !queryReqDTO.getEvaColumn().trim().isEmpty()) {
            String[] columns = queryReqDTO.getEvaColumn().split(",");
            for (String col : columns) {
                if (col != null && !col.trim().isEmpty()) {
                    this.ruleColumns.add(col.trim());
                }
            }
        }
    }

    public QualityRuleEntity(QualityTaskEvaluateDO QualityTaskEvaluateDO) {
        this.ruleCode = QualityTaskEvaluateDO.getRuleCode();
        this.ruleName = QualityTaskEvaluateDO.getRuleName();
        this.dimensionType = QualityTaskEvaluateDO.getDimensionType();
        this.ruleDescription = QualityTaskEvaluateDO.getRuleDescription();
        this.evaluateId = QualityTaskEvaluateDO.getId();
        this.taskId = QualityTaskEvaluateDO.getTaskId();

        this.id = String.valueOf(QualityTaskEvaluateDO.getId());

        this.ruleType = QualityTaskEvaluateDO.getRuleType();
        this.tableName = QualityTaskEvaluateDO.getTableName();
        this.whereClause = QualityTaskEvaluateDO.getWhereClause();
        this.ruleColumn = QualityTaskEvaluateDO.getEvaColumn(); // 若是单字段规则，取 evaColumn

        Map<String, Object> map = JSONUtils.convertTaskDefinitionJsonMap(QualityTaskEvaluateDO.getRule());

        map.put("errDescription",QualityTaskEvaluateDO.getErrDescription());
        map.put("suggestion",QualityTaskEvaluateDO.getSuggestion());
        map.put("warningLevel",QualityTaskEvaluateDO.getWarningLevel());
        map.put("evaluateName",QualityTaskEvaluateDO.getName());

        this.config = map;

         ruleColumns = new ArrayList<>();
        if (QualityTaskEvaluateDO.getEvaColumn() != null && !QualityTaskEvaluateDO.getEvaColumn().trim().isEmpty()) {
            String[] columns = QualityTaskEvaluateDO.getEvaColumn().split(",");
            for (String col : columns) {
                if (col != null && !col.trim().isEmpty()) {
                    this.ruleColumns.add(col.trim());
                }
            }
        }
    }
}
