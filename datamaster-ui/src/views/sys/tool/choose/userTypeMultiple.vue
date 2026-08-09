<template>
  <a-modal
      :title="'用户类型-多选'"
      v-model:open="visible"
      width="1200px"
      destroy-on-close
      @close="cancel"
  >
    <a-form
        class="btn-style"
        :model="queryParams"
        ref="queryRef"
        :inline="true"
        v-show="showSearch"
        :label-col="{ style: { width: '68px' } }"
    >
      <a-form-item label="ID" name="id">
        <a-input
            style="width:240px"
            v-model:value="queryParams.id"
            placeholder="请输入ID"
            allow-clear
            @pressEnter="handleQuery"
        />
      </a-form-item>
      <a-form-item label="类型名称" name="name">
        <a-input
            style="width:240px"
            v-model:value="queryParams.name"
            placeholder="请输入类型名称"
            allow-clear
            @pressEnter="handleQuery"
        />
      </a-form-item>
      <a-form-item label="是否有效" name="validFlag">
        <a-input
            style="width:240px"
            v-model:value="queryParams.validFlag"
            placeholder="请输入是否有效"
            allow-clear
            @pressEnter="handleQuery"
        />
      </a-form-item>
      <a-form-item label="删除标志" name="delFlag">
        <a-input
            style="width:240px"
            v-model:value="queryParams.delFlag"
            placeholder="请输入删除标志"
            allow-clear
            @pressEnter="handleQuery"
        />
      </a-form-item>
      <a-form-item label="创建人" name="createBy">
        <a-input
            style="width:240px"
            v-model:value="queryParams.createBy"
            placeholder="请输入创建人"
            allow-clear
            @pressEnter="handleQuery"
        />
      </a-form-item>
      <a-form-item label="创建人id" name="creatorId">
        <a-input
            style="width:240px"
            v-model:value="queryParams.creatorId"
            placeholder="请输入创建人id"
            allow-clear
            @pressEnter="handleQuery"
        />
      </a-form-item>
      <a-form-item label="创建时间" style="width: 308px">
        <a-range-picker
            style="width:240px"
            v-model:value="daterangeCreateTime"
            valueFormat="YYYY-MM-DD"
            :separator="'-'"
            :placeholder="['开始日期', '结束日期']"
        ></a-range-picker>
      </a-form-item>
      <a-form-item label="更新人" name="updateBy">
        <a-input
            style="width:240px"
            v-model:value="queryParams.updateBy"
            placeholder="请输入更新人"
            allow-clear
            @pressEnter="handleQuery"
        />
      </a-form-item>
      <a-form-item label="更新人id" name="updaterId">
        <a-input
            style="width:240px"
            v-model:value="queryParams.updaterId"
            placeholder="请输入更新人id"
            allow-clear
            @pressEnter="handleQuery"
        />
      </a-form-item>
      <a-form-item label="更新时间" style="width: 308px">
        <a-range-picker
            style="width:240px"
            v-model:value="daterangeUpdateTime"
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
        :scroll="{ y: '300px' }"
        :loading="loading"
        :data-source="dataList"
        :row-key="'id'"
        :row-selection="{ onChange: (selectedRowKeys, selectedRows) => handleSelectionChange(selectedRows) }"
        @row-click="handleRowClick"
        :columns="tableColumns"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'name'">{{ record.name || '-' }}</template>
        <template v-else-if="column.key === 'validFlag'">{{ record.validFlag || '-' }}</template>
        <template v-else-if="column.key === 'delFlag'">{{ record.delFlag || '-' }}</template>
        <template v-else-if="column.key === 'createBy'">{{ record.createBy || '-' }}</template>
        <template v-else-if="column.key === 'creatorId'">{{ record.creatorId || '-' }}</template>
        <template v-else-if="column.key === 'createTime'">
          <span>{{ parseTime(record.createTime, '{y}-{m}-{d}') }}</span>
        </template>
        <template v-else-if="column.key === 'updateBy'">{{ record.updateBy || '-' }}</template>
        <template v-else-if="column.key === 'updaterId'">{{ record.updaterId || '-' }}</template>
        <template v-else-if="column.key === 'updateTime'">
          <span>{{ parseTime(record.updateTime, '{y}-{m}-{d}') }}</span>
        </template>
        <template v-else-if="column.key === 'remark'">{{ record.remark || '-' }}</template>
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

<script setup name="UserTypeMultiple">
import { listUserType } from "@/api/example/user/userType";
import { ref } from "vue";
const daterangeCreateTime = ref([]);
const daterangeUpdateTime = ref([]);
const { proxy } = getCurrentInstance();

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
    validFlag: null,
    delFlag: null,
    createBy: null,
    creatorId: null,
    createTime: null,
    updateBy: null,
    updaterId: null,
    updateTime: null,
    remark: null
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
// 当前界面table
const multipletableRef = ref();

const tableColumns = [
  { title: 'ID', dataIndex: 'id', key: 'id', align: 'center' },
  { title: '类型名称', dataIndex: 'name', key: 'name', align: 'center' },
  { title: '是否有效', dataIndex: 'validFlag', key: 'validFlag', align: 'center' },
  { title: '删除标志', dataIndex: 'delFlag', key: 'delFlag', align: 'center' },
  { title: '创建人', dataIndex: 'createBy', key: 'createBy', align: 'center' },
  { title: '创建人id', dataIndex: 'creatorId', key: 'creatorId', align: 'center' },
  { title: '创建时间', dataIndex: 'createTime', key: 'createTime', align: 'center', width: 180 },
  { title: '更新人', dataIndex: 'updateBy', key: 'updateBy', align: 'center' },
  { title: '更新人id', dataIndex: 'updaterId', key: 'updaterId', align: 'center' },
  { title: '更新时间', dataIndex: 'updateTime', key: 'updateTime', align: 'center', width: 180 },
  { title: '备注', dataIndex: 'remark', key: 'remark', align: 'center' },
];

const emit = defineEmits(["open", "confim", "cancel"]);

/** 多选框选中事件 */
function handleSelectionChange(selection) {
  // console.log(selection, "===handleSelectionChange");
  if (selection.length > 0) {
    // 如果选中值不是空值且少选了一个值
    if (oldSelection.value.length > selection.length) {
      oldSelection.value.forEach((item) => {
        let index = selection.findIndex((ece) => ece.id == item.id);
        if (index == -1) {
          multiple.value = multiple.value.filter(
              (ece) => item.id != ece.id
          );
        }
      });
    }
    if (multiple.value.length > 0) {
      selection.forEach((item) => {
        let index = multiple.value.findIndex(
            (ece) => ece.id == item.id
        );
        if (index == -1) {
          multiple.value.push(item);
        }
      });
    } else {
      multiple.value.push(...selection);
    }
  } else {
    // 如果不是分页导致的
    if (!isAuto.value) {
      // 如果选中值，取消到没有选择任何值
      oldSelection.value.forEach((item) => {
        let index = selection.findIndex((ece) => ece.id == item.id);
        if (index == -1) {
          multiple.value = multiple.value.filter(
              (ece) => item.id != ece.id
          );
        }
      });
    }
  }
  oldSelection.value = selection;
}

/** 行单机事件 */
function handleRowClick(row) {
  // 检查当前行是否已经在 multiple 中
  const index = multiple.value.findIndex(item => item.id === row.id);

  // 如果行已经被选中，移除它
  if (index > -1) {
    multiple.value = multiple.value.filter(item => item.id !== row.id);
  } else {
    // 如果行未被选中，添加到 multiple 中
    multiple.value.push(row);
  }

  // 同步更新表格的选中状态
  multipletableRef.value.toggleRowSelection(row, index === -1);
}

/**
 * 选中table的复选框
 * @param {Array} rows 选中的对象数组
 * @param {Boolean} ignoreSelectable 是否忽略可选
 */
function setSelectionRow(rows, ignoreSelectable) {
  // 选中数据
  if (rows.length > 0) {
    rows.forEach((row) => {
      let data = dataList.value.filter((item) => item.id == row.id);
      if (data.length > 0) {
        multipletableRef.value.toggleRowSelection(data[0], undefined, ignoreSelectable);
      }
    });
  }
}

function rest(){
  queryParams.value.pageNum = 1;
  proxy.resetForm("queryRef");
  oldSelection.value = []
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
  listUserType(proxy.addDateRange(queryParams.value, dateRange.value)).then(
      async (response) => {
        dataList.value = response.data.rows;
        total.value = response.data.total;
        loading.value = false;
        // 初始化及分页切换选中逻辑
        isAuto.value = true;
        await nextTick();
        setSelectionRow(multiple.value);
        isAuto.value = false;
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
  daterangeCreateTime.value = null;
  daterangeUpdateTime.value = null;
  handleQuery();
}

defineExpose({ open });
</script>

