<template>
   <div class="app-container" ref="app-container">
      <div class="pagecont-top" v-show="showSearch">
         <a-form class="btn-style" :model="queryParams" ref="queryRef" layout="inline" :label-col="{ style: { width: '68px' } }">
            <a-form-item label="岗位编码" name="postCode">
               <a-input
                  v-model:value="queryParams.postCode"
                  placeholder="请输入岗位编码"
                  allow-clear
                  style="width: 130px"
                  @pressEnter="handleQuery"
               />
            </a-form-item>
            <a-form-item label="岗位名称" name="postName">
               <a-input
                  v-model:value="queryParams.postName"
                  placeholder="请输入岗位名称"
                  allow-clear
                  style="width: 130px"
                  @pressEnter="handleQuery"
               />
            </a-form-item>
            <a-form-item label="状态" name="status">
               <a-select v-model:value="queryParams.status" placeholder="岗位状态" allow-clear style="width: 120px">
                  <a-select-option
                     v-for="dict in sys_normal_disable"
                     :key="dict.value"
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
            <a-button type="primary" :icon="h(PlusOutlined)" @click="handleAdd" v-hasPermi="['system:post:add']">新增</a-button>
            <a-dropdown trigger="click" v-hasPermi="['system:post:edit', 'system:post:remove', 'system:post:export']">
              <a-button>
                更多<DownOutlined style="font-size: 12px; margin-left: 4px;" />
              </a-button>
              <template #overlay>
                <a-menu @click="handleMoreMenu">
                  <a-menu-item key="edit" :disabled="single" v-hasPermi="['system:post:edit']">
                    <EditOutlined />修改
                  </a-menu-item>
                  <a-menu-item key="delete" :disabled="multiple" v-hasPermi="['system:post:remove']">
                    <DeleteOutlined />删除
                  </a-menu-item>
                  <a-menu-item key="export" v-hasPermi="['system:post:export']">
                    <DownloadOutlined />导出
                  </a-menu-item>
                </a-menu>
              </template>
            </a-dropdown>
         </div>
         <div class="top-right-btn">
            <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
         </div>
      </div>
      <div class="pagecont-bottom">

         <a-spin :spinning="loading">
            <a-table
              :data-source="postList"
              :columns="tableColumns"
              :pagination="false"
              striped
              :scroll="{ y: '60vh' }"
              :row-selection="{ type: 'checkbox', onChange: handleSelectionChange }"
              row-key="postId"
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
                  <a-button type="link" size="small" @click="handleUpdate(record)" v-hasPermi="['system:post:edit']">修改</a-button>
                  <a-button type="link" danger size="small" @click="handleDelete(record)" v-hasPermi="['system:post:remove']">删除</a-button>
                </template>
                <template v-else>
                  <span>{{ record[column.dataIndex] || '-' }}</span>
                </template>
              </template>
            </a-table>
         </a-spin>

         <pagination
            v-show="total > 0"
            :total="total"
            v-model:page="queryParams.pageNum"
            v-model:limit="queryParams.pageSize"
            @pagination="getList"
         />
      </div>

      <!-- 添加或修改岗位对话框 -->
      <a-modal :title="title" v-model:open="open" width="800px" destroy-on-close>
         <a-form ref="postRef" :model="form" :rules="rules" :label-col="{ style: { width: '80px' } }">
            <a-row :gutter="20">
               <a-col :span="12">
                  <a-form-item label="岗位名称" name="postName">
                     <a-input v-model:value="form.postName" placeholder="请输入岗位名称" />
                  </a-form-item>
               </a-col>
               <a-col :span="12">
                  <a-form-item label="岗位编码" name="postCode">
                     <a-input v-model:value="form.postCode" placeholder="请输入编码名称" />
                  </a-form-item>
               </a-col>
               <a-col :span="12">
                  <a-form-item label="岗位顺序" name="postSort">
                     <a-input-number style="width:100%" v-model:value="form.postSort" :min="0" />
                  </a-form-item>
               </a-col>
               <a-col :span="12">
                  <a-form-item label="岗位状态" name="status">
                     <a-radio-group v-model:value="form.status">
                        <a-radio
                           v-for="dict in sys_normal_disable"
                           :key="dict.value"
                           :value="dict.value"
                        >{{ dict.label }}</a-radio>
                     </a-radio-group>
                  </a-form-item>
               </a-col>
               <a-col :span="24">
                  <a-form-item label="备注" name="remark">
                     <a-textarea v-model:value="form.remark" placeholder="请输入内容" />
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

<script setup name="Post">

import { listPost, addPost, delPost, getPost, updatePost } from "@/api/system/system/post.js";

import { normalizePage, pageRows } from "@/utils/page.js";
import { h } from 'vue';
import { DeleteOutlined, DownOutlined, DownloadOutlined, EditOutlined, PlusOutlined } from "@ant-design/icons-vue";

const { proxy } = getCurrentInstance();
const { sys_normal_disable } = proxy.useDict("sys_normal_disable");

const tableColumns = [
  { title: '岗位编号', dataIndex: 'postId', align: 'center' },
  { title: '岗位编码', dataIndex: 'postCode', align: 'center' },
  { title: '岗位名称', dataIndex: 'postName', align: 'center' },
  { title: '岗位排序', dataIndex: 'postSort', align: 'center' },
  { title: '状态', dataIndex: 'status', align: 'center' },
  { title: '创建时间', dataIndex: 'createTime', align: 'center', width: 180 },
  { title: '操作', key: 'actions', align: 'center', fixed: 'right', width: 240 },
];

const emptyContent = h('div', { class: 'emptyBg' }, [
  h('img', { src: new URL('@/assets/system/images/no_data/noData.png', import.meta.url).href, alt: '' }),
  h('p', '没有记录哦~'),
]);

const postList = ref([]);
const open = ref(false);
const loading = ref(true);
const showSearch = ref(true);
const ids = ref([]);
const single = ref(true);
const multiple = ref(true);
const total = ref(0);
const title = ref("");

const data = reactive({
  form: {},
  queryParams: {
    pageNum: 1,
    pageSize: 6,
    postCode: undefined,
    postName: undefined,
    status: undefined
  },
  rules: {
    postName: [{ required: true, message: "岗位名称不能为空", trigger: "blur" }],
    postCode: [{ required: true, message: "岗位编码不能为空", trigger: "blur" }],
    postSort: [{ required: true, message: "岗位顺序不能为空", trigger: "blur" }],
  }
});

const { queryParams, form, rules } = toRefs(data);

/** 查询岗位列表 */
function getList() {
  loading.value = true;
  listPost(queryParams.value).then(response => {
    const page = normalizePage(response);
    total.value = page.total;
    postList.value = pageRows(page.rows, page.total, queryParams.value);
    loading.value = false;
  });
}

/** 取消按钮 */
function cancel() {
  open.value = false;
  reset();
}

/** 表单重置 */
function reset() {
  form.value = {
    postId: undefined,
    postCode: undefined,
    postName: undefined,
    postSort: 0,
    status: "0",
    remark: undefined
  };
  proxy.resetForm("postRef");
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

/** 多选框选中数据 */
function handleSelectionChange(selectedRowKeys, selectedRows) {
  ids.value = selectedRows.map(item => item.postId);
  single.value = selectedRows.length != 1;
  multiple.value = !selectedRows.length;
}

/** 新增按钮操作 */
function handleAdd() {
  reset();
  open.value = true;
  title.value = "新增岗位";
}

/** 修改按钮操作 */
function handleUpdate(row) {
  reset();
  const postId = row.postId || ids.value;
  getPost(postId).then(response => {
    form.value = response.data;
    open.value = true;
    title.value = "修改岗位";
  });
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs["postRef"].validate(valid => {
    if (valid) {
      if (form.value.postId != undefined) {
        updatePost(form.value).then(response => {
          proxy.$modal.msgSuccess("修改成功");
          open.value = false;
          getList();
        });
      } else {
        addPost(form.value).then(response => {
          proxy.$modal.msgSuccess("新增成功");
          open.value = false;
          getList();
        });
      }
    }
  });
}

/** 删除按钮操作 */
function handleDelete(row) {
  const postIds = row.postId || ids.value;
  proxy.$modal.confirm('是否确认删除岗位编号为"' + postIds + '"的数据项？').then(function() {
    return delPost(postIds);
  }).then(() => {
    getList();
    proxy.$modal.msgSuccess("删除成功");
  }).catch(() => {});
}

/** 导出按钮操作 */
function handleExport() {
  proxy.download("system/post/export", {
    ...queryParams.value
  }, `post_${new Date().getTime()}.xlsx`);
}

getList();
</script>
