package com.datamaster.module.ai.agent.intent;

import java.util.Locale;

/** 只对用户请求分类；路由不替代权限校验，也不自动降级到自由问数。 */
public final class IntentRouter {
    public enum Intent { MANAGEMENT, METRIC, EXPLORATORY, CLARIFY }

    private IntentRouter() { }

    public static Intent route(String original) {
        if (original == null || original.trim().isEmpty()) return Intent.CLARIFY;
        String text = original.trim().toLowerCase(Locale.ROOT);
        boolean draftCreation = containsAny(text, "生成指标", "预览指标", "创建指标", "新增指标", "保存指标草稿", "指标草稿生成");
        if (draftCreation) {
            return containsAny(text, "然后分析", "再分析", "查询结果", "同比", "环比") ? Intent.CLARIFY : Intent.MANAGEMENT;
        }
        boolean metric = containsAny(text, "保存分析", "保存这个分析", "保存当前分析", "另存为分析", "指标", "同比", "环比", "趋势", "按维度", "度量", "metric", "排名", "分布", "画图", "图表",
                "支付金额", "销售额", "订单数", "日活", "月活", "活跃用户", "转化率", "毛利", "gmv");
        boolean rawQuery = containsAny(text, "明细", "表数据", "表结构", "字段", "空值", "重复记录", "sql");
        boolean managementWrite = explicitManagementWrite(text);
        if (managementWrite) {
            // “接入数据库并生成本体”属于管理流程；“生成本体再分析销售额”则需要分阶段。
            return metric || rawQuery ? Intent.CLARIFY : Intent.MANAGEMENT;
        }
        if (metric) return rawQuery ? Intent.CLARIFY : Intent.METRIC;
        boolean platformRead = containsAny(text, "数据源", "数据服务", "本体", "动作", "工作流", "编排", "探查任务", "开发任务", "审批")
                && containsAny(text, "查询", "查看", "列出", "搜索", "有哪些", "状态", "预览", "分析动作");
        if (platformRead && !rawQuery) return Intent.MANAGEMENT;
        if (rawQuery || containsAny(text, "查数", "查数据", "查一下", "查询数据", "探索", "数据库", "自然语言查询",
                "经营分析", "报告", "分析数据")) return Intent.EXPLORATORY;
        return Intent.CLARIFY;
    }

    private static boolean explicitManagementWrite(String text) {
        if (containsAny(text, "启动任务", "运行任务", "生成本体", "生成动作", "提交动作", "执行动作",
                "创建工作流", "生成工作流", "创建编排", "探查元数据", "同步资产", "同步能力", "生成skill", "生成 skill")) return true;
        boolean writeVerb = containsAny(text, "创建", "新增", "修改", "更新", "删除", "发布", "停用", "保存", "启动", "运行", "提交", "生成", "接入", "绑定");
        boolean resource = containsAny(text, "数据源", "数据服务", "任务", "本体", "动作", "工作流", "编排", "资产", "探查", "skill");
        return writeVerb && resource;
    }

    private static boolean containsAny(String text, String... terms) {
        for (String term : terms) if (text.contains(term)) return true;
        return false;
    }
}
