<template>
   <div class="app-container" ref="app-container">
      <div class="pagecont-top" v-show="showSearch">
         <a-form class="btn-style" :model="queryParams" ref="queryRef" layout="inline" :label-col="{ style: { width: '68px' } }">
            <a-form-item label="部门名称" name="deptName">
               <a-input v-model:value="queryParams.deptName" placeholder="请输入部门名称" allow-clear class="el-form-input-width"
                  @pressEnter="handleQuery" />
            </a-form-item>
            <a-form-item label="状态" name="status">
               <a-select v-model:value="queryParams.status" placeholder="部门状态" allow-clear class="el-form-input-width">
                  <a-select-option v-for="dict in sys_normal_disable" :key="dict.value"
                     :value="dict.value">{{ dict.label }}</a-select-option>
               </a-select>
            </a-form-item>
            <a-form-item>
               <a-button type="primary" @click="handleQuery" @mousedown="(e) => e.preventDefault()">
                  <i class="iconfont-mini icon-a-zu22377 mr5"></i>查询
               </a-button>
               <a-button @click="resetQuery" @mousedown="e => e.preventDefault()">
                  <i class="iconfont-mini icon-a-zu22378 mr5"></i>重置
               </a-button>
            </a-form-item>
         </a-form>
         <div class="data-action-btns">
            <a-button type="primary" :icon="h(PlusOutlined)" @click="handleAdd" v-hasPermi="['system:dept:add']">新增</a-button>
            <a-button :icon="h(SwitcherOutlined)" @click="toggleExpandAll">展开/折叠</a-button>
         </div>
         <div class="top-right-btn">
            <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
         </div>
      </div>
      <div class="pagecont-bottom">

         <a-spin :spinning="loading">
            <a-table
              v-if="refreshTable"
              :data-source="deptList"
              :columns="tableColumns"
              :pagination="false"
              :scroll="{ y: '60vh' }"
              row-key="deptId"
              :default-expand-all-rows="isExpandAll"
              :children-column-name="'children'"
              :locale="{ emptyText: emptyContent }"
            >
              <template #bodyCell="{ column, record }">
                <template v-if="column.dataIndex === 'status'">
                  <dict-tag :options="sys_normal_disable" :value="record.status" />
                </template>
                <template v-else-if="column.dataIndex === 'createTime'">
                  <span>{{ parseTime(record.createTime) }}</span>
                </template>
                <template v-else-if="column.key === 'actions'">
                  <a-button type="link" size="small" @click="handleUpdate(record)" v-hasPermi="['system:dept:edit']">修改</a-button>
                  <a-button type="link" size="small" @click="handleAdd(record)" v-hasPermi="['system:dept:add']">新增</a-button>
                  <a-button v-if="record.parentId != 0" type="link" danger size="small" @click="handleDelete(record)" v-hasPermi="['system:dept:remove']">删除</a-button>
                </template>
                <template v-else>
                  <span>{{ record[column.dataIndex] || '-' }}</span>
                </template>
              </template>
            </a-table>
         </a-spin>
         <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum"
            v-model:limit="queryParams.pageSize" @pagination="getList" />
      </div>

      <!-- 添加或修改部门对话框 -->
      <a-modal :title="title" v-model:open="open" width="800px" destroy-on-close>
         <a-form ref="deptRef" :model="form" :rules="rules" :label-col="{ style: { width: '80px' } }">
            <a-row :gutter="20">
               <a-col :span="24" v-if="form.parentId !== 0">
                  <a-form-item label="上级部门" name="parentId">
                     <a-tree-select v-model:value="form.parentId" :tree-data="deptOptions"
                        :field-names="{ value: 'deptId', label: 'deptName', children: 'children' }"
                        placeholder="选择上级部门" />
                  </a-form-item>
               </a-col>
               <a-col :span="12">
                  <a-form-item label="部门名称" name="deptName">
                     <a-input v-model:value="form.deptName" placeholder="请输入部门名称" />
                  </a-form-item>
               </a-col>
               <a-col :span="12">
                  <a-form-item label="显示排序" name="orderNum">
                     <a-input-number style="width:100%" v-model:value="form.orderNum" :min="0" />
                  </a-form-item>
               </a-col>
               <a-col :span="12">
                  <a-form-item label="邮箱" name="email">
                     <a-input v-model:value="form.email" placeholder="请输入邮箱" :maxlength="50" />
                  </a-form-item>
               </a-col>
               <a-col :span="12">
                  <a-form-item label="部门状态" name="status">
                     <a-radio-group v-model:value="form.status">
                        <a-radio v-for="dict in sys_normal_disable" :key="dict.value" :value="dict.value">{{ dict.label
                           }}</a-radio>
                     </a-radio-group>
                  </a-form-item>
               </a-col>
            </a-row>
         </a-form>
         <template #footer>
            <div class="dialog-footer">
               <a-button @click="cancel">取 消</a-button>
               <a-button type="primary" @click="submitForm">确 定</a-button>
            </div>
         </template>
      </a-modal>
   </div>
</template>

<script setup name="Dept">

import { listDept, getDept, delDept, addDept, updateDept, listDeptExcludeChild } from "@/api/system/system/dept.js";
import { h } from 'vue';
import { PlusOutlined, SwitcherOutlined } from "@ant-design/icons-vue";
const { proxy } = getCurrentInstance();
const { sys_normal_disable } = proxy.useDict("sys_normal_disable");

const tableColumns = [
  { title: '部门名称', dataIndex: 'deptName', align: 'left', ellipsis: true },
  { title: '排序', dataIndex: 'orderNum', align: 'center' },
  { title: '状态', dataIndex: 'status', align: 'center' },
  { title: '创建时间', dataIndex: 'createTime', align: 'center', width: 160 },
  { title: '操作', key: 'actions', align: 'center', fixed: 'right', width: 240 },
];

const emptyContent = h('div', { class: 'emptyBg' }, [
  h('img', { src: new URL('@/assets/system/images/no_data/noData.png', import.meta.url).href, alt: '' }),
  h('p', '没有记录哦~'),
]);

const deptList = ref([]);
const open = ref(false);
const loading = ref(true);
const showSearch = ref(true);
const title = ref("");
const deptOptions = ref([]);
const isExpandAll = ref(true);
const refreshTable = ref(true);
const total = ref(0);
const data = reactive({
   form: {},
   queryParams: {
      deptName: undefined,
      status: undefined,
      pageNum: 1,
      pageSize: 6
   },
   rules: {
      parentId: [{ required: true, message: "上级部门不能为空", trigger: "blur" }],
      deptName: [{ required: true, message: "部门名称不能为空", trigger: "blur" }],
      orderNum: [{ required: true, message: "显示排序不能为空", trigger: "blur" }],
      email: [{ type: "email", message: "请输入正确的邮箱地址", trigger: ["blur", "change"] }],
      phone: [{ pattern: /^1[3|4|5|6|7|8|9][0-9]\d{8}$/, message: "请输入正确的手机号码", trigger: "blur" }]
   },
});

const { queryParams, form, rules } = toRefs(data);

/** 查询部门列表 */
function getList() {
   loading.value = true;
   listDept(queryParams.value).then(response => {
      const treeData = proxy.handleTree(response.data || [], "deptId");
      total.value = treeData.length;
      deptList.value = paginateTreeRoots(treeData);
      loading.value = false;
   });

}

function paginateTreeRoots(treeData) {
   const pageNum = queryParams.value.pageNum || 1;
   const pageSize = queryParams.value.pageSize || 6;
   const start = (pageNum - 1) * pageSize;
   return treeData.slice(start, start + pageSize);
}

/** 取消按钮 */
function cancel() {
   open.value = false;
   reset();
}

/** 表单重置 */
function reset() {
   form.value = {
      deptId: undefined,
      parentId: undefined,
      deptName: undefined,
      orderNum: 0,
      leader: undefined,
      phone: undefined,
      email: undefined,
      status: "0"
   };
   proxy.resetForm("deptRef");
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

/** 新增按钮操作 */
function handleAdd(row) {
   reset();
   listDept().then(response => {
      deptOptions.value = proxy.handleTree(response.data, "deptId");
   });
   if (row != undefined) {
      form.value.parentId = row.deptId;
   }
   open.value = true;
   title.value = "新增部门";
}

/** 展开/折叠操作 */
function toggleExpandAll() {
   refreshTable.value = false;
   isExpandAll.value = !isExpandAll.value;
   nextTick(() => {
      refreshTable.value = true;
   });
}

/** 修改按钮操作 */
function handleUpdate(row) {
   reset();
   listDeptExcludeChild(row.deptId).then(response => {
      deptOptions.value = proxy.handleTree(response.data, "deptId");
   });
   getDept(row.deptId).then(response => {
      form.value = response.data;
      open.value = true;
      title.value = "修改部门";
   });
}

/** 提交按钮 */
function submitForm() {
   proxy.$refs["deptRef"].validate().then(() => {
      if (form.value.deptId != undefined) {
         updateDept(form.value).then(response => {
            proxy.$modal.msgSuccess("修改成功");
            open.value = false;
            getList();
         });
      } else {
         addDept(form.value).then(response => {
            proxy.$modal.msgSuccess("新增成功");
            open.value = false;
            getList();
         });
      }
   }).catch(() => {});
}

/** 删除按钮操作 */
function handleDelete(row) {
   proxy.$modal.confirm('是否确认删除名称为"' + row.deptName + '"的数据项?').then(function () {
      return delDept(row.deptId);
   }).then(() => {
      getList();
      proxy.$modal.msgSuccess("删除成功");
   }).catch(() => { });
}

getList();
</script>

