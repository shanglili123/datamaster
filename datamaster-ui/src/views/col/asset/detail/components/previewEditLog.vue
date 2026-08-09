<template>
    <!-- 数据预览的修改记录弹窗 -->
    <a-modal v-model:open="visible" class="dialog" width="1200px" destroy-on-close>
        <template #header="{ close, titleId, titleClass }">
            <span role="heading" aria-level="2">
                {{ title }}
            </span>
        </template>
        <a-form ref="queryForm" :model="queryParams" layout="inline">
            <a-form-item label="时间" name="dataTime">
                <a-range-picker v-model:value="queryParams.dataTime" style="width: 250px" :allow-clear="false"
                    valueFormat="YYYY-MM-DD" @change="handleRangeChange" />
            </a-form-item>
            <a-form-item label="创建人" name="createBy">
                <a-input v-model:value="queryParams.createBy" placeholder="请输入创建人" style="width: 180px; margin-right: 10px"
                    class="filter-item" />
            </a-form-item>
            <a-form-item>
                <a-button style="margin-left: 7px" type="primary" @click="fetchData"
                    @mousedown="(e) => e.preventDefault()">
                    <i class="iconfont-mini icon-a-zu22377 mr5"></i>查询
                </a-button>
                <a-button @click="resetQuery" @mousedown="(e) => e.preventDefault()">
                    <i class="iconfont-mini icon-a-zu22378 mr5"></i>重置
                </a-button>
            </a-form-item>
        </a-form>
        <a-table :loading="loading" :data-source="list" :columns="columns" @change="handleTableChange"
            :size="tableSize === 'medium' ? 'middle' : tableSize" :scroll="{ y: tableHeight }"
            style="width: 100%; margin: 15px 0;">
            <template #bodyCell="{ column, record }">
                <template v-if="column.key === 'id'">
                    <div>{{ record.id || '-' }}</div>
                </template>
                <template v-else-if="column.key === 'createBy'">
                    <div>{{ record.createBy || '-' }}</div>
                </template>
                <template v-else-if="column.key === 'createTime'">
                    <div>{{ parseTime(record.createTime, "{y}-{m}-{d} {h}:{i}") || '-' }}</div>
                </template>
                <template v-else-if="column.key === 'updateBy'">
                    <div>{{ record.updateBy || '-' }}</div>
                </template>
                <template v-else-if="column.key === 'updateTime'">
                    <div>{{ parseTime(record.updateTime, "{y}-{m}-{d} {h}:{i}") || '-' }}</div>
                </template>
                <template v-else-if="column.key === 'status'">
                    <dict-tag :options="da_asset_operate_status" :value="record.status" />
                </template>
                <template v-else-if="column.key === 'action'">
                    <a-button type="link" v-if="record.updateBefore" :icon="h(EyeOutlined)"
                        @click="showDataDialog(record.id, record.updateBefore, record.updateAfter)">查看</a-button>
                    <a-button type="link" :icon="h(RollbackOutlined)" :disabled="record.status == 5"
                        @click="rollBackrollBack(record)">回滚</a-button>
                </template>
            </template>
        </a-table>
        <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum"
            v-model:limit="queryParams.pageSize" @pagination="getList" />
        <dataDiffDialog ref="dataDiff" @ok="ok" />
    </a-modal>
</template>

<script setup>
import { ref, reactive, watch } from "vue";
import { h } from 'vue';
import { EyeOutlined, RollbackOutlined } from '@ant-design/icons-vue';
// import { page } from "@/api/metadata/contentsTypeTaUp";
import dataDiffDialog from "./previewEditDiff.vue";
import { getDaAssetList, rollBack } from '@/api/ast/assetchild/operate/daAssetOperateLog.js';

const { proxy } = getCurrentInstance();
const { da_asset_operate_status, } = proxy.useDict(
    "da_asset_operate_status",
);
const emit = defineEmits(['success']);
const props = defineProps({
    columns: {
        type: Array,
        default: () => [],
    },
});

const title = "修改记录";
const loading = ref(false);
const visible = ref(false);
const tableSize = ref("medium");
const list = ref([]);
const total = ref(0);
const tableHeight = ref(document.body.offsetHeight - 650 + "px");
const queryForm = ref(null);
const dataDiff = ref(null);
const tableColumns = reactive([
    { prop: "updateTime", label: "修改时间", show: true, width: 150 },
    { prop: "createBy ", label: "修改人", show: true, width: 150 },
]);
const defaultSort = ref({ columnKey: 'create_time', order: 'desc' });
const columns = [
    { title: '编号', key: 'id', width: 75, align: 'left' },
    { title: '创建人', key: 'createBy', align: 'left' },
    { title: '创建时间', key: 'createTime', align: 'left', sorter: true, sorterKey: 'create_time', defaultSortOrder: 'descend' },
    { title: '更新人', key: 'updateBy', align: 'left' },
    { title: '更新时间', key: 'updateTime', align: 'left', sorter: true, sorterKey: 'update_time' },
    { title: '状态', key: 'status', align: 'left' },
    { title: '查看前后对比', key: 'action', align: 'left', width: 150 }
];

const queryParams = reactive({
    startTime: null,
    endTime: null,
    dataTime: [],
    pageNum: 1,
    pageSize: 6,
    creatorId: ""
    // updateWhere: {},
});

/** 排序触发事件 */
function handleSortChange({ column, prop, order }) {
    queryParams.orderByColumn = column?.columnKey || prop;
    queryParams.isAsc = column.order;
    getList();
}

/** antd 表格 change 适配：将 sorter 转为原 handleSortChange 期望的 { column, prop, order } 结构 */
function handleTableChange(pagination, filters, sorter) {
    const column = (sorter && sorter.column) || {};
    handleSortChange({
        column: { columnKey: column.sorterKey || column.dataIndex, order: sorter && sorter.order ? (sorter.order === 'descend' ? 'descending' : 'ascending') : undefined },
        prop: column.dataIndex
    });
}

/** antd 范围日期选择 change 适配：dateStrings 为格式化后的字符串 */
function handleRangeChange(dates, dateStrings) {
    if (dateStrings && dateStrings.length === 2) {
        queryParams.startTime = dateStrings[0] + " 00:00:00";
        queryParams.endTime = dateStrings[1] + " 23:59:59";
    } else {
        queryParams.startTime = "";
        queryParams.endTime = "";
    }
}

function formatDateTime(date) {
    const year = date.getFullYear();
    const month = (date.getMonth() + 1).toString().padStart(2, "0");
    const day = date.getDate().toString().padStart(2, "0");
    return `${year}-${month}-${day}`;
}

function handleDateChange(value) {
    if (value && value.length === 2) {
        queryParams.startTime = formatDateTime(value[0]) + " 00:00:00";
        queryParams.endTime = formatDateTime(value[1]) + " 23:59:59";
    } else {
        queryParams.startTime = "";
        queryParams.endTime = "";
    }
}
function resetQuery() {
    queryParams.pageNum = 1;
    queryParams.startTime = null;
    queryParams.endTime = null;
    queryParams.dataTime = [];
    queryParams.creatorId = "";
    queryParams.createBy = "";
    // queryForm.value.resetFields
    getList();
}
function fetchData() {
    queryParams.pageNum = 1;
    getList();
}

function showDataDialog(id, updateBefore, updateAfter) {
    dataDiff.value.show(id, updateBefore, updateAfter);
}
let columnsTwo = ref([])
function show(row, data) {
    queryParams.pageNum = 1;
    queryParams.startTime = null;
    queryParams.endTime = null;
    queryParams.dataTime = [];
    queryParams.creatorId = "";
    visible.value = true;
    list.value = [];
    queryParams.updateBefore = JSON.stringify(row)
    getList();

}
let uniqueKeys = ref([])
watch(
    () => props.columns,
    (arr) => {
        if (arr && arr.length > 0) {
            // 必填字段
            const requiredFields = arr.filter(item => item.columnNullable == true);
            // 所有非唯一键字段
            columnsTwo.value = arr.filter(item => item.columnKey == false);
            // 所有唯一键字段
            uniqueKeys.value = arr.filter(item => item.columnKey != false);
        }
    },
    { immediate: true }
);

function getList() {
    loading.value = true;
    // 唯一键字段数组拼成字符串
    const commentKeyList = uniqueKeys.value.map(item => item.en).join(',');
    const tableCommentList = [];
    // 组装 map-json 结构对象
    const fieldNamesObj = {
        tableCommentList,
        commentKeyList,
    };
    getDaAssetList({ ...queryParams, fieldNames: JSON.stringify(fieldNamesObj) }).then((response) => {
        loading.value = false;
        if (response.code == '200') {
            const { data } = response;
            list.value = data.rows;
            total.value = parseInt(data.total);
        }
    });
}

function rollBackrollBack(row) {
    loading.value = true
    rollBack(row.id)
        .then(res => {
            if (res.code == '200') {
                getList()
                emit('success');
            }
        })
        .finally(() => {
            loading.value = false
        })
}
function ok() {
    queryParams.pageNum = 1;
    getList();
}
defineExpose({ show });
</script>

