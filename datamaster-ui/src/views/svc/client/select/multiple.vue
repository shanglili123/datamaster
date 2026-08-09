<template>
  <a-modal title="应用管理-多选" v-model:open="visible" width="1200px" @cancel="cancel">
    <a-form class="btn-style" :model="queryParams" ref="queryRef" :layout="'inline'" v-show="showSearch"
      :label-col="{ style: { width: '68px' } }">
      <a-form-item label="ID" name="id">
        <a-input style="width:240px" v-model:value="queryParams.id" placeholder="请输入ID" allow-clear
          @pressEnter="handleQuery" />
      </a-form-item>
      <a-form-item label="应用名称" name="name">
        <a-input style="width:240px" v-model:value="queryParams.name" placeholder="请输入应用名称" allow-clear
          @pressEnter="handleQuery" />
      </a-form-item>
      <a-form-item label="应用类型" name="type">
        <a-select style="width:240px" v-model:value="queryParams.type" placeholder="请选择应用类型" allow-clear>
          <a-select-option v-for="dict in auth_app_type" :key="dict.value" :value="dict.value">
            {{ dict.label }}
          </a-select-option>
        </a-select>
      </a-form-item>
      <a-form-item label="应用秘钥" name="secret">
        <a-input style="width:240px" v-model:value="queryParams.secret" placeholder="请输入应用秘钥" allow-clear
          @pressEnter="handleQuery" />
      </a-form-item>
      <a-form-item label="主页地址" name="homepageUrl">
        <a-input style="width:240px" v-model:value="queryParams.homepageUrl" placeholder="请输入主页地址" allow-clear
          @pressEnter="handleQuery" />
      </a-form-item>
      <a-form-item label="同步地址" name="syncUrl">
        <a-input style="width:240px" v-model:value="queryParams.syncUrl" placeholder="请输入同步地址" allow-clear
          @pressEnter="handleQuery" />
      </a-form-item>
      <a-form-item label="是否公开" name="publicFlag">
        <a-select style="width:240px" v-model:value="queryParams.publicFlag" placeholder="请选择是否公开" allow-clear>
          <a-select-option v-for="dict in auth_public" :key="dict.value" :value="dict.value">
            {{ dict.label }}
          </a-select-option>
        </a-select>
      </a-form-item>
      <a-form-item label="创建时间" name="createTime">
        <a-date-picker style="width:240px" allow-clear v-model:value="queryParams.createTime" value-format="YYYY-MM-DD"
          placeholder="请选择创建时间" />
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

    <a-table
      size="middle"
      :loading="loading"
      :data-source="dataList"
      :pagination="false"
      :scroll="{ y: 300 }"
      :row-key="(record) => record.id"
      :row-selection="{ selectedRowKeys: selectedRowKeys, onChange: handleSelectionChange }"
      :columns="tableColumns"
      @row-click="handleRowClick"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.dataIndex === 'name'">
          {{ record.name || '-' }}
        </template>
        <template v-else-if="column.dataIndex === 'type'">
          <dict-tag :options="auth_app_type" :value="record.type" />
        </template>
        <template v-else-if="column.dataIndex === 'allowUrl'">
          {{ record.allowUrl || '-' }}
        </template>
        <template v-else-if="column.dataIndex === 'syncUrl'">
          {{ record.syncUrl || '-' }}
        </template>
        <template v-else-if="column.dataIndex === 'logo'">
          <image-preview :src="record.logo" :width="50" :height="50" />
        </template>
        <template v-else-if="column.dataIndex === 'description'">
          {{ record.description || '-' }}
        </template>
        <template v-else-if="column.dataIndex === 'publicFlag'">
          <dict-tag :options="auth_public" :value="record.publicFlag" />
        </template>
        <template v-else-if="column.dataIndex === 'createBy'">
          {{ record.createBy || '-' }}
        </template>
        <template v-else-if="column.dataIndex === 'createTime'">
          <span>{{ parseTime(record.createTime, '{y}-{m}-{d}') }}</span>
        </template>
        <template v-else-if="column.dataIndex === 'remark'">
          {{ record.remark || '-' }}
        </template>
      </template>
    </a-table>

    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum"
      v-model:limit="queryParams.pageSize" @pagination="getList" />
    <template #footer>
      <div class="dialog-footer">
        <a-button size="small" @click="cancel">取 消</a-button>
        <a-button type="primary" size="small" @click="confirm">
          确 定
        </a-button>
      </div>
    </template>
  </a-modal>
</template>

<script setup name="ClientMultiple">
import { listClient } from "@/api/svc/client/client";
import { ref } from "vue";
const { proxy } = getCurrentInstance();

const { auth_public, auth_app_type } = proxy.useDict('auth_public', 'auth_app_type');

const tableColumns = [
  { title: 'ID', dataIndex: 'id', align: 'center' },
  { title: '应用名称', dataIndex: 'name', align: 'center' },
  { title: '应用类型', dataIndex: 'type', align: 'center' },
  { title: '允许授权的url', dataIndex: 'allowUrl', align: 'center' },
  { title: '同步地址', dataIndex: 'syncUrl', align: 'center' },
  { title: '应用图标', dataIndex: 'logo', align: 'center', width: 100 },
  { title: '描述', dataIndex: 'description', align: 'center' },
  { title: '是否公开', dataIndex: 'publicFlag', align: 'center' },
  { title: '创建人', dataIndex: 'createBy', align: 'center' },
  { title: '创建时间', dataIndex: 'createTime', align: 'center', width: 180 },
  { title: '备注', dataIndex: 'remark', align: 'center' },
];

const dataList = ref([]);
const loading = ref(true);
const showSearch = ref(true);
const total = ref(0);
const dateRange = ref([]);
const data = reactive({
  form: {},
  queryParams: {
    pageNum: 1,
    pageSize: 6,
    id: null,
    name: null,
    type: null,
    secret: null,
    homepageUrl: null,
    allowUrl: null,
    syncUrl: null,
    logo: null,
    description: null,
    publicFlag: null,
    createTime: null,
  }
});
const { queryParams, form } = toRefs(data);

// -------------------------------------------
const visible = ref(false);
// 定义多选数据
const multiple = ref([]);
// 定义上次勾选数据==用于对比删除
const oldSelection = ref([]);
// 是否分页切换
const isAuto = ref(false);
// 当前页表格勾选 key（驱动 antd 表格勾选状态）
const selectedRowKeys = ref([]);

const emit = defineEmits(["open", "confirm", "cancel"]);

/** 多选框选中事件 */
function handleSelectionChange(keys, rows) {
  // rows 为当前页勾选的行对象
  if (rows.length > 0) {
    // 如果选中值不是空值且少选了一个值
    if (oldSelection.value.length > rows.length) {
      oldSelection.value.forEach((item) => {
        let index = rows.findIndex((ece) => ece.id == item.id);
        if (index == -1) {
          multiple.value = multiple.value.filter(
            (ece) => item.id != ece.id
          );
        }
      });
    }
    if (multiple.value.length > 0) {
      rows.forEach((item) => {
        let index = multiple.value.findIndex(
          (ece) => ece.id == item.id
        );
        if (index == -1) {
          multiple.value.push(item);
        }
      });
    } else {
      multiple.value.push(...rows);
    }
  } else {
    // 如果不是分页导致的
    if (!isAuto.value) {
      // 如果选中值，取消到没有选择任何值
      oldSelection.value.forEach((item) => {
        let index = rows.findIndex((ece) => ece.id == item.id);
        if (index == -1) {
          multiple.value = multiple.value.filter(
            (ece) => item.id != ece.id
          );
        }
      });
    }
  }
  oldSelection.value = rows;
  selectedRowKeys.value = keys;
}

/** 行单机事件 */
function handleRowClick(row, event) {
  // 点击勾选列不触发行切换，避免与 handleSelectionChange 重复处理
  if (event && event.target && event.target.closest && event.target.closest('.ant-table-selection-column')) {
    return;
  }
  // 检查当前行是否已经在 multiple 中
  const index = multiple.value.findIndex(item => item.id === row.id);

  // 如果行已经被选中，移除它
  if (index > -1) {
    multiple.value = multiple.value.filter(item => item.id !== row.id);
    selectedRowKeys.value = selectedRowKeys.value.filter(key => key !== row.id);
  } else {
    // 如果行未被选中，添加到 multiple 中
    multiple.value.push(row);
    selectedRowKeys.value.push(row.id);
  }
}

/**
 * 同步当前页表格勾选状态（跨页保持已选）
 */
function syncSelectionRow() {
  // 当前页中已选中的行
  selectedRowKeys.value = multiple.value
    .filter((item) => dataList.value.some((d) => d.id === item.id))
    .map((item) => item.id);
}

function rest() {
  queryParams.value.pageNum = 1;
  proxy.resetForm("queryRef");
  oldSelection.value = []
  selectedRowKeys.value = [];
}

/**
 * 打开选择框
 * @param {Array} val 选中的对象数组
 */
function open(val) {
  if (!Array.isArray(val)) {
    val = [val];  // 将非可迭代值转化为数组
  }
  visible.value = true;
  multiple.value = [...val];
  getList();
}

/**
 * 取消按钮
 * @description 取消按钮时，重置所有状态
 */
function cancel() {
  rest();
  visible.value = false;
}

/**
 * 确定按钮
 * @description 确定按钮时，emit confirm 事件，以便父组件接收到选中的数据
 */
function confirm() {
  if (multiple.value.length == 0) {
    proxy.$modal.msgWarning("未选择数据，请选择完成后重试");
    return;
  }
  emit("confirm", [...multiple.value]);
  rest();
  visible.value = false;
}

function normalizePageData(response) {
  const data = response?.data ?? response ?? {};
  if (Array.isArray(data)) {
    return { rows: data, total: data.length };
  }
  const rows = Array.isArray(data.rows)
    ? data.rows
    : Array.isArray(data.list)
      ? data.list
      : Array.isArray(data.records)
        ? data.records
        : Array.isArray(response?.rows)
          ? response.rows
          : [];
  const total = Number(data.total ?? data.totalCount ?? response?.total ?? rows.length);
  return { rows, total: Number.isNaN(total) ? rows.length : total };
}

/** 查询字典类型列表 */
function getList() {
  loading.value = true;
  listClient(proxy.addDateRange(queryParams.value, dateRange.value))
    .then(async (response) => {
      const pageData = normalizePageData(response);
      dataList.value = pageData.rows;
      total.value = pageData.total;
      // 初始化及分页切换选中逻辑
      isAuto.value = true;
      await nextTick();
      syncSelectionRow();
      isAuto.value = false;
    })
    .catch((error) => { })
    .finally(() => {
      loading.value = false;
    });
}

/** 搜索按钮操作 */
function handleQuery() {
  getList();
}

/** 重置按钮操作 */
function resetQuery() {
  proxy.resetForm("queryRef");
  queryParams.value.pageNum = 1;
  handleQuery();
}

defineExpose({ open });
</script>

