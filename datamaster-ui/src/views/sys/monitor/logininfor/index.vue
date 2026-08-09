<template>
   <div class="app-container" ref="app-container">
      <div class="pagecont-top" v-show="showSearch">
         <a-form class="btn-style" :model="queryParams" ref="queryRef" :layout="inline" :label-col="{ style: { width: '68px' } }">
            <a-form-item label="登录地址" name="ipaddr">
               <a-input
                  v-model:value="queryParams.ipaddr"
                  placeholder="请输入登录地址"
                  allow-clear
                  class="el-form-input-width"
                  @pressEnter="handleQuery"
               />
            </a-form-item>
            <a-form-item label="用户名称" name="userName">
               <a-input
                  v-model:value="queryParams.userName"
                  placeholder="请输入用户名称"
                  allow-clear
                  class="el-form-input-width"
                  @pressEnter="handleQuery"
               />
            </a-form-item>
            <a-form-item label="状态" name="status">
               <a-select
                  v-model:value="queryParams.status"
                  placeholder="登录状态"
                  allow-clear
                  class="el-form-input-width"
               >
                  <a-select-option
                     v-for="dict in sys_common_status"
                     :key="dict.value"
                     :value="dict.value">{{ dict.label }}</a-select-option>
               </a-select>
            </a-form-item>
            <a-form-item label="登录时间">
               <a-range-picker
                  class="el-form-input-width"
                  v-model:value="dateRange"
                  valueFormat="YYYY-MM-DD HH:mm:ss"
                  :show-time="{ format: 'HH:mm:ss' }"
                  start-placeholder="开始日期"
                  end-placeholder="结束日期"
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
      </div>
      <div  class="pagecont-bottom">
         <div class="justify-between mb15">
         <a-row :gutter="10" class="btn-style">
            <a-col :span="1.5">
               <a-button
                  danger
                  :icon="h(DeleteOutlined)"
                  :disabled="multiple"
                  @click="handleDelete"
                  v-hasPermi="['monitor:logininfor:remove']"
               >删除</a-button>
            </a-col>
            <a-col :span="1.5">
               <a-button
                  danger
                  :icon="h(DeleteOutlined)"
                  @click="handleClean"
                  v-hasPermi="['monitor:logininfor:remove']"
               >清空</a-button>
            </a-col>
            <a-col :span="1.5">
               <a-button
                  type="primary"
                  :icon="h(UnlockOutlined)"
                  :disabled="single"
                  @click="handleUnlock"
                  v-hasPermi="['monitor:logininfor:unlock']"
               >解锁</a-button>
            </a-col>
            <a-col :span="1.5">
               <a-button
                  :icon="h(DownloadOutlined)"
                  @click="handleExport"
                  v-hasPermi="['monitor:logininfor:export']"
               >导出</a-button>
            </a-col>
         </a-row>
         <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
         </div>

         <a-spin :spinning="loading">
            <a-table
              ref="logininforRef"
              :data-source="logininforList"
              :columns="tableColumns"
              :pagination="false"
              striped
              :scroll="{ y: '60vh' }"
              :row-selection="{ type: 'checkbox', onChange: handleSelectionChange }"
              row-key="infoId"
              :locale="{ emptyText: emptyContent }"
              @change="handleSortChange"
            >
              <template #bodyCell="{ column, record }">
                <template v-if="column.dataIndex === 'status'">
                  <dict-tag :options="sys_common_status" :value="record.status" />
                </template>
                <template v-else-if="column.dataIndex === 'loginTime'">
                  <span>{{ parseTime(record.loginTime) }}</span>
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
   </div>
</template>

<script setup name="Logininfor">

import { list, delLogininfor, cleanLogininfor, unlockLogininfor } from "@/api/system/monitor/logininfor.js";
import { h } from 'vue';
import { DeleteOutlined, DownloadOutlined, UnlockOutlined } from "@ant-design/icons-vue";

const { proxy } = getCurrentInstance();
const { sys_common_status } = proxy.useDict("sys_common_status");

const tableColumns = [
  { title: '访问编号', dataIndex: 'infoId', align: 'center' },
  { title: '用户名称', dataIndex: 'userName', align: 'center', width: 120, ellipsis: true, sorter: true },
  { title: '地址', dataIndex: 'ipaddr', align: 'center', ellipsis: true },
  { title: '登录地点', dataIndex: 'loginLocation', align: 'center', ellipsis: true },
  { title: '操作系统', dataIndex: 'os', align: 'center', ellipsis: true },
  { title: '浏览器', dataIndex: 'browser', align: 'center', ellipsis: true },
  { title: '登录状态', dataIndex: 'status', align: 'center' },
  { title: '描述', dataIndex: 'msg', align: 'center', ellipsis: true },
  { title: '访问时间', dataIndex: 'loginTime', align: 'center', width: 180, sorter: true, defaultSortOrder: 'descend' },
];

const emptyContent = h('div', { class: 'emptyBg' }, [
  h('img', { src: new URL('@/assets/system/images/no_data/noData.png', import.meta.url).href, alt: '' }),
  h('p', '没有记录哦~'),
]);

const logininforList = ref([]);
const loading = ref(true);
const showSearch = ref(true);
const ids = ref([]);
const single = ref(true);
const multiple = ref(true);
const selectName = ref("");
const total = ref(0);
const dateRange = ref([]);
const defaultSort = ref({ prop: "loginTime", order: "descending" });

// 查询参数
const queryParams = ref({
  pageNum: 1,
  pageSize: 6,
  ipaddr: undefined,
  userName: undefined,
  status: undefined,
  orderByColumn: undefined,
  isAsc: undefined
});

/** 查询登录日志列表 */
function getList() {
  loading.value = true;
  list(proxy.addDateRange(queryParams.value, dateRange.value)).then(response => {
    logininforList.value = response.rows;
    total.value = response.total;
    loading.value = false;
  });
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

/** 多选框选中数据 */
function handleSelectionChange(selectedRowKeys, selectedRows) {
  ids.value = selectedRows.map(item => item.infoId);
  multiple.value = !selectedRows.length;
  single.value = selectedRows.length != 1;
  selectName.value = selectedRows.map(item => item.userName);
}

/** 排序触发事件 */
function handleSortChange(pag, filters, sorter) {
  const prop = sorter.field || sorter.column?.dataIndex;
  const order = sorter.order === 'ascend' ? 'ascending' : sorter.order === 'descend' ? 'descending' : null;
  queryParams.value.orderByColumn = prop;
  queryParams.value.isAsc = order;
  getList();
}

/** 删除按钮操作 */
function handleDelete(row) {
  const infoIds = row.infoId || ids.value;
  proxy.$modal.confirm('是否确认删除访问编号为"' + infoIds + '"的数据项?').then(function () {
    return delLogininfor(infoIds);
  }).then(() => {
    getList();
    proxy.$modal.msgSuccess("删除成功");
  }).catch(() => {});
}

/** 清空按钮操作 */
function handleClean() {
  proxy.$modal.confirm("是否确认清空所有登录日志数据项?").then(function () {
    return cleanLogininfor();
  }).then(() => {
    getList();
    proxy.$modal.msgSuccess("清空成功");
  }).catch(() => {});
}

/** 解锁按钮操作 */
function handleUnlock() {
  const username = selectName.value;
  proxy.$modal.confirm('是否确认解锁用户"' + username + '"数据项?').then(function () {
    return unlockLogininfor(username);
  }).then(() => {
    proxy.$modal.msgSuccess("用户" + username + "解锁成功");
  }).catch(() => {});
}

/** 导出按钮操作 */
function handleExport() {
  proxy.download("monitor/logininfor/export", {
    ...queryParams.value,
  }, `config_${new Date().getTime()}.xlsx`);
}

getList();
</script>

