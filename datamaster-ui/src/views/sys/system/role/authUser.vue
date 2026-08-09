<template>
  <div class="app-container" ref="app-container">
    <div class="pagecont-top" v-show="showSearch">
      <a-form class="btn-style" :model="queryParams" ref="queryRef" v-show="showSearch" layout="inline">
        <a-form-item label="用户名称" name="userName">
          <a-input v-model:value="queryParams.userName" placeholder="请输入用户名称" allow-clear @pressEnter="handleQuery" />
        </a-form-item>
        <a-form-item label="手机号码" name="phonenumber">
          <a-input v-model:value="queryParams.phonenumber" placeholder="请输入手机号码" allow-clear @pressEnter="handleQuery" />
        </a-form-item>
        <a-form-item>
          <a-button type="primary" @click="handleQuery" @mousedown="(e) => e.preventDefault()">
            <i class="iconfont-mini icon-a-zu22377 mr5"></i>查询
          </a-button>
          <a-button @click="resetQuery" @mousedown="(e) => e.preventDefault()">
            <i class="iconfont-mini icon-a-zu22378 mr5"></i>重置
          </a-button>
        </a-form-item>
      </a-form>
    </div>

    <div class="pagecont-bottom">
      <div class="justify-between mb15">
        <div class="btn-style">
          <a-button type="primary" @click="openSelectUser"
            v-hasPermi="['system:role:add']">新增用户</a-button>
          <a-button danger :disabled="multiple" @click="cancelAuthUserAll"
            v-hasPermi="['system:role:remove']">批量取消授权</a-button>
          <a-button @click="handleClose">关闭</a-button>
        </div>
        <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
      </div>

      <a-table
        striped
        :loading="loading"
        :data-source="userList"
        :pagination="false"
        :columns="tableColumns"
        :scroll="{ y: '60vh' }"
        row-key="userId"
        :row-selection="{ selectedRowKeys, onChange: handleSelectionChange }"
        :locale="{ emptyText: '暂无记录' }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.dataIndex === 'status'">
            <dict-tag :options="sys_normal_disable" :value="record.status" />
          </template>
          <template v-if="column.dataIndex === 'createTime'">
            <span>{{ parseTime(record.createTime) }}</span>
          </template>
          <template v-if="column.key === 'actions'">
            <a-button type="link" size="small" @click="cancelAuthUser(record)"
              v-hasPermi="['system:role:remove']">取消授权</a-button>
          </template>
        </template>
      </a-table>

      <pagination :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize"
        @pagination="getList" />
    </div>
    <select-user ref="selectRef" :roleId="queryParams.roleId" @ok="handleQuery" />
  </div>
</template>

<script setup name="AuthUser">
import selectUser from "./selectUser.vue";
import { allocatedUserList, authUserCancel, authUserCancelAll } from "@/api/system/system/role.js";
import { normalizePage, pageRows } from "@/utils/page.js";

const route = useRoute();
const { proxy } = getCurrentInstance();
const { sys_normal_disable } = proxy.useDict("sys_normal_disable");

const userList = ref([]);
const loading = ref(true);
const showSearch = ref(true);
const multiple = ref(true);
const total = ref(0);
const userIds = ref([]);
const selectedRowKeys = ref([]);
const tableColumns = [
  { title: '用户名称', dataIndex: 'userName', ellipsis: true },
  { title: '用户昵称', dataIndex: 'nickName', ellipsis: true },
  { title: '邮箱', dataIndex: 'email', ellipsis: true },
  { title: '手机', dataIndex: 'phonenumber', ellipsis: true },
  { title: '状态', dataIndex: 'status', align: 'center' },
  { title: '创建时间', dataIndex: 'createTime', align: 'center', width: 180 },
  { title: '操作', key: 'actions', align: 'center' },
];

const queryParams = reactive({
  pageNum: 1,
  pageSize: 6,
  roleId: route.params.roleId,
  userName: undefined,
  phonenumber: undefined,
});

/** 查询授权用户列表 */
function getList() {
  loading.value = true;
  allocatedUserList(queryParams).then((response) => {
    const page = normalizePage(response);
    total.value = page.total;
    userList.value = pageRows(page.rows, page.total, queryParams);
    loading.value = false;
  });
}

/** 返回按钮 */
function handleClose() {
  const obj = { path: "/system/role" };
  proxy.$tab.closeOpenPage(obj);
}

/** 搜索按钮操作 */
function handleQuery() {
  queryParams.pageNum = 1;
  getList();
}

/** 重置按钮操作 */
function resetQuery() {
  proxy.resetForm("queryRef");
  handleQuery();
}

/** 多选框选中数据 */
function handleSelectionChange(selectedKeys, selectedRows) {
  selectedRowKeys.value = selectedKeys;
  userIds.value = selectedRows.map((item) => item.userId);
  multiple.value = !selectedRows.length;
}

/** 打开授权用户表弹窗 */
function openSelectUser() {
  proxy.$refs["selectRef"].show();
}

/** 取消授权按钮操作 */
function cancelAuthUser(row) {
  proxy.$modal
    .confirm('确认要取消该用户"' + row.userName + '"角色吗？')
    .then(function () {
      return authUserCancel({ userId: row.userId, roleId: queryParams.roleId });
    })
    .then(() => {
      getList();
      proxy.$modal.msgSuccess("取消授权成功");
    })
    .catch(() => { });
}

/** 批量取消授权按钮操作 */
function cancelAuthUserAll(row) {
  const roleId = queryParams.roleId;
  const uIds = userIds.value.join(",");
  proxy.$modal
    .confirm("是否取消选中用户授权数据项?")
    .then(function () {
      return authUserCancelAll({ roleId: roleId, userIds: uIds });
    })
    .then(() => {
      getList();
      proxy.$modal.msgSuccess("取消授权成功");
    })
    .catch(() => { });
}

getList();
</script>

