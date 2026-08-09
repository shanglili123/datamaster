<template>
    <dp-main>
        <template #header>
            <a-form :model="params" ref="realForm" :label-col="{ style: { width: '90px' } }" layout="inline">
                <a-form-item label="年份：" name="years">
                    <a-dropdown :trigger="['click']" placement="bottom-start">
                        <a-input style="width: 120px" v-model:value="params.years" readonly></a-input>
                        <template #overlay>
                            <div style="padding: 6px 10px; width: 120px">
                                <a-checkbox-group v-model:value="params.years">
                                    <a-checkbox
                                        v-for="(item, index) in yearsOptions"
                                        :key="index"
                                        :value="item.label"
                                        style="width: 100%"
                                        @click.stop
                                    >
                                        {{ item.label }}
                                    </a-checkbox>
                                </a-checkbox-group>
                            </div>
                        </template>
                    </a-dropdown>
                </a-form-item>
                <a-form-item label="时段选择：" name="avgTypeId">
                    <a-select style="width: 80px" v-model:value="params.avgTypeId" placeholder="请选择">
                        <a-select-option
                            v-for="item in avgTypeIdOptions"
                            :key="item.value"
                            :value="item.value"
                        >
                            {{ item.label }}
                        </a-select-option>
                    </a-select>
                </a-form-item>
                <a-form-item label="极值类型" name="sizeType">
                    <a-radio-group v-model:value="params.sizeType">
                        <a-radio value="极大值">极大值</a-radio>
                        <a-radio value="极小值">极小值</a-radio>
                    </a-radio-group>
                </a-form-item>
                <a-form-item label="图表类型" name="chartType">
                    <a-radio-group v-model:value="params.chartType">
                        <a-radio value="bar">柱状图</a-radio>
                        <a-radio value="line">折线图</a-radio>
                    </a-radio-group>
                </a-form-item>
                <a-form-item>
                    <a-button type="primary" :icon="SearchOutlined" @click="getChartData">
                        查询
                    </a-button>
                </a-form-item>
            </a-form>
        </template>
        <dp-shrink width="600px" placement="right" @change="handleShrinkChange">
            <template #flex>
                <div class="flex-content">
                    <dp-table :fn="getTableData" :column="tableColumn" />
                </div>
            </template>

            <dp-chart-a
                ref="dpChartARef"
                title="极大值折线图"
                :xData="dpChartAXData"
                :yData="dpChartAYData"
            />
        </dp-shrink>
    </dp-main>
</template>

<script setup name="DetailPopResViewA4">

    import { SearchOutlined } from '@ant-design/icons-vue';

    const params = ref({
        avgTypeId: '1',
        years: ['2024', '2023'],
        sizeType: '极大值',
        chartType: 'line'
    });

    const yearsOptions = [
        {
            label: '2024'
        },
        {
            label: '2023'
        },
        {
            label: '2022'
        },
        {
            label: '2021'
        },
        {
            label: '2020'
        },
        {
            label: '2019'
        }
    ];

    const avgTypeIdOptions = [
        {
            value: '1',
            label: '日'
        },
        {
            value: '4',
            label: '旬'
        },
        {
            value: '5',
            label: '月'
        },
        {
            value: '6',
            label: '年'
        }
    ];

    const tableColumnConfig = {
        极大值: [
            {
                prop: 'htrz',
                label: '最高水位',
                width: '120',
                unit: '(m)'
            },
            {
                prop: 'htrzTm',
                label: '发生时间'
            },
            {
                prop: 'mxW',
                label: '最大蓄水量',
                unit: '(10⁶m³)'
            },
            {
                prop: 'mxWTm',
                label: '发生时间'
            }
        ],

        极小值: [
            {
                prop: 'ltrz',
                label: '最低水位',
                unit: '(m)',
                width: '120'
            },
            {
                prop: 'ltrzTm',
                label: '发生时间'
            },
            {
                prop: 'mnW',
                label: '最小蓄水量',
                unit: '(10⁶m³)'
            },
            {
                prop: 'mnWTm',
                label: '发生时间'
            }
        ]
    };

    // el-table动态表头
    const tableColumn = ref([]);
    watchEffect(() => {
        tableColumn.value = tableColumnConfig[params.value.sizeType];
    });
    function getTableData() {
        return new Promise((resolve) => {
            setTimeout(() => {
                const data = [];
                for (let i = 0; i < 23; i++) {
                    data.push({
                        htrz: i,
                        ltrz: i
                    });
                }
                resolve(data);
            }, 2000);
        });
    }

    const dpChartARef = ref(null);
    const dpChartAXData = ref([]);
    const dpChartAYData = ref([]);
    function getChartData() {
        setTimeout(() => {
            dpChartAXData.value = ['x0', 'x1', 'x2', 'x3', 'x4'];
            dpChartAYData.value = [
                {
                    name: `2023`,
                    data: [150, 230, 224, 218, 135],
                    barWidth: 10,
                    type: params.value.chartType
                },
                {
                    name: `2024`,
                    data: [220, 182, 191, 234, 290],
                    barWidth: 10,
                    type: params.value.chartType
                }
            ];
            const { chartOptions } = dpChartARef.value.useDpChartA();
            const isLine = params.value.chartType == 'line';
            chartOptions.value.xAxis.boundaryGap = !isLine ? true : false;
            chartOptions.value.yAxis.max = function (value, maxValue) {
                if (value.max === value.min) {
                    return parseFloat((value.max + value.max + 0.1).toFixed(1)); // 更小的增量
                }
                var maxN = value.max + (value.max - value.min) * 0.1 + 0.1; // 更小的增量
                if (maxValue) {
                    if (maxN < maxValue) {
                        maxN = maxValue + 1; // 更小的修正值
                    }
                }
                return parseFloat(maxN.toFixed(1));
            };
            chartOptions.value.yAxis.min = function (value, minValue) {
                if (value.max === value.min) {
                    var minN1 = value.min - value.min * 0.1; // 更小的增量
                    if (minN1 < 0) {
                        return 0;
                    } else {
                        return parseFloat(minN1.toFixed(1));
                    }
                }
                var minN = value.min - (value.max - value.min) * 0.1; // 更小的增量
                if (minN < 0) {
                    return 0;
                } else {
                    if (minValue) {
                        if (minN > minValue) {
                            minN = minValue - 1; // 更小的修正值
                        }
                    }
                    return parseFloat(minN.toFixed(1));
                }
            };
            dpChartARef.value.updateEcharts();
        }, 1000);
    }
    getChartData();

    function handleShrinkChange(e) {
        console.log('收缩区域状态改变', e);
        setTimeout(() => {
            dpChartARef.value.resize();
        }, 300);
    }
</script>

