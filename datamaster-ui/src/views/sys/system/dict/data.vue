<template>
   <div class="app-container" ref="app-container">
      <div class="pagecont-top" v-show="showSearch">
         <a-form class="btn-style" :model="queryParams" ref="queryRef" layout="inline" :label-col="{ style: { width: '68px' } }">
            <a-form-item label="字典名称" name="dictType">
               <a-select v-model:value="queryParams.dictType" class="el-form-input-width">
                  <a-select-option
                     v-for="item in typeOptions"
                     :key="item.dictId"
                     :value="item.dictType">{{ item.dictName }}</a-select-option>
               </a-select>
            </a-form-item>
            <a-form-item label="字典标签" name="dictLabel">
               <a-input
                  v-model:value="queryParams.dictLabel"
                  placeholder="请输入字典标签"
                  allow-clear
                  class="el-form-input-width"
                  @pressEnter="handleQuery"
               />
            </a-form-item>
            <a-form-item label="状态" name="status">
               <a-select v-model:value="queryParams.status" placeholder="数据状态" allow-clear class="el-form-input-width">
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
      </div>
      <div  class="pagecont-bottom">
         <div class="justify-between mb15">
            <a-row :gutter="10" class="btn-style">
               <a-col :span="1.5">
                  <a-button
                     type="primary"
                     :icon="h(PlusOutlined)"
                     @click="handleAdd"
                     v-hasPermi="['system:dict:add']"
                  >新增</a-button>
               </a-col>
               <a-col :span="1.5">
                  <a-button
                     type="primary"
                     :icon="h(EditOutlined)"
                     :disabled="single"
                     @click="handleUpdate"
                     v-hasPermi="['system:dict:edit']"
                  >修改</a-button>
               </a-col>
               <a-col :span="1.5">
                  <a-button
                     type="primary"
                     danger
                     :icon="h(DeleteOutlined)"
                     :disabled="multiple"
                     @click="handleDelete"
                     v-hasPermi="['system:dict:remove']"
                  >删除</a-button>
               </a-col>
               <a-col :span="1.5">
                  <a-button
                     :icon="h(DownloadOutlined)"
                     @click="handleExport"
                     v-hasPermi="['system:dict:export']"
                  >导出</a-button>
               </a-col>
               <a-col :span="1.5">
                  <a-button
                     :icon="h(CloseOutlined)"
                     @click="handleClose"
                  >关闭</a-button>
               </a-col>
            </a-row>
            <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
         </div>

         <!-- <a-table :loading="loading" :data-source="dataList" :row-selection="{ onChange: handleSelectionChange }"> -->
         <a-spin :spinning="loading">
            <a-table
              :data-source="dataList"
              :columns="tableColumns"
              :pagination="false"
              striped
              :scroll="{ y: '60vh' }"
              :row-selection="{ type: 'checkbox', onChange: handleSelectionChange }"
              row-key="dictCode"
              :locale="{ emptyText: emptyContent }"
            >
              <template #bodyCell="{ column, record }">
                <template v-if="column.dataIndex === 'dictLabel'">
                  <span v-if="(record.listClass == '' || record.listClass == 'default') && (record.cssClass == '' || record.cssClass == null)">{{ record.dictLabel }}</span>
                  <a-tag v-else :color="record.listClass === 'primary' ? 'blue' : record.listClass" :class="record.cssClass">{{ record.dictLabel }}</a-tag>
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
         <a-form ref="dataRef" :model="form" :rules="rules" :label-col="{ style: { width: '80px' } }">
            <a-row :gutter="20">
               <a-col :span="12">
                  <a-form-item label="字典类型">
                     <a-input v-model:value="form.dictType" disabled />
                  </a-form-item>
               </a-col>
               <a-col :span="12">
                  <a-form-item label="数据标签" name="dictLabel">
                     <a-input v-model:value="form.dictLabel" placeholder="请输入数据标签" />
                  </a-form-item>
               </a-col>
               <a-col :span="12">
                  <a-form-item label="数据键值" name="dictValue">
                     <a-input v-model:value="form.dictValue" placeholder="请输入数据键值" />
                  </a-form-item>
               </a-col>
               <a-col :span="12">
                  <a-form-item label="样式属性" name="cssClass">
                     <a-input v-model:value="form.cssClass" placeholder="请输入样式属性" />
                  </a-form-item>
               </a-col>
               <a-col :span="12">
                  <a-form-item label="显示排序" name="dictSort">
                     <a-input-number style="width:100%" v-model:value="form.dictSort" :min="0" />
                  </a-form-item>
               </a-col>
               <a-col :span="12">
                  <a-form-item label="回显样式" name="listClass">
                     <a-select v-model:value="form.listClass">
                        <a-select-option
                           v-for="item in listClassOptions"
                           :key="item.value"
                           :value="item.value"
                        >{{ item.label + '(' + item.value + ')' }}</a-select-option>
                     </a-select>
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

<script setup name="Data">

import useDictStore from '@/store/system/dict.js'

import { optionselect as getDictOptionselect, getType } from "@/api/system/system/dict/type.js";

import { listData, getData, delData, addData, updateData } from "@/api/system/system/dict/data.js";

import { normalizePage, pageRows } from "@/utils/page.js";
import { h } from 'vue';
import { CloseOutlined, DeleteOutlined, DownloadOutlined, EditOutlined, PlusOutlined } from "@ant-design/icons-vue";

const { proxy } = getCurrentInstance();
const { sys_normal_disable } = proxy.useDict("sys_normal_disable");

const tableColumns = [
  { title: '字典编码', dataIndex: 'dictCode', align: 'center' },
  { title: '字典标签', dataIndex: 'dictLabel', align: 'center' },
  { title: '字典键值', dataIndex: 'dictValue', align: 'center' },
  { title: '字典排序', dataIndex: 'dictSort', align: 'center' },
  { title: '状态', dataIndex: 'status', align: 'center' },
  { title: '备注', dataIndex: 'remark', align: 'center', ellipsis: true },
  { title: '创建时间', dataIndex: 'createTime', align: 'center', width: 180 },
  { title: '操作', key: 'actions', align: 'center', fixed: 'right', width: 240 },
];

const emptyContent = h('div', { class: 'emptyBg' }, [
  h('img', { src: new URL('@/assets/system/images/no_data/noData.png', import.meta.url).href, alt: '' }),
  h('p', '没有记录哦~'),
]);

const dataList = ref([]);
const open = ref(false);
const loading = ref(true);
const showSearch = ref(true);
const ids = ref([]);
const single = ref(true);
const multiple = ref(true);
const total = ref(0);
const title = ref("");
const defaultDictType = ref("");
const typeOptions = ref([]);
const route = useRoute();
// 数据标签回显样式
const listClassOptions = ref([
  { value: "default", label: "默认" },
  { value: "primary", label: "主要" },
  { value: "success", label: "成功" },
  { value: "info", label: "信息" },
  { value: "warning", label: "警告" },
  { value: "danger", label: "危险" }
]);

const data = reactive({
  form: {},
  queryParams: {
    pageNum: 1,
    pageSize: 6,
    dictType: undefined,
    dictLabel: undefined,
    status: undefined
  },
  rules: {
    dictLabel: [{ required: true, message: "数据标签不能为空", trigger: "blur" }],
    dictValue: [{ required: true, message: "数据键值不能为空", trigger: "blur" }],
    dictSort: [{ required: true, message: "数据顺序不能为空", trigger: "blur" }]
  }
});

const { queryParams, form, rules } = toRefs(data);

/** 查询字典类型详细 */
function getTypes(dictId) {
  getType(dictId).then(response => {
    queryParams.value.dictType = response.data.dictType;
    defaultDictType.value = response.data.dictType;
    getList();
  });
}

/** 查询字典类型列表 */
function getTypeList() {
  getDictOptionselect().then(response => {
    typeOptions.value = response.data;
  });
}

/** 查询字典数据列表 */
function getList() {
  loading.value = true;
  listData(queryParams.value).then(response => {
    const page = normalizePage(response);
    total.value = page.total;
    dataList.value = pageRows(page.rows, page.total, queryParams.value);
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
    dictCode: undefined,
    dictLabel: undefined,
    dictValue: undefined,
    cssClass: undefined,
    listClass: "default",
    dictSort: 0,
    status: "0",
    remark: undefined
  };
  proxy.resetForm("dataRef");
}

/** 搜索按钮操作 */
function handleQuery() {
  queryParams.value.pageNum = 1;
  getList();
}

/** 返回按钮操作 */
function handleClose() {
  const obj = { path: "/system/dict" };
  proxy.$tab.closeOpenPage(obj);
}

/** 重置按钮操作 */
function resetQuery() {
  proxy.resetForm("queryRef");
  queryParams.value.dictType = defaultDictType.value;
  handleQuery();
}

/** 新增按钮操作 */
function handleAdd() {
  reset();
  open.value = true;
  title.value = "新增字典数据";
  form.value.dictType = queryParams.value.dictType;
}

/** 多选框选中数据 */
function handleSelectionChange(selectedRowKeys, selectedRows) {
  ids.value = selectedRows.map(item => item.dictCode);
  single.value = selectedRows.length != 1;
  multiple.value = !selectedRows.length;
}

/** 修改按钮操作 */
function handleUpdate(row) {
  reset();
  const dictCode = row.dictCode || ids.value;
  getData(dictCode).then(response => {
    form.value = response.data;
    open.value = true;
    title.value = "修改字典数据";
  });
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs["dataRef"].validate().then(() => {
    if (form.value.dictCode != undefined) {
      updateData(form.value).then(response => {
        useDictStore().removeDict(queryParams.value.dictType);
        proxy.$modal.msgSuccess("修改成功");
        open.value = false;
        getList();
      });
    } else {
      addData(form.value).then(response => {
        useDictStore().removeDict(queryParams.value.dictType);
        proxy.$modal.msgSuccess("新增成功");
        open.value = false;
        getList();
      });
    }
  }).catch(() => {});
}

/** 删除按钮操作 */
function handleDelete(row) {
  const dictCodes = row.dictCode || ids.value;
  proxy.$modal.confirm('是否确认删除字典编码为"' + dictCodes + '"的数据项？').then(function() {
    return delData(dictCodes);
  }).then(() => {
    getList();
    proxy.$modal.msgSuccess("删除成功");
    useDictStore().removeDict(queryParams.value.dictType);
  }).catch(() => {});
}

/** 导出按钮操作 */
function handleExport() {
  proxy.download("system/dict/data/export", {
    ...queryParams.value
  }, `dict_data_${new Date().getTime()}.xlsx`);
}

getTypes(route.params && route.params.dictId);
getTypeList();
</script>

