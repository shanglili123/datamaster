<template>
  <a-modal
    :title="title"
    v-model:open="visible"
    class="medium-dialog max-dialogs-status0"
    :draggable="true"
    width="90%"
    :footer="null"
  >
    <div class="flex-row">
      <!-- 左侧树 -->
      <div class="left-col">
        <DeptTree
          :deptOptions="deptOptions"
          :leftWidth="leftWidth"
          placeholder="请输入标准数据元目录"
          @node-click="handleNodeClick"
          ref="DeptTreeRef"
          :showFilter="false"
          :show-background="false"
          style="height: 650px"
        />
      </div>

      <!-- 分隔线 -->
      <div class="divider"></div>

      <!-- 右侧表格 + 分页 -->
      <div class="content-col">
        <!-- 表格 -->
        <a-table
          :data-source="dpDataElemList"
          striped
          :loading="loading"
          @row-click="handleRowClick"
          :row-class-name="rowClassName"
          :row-key="(record) => record.id"
          :pagination="false"
          bordered
          :scroll="{ y: '62vh' }"
        >
          <a-table-column
            v-if="getColumnVisibility(0)"
            title="编号"
            align="left"
            data-index="id"
            :width="80"
          />
          <a-table-column
            v-if="getColumnVisibility(1)"
            title="中文名称"
            align="left"
            data-index="name"
            :width="80"
            ellipsis
          >
            <template #default="{ record }">{{ record.name || "-" }}</template>
          </a-table-column>
          <a-table-column
            v-if="getColumnVisibility(2)"
            title="英文名称"
            align="left"
            data-index="engName"
            :width="80"
            ellipsis
          >
            <template #default="{ record }">{{ record.engName || "-" }}</template>
          </a-table-column>
          <a-table-column
            v-if="getColumnVisibility(3)"
            title="类型"
            align="left"
            data-index="type"
          >
            <template #default="{ record }">{{ typeFormat(record) }}</template>
          </a-table-column>
          <a-table-column
            v-if="getColumnVisibility(6)"
            :width="140"
            title="元描述"
            align="left"
            data-index="description"
            ellipsis
          >
            <template #default="{ record }">{{
              record.description || "-"
            }}</template>
          </a-table-column>
          <a-table-column
            title="操作"
            align="center"
            :width="240"
            fixed="right"
          >
            <template #default="{ record }">
              <a-button
                type="link"
                @click="showDialog(record)"
                v-hasPermi="['dp:dataElem:dataelem:edit']"
                ><template #icon><EyeOutlined /></template>查看
              </a-button>
            </template>
          </a-table-column>
          <template #emptyText>
            <div class="emptyBg">
              <img
                src="../../../../../../../../assets/system/images/no_data/noData.png"
                alt=""
              />
              <p>无数据</p>
            </div>
          </template>
        </a-table>
        <!-- 分页 -->
        <div
          class="pagination-wrapper"
          style="margin-top: 10px; text-align: right"
        >
          <pagination
            :total="total"
            v-model:page="queryParams.pageNum"
            v-model:limit="queryParams.pageSize"
            @pagination="getList"
          />
        </div>
      </div>
    </div>

    <!-- 底部按钮 -->
    <template #footer>
      <div class="dialog-footer">
        <a-button @click="handleCancel">取 消</a-button>
        <a-button
          type="primary"
          @click="handleConfirm"
          :disabled="!selectedRow"
          :loading="loading"
        >
          保 存
        </a-button>
      </div>
    </template>
    <CodeValueInput ref="dialogRef" @confirm="handleConfirm" />
  </a-modal>
</template>

<script setup>
import { ref, reactive } from "vue";
import { EyeOutlined } from "@ant-design/icons-vue";
const emit = defineEmits(["confirm"]);
import DeptTree from "@/components/DeptTree/tree.vue";
import { listDpDataElem } from "@/api/std/dataElem/dataElem.js";
import { listDpDataElemCode } from "@/api/std/dataElem/dataElem.js";
import { listAttDataElemCat } from "@/api/tax/cat/dataElemCat/dataElemCat.js";
const { proxy } = getCurrentInstance();
const { dp_data_elem_code_type } = proxy.useDict("dp_data_elem_code_type");
import CodeValueInput from "./dataElemDetail.vue";
const deptOptions = ref(undefined);
const leftWidth = ref(240); // 初始左侧宽度
const isResizing = ref(false); // 判断是否正在拖拽
let startX = 0; // 鼠标按下时的初始位置// 初始左侧宽度
/** 类型字典翻译 */
function typeFormat(row) {
  return proxy.selectDictLabel(dp_data_elem_code_type.value, row.type);
}

const dpDataElemList = ref([]);
const dpDataElemRuleRelList = ref([]);

// 列显隐信息
const columns = ref([
  { key: 1, label: "中文名称", visible: true },
  { key: 2, label: "英文名称", visible: true },
  { key: 3, label: "类型", visible: true },
  { key: 4, label: "标准数据元目录", visible: true },
  { key: 5, label: "当前状态", visible: true },
  { key: 6, label: "元描述", visible: true },
]);
const dialogRef = ref();
function handleQuery() {
  queryParams.value.pageNum = 1;
  getList();
}
function showDialog(row) {
  dialogRef.value.openDialog(row);
}
const getColumnVisibility = (key) => {
  const column = columns.value.find((col) => col.key === key);
  // 如果没有找到对应列配置，默认显示
  if (!column) return true;
  // 如果找到对应列配置，根据visible属性来控制显示
  return column.visible;
};

const open = ref(false);
const loading = ref(true);
const ids = ref([]);
const checkedDpDataElemRuleRel = ref([]);
const single = ref(true);
const multiple = ref(true);
const total = ref(0);
const title = ref("");
const data = reactive({
  form: { status: "0" },
  queryParams: {
    pageNum: 1,
    pageSize: 6,
    name: null,
    engName: null,
    catCode: null,
    type: 2,
  },
});

const { queryParams, form } = toRefs(data);
/** 查询数据元列表 */
function getList() {
  loading.value = true;
  listDpDataElem(queryParams.value).then((response) => {
    dpDataElemList.value = response.data.rows;
    total.value = response.data.total;
    loading.value = false;
  });
}
// 树组件 传值
function handleNodeClick(data) {
  queryParams.value.catCode = data.code;
  handleQuery();
}

// 表单重置
function reset() {
  form.value = {
    id: null,
    code: null,
    name: null,
    engName: null,
    catCode: null,
    type: "1",
    columnType: null,
    status: "0",
    description: null,
    validFlag: null,
    delFlag: null,
    createBy: null,
    creatorId: null,
    createTime: null,
    updateBy: null,
    updaterId: null,
    updateTime: null,
    remark: null,
  };
  dpDataElemRuleRelList.value = [];
  proxy.resetForm("dpDataElemRef");
}

const DeptTreeRef = ref(null);
/** 重置按钮操作 */
function resetQuery() {
  if (DeptTreeRef.value?.resetTree) {
    DeptTreeRef.value.resetTree();
  }
  queryParams.value.catCode = "";
  queryParams.value.pageNum = 1;
  selectedRow.value = null;
  reset();
  proxy.resetForm("queryRef");
}

function getDeptTree() {
  listAttDataElemCat({ validFlag: true }).then((response) => {
    deptOptions.value = proxy.handleTree(response.data, "id", "parentId");
    deptOptions.value = [
      {
        name: "标准数据元目录",
        value: "",
        id: 0,
        children: deptOptions.value,
      },
    ];
  });
}

const visible = ref(false);
/**
 * 打开弹窗
 * @param {String} dialogTitle 弹窗标题
 */
function openDialog(dialogTitle = "选择数据") {
  title.value = dialogTitle;
  visible.value = true;
  getDeptTree();
  getList();
}
const selectedRow = ref(null);
const tableRef = ref(null);
/** 选中行高亮 */
function rowClassName(record) {
  return selectedRow.value && selectedRow.value.id === record.id
    ? "selected-row"
    : "";
}
function handleRowClick(row) {
  selectedRow.value = row;
  console.log("选中行数据:", row);
}
/**
 * 取消
 */
function handleCancel() {
  visible.value = false;
  selectedRow.value = null;
  resetQuery();
}
async function ElemCode(id) {
  if (id === -1) {
    return [];
  }
  loading.value = true;
  try {
    const response = await listDpDataElemCode({
      pageNum: 1,
      pageSize: 999,
      id,
    });
    return response.data.rows || [];
  } catch (error) {
    console.error("请求失败", error);
    return [];
  } finally {
    loading.value = false;
  }
}

/**
 * 保存
 */
async function handleConfirm() {
  if (!selectedRow.value) {
    proxy.$modal.msgWarning("请选择一条记录");
    return;
  }
  // const list = await ElemCode(selectedRow.value.id);
  emit("confirm", selectedRow.value);
  resetQuery();
  visible.value = false;
}

defineExpose({ openDialog });
</script>
<style scoped lang="scss">
.flex-row {
  display: flex;
  height: 71vh;
}

.left-col {
  width: 250px;
  overflow-y: auto;
}

.divider {
  width: 1px;
  background-color: #dcdfe6;
  margin: 0 20px;
  height: 700px;
}

.content-col {
  margin-top: 50px;
  flex: 1;
  display: flex;
  flex-direction: column;
  padding-right: 20px;
}

.el-table {
  flex: none;
  /* 不占满父容器 */
}

.pagination-wrapper {
  margin-top: 10px;
  text-align: right;
}

.emptyBg {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 20px;
}

:deep(.selected-row) > td {
  background: #e6f4ff !important;
}
</style>

