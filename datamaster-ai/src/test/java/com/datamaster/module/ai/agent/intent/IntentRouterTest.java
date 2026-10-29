package com.datamaster.module.ai.agent.intent;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class IntentRouterTest {
    @Test
    public void routesManagementFlowsAndReadOnlyResourceQueries() {
        assertEquals(IntentRouter.Intent.MANAGEMENT, IntentRouter.route("接入数据库并生成本体"));
        assertEquals(IntentRouter.Intent.MANAGEMENT, IntentRouter.route("创建数据源"));
        assertEquals(IntentRouter.Intent.MANAGEMENT, IntentRouter.route("查看本体动作定义"));
        assertEquals(IntentRouter.Intent.MANAGEMENT, IntentRouter.route("查询数据源列表"));
        assertEquals(IntentRouter.Intent.MANAGEMENT, IntentRouter.route("发布探查任务"));
        assertEquals(IntentRouter.Intent.MANAGEMENT, IntentRouter.route("为当前概念生成指标草稿"));
        assertEquals(IntentRouter.Intent.MANAGEMENT, IntentRouter.route("预览指标候选"));
    }

    @Test
    public void routesMetricsEvenWhenAskedForAnAnalysisReport() {
        assertEquals(IntentRouter.Intent.METRIC, IntentRouter.route("本月支付金额是多少"));
        assertEquals(IntentRouter.Intent.METRIC, IntentRouter.route("执行指标分析"));
        assertEquals(IntentRouter.Intent.METRIC, IntentRouter.route("生成销售额趋势报告"));
        assertEquals(IntentRouter.Intent.METRIC, IntentRouter.route("日活用户同比"));
        assertEquals(IntentRouter.Intent.METRIC, IntentRouter.route("保存当前分析"));
        assertEquals(IntentRouter.Intent.METRIC, IntentRouter.route("按机构排名并生成图表"));
    }

    @Test
    public void keepsRawQueriesAndMixedRequestsSeparate() {
        assertEquals(IntentRouter.Intent.EXPLORATORY, IntentRouter.route("查看订单表字段和空值"));
        assertEquals(IntentRouter.Intent.EXPLORATORY, IntentRouter.route("查询订单明细"));
        assertEquals(IntentRouter.Intent.CLARIFY, IntentRouter.route("生成本体后分析销售额"));
        assertEquals(IntentRouter.Intent.CLARIFY, IntentRouter.route("查指标并给出明细 SQL"));
        assertEquals(IntentRouter.Intent.CLARIFY, IntentRouter.route("继续"));
        assertEquals(IntentRouter.Intent.CLARIFY, IntentRouter.route(null));
    }
}
