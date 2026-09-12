<template>
    <div class="app-container" ref="app-container">
        <div class="pagecont-top" v-show="showSearch">
            <a-form class="btn-style" :model="queryParams" ref="queryRef" layout="inline" :label-col="{ style: { width: '45px' } }"
                v-show="showSearch" @submit.prevent>
                <a-form-item label="名称" name="name">
                    <a-input v-model:value="queryParams.name" placeholder="请输入任务名称" allow-clear
                        @pressEnter="handleQuery" style="width: 150px;" />
                </a-form-item>
                <a-form-item label="状态" name="successFlag">
                    <a-select v-model:value="queryParams.successFlag" placeholder="请选择执行状态" allow-clear
                        style="width: 150px;">
                        <a-select-option v-for="dict in quality_log_success_flag" :key="dict.value" :value="dict.value">{{
                            dict.label }}</a-select-option>
                    </a-select>
                </a-form-item>
                <a-form-item>
                    <a-button type="primary" @click="handleQuery" @mousedown="(e) => e.preventDefault()">
                        <i class="iconfont-mini icon-a-zu22377 mr5"></i>查询
                    </a-button>
                    <a-button @click="resetQuery" @mousedown="(e) => e.preventDefault()">
                        <i class="iconfont-mini icon-a-zu22378 mr5"></i>重置
                    </a-button>
                </a-form-item>
            </a-form>
            <div class="top-right-btn">
                <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"
                    :columns="columns"></right-toolbar>
            </div>
        </div>
        <div>
            <a-table
                striped
                :loading="loading"
                :data-source="ProbeTaskInstanceList"
                :pagination="false"
                :columns="tableColumns"
                :locale="{ emptyText: '暂无记录' }"
                @change="handleTableChange"
            >
                <template #bodyCell="{ column, record }">
                    <template v-if="column.dataIndex === 'name'">
                        {{ record.name || '-' }}
                    </template>
                    <template v-if="column.dataIndex === 'score'">
                        {{ record.score }}
                    </template>
                    <template v-if="column.dataIndex === 'problemData'">
                        {{ record.problemData || '-' }}
                    </template>
                    <template v-if="column.dataIndex === 'successFlag'">
                        <dict-tag :options="quality_log_success_flag" :value="record.successFlag" />
                    </template>
                    <template v-if="column.dataIndex === 'startTime'">
                        <span>{{ parseTime(record.startTime, '{y}-{m}-{d} {h}:{i}') }}</span>
                    </template>
                    <template v-if="column.dataIndex === 'endTime'">
                        <span>{{ parseTime(record.endTime, '{y}-{m}-{d} {h}:{i}') }}</span>
                    </template>
                    <template v-if="column.key === 'actions'">
                        <a-button type="link" size="small" @click="
                            routeTo('/ast/quality/probeTaskInstance/detail', {
                                ...record,
                                info: true,
                            })
                            " v-hasPermi="['ast:probeTaskInstance:detail']">详情</a-button>
                    </template>
                </template>
            </a-table>

            <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum"
                v-model:limit="queryParams.pageSize" @pagination="getList" />
        </div>
    </div>
</template>

<script setup name="ProbeTaskInstance">
import { listProbeTaskInstance, doSendMessage } from "@/api/ast/quality/probeTaskInstance";
const { proxy } = getCurrentInstance();
import { useRoute, useRouter } from "vue-router"
import { ref, inject } from "vue";
const stationNavigation = inject("spaceWorkstationNavigation", null);
const defaultSort = ref({ prop: 'startTime', order: 'descending' });
const { quality_log_success_flag } = proxy.useDict(

    'quality_log_success_flag'
);
const ProbeTaskInstanceList = ref([]);
// 列显隐信息
const columns = ref([
    { key: 0, label: "编号", visible: true },
    { key: 1, label: "任务名称", visible: true },
    { key: 2, label: "质量评分", visible: true },
    { key: 3, label: "问题数据", visible: true },
    { key: 4, label: "执行状态", visible: true },
    { key: 5, label: "开始时间", visible: true },
    { key: 6, label: "结束时间", visible: true },
    { key: 7, label: "操作", visible: true },
]);
const getColumnVisibility = (key) => {
    const column = columns.value.find(col => col.key == key);
    if (!column) return true;
    return column.visible;
};

const tableColumns = computed(() => {
    const allCols = [
        { title: '编号', dataIndex: 'id', align: 'center', width: 120, colKey: 0 },
        { title: '任务名称', dataIndex: 'name', align: 'center', colKey: 1 },
        { title: '质量评分', dataIndex: 'score', align: 'center', key: 'score', sorter: true, colKey: 2 },
        { title: '问题数据', dataIndex: 'problemData', align: 'center', width: 300, ellipsis: true, colKey: 3 },
        { title: '执行状态', dataIndex: 'successFlag', align: 'center', colKey: 4 },
        { title: '开始时间', dataIndex: 'startTime', align: 'center', width: 160, key: 'start_time', sorter: true, defaultSortOrder: 'descend', ellipsis: true, colKey: 5 },
        { title: '结束时间', dataIndex: 'endTime', align: 'center', width: 160, key: 'end_time', sorter: true, ellipsis: true, colKey: 6 },
        { title: '操作', key: 'actions', align: 'center', fixed: 'right', width: 240, colKey: 7 },
    ];
    return allCols.filter(col => getColumnVisibility(col.colKey));
});
const loading = ref(false);
const showSearch = ref(true);
const total = ref(0);
const router = useRouter();
const data = reactive({
    queryParams: {
        pageNum: 1,
        pageSize: 6,
        name: null,
        successFlag: null,
        startTime: null,
        endTime: null,
        qualityId: null,
        score: null,
        problemData: null,
        createTime: null,
        orderByColumn: 'start_time',
        isAsc: 'descending',
    },

});

const { queryParams, } = toRefs(data);

/** 排序触发事件 */
function handleTableChange(pagination, filters, sorter) {
    const field = sorter.column?.key || sorter.field;
    const orderMap = { ascend: 'ascending', descend: 'descending' };
    queryParams.value.orderByColumn = field;
    queryParams.value.isAsc = sorter.order ? orderMap[sorter.order] : null;
    queryParams.value.pageNum = 1;
    getList();
}

/** 查询探查任务实例列表 */
function getList() {
    loading.value = true;
    listProbeTaskInstance(queryParams.value).then(response => {
        const page = response.data || {};
        ProbeTaskInstanceList.value = page.rows || [];
        total.value = Number(page.total || ProbeTaskInstanceList.value.length || 0);
        loading.value = false;
    });
}
/** 搜索按钮操作 */
function handleQuery() {
    queryParams.value.pageNum = 1;
    getList();
}
/** 重置按钮操作 */
function resetQuery() {
    proxy.resetForm("queryRef");
    handleQuery();
}
function routeTo(link, row) {
    if (link !== "" && link.indexOf("http") !== -1) {
        window.location.href = link;
        return
    }
    if (link !== "") {
        if (stationNavigation?.openPage?.({
            path: link,
            query: { id: row?.id, score: row?.score },
            title: "探查质量报告",
            routeName: "ProbeTaskInstanceDetail",
        })) return;
        if (link === router.currentRoute.value.path) {
            window.location.reload();
        } else {
            router.push({
                path: link,
                query: {
                    id: row.id,
                    score: row.score

                }
            });
        }
    }
}

async function sendMessage(row) {
    if (!row?.id) {
        proxy.$modal.msgWarning("无效的任务id，请刷新后重试");
        return;
    }
    const res = await doSendMessage(row.id);
    if (Number(res?.code) === 200) {
        proxy.$modal.msgSuccess("发送成功");
    } else {
        proxy.$modal.msgWarning(res?.msg || "发送失败");
    }
}

getList();
</script>

<style scoped lang="scss">
.pagecont-top {
  display: flex !important;
  flex-wrap: nowrap !important;
  align-items: center !important;
  gap: 8px;

  .ant-form {
    display: flex !important;
    flex-wrap: nowrap !important;
    flex: 0 1 auto !important;

    .ant-form-item {
      display: inline-flex !important;
      flex-shrink: 0 !important;
      margin-bottom: 0 !important;
    }
  }

  .top-right-btn {
    flex-shrink: 0;
  }
}
</style>
