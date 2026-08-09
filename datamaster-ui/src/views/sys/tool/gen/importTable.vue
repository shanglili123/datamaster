<template>
  <!-- 导入表 -->
  <a-modal :title="'导入表'" v-model:open="visible" width="800px" destroy-on-close>
    <a-form :model="queryParams" ref="queryRef" layout="inline">
      <a-form-item label="表名称" name="tableName">
        <a-input
          v-model:value="queryParams.tableName"
          placeholder="请输入表名称"
          allow-clear
          class="el-form-input-width"
          @pressEnter="handleQuery"
        />
      </a-form-item>
      <a-form-item>
        <a-button type="primary" @click="handleQuery">
            <i class="iconfont-mini icon-a-zu22377 mr5"></i>查询
        </a-button>
        <a-button @click="resetQuery">
            <i class="iconfont-mini icon-a-zu22378 mr5"></i>重置
        </a-button>
      </a-form-item>
    </a-form>
      <a-table
        @row-click="clickRow"
        ref="table"
        :data-source="dbTableList"
        :row-key="'tableName'"
        :row-selection="{ onChange: (selectedRowKeys, selectedRows) => handleSelectionChange(selectedRows) }"
        :scroll="{ y: '380px' }"
        :columns="columns"
      />
      <pagination
        v-show="total>0"
        :total="total"
        v-model:page="queryParams.pageNum"
        v-model:limit="queryParams.pageSize"
        @pagination="getList"
      />
    <template #footer>
      <div class="dialog-footer">
        <a-button @click="visible = false">取 消</a-button>
        <a-button type="primary" @click="handleImportTable">确 定</a-button>
      </div>
    </template>
  </a-modal>
</template>

<script setup>
import { listDbTable, importTable } from "@/api/system/tool/gen.js";

const total = ref(0);
const visible = ref(false);
const tables = ref([]);
const dbTableList = ref([]);
const { proxy } = getCurrentInstance();

const columns = [
  { title: '表名称', dataIndex: 'tableName', key: 'tableName', ellipsis: true },
  { title: '表描述', dataIndex: 'tableComment', key: 'tableComment', ellipsis: true },
  { title: '创建时间', dataIndex: 'createTime', key: 'createTime' },
  { title: '更新时间', dataIndex: 'updateTime', key: 'updateTime' },
];

const queryParams = reactive({
  pageNum: 1,
  pageSize: 6,
  tableName: undefined,
  tableComment: undefined
});

const emit = defineEmits(["ok"]);

/** 查询参数列表 */
function show() {
  getList();
  visible.value = true;
}

/** 单击选择行 */
function clickRow(row) {
  proxy.$refs.table.toggleRowSelection(row);
}

/** 多选框选中数据 */
function handleSelectionChange(selection) {
  tables.value = selection.map(item => item.tableName);
}

/** 查询表数据 */
function getList() {
  listDbTable(queryParams).then(res => {
    dbTableList.value = res.rows;
    total.value = res.total;
  });
}

/** 搜索按钮操作 */
function handleQuery() {
  queryParams.pageNum = 1;
  getList();
}

/** 重置按钮操作 */
function resetQuery() {
  proxy.resetForm("queryRef");
  handleQuery();
}

/** 导入按钮操作 */
function handleImportTable() {
  const tableNames = tables.value.join(",");
  if (tableNames == "") {
    proxy.$modal.msgError("请选择要导入的表");
    return;
  }
  importTable({ tables: tableNames }).then(res => {
    proxy.$modal.msgSuccess(res.msg);
    if (res.code === 200) {
      visible.value = false;
      emit("ok");
    }
  });
}

defineExpose({
  show,
});
</script>
