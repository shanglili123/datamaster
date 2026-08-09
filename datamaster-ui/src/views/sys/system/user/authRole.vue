<template>
  <div class="app-container" ref="app-container">
    <div class="pagecont-top">
      <h4 class="form-header h4">基本信息</h4>
      <a-form class="btn-style" :model="form" :label-col="{ style: { width: '80px' } }">
        <a-row>
          <a-col :span="8" :offset="2">
            <a-form-item label="用户名称" name="nickName">
              <a-input v-model:value="form.nickName" disabled />
            </a-form-item>
          </a-col>
          <a-col :span="8" :offset="2">
            <a-form-item label="登录账号" name="userName">
              <a-input v-model:value="form.userName" disabled />
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>
    </div>

    <div class="pagecont-bottom">
      <h4 class="form-header h4">角色信息</h4>
      <a-table
        :scroll="{ y: '500px' }"
        :loading="loading"
        :row-key="getRowKey"
        @row-click="clickRow"
        ref="roleRef"
        :row-selection="{ onChange: (selectedRowKeys, selectedRows) => handleSelectionChange(selectedRows) }"
        :data-source="roles.slice((pageNum - 1) * pageSize, pageNum * pageSize)"
        :columns="columns"
      >
        <template #bodyCell="{ column, record, index }">
          <template v-if="column.key === 'index'">
            <span>{{ (pageNum - 1) * pageSize + index + 1 }}</span>
          </template>
          <template v-else-if="column.key === 'createTime'">
            <span>{{ parseTime(record.createTime) }}</span>
          </template>
        </template>
      </a-table>

      <pagination v-show="total > 0" :total="total" v-model:page="pageNum" v-model:limit="pageSize" />

      <a-form :label-col="{ style: { width: '100px' } }">
        <div style="text-align: center; margin-left: -120px; margin-top: 30px">
          <a-button type="primary" @click="submitForm()">提交</a-button>
          <a-button @click="close()">返回</a-button>
        </div>
      </a-form>
    </div>
  </div>
</template>

<script setup name="AuthRole">
import { getAuthRole, updateAuthRole } from "@/api/system/system/user.js";

const route = useRoute();
const { proxy } = getCurrentInstance();

const loading = ref(true);
const total = ref(0);
const pageNum = ref(1);
const pageSize = ref(10);
const roleIds = ref([]);
const roles = ref([]);
const form = ref({
  nickName: undefined,
  userName: undefined,
  userId: undefined,
});

const columns = [
  { title: '序号', key: 'index', width: 80, align: 'center' },
  { title: '角色编号', dataIndex: 'roleId', key: 'roleId', align: 'center' },
  { title: '角色名称', dataIndex: 'roleName', key: 'roleName', align: 'center' },
  { title: '权限字符', dataIndex: 'roleKey', key: 'roleKey', align: 'center' },
  { title: '创建时间', dataIndex: 'createTime', key: 'createTime', align: 'center', width: 180 },
];

/** 单击选中行数据 */
function clickRow(row) {
  proxy.$refs["roleRef"].toggleRowSelection(row);
}

/** 多选框选中数据 */
function handleSelectionChange(selection) {
  roleIds.value = selection.map((item) => item.roleId);
}

/** 保存选中的数据编号 */
function getRowKey(row) {
  return row.roleId;
}

/** 关闭按钮 */
function close() {
  const obj = { path: "/system/user" };
  proxy.$tab.closeOpenPage(obj);
}

/** 提交按钮 */
function submitForm() {
  const userId = form.value.userId;
  const rIds = roleIds.value.join(",");
  updateAuthRole({ userId: userId, roleIds: rIds }).then((response) => {
    proxy.$modal.msgSuccess("授权成功");
    close();
  });
}

(() => {
  const userId = route.params && route.params.userId;
  if (userId) {
    loading.value = true;
    getAuthRole(userId).then((response) => {
      form.value = response.user;
      roles.value = response.roles;
      total.value = roles.value.length;
      nextTick(() => {
        roles.value.forEach((row) => {
          if (row.flag) {
            proxy.$refs["roleRef"].toggleRowSelection(row);
          }
        });
      });
      loading.value = false;
    });
  }
})();
</script>
