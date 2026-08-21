<template>
    <div class="app-container stagingIndex">
        <a-spin :spinning="loading">
        <!-- 顶部区域：评分 + 折线图 -->
        <a-row :gutter="20" class="top-section">
            <!-- 左侧评分 -->
            <a-col :xs="24" :sm="24" :md="12" class="stats-panel">
                <div class="module-8 border-item">
                    <div class="border-item-head">
                        <span class="head-title">质量探查维度统计 </span>
                    </div>
                    <div class="border-item-body">
                        <div class="overall-score">
                            <span>整体质量探查评分：</span>
                            <span class="score" :class="getScoreClass(overallScore)">
                                {{ overallScore || '-' }}
                            </span>
                        </div>
                        <a-table
                            :data-source="summaryList"
                            :columns="[
                                { title: '质量维度', dataIndex: 'dimensionType', align: 'center' },
                                { title: '规则数', dataIndex: 'succesTotal', align: 'center' },
                                { title: '问题数占比', dataIndex: 'proportion', align: 'center' },
                                { title: '趋势', key: 'trend', align: 'center' },
                            ]"
                            :bordered="true"
                            size="small"
                            :pagination="false"
                            :scroll="{ y: 246 }"
                            style="margin-top: 12px"
                        >
                            <template #bodyCell="{ column, record }">
                                <template v-if="column.dataIndex === 'dimensionType'">
                                    <dict-tag :options="att_rule_audit_q_dimension" :value="record.dimensionType" />
                                </template>
                                <template v-if="column.dataIndex === 'succesTotal'">
                                    {{ record.succesTotal || '-' }}
                                </template>
                                <template v-if="column.dataIndex === 'proportion'">
                                    {{ record.proportion != null ? record.proportion + '%' : '-' }}
                                </template>
                                <template v-if="column.key === 'trend'">
                                    <template v-if="record.trendType == '-3'">-</template>
                                    <template v-else-if="record.trendType == '1'">
                                        <ArrowUpOutlined style="color: green" />
                                    </template>
                                    <template v-else>
                                        <ArrowDownOutlined style="color: red" />
                                    </template>
                                </template>
                            </template>
                        </a-table>
                    </div>
                </div>
            </a-col>

            <!-- 右侧折线图 -->
            <a-col :xs="24" :sm="24" :md="12" class="trend-chart-panel">
                <div class="module-8 border-item">
                    <div class="border-item-head">
                        <span class="head-title">治理数据量变化趋势</span>
                        <a-select v-model:value="selectedRange" size="small" placeholder="选择时间范围"
                            style="width: 120px" @change="onRangeChange">
                            <a-select-option v-for="item in rangeOptions" :key="item.value" :value="item.value">
                                {{ item.label }}
                            </a-select-option>
                        </a-select>
                    </div>
                    <div class="border-item-body">
                        <div ref="chartRef" class="echart-container"></div>
                    </div>
                </div>
            </a-col>
        </a-row>

        <!-- 规则列表 -->
        <a-row>
            <div class="module-8 border-item" style="width: 100%">
                <div class="border-item-head">
                    <span class="head-title">规则列表</span>
                </div>
                <div class="border-item-body" style="height: 360px;">
                    <a-table
                        striped
                        :loading="loading"
                        :data-source="pagedRuleList"
                        :columns="ruleTableColumns"
                        :pagination="false"
                        :scroll="{ y: 300 }"
                        :locale="{ emptyText: '暂无记录' }"
                    >
                        <template #bodyCell="{ column, record }">
                            <template v-if="column.key === 'evaluateName'">
                                {{ getEvaluateName(record) }}
                            </template>
                            <template v-if="column.dataIndex === 'datasourceName'">
                                {{ record.datasourceName || '-' }}
                            </template>
                            <template v-if="column.dataIndex === 'columnLabel'">
                                {{ record.columnLabel || '-' }}
                            </template>
                            <template v-if="column.dataIndex === 'dimensionType'">
                                <dict-tag :options="att_rule_audit_q_dimension" :value="record.dimensionType" />
                            </template>
                            <template v-if="column.dataIndex === 'ruleName'">
                                {{ record.ruleName || '-' }}
                            </template>
                            <template v-if="column.dataIndex === 'proportion'">
                                {{
                                    (record.problemTotal != -1 && record.problemTotal != null)
                                        ? `${record.problemTotal} /条 ${record.proportion ?? '-'}%`
                                        : '-'
                                }}
                            </template>
                            <template v-if="column.key === 'actions'">
                                <a-button type="link" size="small" @click="openDialog(record)">查看问题数据</a-button>
                            </template>
                        </template>
                    </a-table>
                    <pagination
                        v-show="ruleList.length > 0"
                        :total="ruleList.length"
                        v-model:page="ruleQueryParams.pageNum"
                        v-model:limit="ruleQueryParams.pageSize"
                    />
                </div>
            </div>
        </a-row>

        <!-- 问题数据弹窗 -->
        <ProblemDialog ref="problemDialogRef" />
        </a-spin>
    </div>
</template>

<script setup>
import * as echarts from 'echarts';
import { useRoute } from 'vue-router';
import { computed, ref, onMounted, onBeforeUnmount } from 'vue';
import moment from 'moment';
const { proxy } = getCurrentInstance();
import { ArrowUpOutlined, ArrowDownOutlined } from '@ant-design/icons-vue';
import ProblemDialog from '../components/problemData.vue';
import {
    statisticsEvaluateOne,
    statisticsEvaluateTow,
    statisticsEvaluateTable
} from "@/api/ast/quality/probeTaskInstance";
const { att_rule_audit_q_dimension, } = proxy.useDict(

    'att_rule_audit_q_dimension'
);
const getScoreClass = (score) => {
    if (score == null || score === '-') return 'score-null';
    if (score >= 85) return 'score-high';
    if (score >= 60) return 'score-medium';
    return 'score-low';
};
const route = useRoute();
const chartRef = ref(null);
let chartInstance = null;
let problemDialogRef = ref();
function getEvaluateName(row) {
    if (!row.rule) return '-';
    try {
        return JSON.parse(row.rule)?.evaluateName || '-';
    } catch {
        return '-';
    }
}
const openDialog = (row) => {
    problemDialogRef.value?.open(row);
};

const selectedRange = ref('7');
const rangeOptions = [
    { label: '近7天', value: '7' },
    { label: '近15天', value: '15' },
    { label: '近30天', value: '30' }
];

const ruleList = ref([]);
const ruleQueryParams = ref({
    pageNum: 1,
    pageSize: 6,
});
const pagedRuleList = computed(() => {
    const start = (ruleQueryParams.value.pageNum - 1) * ruleQueryParams.value.pageSize;
    return ruleList.value.slice(start, start + ruleQueryParams.value.pageSize);
});
const overallScore = ref();
const summaryList = ref([]);
const loading = ref(false);

const columns = ref([

    { key: 8, label: "规则名称", visible: true },
    { key: 1, label: "数据库名称", visible: true },
    { key: 2, label: "字段名/中文名", visible: true },
    { key: 3, label: "质量维度", visible: true },
    { key: 5, label: "规则名称", visible: true },
    { key: 7, label: "问题数据量占比", visible: true },
]);
function getLabelsByColumnName(row, columnName) {
    if (!columnName) return '-';
    const names = String(columnName).split(',').map(n => n.trim()).filter(Boolean);
    if (!names.length) return '-';
    // 旧执行记录可能没有保留规则 JSON，此时至少展示原始字段名称。
    if (!row.rule) return names.join(' , ');
    let evaColumns = [];
    try {
        const ruleObj = typeof row.rule === 'string' ? JSON.parse(row.rule) : row.rule;
        evaColumns = Array.isArray(ruleObj.evaColumns)
            ? ruleObj.evaColumns
            : Object.values(ruleObj.evaColumns || {});
    } catch (err) {
        console.warn('规则字段解析失败', err);
        return names.join(' , ');
    }

    if (!Array.isArray(evaColumns)) return names.join(' , ');

    const labels = names.map(name => {
        const match = evaColumns.find(col => col.name === name);
        return match?.label || name;
    });

    return labels.join(' , ');
}


const getColumnVisibility = (key) => {
    const column = columns.value.find((col) => col.key === key);
    return column ? column.visible : true;
};

const ruleTableColumns = computed(() => {
    const allCols = [
        { title: '评测名称', key: 'evaluateName', align: 'center', ellipsis: true, colKey: 8 },
        { title: '数据库名称', dataIndex: 'datasourceName', align: 'center', ellipsis: true, colKey: 1 },
        { title: '字段名/中文名', dataIndex: 'columnLabel', align: 'center', ellipsis: true, colKey: 2 },
        { title: '质量维度', dataIndex: 'dimensionType', align: 'center', ellipsis: true, colKey: 3 },
        { title: '稽查名称', dataIndex: 'ruleName', align: 'center', ellipsis: true, colKey: 5 },
        { title: '问题数据量占比', dataIndex: 'proportion', align: 'center', ellipsis: true, colKey: 7 },
        { title: '操作', key: 'actions', align: 'center', fixed: 'right', width: 140, colKey: 'actions' },
    ];
    return allCols.filter(col => getColumnVisibility(col.colKey));
});

const loadChartWithData = (data = []) => {
    let { title = [], value = [] } = data;
    if (!chartInstance && chartRef.value) {
        chartInstance = echarts.init(chartRef.value);
    }

    const range = Number(selectedRange.value);
    const dateList = Array.from({ length: range }, (_, i) =>
        moment().subtract(range - i - 1, 'days').format('MM-DD')
    );

    const maxValue = Math.max(...value, 0);
    const minYMax = 30;
    const yMax = Math.max(minYMax, Math.ceil(maxValue / 5) * 5);

    const option = {
        legend: {
            data: ['质量趋势'],
            left: 'center',
        },
        tooltip: { trigger: 'axis' },
        xAxis: {
            type: 'category',
            data: title,
            axisTick: { show: false },
            axisLine: {
                lineStyle: { color: 'rgba(0,0,0,0.15)' }
            },
            axisLabel: {
                margin: 14,
                fontSize: 12,
                color: 'rgba(0,0,0,0.65)',
                fontFamily: 'PingFangSC, PingFang SC',
            }
        },
        yAxis: {
            type: 'value',
            min: 0,
            max: yMax,
            interval: 5,
            nameTextStyle: {
                color: 'rgba(0,0,0,0.85)',
                fontSize: 14,
                padding: [0, 0, 10, -18],
                fontFamily: 'PingFangSC, PingFang SC',
            },
            axisLine: {
                lineStyle: { color: 'rgba(0,0,0,0.15)' }
            },
            axisLabel: {
                fontSize: 12,
                color: 'rgba(0,0,0,0.65)',
                fontFamily: 'PingFangSC, PingFang SC',
            }
        },
        grid: { left: '3%', right: '4%', bottom: '0%', containLabel: true },
        series: [{
            name: '质量趋势',
            type: 'line',
            data: value,
            symbolSize: 8,
            itemStyle: {
                color: '#427afd',
                borderColor: '#427afd',
                borderWidth: 1
            },
            lineStyle: {
                color: '#5285fd',
                width: 2
            },
            areaStyle: {
                color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                    { offset: 0, color: 'rgba(204, 220, 254, 1)' },
                    { offset: 1, color: 'rgba(204, 220, 254, 0)' }
                ])
            }
        }]
    };

    chartInstance.setOption(option);
};



// 评分和质量维度汇总
// 评分和质量维度汇总
const loadScoreAndSummary = async (id) => {
    try {
        const res = await statisticsEvaluateOne(id);
        const result = res?.data || [];

        // 构造一个维度映射，用于快速查找
        const resultMap = result.reduce((map, item) => {
            map[item.dimensionType] = item;
            return map;
        }, {});

        summaryList.value = att_rule_audit_q_dimension.value.map(dim => {
            return resultMap[dim.value] || {
                dimensionType: dim.value,
                succesTotal: 0,
                proportion: 0,
                trendType: '-1',
            };
        });
    } catch (err) {
        console.warn('评分/维度汇总失败', err);
    }
};
// 规则列表
const loadRuleTable = async (id) => {
    try {
        const res = await statisticsEvaluateTable(id);
        if (res.data && Array.isArray(res.data)) {
            ruleList.value = res.data.map(item => {
                return {
                    ...item,
                    columnLabel: getLabelsByColumnName(item, item.columnName)
                };
            });
        } else {
            ruleList.value = [];
        }
        ruleQueryParams.value.pageNum = 1;
    } catch (err) {
        console.warn('规则列表失败', err);
    } finally {
    }
};


// 折线图数据
const loadTrendChart = async (id) => {
    try {
        const range = Number(selectedRange.value);
        const today = moment().format('YYYY-MM-DD');
        const oldDate = moment().subtract(Number(selectedRange.value), 'days').format('YYYY-MM-DD');
        const type = selectedRange.value === '7' ? 0 : selectedRange.value === '15' ? 1 : 2;
        const res = await statisticsEvaluateTow({ id, deDate: today, oldDate, type });
        console.log("🚀 ~ loadTrendChart ~ res:", res)

        loadChartWithData(res?.data || []);
    } catch (err) {
        console.warn('折线图数据失败', err);
    }
};

//
const fetchData = async (id) => {
    loading.value = true;
    await Promise.all([
        loadScoreAndSummary(id),
        loadRuleTable(id),
        loadTrendChart(id)
    ]);
    loading.value = false;
};

const onRangeChange = () => {
    const id = route.query.id || 'default';
    loadTrendChart(id)
};

const handleResize = () => {
    chartInstance?.resize();
};

onMounted(() => {
    const id = route.query.id || 'default';
    overallScore.value = route.query.score
    fetchData(id);
    window.addEventListener('resize', handleResize);
});

onBeforeUnmount(() => {
    window.removeEventListener('resize', handleResize);
});
</script>

<style lang="scss" scoped>
.top-section {
    margin-bottom: 20px;
}

.echart-container {
    height: 100%;
    width: 100%;
}

.border-item {
    width: 100%;
    background: #fff;
    border-radius: 2px;

    .border-item-head {
        height: 50px;
        padding: 0 20px;
        display: flex;
        justify-content: space-between;
        align-items: center;
        border-bottom: 1px solid #e8e8e8;

        .head-title {
            font-size: 16px;
            font-weight: 500;
            display: flex;
            align-items: center;

            &::before {
                content: "";
                display: inline-block;
                width: 3px;
                height: 20px;
                background: var(--el-color-primary);
                margin-right: 10px;
                border-radius: 2px;
            }
        }
    }

    .border-item-body {
        height: 360px;
        padding: 10px 20px;
        background-color: #fff;
    }
}

.overall-score {
    display: flex;
    justify-content: center;
    align-items: center;
    font-size: 16px;
    margin-bottom: 10px;

    .score {
        font-size: 24px;
        font-weight: 700;
        margin-left: 8px;
    }
}

.score-high {
    color: #16a34a;
    ;
}

.score-medium {
    color: #faad14;
}

.score-low {
    color: #f5222d;
}

.score-null {
    color: #999;
}
</style>

