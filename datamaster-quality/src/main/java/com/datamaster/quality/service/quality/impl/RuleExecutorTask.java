

package com.datamaster.quality.service.quality.impl;

import com.alibaba.fastjson2.JSONObject;
import com.datamaster.common.database.DbQuery;
import com.datamaster.common.httpClient.HttpTaskLogger;
import com.datamaster.quality.controller.qa.vo.EvaluateLogSaveReqVO;
import com.datamaster.quality.dal.dataobject.quality.QualityCheckResult;
import com.datamaster.quality.dal.dataobject.quality.QualityRuleEntity;
import com.datamaster.quality.service.qa.IEvaluateLogService;
import com.datamaster.quality.storage.ErrorDataStorage;
import com.datamaster.quality.utils.quality.QualitySqlGenerateFactory;
import com.datamaster.quality.utils.quality.QualitySqlGenerator;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

public class RuleExecutorTask implements Callable<QualityCheckResult> {

    private final QualityRuleEntity rule;
    private final String batch;
    private final DbQuery dbQuery;
    private final QualitySqlGenerateFactory sqlFactory;
    private final ErrorDataStorage errorDataStorage;
    private final HttpTaskLogger logger;
    private final IEvaluateLogService iEvaluateLogService;

    public RuleExecutorTask(QualityRuleEntity rule, String batch,
                            DbQuery dbQuery,
                            QualitySqlGenerateFactory sqlFactory,
                            ErrorDataStorage errorDataStorage,
                            HttpTaskLogger logger,
                            IEvaluateLogService iEvaluateLogService) {
        this.rule = rule;
        this.batch = batch;
        this.dbQuery = dbQuery;
        this.sqlFactory = sqlFactory;
        this.errorDataStorage = errorDataStorage;
        this.logger = logger;
        this.iEvaluateLogService = iEvaluateLogService;
    }

    @Override
    public QualityCheckResult call() {
        try {
            EvaluateLogSaveReqVO createReqVO = new EvaluateLogSaveReqVO(rule);
            logger.log("质量任务-开始执行规则，规则ID：" + rule.getId() + "，规则类型：" + rule.getRuleType());
            // 1. 生成 SQL 脚本（策略模式）
            logger.log("质量任务-获取 SQL 生成器并生成 SQL 脚本。");
            QualitySqlGenerator generator = sqlFactory.getGenerator(rule.getRuleType());
            String checkSql = generator.generateSql(rule);
            String errorSql = generator.generateErrorSql(rule);
            logger.log("质量任务-SQL 生成完成，CheckSQL：" + checkSql + "，ErrorSQL：" + errorSql);

            // 2. 执行 SQL
            try (Connection conn = dbQuery.getConnection();
                 Statement stmt = conn.createStatement()) {

                logger.log("质量任务-开始执行校验 SQL。");
                // 查询异常数 & 总数
                int errorCount = 0;
                int totalCount = 0;
                try (ResultSet rs = stmt.executeQuery(checkSql)) {
                    if (rs.next()) {
//                        errorCount = rs.getInt(1);
//                        totalCount = rs.getInt(2);
                        totalCount = ((Number) rs.getObject("totalCount")).intValue();
                        errorCount = ((Number) rs.getObject("errorCount")).intValue();
                    }
                }
                logger.log("质量任务-校验 SQL 执行完成，异常数：" + errorCount + "，总数：" + totalCount);
                // 查询异常明细
                logger.log("质量任务-开始查询错误明细数据。");
                List<JSONObject> errorList = new ArrayList<>();
                try (ResultSet rs = stmt.executeQuery(errorSql)) {
                    while (rs.next()) {
                        JSONObject row = new JSONObject();
                        for (String col : rule.getShowErrorColumns()) {
                            row.put(col, rs.getObject(col));
                        }
                        errorList.add(row);
                    }
                }

                logger.log("质量任务-错误明细数据查询完成，共：" + errorList.size() + " 条。");

                createReqVO.setTotal((long)totalCount);
                createReqVO.setProblemTotal((long)errorCount);
                Long EvaluateLog = iEvaluateLogService.createEvaluateLog(createReqVO);

                logger.log("质量任务-开始写入错误明细数据。");
                errorDataStorage.saveErrorData(String.valueOf(EvaluateLog), totalCount, errorCount, errorList);
                logger.log("质量任务-错误明细数据写入完成。");
                // 构建返回
                logger.log("质量任务-规则执行完成，构建结果对象返回。");
                return new QualityCheckResult(rule.getId(), batch, errorCount, totalCount);
            }
        } catch (Exception e) {
            logger.log("质量任务-规则执行异常，规则ID：" + rule.getId() + "，错误信息：" + e.getMessage());
            e.printStackTrace();
            return new QualityCheckResult(rule.getId(), batch, e.getMessage());
        }
    }


}
