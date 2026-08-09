<template>
   <div class="app-container" ref="app-container">
      <div class="pagecont-top" v-show="showSearch">
         <a-form class="btn-style" :model="queryParams" ref="queryRef" layout="inline" :label-col="{ style: { width: '45px' } }">
            <a-form-item label="名称" name="dictName">
               <a-input
                  v-model:value="queryParams.dictName"
                  placeholder="请输入字典名称"
                  allow-clear
                  style="width: 150px"
                  @pressEnter="handleQuery"
               />
            </a-form-item>
            <a-form-item label="类型" name="dictType">
               <a-input
                  v-model:value="queryParams.dictType"
                  placeholder="请输入字典类型"
                  allow-clear
                  style="width: 150px"
                  @pressEnter="handleQuery"
               />
            </a-form-item>
            <a-form-item label="状态" name="status">
               <a-select
                  v-model:value="queryParams.status"
                  placeholder="字典状态"
                  allow-clear
                  style="width: 150px"
               >
                  <a-select-option
                     v-for="dict in sys_normal_disable"
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
            <a-button type="primary" :icon="h(PlusOutlined)" @click="handleAdd" v-hasPermi="['system:dict:add']">新增</a-button>
            <a-dropdown trigger="click" v-hasPermi="['system:dict:export', 'system:dict:remove']">
              <a-button>
                更多<DownOutlined style="margin-left: 4px; font-size: 12px" />
              </a-button>
              <template #overlay>
                <a-menu @click="handleMoreMenu">
                  <a-menu-item key="export" v-hasPermi="['system:dict:export']">
                    <DownloadOutlined />导出
                  </a-menu-item>
                  <a-menu-item key="enum" :disabled="multiple">
                    <DownloadOutlined />下载
                  </a-menu-item>
                  <a-menu-item key="refresh" v-hasPermi="['system:dict:remove']">
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
              :data-source="typeList"
              :columns="tableColumns"
              :pagination="false"
              striped
              :scroll="{ y: '60vh' }"
              :row-selection="{ type: 'checkbox', onChange: handleSelectionChange }"
              row-key="dictId"
              :locale="{ emptyText: emptyContent }"
            >
              <template #bodyCell="{ column, record }">
                <template v-if="column.dataIndex === 'dictType'">
                  <router-link :to="'/system/dict-data/index/' + record.dictId" class="link-type">
                     <span>{{ record.dictType }}</span>
                  </router-link>
                </template>
                <template v-else-if="column.dataIndex === 'status'">
                  <dict-tag :options="sys_normal_disable" :value="record.status" />
                </template>
                <template v-else-if="column.dataIndex === 'createTime'">
                  <span>{{ parseTime(record.createTime) }}</span>
                </template>
                <template v-else-if="column.key === 'actions'">
                  <a-button type="link" size="small" @click="handleUpdate(record)" v-hasPermi="['system:dict:edit']">修改</a-button>
                  <a-button type="link" danger size="small" @click="handleDelete(record)" v-hasPermi="['system:dict:remove']">删除</a-button>
                  <a-button type="link" size="small" @click="handleEnum(record)">下载</a-button>
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
         <a-form ref="dictRef" :model="form" :rules="rules" :label-col="{ style: { width: '80px' } }">
            <a-row :gutter="20">
               <a-col :span="12">
                  <a-form-item label="字典名称" name="dictName">
                     <a-input v-model:value="form.dictName" placeholder="请输入字典名称" />
                  </a-form-item>
               </a-col>
               <a-col :span="12">
                  <a-form-item label="字典类型" name="dictType">
                     <a-input v-model:value="form.dictType" placeholder="请输入字典类型" />
                  </a-form-item>
               </a-col>
               <a-col :span="12">
                  <a-form-item label="状态" name="status">
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
                     <a-textarea v-model:value="form.remark" placeholder="请输入内容"></a-textarea>
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

<script setup name="Dict">

import useDictStore from '@/store/system/dict.js'

import { listType, getType, delType, addType, updateType, refreshCache } from "@/api/system/system/dict/type.js";

import {genCode} from "@/api/system/tool/gen.js";

import { normalizePage, pageRows } from "@/utils/page.js";
import { h } from 'vue';
import { DownloadOutlined, PlusOutlined, ReloadOutlined } from "@ant-design/icons-vue";

const { proxy } = getCurrentInstance();
const { sys_normal_disable } = proxy.useDict("sys_normal_disable");

const tableColumns = [
  { title: '字典编号', dataIndex: 'dictId', align: 'center' },
  { title: '字典名称', dataIndex: 'dictName', align: 'center', ellipsis: true },
  { title: '字典类型', dataIndex: 'dictType', align: 'center', ellipsis: true },
  { title: '状态', dataIndex: 'status', align: 'center' },
  { title: '备注', dataIndex: 'remark', align: 'center', ellipsis: true },
  { title: '创建时间', dataIndex: 'createTime', align: 'center', width: 180 },
  { title: '操作', key: 'actions', align: 'center', fixed: 'right', width: 240 },
];

const emptyContent = h('div', { class: 'emptyBg' }, [
  h('img', { src: new URL('@/assets/system/images/no_data/noData.png', import.meta.url).href, alt: '' }),
  h('p', '没有记录哦~'),
]);

const typeList = ref([]);
const open = ref(false);
const loading = ref(true);
const showSearch = ref(true);
const ids = ref([]);
const single = ref(true);
const multiple = ref(true);
const total = ref(0);
const title = ref("");
const dateRange = ref([]);
const dictTypes = ref([]);

const data = reactive({
  form: {},
  queryParams: {
    pageNum: 1,
    pageSize: 6,
    dictName: undefined,
    dictType: undefined,
    status: undefined
  },
  rules: {
    dictName: [{ required: true, message: "字典名称不能为空", trigger: "blur" }],
    dictType: [{ required: true, message: "字典类型不能为空", trigger: "blur" }]
  },
});

const { queryParams, form, rules } = toRefs(data);

/** 查询字典类型列表 */
function getList() {
  loading.value = true;
  listType(proxy.addDateRange(queryParams.value, dateRange.value)).then(response => {
    const page = normalizePage(response);
    total.value = page.total;
    typeList.value = pageRows(page.rows, page.total, queryParams.value);
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
    dictId: undefined,
    dictName: undefined,
    dictType: undefined,
    status: "0",
    remark: undefined
  };
  proxy.resetForm("dictRef");
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

/** 新增按钮操作 */
function handleAdd() {
  reset();
  open.value = true;
  title.value = "新增字典类型";
}

/** 多选框选中数据 */
function handleSelectionChange(selectedRowKeys, selectedRows) {
  ids.value = selectedRows.map(item => item.dictId);
  single.value = selectedRows.length != 1;
  multiple.value = !selectedRows.length;
  dictTypes.value = selectedRows.map(item => item.dictType);

}

/** 修改按钮操作 */
function handleUpdate(row) {
  reset();
  const dictId = row.dictId || ids.value;
  getType(dictId).then(response => {
    form.value = response.data;
    open.value = true;
    title.value = "修改字典类型";
  });
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs["dictRef"].validate().then(() => {
    if (form.value.dictId != undefined) {
      updateType(form.value).then(response => {
        proxy.$modal.msgSuccess("修改成功");
        open.value = false;
        getList();
      });
    } else {
      addType(form.value).then(response => {
        proxy.$modal.msgSuccess("新增成功");
        open.value = false;
        getList();
      });
    }
  }).catch(() => {});
}

/** 删除按钮操作 */
function handleDelete(row) {
  const dictIds = row.dictId || ids.value;
  proxy.$modal.confirm('是否确认删除字典编号为"' + dictIds + '"的数据项？').then(function() {
    return delType(dictIds);
  }).then(() => {
    getList();
    proxy.$modal.msgSuccess("删除成功");
  }).catch(() => {});
}

/** 导出按钮操作 */
function handleExport() {
  proxy.download("system/dict/type/export", {
    ...queryParams.value
  }, `dict_${new Date().getTime()}.xlsx`);
}

/** 刷新缓存按钮操作 */
function handleRefreshCache() {
  refreshCache().then(() => {
    proxy.$modal.msgSuccess("刷新成功");
    useDictStore().cleanDict();
  });
}

/** 更多菜单操作 */
function handleMoreMenu({ key }) {
  if (key === "export") {
    handleExport();
  } else if (key === "enum") {
    handleEnum();
  } else if (key === "refresh") {
    handleRefreshCache();
  }
}

/** 生成枚举类操作 */
function handleEnum(row) {
  const dtNames = row.dictType || dictTypes.value;
  if (dtNames == "") {
    proxy.$modal.msgError("请选择要生成的字典");
    return;
  }

    proxy.$download.zip("/system/dict/type/batchDictData?dictTypes=" + dtNames, "anivia.zip");
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
    flex-wrap: wrap !important;
    align-items: center;
    row-gap: 8px;
    flex: 1 1 auto !important;
    min-width: 0 !important;

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