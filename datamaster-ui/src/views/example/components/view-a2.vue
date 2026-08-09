<template>
    <dp-main>
        <template #header>
            <a-form :model="params" ref="realForm" :label-col="{ style: { width: '90px' } }" layout="inline">
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
                <a-form-item>
                    <a-button type="primary" :icon="SearchOutlined"> 查询 </a-button>
                </a-form-item>
            </a-form>
        </template>
        <dp-shrink width="600px" placement="right" @change="handleShrinkChange">
            <template #flex>
                <div class="flex-content">
                    <dp-table :fn="getTableData" :column="tableColumn" />
                </div>
            </template>

            <view-a2-chart ref="viewA2ChartRef" />
        </dp-shrink>
    </dp-main>
</template>

<script setup name="DetailPopResViewA2">
    import ViewA2Chart from './view-a2-chart.vue';
    import { SearchOutlined } from '@ant-design/icons-vue';
    import moment from 'moment';

    const params = ref({
        avgTypeId: '1',
        startDate: moment().subtract(7, 'days').format('YYYY-MM-DD'),
        endDate: moment().format('YYYY-MM-DD')
    });

    const avgTypeIdOptions = [
        {
            value: '1',
            label: '日均'
        },
        {
            value: '4',
            label: '旬均'
        },
        {
            value: '5',
            label: '月均'
        },
        {
            value: '6',
            label: '年均'
        }
    ];

    const tableColumn = ref([
        {
            prop: 'tb1',
            label: '时间',
            width: '150'
        },
        {
            prop: 'tb2',
            label: '库水位',
            unit: '(m)'
        },
        {
            prop: 'tb3',
            label: '入库流量',
            unit: '(m³/s)'
        },
        {
            prop: 'tb4',
            label: '出库流量',
            unit: '(m³/s)'
        },
        {
            prop: 'tb5',
            label: '蓄水量',
            unit: '(10⁶m³)'
        }
    ]);

    function getTableData() {
        return new Promise((resolve) => {
            setTimeout(() => {
                const data = [];
                for (let i = 0; i < 23; i++) {
                    data.push({
                        tb1: '2024-10-21 08:00:00',
                        tb2: 863.2,
                        tb3: 0,
                        tb4: undefined,
                        tb5: 100 + i
                    });
                }
                resolve(data);
            }, 2000);
        });
    }

    const viewA2ChartRef = ref(null);
    function handleShrinkChange(e) {
        console.log('收缩区域状态改变', e);
        setTimeout(() => {
            viewA2ChartRef.value.resize();
        }, 300);
    }
</script>

