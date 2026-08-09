<template>
    <a-modal :title="dialogTitle" v-model:open="dialogVisible" width="70%" style="min-height:600px;"
        :mask-closable="false" @close="handleClose"
>
        <a-table :height="tableHeight" :data-source="tableData" ref="multipleTable" stripe
            style="width: 100%; margin: 15px 0;" :row-key="'id'"
            :row-selection="{ onChange: (selectedRowKeys, selectedRows) => handleSelectionChange(selectedRows) }"
            :columns="tableColumns"
>
            <!--            原数据精度列（table-column 形式），迁移后由 tableColumns + bodyCell 模式实现：data-index="dataPrecision" title="数据精度" ellipsis -->
            <template #bodyCell="{ column, record }">
                <template v-if="column.key === 'pkFlag'">
                    <span v-if="record.pkFlag === '1'">是</span>
                    <span v-if="record.pkFlag === '0'">否</span>
                </template>
                <template v-else-if="column.key === 'nullableFlag'">
                    <span v-if="record.nullableFlag === '1'">是</span>
                    <span v-if="record.nullableFlag === '0'">否</span>
                </template>
                <template v-else-if="column.key === 'defaultValue'">
                    {{ record.defaultValue || "-" }}
                </template>
            </template>
        </a-table>
        <template #footer>
            <div class="dialog-footer">
                <a-button @click="handleClose">取消</a-button>
                <a-button type="primary" @click="confirm">确定</a-button>
            </div>
        </template>
    </a-modal>
</template>

<script setup name="AddList">
const { proxy } = getCurrentInstance();

const props = defineProps({
    dialogTitle: {
        type: String,
        default: "表格数据",
    },
    visible: {
        type: Boolean,
        default: false,
    },
    tableData: {
        type: Array,
        default: () => [],
    },
    list: {
        type: Array,
        default: () => [],
    },
})

const tableColumns = [
    { title: '序号', dataIndex: 'sortOrder', key: 'sortOrder', width: 80, align: 'center', ellipsis: true },
    { title: '列名', dataIndex: 'engName', key: 'engName', align: 'center', width: 200, ellipsis: true },
    { title: '数据类型', dataIndex: 'columnType', key: 'columnType', align: 'center', width: 120, ellipsis: true },
    { title: '数据长度', dataIndex: 'columnLength', key: 'columnLength', width: 90, align: 'center', ellipsis: true },
    { title: '数据小数位', dataIndex: 'columnScale', key: 'columnScale', width: 100, align: 'center', ellipsis: true },
    { title: '主键', key: 'pkFlag', align: 'center', width: 100, ellipsis: true },
    { title: '允许为空', key: 'nullableFlag', align: 'center', width: 100, ellipsis: true },
    { title: '列默认值', key: 'defaultValue', width: 100, align: 'center', ellipsis: true },
    { title: '列备注', dataIndex: 'cnName', key: 'cnName', align: 'center', ellipsis: true },
];

const data = reactive({
    isInitialized: false, // 标识是否已初始化选中项
    checkedTableColumns: [],
    total: 0,
    queryParams: {
        pageNum: 1,
        pageSize: 6,
        tableAlias: '',
        documentId: '',
        tableName: ""
    },
    loading: true,
    tableHeight: document.body.offsetHeight - 400 + 'px',
    AddListRows: [], lastSqlText: '', // 存储上次的 SQL 文本，用于检测是否发生变化
    firstDialogVisible: false,
    secondDialogVisible: false,
    sortDialogVisible: false,
    isShowTooltip: false,
    filteredTableOptions: [],
});
const { queryParams, AddListRows, tableHeight, loading, isInitialized,
    checkedTableColumns, total, firstDialogVisible, secondDialogVisible,
    sortDialogVisible, isShowTooltip, filteredTableOptions } = toRefs(data);

//添加计算属性
const dialogVisible = computed({
    get: () => props.visible,
    set: (newValue) => {
        console.log("dialogVisible", newValue)
        if (!newValue) {
            handleClose();
        }
    },

});
function handleClose() {
    proxy.$emit("close");
    isInitialized.value = false;
    AddListRows.value = [];
}
function echoSelected() { // 回显选中
    this.$nextTick(() => {
        proxy.$refs.multipleTable.clearSelection();
        tableData.forEach(item => {
            if (AddListRows.some(user => user.id === item.id)) {
                proxy.$refs.multipleTable.toggleRowSelection(item, true);
            }

        });

        isInitialized.value = true;
    });
}

function confirm() {
    proxy.$emit("confirm", AddListRows.value);
}
function handleSelectionChange(selectedRows) {
    if (isInitialized) {
        AddListRows.value = selectedRows;
    }
}

function resetQuery() {

}
</script>

<style scoped lang="scss">
.button-style-right {
    margin: -15px 15px 15px 15px;
    background-color: white;
    text-align: right;
    padding: 20px 0;
}
</style>

