<template>
    <dp-main>
        <template #header>
            <a-form :model="params" ref="realForm" :label-col="{ style: { width: '90px' } }" layout="inline">
                <a-form-item label="监测点：" name="jcd">
                    <a-select v-model:value="params.jcd" placeholder="全部监测点" style="width: 120px">
                        <a-select-option
                            v-for="(item, index) in jcdOptions"
                            :key="index"
                            :value="item"
                        >
                            {{ item }}
                        </a-select-option>
                    </a-select>
                </a-form-item>
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
                <a-form-item label="位移：" name="checkVal">
                    <a-radio-group v-model:value="params.checkVal">
                        <a-radio
                            v-for="(item, index) in checkValOptions"
                            :key="index"
                            :value="item.value"
                        >
                            {{ item.label }}
                        </a-radio>
                    </a-radio-group>
                </a-form-item>
                <a-form-item>
                    <a-button type="primary" :icon="SearchOutlined"> 查询 </a-button>
                </a-form-item>
            </a-form>
        </template>
        <dp-shrink width="600px" placement="right" @change="handleShrinkChange">
            <template #flex>
                <div class="flex-content">
                    <div class="dp-main--h3">监测预警</div>
                    <div class="tables-wrap">
                        <dp-table :table="tableOptions" :fn="getTableDataA" :column="tableColumnA">
                            <!-- 自定义插槽 -->
                            <template #warning="scope">
                                <a-tag>{{ scope.row.rz }}</a-tag>
                            </template>
                        </dp-table>
                        <dp-table
                            :table="tableOptions"
                            :fn="getTableDataB"
                            :column="tableColumnB"
                        />
                    </div>
                </div>
            </template>

            <dp-chart-a
                ref="dpChartARef"
                title="测点水平/垂直位移过程线"
                :xData="dpChartAXData"
                :yData="dpChartAYData"
            />
        </dp-shrink>
    </dp-main>
</template>

<script setup name="DetailPopResViewC1">

    import { SearchOutlined } from '@ant-design/icons-vue';
    import moment from 'moment';

    const params = ref({
        jcd: '',
        checkVal: 'x',
        startDate: moment().subtract(7, 'days').format('YYYY-MM-DD'),
        endDate: moment().format('YYYY-MM-DD')
    });

    const jcdOptions = ref(['WY01', 'WY02', 'WY03', 'WY04']);

    const checkValOptions = ref([
        {
            label: 'x向水平位移',
            value: 'x'
        },
        {
            label: 'y向水平位移',
            value: 'y'
        },
        {
            label: 'z向垂直位移',
            value: 'z'
        }
    ]);

    const tableOptions = {
        height: '230'
    };

    const tableColumnA = ref([
        {
            prop: 'tm',
            label: '时间',
            width: '140'
        },
        {
            prop: 'rz',
            label: '实际值',
            unit: '(m)'
        },
        {
            prop: 'inq',
            label: '预警类型',
            slot: 'warning'
        },
        {
            prop: 'otq',
            label: '差值',
            unit: '(m)'
        },
        {
            prop: 'w',
            label: '监测方位'
        }
    ]);

    function getTableDataA() {
        return new Promise((resolve) => {
            setTimeout(() => {
                const data = [];
                for (let i = 0; i < 23; i++) {
                    data.push({
                        tm: '2024-10-21 08:00:00',
                        rz: 863.2,
                        inq: 0,
                        otq: undefined,
                        w: 100 + i
                    });
                }
                resolve(data);
            }, 2000);
        });
    }

    const tableColumnB = ref([
        {
            prop: 'MPCD',
            label: '测点编号',
            width: '140'
        },
        {
            prop: 'MSTM',
            label: '时间'
        },
        {
            prop: 'inq',
            label: '入库流量',
            unit: '(m³/s)'
        },
        {
            prop: 'XHRDS',
            label: 'x向水平位移',
            unit: '(m)'
        }
    ]);

    function getTableDataB() {
        return new Promise((resolve) => {
            setTimeout(() => {
                const data = [];
                for (let i = 0; i < 23; i++) {
                    data.push({
                        tm: '2024-10-21 08:00:00',
                        rz: 863.2,
                        inq: 0,
                        otq: undefined,
                        w: 100 + i
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
                    type: 'line'
                },
                {
                    name: `2024`,
                    data: [220, 182, 191, 234, 290],
                    barWidth: 10,
                    type: 'line'
                }
            ];

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

<style lang="scss" scoped>
    .flex-content {
        .dp-main--h3 {
            margin: 10px 0 20px;
        }

        .tables-wrap {
            height: calc(100% - 50px);
            .dp-table {
                height: 50%;
            }
        }
    }
</style>

