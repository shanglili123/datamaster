<template>
  <a-modal
    title="多选-字典管理"
    v-model:open="visible"
    width="1200px"
    draggable
    destroy-on-close
    @close="cancel"
  >
    <a-form
      class="btn-style"
      :model="queryParams"
      ref="queryRef"
      layout="inline"
      v-show="showSearch"
      :label-col="{ style: { width: '68px' } }"
    >
      <a-form-item label="字典名称" name="dictName">
        <a-input
          v-model:value="queryParams.dictName"
          placeholder="请输入字典名称"
          allow-clear
          class="el-form-input-width"
          @pressEnter="handleQuery"
        />
      </a-form-item>
      <a-form-item label="字典类型" name="dictType">
        <a-input
          v-model:value="queryParams.dictType"
          placeholder="请输入字典类型"
          allow-clear
          class="el-form-input-width"
          @pressEnter="handleQuery"
        />
      </a-form-item>
      <a-form-item label="状态" name="status">
        <a-select
          v-model:value="queryParams.status"
          placeholder="字典状态"
          allow-clear
          class="el-form-input-width"
        >
          <a-select-option
            v-for="dict in sys_normal_disable"
            :key="dict.value"
            :value="dict.value"
          >{{ dict.label }}</a-select-option>
        </a-select>
      </a-form-item>
      <a-form-item label="创建时间">
        <a-range-picker
          class="el-form-input-width"
          v-model:value="dateRange"
          valueFormat="YYYY-MM-DD"
          :separator="'-'"
          :placeholder="['开始日期', '结束日期']"
        ></a-range-picker>
      </a-form-item>
      <a-form-item>
        <a-button
          type="primary"
          @click="handleQuery"
          @mousedown="(e) => e.preventDefault()"
        >
          <i class="iconfont-mini icon-a-zu22377 mr5"></i>查询
        </a-button>
        <a-button @click="resetQuery" @mousedown="(e) => e.preventDefault()">
          <i class="iconfont-mini icon-a-zu22378 mr5"></i>重置
        </a-button>
      </a-form-item>
    </a-form>

    <a-table
      ref="multipletableRef"
      striped
      :scroll="{ y: 300 }"
      :loading="loading"
      :data-source="dataList"
      :row-key="(record) => record.dictId"
      :row-selection="{
        selectedRowKeys: selectedRowKeys,
        preserveSelectedRowKeys: true,
        onChange: handleSelectionChange,
      }"
      :columns="tableColumns"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.dataIndex === 'dictType'">
          <router-link
            :to="'/system/dict-data/index/' + record.dictId"
            class="link-type"
          >
            <span>{{ record.dictType }}</span>
          </router-link>
        </template>
        <template v-else-if="column.dataIndex === 'status'">
          <dict-tag :options="sys_normal_disable" :value="record.status" />
        </template>
        <template v-else-if="column.dataIndex === 'createTime'">
          <span>{{ parseTime(record.createTime) }}</span>
        </template>
      </template>
    </a-table>

    <pagination
      v-show="total > 0"
      :total="total"
      v-model:page="queryParams.pageNum"
      v-model:limit="queryParams.pageSize"
      @pagination="getList"
    />
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

<script setup name="Dict">
import { listType, getType, delType } from "@/api/system/system/dict/type.js";
import { ref } from "vue";

const { proxy } = getCurrentInstance();
const { sys_normal_disable } = proxy.useDict("sys_normal_disable");

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
    dictName: undefined,
    dictType: undefined,
    status: undefined,
  },
  rules: {
    dictName: [
      { required: true, message: "字典名称不能为空", trigger: "blur" },
    ],
    dictType: [
      { required: true, message: "字典类型不能为空", trigger: "blur" },
    ],
  },
});
const { queryParams, form, rules } = toRefs(data);

// -------------------------------------------
const visible = ref(false);
// 定义多选数据
const multiple = ref([]);
// 当前界面table
const multipletableRef = ref();

const tableColumns = [
  { title: "字典编号", dataIndex: "dictId", width: 85, align: "center" },
  { title: "字典名称", dataIndex: "dictName", align: "center", ellipsis: true },
  { title: "字典类型", dataIndex: "dictType", align: "center", ellipsis: true },
  { title: "状态", dataIndex: "status", align: "center" },
  { title: "备注", dataIndex: "remark", align: "center", ellipsis: true },
  { title: "创建时间", dataIndex: "createTime", align: "center", width: 180 },
];

const selectedRowKeys = computed(() => multiple.value.map((item) => item.dictId));

const emit = defineEmits(["open", "confim", "cancel"]);

/** 多选框选中事件（跨页保留选中） */
function handleSelectionChange(keys) {
  // 新增选中的行对象写入缓存
  keys.forEach((key) => {
    if (!multiple.value.some((item) => item.dictId == key)) {
      const row = dataList.value.find((item) => item.dictId == key);
      if (row) {
        multiple.value.push(row);
      }
    }
  });
  // 移除取消选中的行
  multiple.value = multiple.value.filter((item) => keys.includes(item.dictId));
}
function rest(){
  queryParams.value.pageNum = 1;
  proxy.resetForm("queryRef");
  multiple.value = [];
}
/**
 * 打开选择框
 * @param {Array} val 选中的对象数组
 */
function open(val) {
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
    proxy.$modal.msgWarning("未选择数据！");
    return;
  }
  emit("confirm", [...multiple.value]);
  rest();
  visible.value = false;
}
/** 查询字典类型列表 */
function getList() {
  loading.value = true;
  listType(proxy.addDateRange(queryParams.value, dateRange.value)).then(
    (response) => {
      dataList.value = response.rows;
      total.value = response.total;
      loading.value = false;
    }
  );
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

