<template>
   <div class="app-container" ref="app-container">
      <div class="pagecont-top" v-show="showSearch">
         <a-form class="btn-style" :model="queryParams" ref="queryRef" layout="inline" :label-col="{ style: { width: '50px' } }">
            <a-form-item label="名称" name="configName">
               <a-input
                  v-model:value="queryParams.configName"
                  placeholder="请输入参数名称"
                  allow-clear
                  style="width: 150px"
                  @pressEnter="handleQuery"
               />
            </a-form-item>
            <a-form-item label="键名" name="configKey">
               <a-input
                  v-model:value="queryParams.configKey"
                  placeholder="请输入参数键名"
                  allow-clear
                  style="width: 150px"
                  @pressEnter="handleQuery"
               />
            </a-form-item>
            <a-form-item label="内置" name="configType">
               <a-select style="width: 150px" v-model:value="queryParams.configType" placeholder="系统内置" allow-clear>
                  <a-select-option
                     v-for="dict in sys_yes_no"
                     :key="dict.value"
                     :value="dict.value">{{ dict.label }}</a-select-option>
               </a-select>
            </a-form-item>
            <a-form-item label="时间">
               <a-range-picker
                  v-model:value="dateRange"
                  valueFormat="YYYY-MM-DD"
                  start-placeholder="开始"
                  end-placeholder="结束"
                  style="width: 200px"
               ></a-range-picker>
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
            <a-button type="primary" :icon="h(PlusOutlined)" @click="handleAdd" v-hasPermi="['system:config:add']">新增</a-button>
            <a-dropdown trigger="click" v-hasPermi="['system:config:export', 'system:config:remove']">
              <a-button>
                更多<DownOutlined style="margin-left: 4px; font-size: 12px" />
              </a-button>
              <template #overlay>
                <a-menu @click="handleMoreMenu">
                  <a-menu-item key="export" v-hasPermi="['system:config:export']">
                    <DownloadOutlined />导出
                  </a-menu-item>
                  <a-menu-item key="refresh" v-hasPermi="['system:config:remove']">
                    <ReloadOutlined />刷新缓存
                  </a-menu-item>
                </a-menu>
              </template>
            </a-dropdown>
         </div>
         <div class="top-right-btn">
            <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
         </div>
      </div>
      <div>
         <a-spin :spinning="loading">
            <a-table
              :data-source="configList"
              :columns="tableColumns"
              :pagination="false"
              striped
              :scroll="{ y: '60vh' }"
              :row-selection="{ type: 'checkbox', onChange: handleSelectionChange }"
              row-key="configId"
              :locale="{ emptyText: emptyContent }"
            >
              <template #bodyCell="{ column, record }">
                <template v-if="column.dataIndex === 'configType'">
                  <dict-tag :options="sys_yes_no" :value="record.configType" />
                </template>
                <template v-else-if="column.dataIndex === 'createTime'">
                  <span>{{ parseTime(record.createTime) }}</span>
                </template>
                <template v-else-if="column.key === 'actions'">
                  <a-button type="link" size="small" @click="handleUpdate(record)" v-hasPermi="['system:config:edit']">修改</a-button>
                  <a-button type="link" danger size="small" @click="handleDelete(record)" v-hasPermi="['system:config:remove']">删除</a-button>
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

      <!-- 添加或修改参数配置对话框 -->
      <a-modal :title="title" v-model:open="open" width="800px" destroy-on-close>
         <a-form ref="configRef" :model="form" :rules="rules" :label-col="{ style: { width: '80px' } }">
            <a-row :gutter="20">
               <a-col :span="12">
                  <a-form-item label="参数名称" name="configName">
                     <a-input v-model:value="form.configName" placeholder="请输入参数名称" />
                  </a-form-item>
               </a-col>
               <a-col :span="12">
                  <a-form-item label="参数键名" name="configKey">
                     <a-input v-model:value="form.configKey" placeholder="请输入参数键名" />
                  </a-form-item>
               </a-col>
               <a-col :span="12">
                  <a-form-item label="参数键值" name="configValue">
                     <a-input v-model:value="form.configValue" placeholder="请输入参数键值" />
                  </a-form-item>
               </a-col>
               <a-col :span="12">
                  <a-form-item label="系统内置" name="configType">
                     <a-radio-group v-model:value="form.configType">
                        <a-radio
                           v-for="dict in sys_yes_no"
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

<script setup name="Config">

import { listConfig, getConfig, delConfig, addConfig, updateConfig, refreshCache } from "@/api/system/system/config.js";

import { normalizePage, pageRows } from "@/utils/page.js";
import { h } from 'vue';
import { DownloadOutlined, PlusOutlined, ReloadOutlined } from "@ant-design/icons-vue";

const { proxy } = getCurrentInstance();
const { sys_yes_no } = proxy.useDict("sys_yes_no");

const tableColumns = [
  { title: '参数主键', dataIndex: 'configId', align: 'center' },
  { title: '参数名称', dataIndex: 'configName', align: 'center', ellipsis: true },
  { title: '参数键名', dataIndex: 'configKey', align: 'center', ellipsis: true },
  { title: '参数键值', dataIndex: 'configValue', align: 'center', ellipsis: true },
  { title: '系统内置', dataIndex: 'configType', align: 'center' },
  { title: '备注', dataIndex: 'remark', align: 'center', ellipsis: true },
  { title: '创建时间', dataIndex: 'createTime', align: 'center', width: 180 },
  { title: '操作', key: 'actions', align: 'center', fixed: 'right', width: 240 },
];

const emptyContent = h('div', { class: 'emptyBg' }, [
  h('img', { src: new URL('@/assets/system/images/no_data/noData.png', import.meta.url).href, alt: '' }),
  h('p', '没有记录哦~'),
]);

const configList = ref([]);
const open = ref(false);
const loading = ref(true);
const showSearch = ref(true);
const ids = ref([]);
const single = ref(true);
const multiple = ref(true);
const total = ref(0);
const title = ref("");
const dateRange = ref([]);

const data = reactive({
  form: {},
  queryParams: {
    pageNum: 1,
    pageSize: 6,
    configName: undefined,
    configKey: undefined,
    configType: undefined
  },
  rules: {
    configName: [{ required: true, message: "参数名称不能为空", trigger: "blur" }],
    configKey: [{ required: true, message: "参数键名不能为空", trigger: "blur" }],
    configValue: [{ required: true, message: "参数键值不能为空", trigger: "blur" }]
  }
});

const { queryParams, form, rules } = toRefs(data);

/** 查询参数列表 */
function getList() {
  loading.value = true;
  listConfig(proxy.addDateRange(queryParams.value, dateRange.value)).then(response => {
    const page = normalizePage(response);
    total.value = page.total;
    configList.value = pageRows(page.rows, page.total, queryParams.value);
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
    configId: undefined,
    configName: undefined,
    configKey: undefined,
    configValue: undefined,
    configType: "Y",
    remark: undefined
  };
  proxy.resetForm("configRef");
}

/** 搜索按钮操作 */
function handleQuery() {
  queryParams.value.pageNum = 1;
  getList();
}

/** 重置按钮操作 */
function resetQuery() {
  dateRange.value = [];
  proxy.resetForm("queryRef");
  handleQuery();
}

/** 多选框选中数据 */
function handleSelectionChange(selectedRowKeys, selectedRows) {
  ids.value = selectedRows.map(item => item.configId);
  single.value = selectedRows.length != 1;
  multiple.value = !selectedRows.length;
}

/** 新增按钮操作 */
function handleAdd() {
  reset();
  open.value = true;
  title.value = "新增参数";
}

/** 修改按钮操作 */
function handleUpdate(row) {
  reset();
  const configId = row.configId || ids.value;
  getConfig(configId).then(response => {
    form.value = response.data;
    open.value = true;
    title.value = "修改参数";
  });
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs["configRef"].validate().then(() => {
    if (form.value.configId != undefined) {
      updateConfig(form.value).then(response => {
        proxy.$modal.msgSuccess("修改成功");
        open.value = false;
        getList();
      });
    } else {
      addConfig(form.value).then(response => {
        proxy.$modal.msgSuccess("新增成功");
        open.value = false;
        getList();
      });
    }
  }).catch(() => {});
}

/** 删除按钮操作 */
function handleDelete(row) {
  const configIds = row.configId || ids.value;
  proxy.$modal.confirm('是否确认删除参数编号为"' + configIds + '"的数据项？').then(function () {
    return delConfig(configIds);
  }).then(() => {
    getList();
    proxy.$modal.msgSuccess("删除成功");
  }).catch(() => {});
}

/** 导出按钮操作 */
function handleExport() {
  proxy.download("system/config/export", {
    ...queryParams.value
  }, `config_${new Date().getTime()}.xlsx`);
}

/** 更多菜单操作 */
function handleMoreMenu({ key }) {
  if (key === "export") {
    handleExport();
  } else if (key === "refresh") {
    handleRefreshCache();
  }
}

/** 刷新缓存按钮操作 */
function handleRefreshCache() {
  refreshCache().then(() => {
    proxy.$modal.msgSuccess("刷新缓存成功");
  });
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

  .data-action-btns {
    margin-left: auto;
    display: flex;
    align-items: center;
    gap: 8px;
    flex-shrink: 0;
  }

  .top-right-btn {
    flex-shrink: 0;
  }
}
</style>