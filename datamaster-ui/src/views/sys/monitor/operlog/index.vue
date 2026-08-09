<template>
   <div class="app-container" ref="app-container">
      <div class="pagecont-top" v-show="showSearch">
         <a-form class="btn-style" :model="queryParams" ref="queryRef" :layout="inline" v-show="showSearch"
            :label-col="{ style: { width: '45px' } }">
            <a-form-item label="地址" name="operIp">
               <a-input v-model:value="queryParams.operIp" placeholder="请输入操作地址" allow-clear style="width: 120px;"
                  @pressEnter="handleQuery" />
            </a-form-item>
            <a-form-item label="模块" name="title">
               <a-input v-model:value="queryParams.title" placeholder="请输入系统模块" allow-clear style="width: 120px;"
                  @pressEnter="handleQuery" />
            </a-form-item>
            <a-form-item label="人员" name="operName">
               <a-input v-model:value="queryParams.operName" placeholder="请输入操作人员" allow-clear style="width: 120px;"
                  @pressEnter="handleQuery" />
            </a-form-item>
            <a-form-item label="类型" name="businessType">
               <a-select v-model:value="queryParams.businessType" placeholder="操作类型" allow-clear style="width: 120px;">
                  <a-select-option v-for="dict in sys_oper_type" :key="dict.value" :value="dict.value">{{ dict.label }}</a-select-option>
               </a-select>
            </a-form-item>
            <a-form-item label="状态" name="status">
               <a-select v-model:value="queryParams.status" placeholder="操作状态" allow-clear style="width: 120px;">
                  <a-select-option v-for="dict in sys_common_status" :key="dict.value"
                     :value="dict.value">{{ dict.label }}</a-select-option>
               </a-select>
            </a-form-item>
            <a-form-item label="时间">
               <a-range-picker v-model:value="dateRange" valueFormat="YYYY-MM-DD HH:mm:ss"
                  :show-time="{ format: 'HH:mm:ss' }" start-placeholder="开始" end-placeholder="结束"
                  style="width: 200px;"></a-range-picker>
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
            <a-dropdown trigger="click" v-hasPermi="['monitor:operlog:remove', 'monitor:operlog:export']">
              <a-button>
                更多<DownOutlined style="margin-left: 4px; font-size: 12px" />
              </a-button>
              <template #overlay>
                <a-menu @click="handleMoreMenu">
                  <a-menu-item key="delete" :disabled="multiple" v-hasPermi="['monitor:operlog:remove']">
                    <DeleteOutlined />删除
                  </a-menu-item>
                  <a-menu-item key="clean" v-hasPermi="['monitor:operlog:remove']">
                    <DeleteOutlined />清空
                  </a-menu-item>
                  <a-menu-item key="export" v-hasPermi="['monitor:operlog:export']">
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
      <div>
         <a-spin :spinning="loading">
            <a-table
              ref="operlogRef"
              :data-source="operlogList"
              :columns="tableColumns"
              :pagination="false"
              striped
              :row-selection="{ type: 'checkbox', onChange: handleSelectionChange }"
              row-key="operId"
              :locale="{ emptyText: emptyContent }"
              @change="handleSortChange"
            >
              <template #bodyCell="{ column, record }">
                <template v-if="column.dataIndex === 'businessType'">
                  <dict-tag :options="sys_oper_type" :value="record.businessType" />
                </template>
                <template v-else-if="column.dataIndex === 'status'">
                  <dict-tag :options="sys_common_status" :value="record.status" />
                </template>
                <template v-else-if="column.dataIndex === 'operTime'">
                  <span>{{ parseTime(record.operTime) }}</span>
                </template>
                <template v-else-if="column.dataIndex === 'costTime'">
                  <span>{{ record.costTime }}毫秒</span>
                </template>
                <template v-else-if="column.key === 'actions'">
                  <a-button type="link" size="small" @click="handleView(record)" v-hasPermi="['monitor:operlog:query']">详细</a-button>
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

      <!-- 操作日志详细 -->
      <a-modal title="操作日志详细" v-model:open="open" width="800px" destroy-on-close>
         <a-form :model="form" :label-col="{ style: { width: '80px' } }">
            <a-row :gutter="20">
               <a-col :span="12">
                  <a-form-item label="操作模块">
                     <div class="form-readonly">
                        {{ form.title }} / {{ typeFormat(form) }}
                     </div>
                  </a-form-item>
               </a-col>
               <a-col :span="12">
                  <a-form-item label="登录信息">
                     <div class="form-readonly">
                        {{ form.operName }} / {{ form.operIp }} / {{ form.operLocation }}
                     </div>
                  </a-form-item>
               </a-col>
               <a-col :span="12">
                  <a-form-item label="请求地址">
                     <div class="form-readonly">
                        {{ form.operUrl }}
                     </div>
                  </a-form-item>
               </a-col>
               <a-col :span="12">
                  <a-form-item label="请求方式">
                     <div class="form-readonly">
                        {{ form.requestMethod }}
                     </div>
                  </a-form-item>
               </a-col>
               <a-col :span="24">
                  <a-form-item label="操作方法">
                     <div class="form-readonly">
                        {{ form.method }}
                     </div>
                  </a-form-item>
               </a-col>
               <a-col :span="24">
                  <a-form-item label="请求参数">
                     <div class="form-readonly">
                        {{ form.operParam }}
                     </div>
                  </a-form-item>
               </a-col>
               <a-col :span="24">
                  <a-form-item label="返回参数">
                     <div class="form-readonly">
                        {{ form.jsonResult }}
                     </div>
                  </a-form-item>
               </a-col>
               <a-col :span="12">
                  <a-form-item label="操作状态">
                     <div class="form-readonly" v-if="form.status === 0">正常</div>
                     <div class="form-readonly" v-else-if="form.status === 1">失败</div>
                  </a-form-item>
               </a-col>
               <a-col :span="12">
                  <a-form-item label="消耗时间">
                     <div class="form-readonly">{{ form.costTime }}毫秒</div>
                  </a-form-item>
               </a-col>
               <a-col :span="12">
                  <a-form-item label="操作时间">
                     <div class="form-readonly">{{ parseTime(form.operTime) }}</div>
                  </a-form-item>
               </a-col>
               <a-col :span="24">
                  <a-form-item label="异常信息" v-if="form.status === 1">
                     <div class="form-readonly">{{ form.errorMsg }}</div>
                  </a-form-item>
               </a-col>
            </a-row>
         </a-form>
         <template #footer>
            <div class="dialog-footer">
               <a-button @click="open = false">关 闭</a-button>
            </div>
         </template>
      </a-modal>
   </div>
</template>

<script setup name="Operlog">

import { list, delOperlog, cleanOperlog } from "@/api/system/monitor/operlog.js";
import { h } from 'vue';
import { DeleteOutlined, DownloadOutlined } from "@ant-design/icons-vue";

const { proxy } = getCurrentInstance();
const { sys_oper_type, sys_common_status } = proxy.useDict("sys_oper_type", "sys_common_status");

const tableColumns = [
  { title: '日志编号', dataIndex: 'operId', align: 'center' },
  { title: '系统模块', dataIndex: 'title', align: 'center', ellipsis: true },
  { title: '操作类型', dataIndex: 'businessType', align: 'center' },
  { title: '操作人员', dataIndex: 'operName', align: 'center', width: 110, ellipsis: true, sorter: true },
  { title: '操作地址', dataIndex: 'operIp', align: 'center', width: 130, ellipsis: true },
  { title: '操作状态', dataIndex: 'status', align: 'center' },
  { title: '操作日期', dataIndex: 'operTime', align: 'center', width: 180, sorter: true, defaultSortOrder: 'descend' },
  { title: '消耗时间', dataIndex: 'costTime', align: 'center', width: 110, ellipsis: true, sorter: true },
  { title: '操作', key: 'actions', align: 'center', fixed: 'right', width: 240 },
];

const emptyContent = h('div', { class: 'emptyBg' }, [
  h('img', { src: new URL('@/assets/system/images/no_data/noData.png', import.meta.url).href, alt: '' }),
  h('p', '没有记录哦~'),
]);

const operlogList = ref([]);
const open = ref(false);
const loading = ref(true);
const showSearch = ref(true);
const ids = ref([]);
const single = ref(true);
const multiple = ref(true);
const total = ref(0);
const title = ref("");
const dateRange = ref([]);
const defaultSort = ref({ prop: "operTime", order: "descending" });

const data = reactive({
   form: {},
   queryParams: {
      pageNum: 1,
      pageSize: 6,
      operIp: undefined,
      title: undefined,
      operName: undefined,
      businessType: undefined,
      status: undefined
   }
});

const { queryParams, form } = toRefs(data);

/** 查询登录日志 */
function getList() {
   loading.value = true;
   list(proxy.addDateRange(queryParams.value, dateRange.value)).then(response => {
      operlogList.value = response.rows;
      total.value = response.total;
      loading.value = false;
   });
}

/** 操作日志类型字典翻译 */
function typeFormat(row, column) {
   return proxy.selectDictLabel(sys_oper_type.value, row.businessType);
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
   queryParams.value.pageNum = 1;
   queryParams.value.orderByColumn = defaultSort.value.prop;
   queryParams.value.isAsc = defaultSort.value.order;
   getList();
}

/** 更多下拉菜单操作 */
function handleMoreMenu({ key }) {
   if (key === 'delete') {
      handleDelete();
   } else if (key === 'clean') {
      handleClean();
   } else if (key === 'export') {
      handleExport();
   }
}

/** 多选框选中数据 */
function handleSelectionChange(selectedRowKeys, selectedRows) {
   ids.value = selectedRows.map(item => item.operId);
   multiple.value = !selectedRows.length;
}

/** 排序触发事件 */
function handleSortChange(pag, filters, sorter) {
   const prop = sorter.field || sorter.column?.dataIndex;
   const order = sorter.order === 'ascend' ? 'ascending' : sorter.order === 'descend' ? 'descending' : null;
   queryParams.value.orderByColumn = prop;
   queryParams.value.isAsc = order;
   getList();
}

/** 详细按钮操作 */
function handleView(row) {
   open.value = true;
   form.value = row;
}

/** 删除按钮操作 */
function handleDelete(row) {
   const operIds = row.operId || ids.value;
   proxy.$modal.confirm('是否确认删除日志编号为"' + operIds + '"的数据项?').then(function () {
      return delOperlog(operIds);
   }).then(() => {
      getList();
      proxy.$modal.msgSuccess("删除成功");
   }).catch(() => { });
}

/** 清空按钮操作 */
function handleClean() {
   proxy.$modal.confirm("是否确认清空所有操作日志数据项?").then(function () {
      return cleanOperlog();
   }).then(() => {
      getList();
      proxy.$modal.msgSuccess("清空成功");
   }).catch(() => { });
}

/** 导出按钮操作 */
function handleExport() {
   proxy.download("monitor/operlog/export", {
      ...queryParams.value,
   }, `config_${new Date().getTime()}.xlsx`);
}

getList();
</script>
<style scoped lang="scss">
.form-readonly {
   width: 100%;
   border: 1px solid #c0c4cc;
   padding: 0px 10px;
   min-height: 34px;
}

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

