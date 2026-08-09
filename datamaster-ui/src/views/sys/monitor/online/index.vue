<template>
   <div class="app-container" ref="app-container">
      <div class="pagecont-top">
         <a-form class="btn-style" :model="queryParams" ref="queryRef" :layout="inline" :label-col="{ style: { width: '68px' } }">
            <a-form-item label="登录地址" name="ipaddr">
               <a-input v-model:value="queryParams.ipaddr" placeholder="请输入登录地址" allow-clear class="el-form-input-width"
                  @pressEnter="handleQuery" />
            </a-form-item>
            <a-form-item label="用户名称" name="userName">
               <a-input v-model:value="queryParams.userName" placeholder="请输入用户名称" allow-clear class="el-form-input-width"
                  @pressEnter="handleQuery" />
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
      <div class="pagecont-bottom">

         <a-spin :spinning="loading">
            <a-table
              :data-source="onlineList.slice((pageNum - 1) * pageSize, pageNum * pageSize)"
              :columns="tableColumns"
              :pagination="false"
              striped
              :scroll="{ y: '60vh' }"
              row-key="tokenId"
              :locale="{ emptyText: emptyContent }"
            >
              <template #bodyCell="{ column, record, index }">
                <template v-if="column.key === 'index'">
                  <span>{{ (pageNum - 1) * pageSize + index + 1 }}</span>
                </template>
                <template v-else-if="column.dataIndex === 'loginTime'">
                  <span>{{ parseTime(record.loginTime) }}</span>
                </template>
                <template v-else-if="column.key === 'actions'">
                  <a-button type="link" danger size="small" @click="handleForceLogout(record)" v-hasPermi="['monitor:online:forceLogout']">强退</a-button>
                </template>
                <template v-else>
                  <span>{{ record[column.dataIndex] || '-' }}</span>
                </template>
              </template>
            </a-table>
         </a-spin>

         <pagination v-show="total > 0" :total="total" v-model:page="pageNum" v-model:limit="pageSize" />
      </div>
   </div>
</template>

<script setup name="Online">

import { forceLogout, list as initData } from "@/api/system/monitor/online.js";
import { h } from 'vue';

const { proxy } = getCurrentInstance();

const tableColumns = [
  { title: '序号', key: 'index', width: 80, align: 'center' },
  { title: '会话编号', dataIndex: 'tokenId', align: 'center', ellipsis: true },
  { title: '登录名称', dataIndex: 'userName', align: 'center', ellipsis: true },
  { title: '所属部门', dataIndex: 'deptName', align: 'center', ellipsis: true },
  { title: '主机', dataIndex: 'ipaddr', align: 'center', ellipsis: true },
  { title: '登录地点', dataIndex: 'loginLocation', align: 'center', ellipsis: true },
  { title: '操作系统', dataIndex: 'os', align: 'center', ellipsis: true },
  { title: '浏览器', dataIndex: 'browser', align: 'center', ellipsis: true },
  { title: '登录时间', dataIndex: 'loginTime', align: 'center', width: 180 },
  { title: '操作', key: 'actions', align: 'center', fixed: 'right', width: 240 },
];

const emptyContent = h('div', { class: 'emptyBg' }, [
  h('img', { src: new URL('@/assets/system/images/no_data/noData.png', import.meta.url).href, alt: '' }),
  h('p', '没有记录哦~'),
]);

const onlineList = ref([]);
const loading = ref(true);
const total = ref(0);
const pageNum = ref(1);
const pageSize = ref(10);

const queryParams = ref({
   ipaddr: undefined,
   userName: undefined
});

/** 查询登录日志列表 */
function getList() {
   loading.value = true;
   initData(queryParams.value).then(response => {
      onlineList.value = response.rows;
      total.value = response.total;
      loading.value = false;
   });
}

/** 搜索按钮操作 */
function handleQuery() {
   pageNum.value = 1;
   getList();
}

/** 重置按钮操作 */
function resetQuery() {
   proxy.resetForm("queryRef");
   handleQuery();
}

/** 强退按钮操作 */
function handleForceLogout(row) {
   proxy.$modal.confirm('是否确认强退名称为"' + row.userName + '"的用户?').then(function () {
      return forceLogout(row.tokenId);
   }).then(() => {
      getList();
      proxy.$modal.msgSuccess("删除成功");
   }).catch(() => { });
}

getList();
</script>

<style scoped lang="scss"></style>

