<template>
  <div class="app-container" ref="app-container">
    <div class="pagecont-top" v-show="showSearch">
      <a-form class="btn-style" :model="queryParams" ref="queryRef" :layout="inline" :label-col="{ style: { width: '68px' } }">
        <a-form-item label="应用ID" name="id">
          <a-input
              class="el-form-input-width"
              v-model:value="queryParams.id"
              placeholder="请输入应用ID"
              allow-clear
              @pressEnter="handleQuery"
          />
        </a-form-item>
        <a-form-item label="应用名称" name="name">
          <a-input
              class="el-form-input-width"
              v-model:value="queryParams.name"
              placeholder="请输入应用名称"
              allow-clear
              @pressEnter="handleQuery"
          />
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
    </div>
    <div  class="pagecont-bottom">
      <div class="justify-between mb15">
        <a-row :gutter="10" class="btn-style">
          <a-col :span="1.5">
            <a-button
                type="primary"
                :icon="h(PlusOutlined)"
                @click="handleAdd"
                v-hasPermi="['auth:client:add']"
            >新增</a-button>
          </a-col>
        </a-row>
            <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
      </div>

      <a-spin :spinning="loading">
        <a-table
          :data-source="clientList"
          :columns="tableColumns"
          :pagination="false"
          striped
          :scroll="{ y: '60vh' }"
          :row-selection="{ type: 'checkbox', onChange: handleSelectionChange }"
          row-key="id"
          :locale="{ emptyText: emptyContent }"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.dataIndex === 'type'">
              <dict-tag :options="auth_app_type" :value="record.type" />
            </template>
            <template v-else-if="column.dataIndex === 'icon'">
              <image-preview :src="record.icon" :width="50" :height="50" />
            </template>
            <template v-else-if="column.dataIndex === 'publicFlag'">
              <dict-tag :options="auth_public" :value="record.publicFlag" />
            </template>
            <template v-else-if="column.dataIndex === 'validFlag'">
              <dict-tag :options="sys_valid" :value="record.validFlag" />
            </template>
            <template v-else-if="column.dataIndex === 'createTime'">
              <span>{{ parseTime(record.createTime, '{y}-{m}-{d}') }}</span>
            </template>
            <template v-else-if="column.dataIndex === 'homeUrl'">
              <span>{{ record.homeUrl || '-' }}</span>
            </template>
            <template v-else-if="column.dataIndex === 'remark'">
              <span>{{ record.remark || '-' }}</span>
            </template>
            <template v-else-if="column.key === 'actions'">
              <a-button type="link" size="small" @click="handleUpdate(record)" v-hasPermi="['auth:client:edit']">修改</a-button>
              <a-button type="link" danger size="small" @click="handleDelete(record)" v-hasPermi="['auth:client:remove']">删除</a-button>
            </template>
            <template v-else>
              <span>{{ record[column.dataIndex] || '-' }}</span>
            </template>
          </template>
        </a-table>
      </a-spin>

      <pagination
          v-show="total>0"
          :total="total"
          v-model:page="queryParams.pageNum"
          v-model:limit="queryParams.pageSize"
          @pagination="getList"
      />
    </div>

    <!-- 添加或修改应用管理对话框 -->
    <a-modal :title="title" v-model:open="open" width="800px" destroy-on-close>
      <a-form ref="clientRef" :model="form" :rules="rules" :label-col="{ style: { width: '110px' } }">
        <a-row :gutter="20">
          <a-col :span="12">
            <a-form-item label="应用首页" name="homeUrl">
              <a-input v-model:value="form.homeUrl" placeholder="请输入应用首页" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="同步地址" name="syncUrl">
              <a-input v-model:value="form.syncUrl" placeholder="请输入同步地址" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="20">
          <a-col :span="12">
            <a-form-item label="应用名称" name="name">
              <a-input v-model:value="form.name" placeholder="请输入应用名称" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="应用类型" name="type">
              <a-select v-model:value="form.type" placeholder="请选择应用类型">
                <a-select-option
                    v-for="dict in auth_app_type"
                    :key="dict.value"
                    :value="parseInt(dict.value)"
                >{{ dict.label }}</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="20">
          <a-col :span="12">
            <a-form-item label="是否公开" name="publicFlag">
              <a-radio-group v-model:value="form.publicFlag">
                <a-radio
                    v-for="dict in auth_public"
                    :key="dict.value"
                    :value="dict.value"
                >{{ dict.label }}</a-radio>
              </a-radio-group>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="是否有效" name="validFlag">
              <a-radio-group v-model:value="form.validFlag">
                <a-radio
                    v-for="dict in sys_valid"
                    :key="dict.value"
                    :value="dict.value"
                >{{ dict.label }}</a-radio>
              </a-radio-group>
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="20">
          <a-col :span="24">
            <a-form-item label="应用图标" name="icon">
              <div class="xgtpcont">
                  <ImageUpload class="sctplist" v-model="form.icon">
                  </ImageUpload>
              </div>
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="20">
          <a-col :span="24">
            <a-form-item label="允许授权的url" name="redirectUrl">
              <a-textarea v-model:value="form.redirectUrl" placeholder="请输入内容"></a-textarea>
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="20">
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

<script setup name="Client">

import { listClient, getClient, delClient, addClient, updateClient } from "@/api/system/auth/client.js";
import { PlusOutlined } from "@ant-design/icons-vue";
import { h } from 'vue';

const { proxy } = getCurrentInstance();
const { auth_app_type } = proxy.useDict('auth_app_type');
const { sys_valid } = proxy.useDict('sys_valid');
const { auth_public } = proxy.useDict('auth_public');

const tableColumns = [
  { title: '应用ID', dataIndex: 'id', align: 'center' },
  { title: '应用秘钥', dataIndex: 'secretKey', align: 'center', width: 300 },
  { title: '应用名称', dataIndex: 'name', align: 'center', width: 120 },
  { title: '应用类型', dataIndex: 'type', align: 'center' },
  { title: '应用图标', dataIndex: 'icon', align: 'center', width: 100 },
  { title: '应用首页', dataIndex: 'homeUrl', align: 'center' },
  { title: '允许授权的url', dataIndex: 'redirectUrl', align: 'center', width: 150 },
  { title: '是否公开', dataIndex: 'publicFlag', align: 'center' },
  { title: '是否有效', dataIndex: 'validFlag', align: 'center' },
  { title: '创建时间', dataIndex: 'createTime', align: 'center', width: 180 },
  { title: '备注', dataIndex: 'remark', align: 'center' },
  { title: '操作', key: 'actions', align: 'center', fixed: 'right', width: 240 },
];

const emptyContent = h('div', { class: 'emptyBg' }, [
  h('img', { src: new URL('@/assets/system/images/no_data/noData.png', import.meta.url).href, alt: '' }),
  h('p', '没有记录哦~'),
]);

const clientList = ref([]);
const open = ref(false);
const loading = ref(true);
const showSearch = ref(true);
const ids = ref([]);
const single = ref(true);
const multiple = ref(true);
const total = ref(0);
const title = ref("");

const data = reactive({
  form: {
  },
  queryParams: {
    pageNum: 1,
    pageSize: 6,
    id: null,
    secretKey: null,
    name: null,
    type: null,
    icon: null,
    homeUrl: null,
    syncUrl: null,
    redirectUrl: null,
    publicFlag: null,
    validFlag: null,
    delFlag: null,
    createBy: null,
    creatorId: null,
    createTime: null,
    updateBy: null,
    updaterId: null,
    updateTime: null,
    remark: null
  },
  rules: {
    secretKey: [
      { required: true, message: "应用秘钥不能为空", trigger: "blur" }
    ],
    name: [
      { required: true, message: "应用名称不能为空", trigger: "blur" }
    ],
    type: [
      { required: true, message: "应用类型", trigger: "change" }
    ],
    redirectUrl: [
      { required: true, message: "允许授权的url不能为空", trigger: "blur" }
    ],
    publicFlag: [
      { required: true, message: "是否公开不能为空", trigger: "blur" }
    ],
    validFlag: [
      { required: true, message: "是否有效", trigger: "blur" }
    ],
  }
});

const { queryParams, form, rules } = toRefs(data);

/** 查询应用管理列表 */
function getList() {
  loading.value = true;
  listClient(queryParams.value).then(response => {
    clientList.value = response.rows;
    total.value = response.total;
    loading.value = false;
  });
}

// 取消按钮
function cancel() {
  open.value = false;
  reset();
}

// 表单重置
function reset() {
  form.value = {
    id: null,
    secretKey: null,
    name: null,
    type: null,
    icon: null,
    homeUrl: null,
    syncUrl: null,
    redirectUrl: null,
    publicFlag: '1',
    validFlag: '1',
    delFlag: null,
    createBy: null,
    creatorId: null,
    createTime: null,
    updateBy: null,
    updaterId: null,
    updateTime: null,
    remark: null
  };
  proxy.resetForm("clientRef");
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

// 多选框选中数据
function handleSelectionChange(selectedRowKeys, selectedRows) {
  ids.value = selectedRows.map(item => item.id);
  single.value = selectedRows.length != 1;
  multiple.value = !selectedRows.length;
}

/** 新增按钮操作 */
function handleAdd() {
  reset();
  open.value = true;
  title.value = "新增应用管理";
}

/** 修改按钮操作 */
function handleUpdate(row) {
  reset();
  const _id = row.id || ids.value
  getClient(_id).then(response => {
    form.value = response.data;
    open.value = true;
    title.value = "修改应用管理";
  });
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs["clientRef"].validate().then(() => {
    if (form.value.id != null) {
      updateClient(form.value).then(response => {
        proxy.$modal.msgSuccess("修改成功");
        open.value = false;
        getList();
      });
    } else {
      addClient(form.value).then(response => {
        proxy.$modal.msgSuccess("新增成功");
        open.value = false;
        getList();
      });
    }
  }).catch(() => {});
}

/** 删除按钮操作 */
function handleDelete(row) {
  const _ids = row.id || ids.value;
  proxy.$modal.confirm('是否确认删除应用管理编号为"' + _ids + '"的数据项？').then(function() {
    return delClient(_ids);
  }).then(() => {
    getList();
    proxy.$modal.msgSuccess("删除成功");
  }).catch(() => {});
}

/** 导出按钮操作 */
function handleExport() {
  proxy.download('auth/client/export', {
    ...queryParams.value
  }, `client_${new Date().getTime()}.xlsx`)
}

getList();
</script>

<style scoped lang="scss">

</style>

