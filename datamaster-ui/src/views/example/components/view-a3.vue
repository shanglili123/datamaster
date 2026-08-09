<template>
    <dp-main>
        <template #header>
            <a-form :model="params" ref="realForm" :label-col="{ style: { width: '90px' } }" layout="inline">
                <a-form-item label="开始时间：" name="startDate">
                    <a-date-picker
                        v-model:value="params.startDate"
                        valueFormat="YYYY-MM-DD"
                        placeholder="开始时间"
                        :allow-clear="false"
                        style="width: 140px"
                    >
                    </a-date-picker>
                </a-form-item>
                <a-form-item label="结束时间：" name="endDate">
                    <a-date-picker
                        v-model:value="params.endDate"
                        valueFormat="YYYY-MM-DD"
                        placeholder="结束时间"
                        :allow-clear="false"
                        style="width: 140px"
                    >
                    </a-date-picker>
                </a-form-item>
                <a-form-item label="比较年：" name="years">
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
                <a-form-item label="" name="years">
                    <a-radio-group v-model:value="params.radio">
                        <a-radio
                            :value="item.value"
                            :key="item.value"
                            v-for="item in radioOptions"
                        >
                            {{ item.label }}
                        </a-radio>
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
                    <dp-table :fn="getTableData" :pageSize="13" :column="tableColumn" />
                </div>
            </template>

            <dp-chart-a
                ref="dpChartARef"
                title="历史同期对比"
                :xData="dpChartAXData"
                :yData="dpChartAYData"
            />
        </dp-shrink>
    </dp-main>
</template>

<script setup name="DetailPopResViewA3">

    import { SearchOutlined } from '@ant-design/icons-vue';
    import moment from 'moment';

    const params = ref({
        startDate: moment().subtract(7, 'days').format('YYYY-MM-DD'),
        endDate: moment().format('YYYY-MM-DD'),
        years: ['2024', '2023'],
        radio: 'otq'
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

    const radioOptions = [
        { value: 'rz', label: '库水位', unit: 'm' },
        { value: 'inq', label: '入库流量', unit: 'm³/s' },
        { value: 'otq', label: '出库流量', unit: 'm³/s' },
        { value: 'w', label: '蓄水量', unit: '10⁶m³' }
    ];

    const activeRadioConfig = computed(() => {
        return radioOptions.find((item) => item.value === params.value.radio);
    });

    const tableColumn = ref([
        {
            prop: 'tm',
            label: '时间'
        },
        {
            prop: 'y2023',
            label: '2023'
        },
        {
            prop: 'y2024',
            label: '2024'
        }
    ]);

    function getTableData() {
        return new Promise((resolve) => {
            setTimeout(() => {
                const data = [];
                for (let i = 0; i < 23; i++) {
                    data.push({
                        tm: '2024-10-21 08:00:00',
                        y2023: (i + 1) * 20,
                        y2024: (i + 2) * 20
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
            const { label, unit } = activeRadioConfig.value;
            dpChartAXData.value = ['x0', 'x1', 'x2', 'x3', 'x4'];
            dpChartAYData.value = [
                {
                    name: `2023-${label}`,
                    data: [150, 230, 224, 218, 135],
                    type: 'line'
                },
                {
                    name: `2024-${label}`,
                    data: [220, 182, 191, 234, 290],
                    type: 'line'
                }
            ];

            // dp-chart-a自定义配置
            const { chartOptions } = dpChartARef.value.useDpChartA();

            chartOptions.value.yAxis.name = `${label}(${unit}) `;
            chartOptions.value.yAxis.max = function (value) {
                if (value.max === value.min) {
                    return parseFloat((value.max + value.max + 0.3).toFixed(1));
                }
                var maxN = value.max + (value.max - value.min) * 0.3 + 0.1;
                return parseFloat(maxN.toFixed(1));
            };
            chartOptions.value.yAxis.min = function (value) {
                if (value.max === value.min) {
                    var minN1 = value.min - value.min * 0.2;
                    if (minN1 < 0) {
                        return 0;
                    } else {
                        return parseFloat(minN1.toFixed(1));
                    }
                }
                var minN = value.min - (value.max - value.min) * 0.3;
                if (minN < 0) {
                    return 0;
                } else {
                    return parseFloat(minN.toFixed(1));
                }
            };

            // dp-chart-a更新echarts
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

