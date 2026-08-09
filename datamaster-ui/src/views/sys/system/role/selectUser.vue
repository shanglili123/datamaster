<template>
   <!-- 授权用户 -->
   <a-modal :title="'选择用户'" v-model:open="visible" width="800px">
      <a-form :model="queryParams" ref="queryRef" layout="inline">
         <a-form-item label="用户名称" name="userName">
            <a-input v-model:value="queryParams.userName" placeholder="请输入用户名称" allow-clear style="width: 180px"
               @pressEnter="handleQuery" />
         </a-form-item>
         <a-form-item label="手机号码" name="phonenumber">
            <a-input v-model:value="queryParams.phonenumber" placeholder="请输入手机号码" allow-clear style="width: 180px"
               @pressEnter="handleQuery" />
         </a-form-item>
         <a-form-item>
            <a-button type="primary" :icon="h(SearchOutlined)" @click="handleQuery">搜索</a-button>
            <a-button :icon="h(ReloadOutlined)" @click="resetQuery">重置</a-button>
         </a-form-item>
      </a-form>
      <a-row>
         <a-table @row-click="clickRow" ref="refTable" :data-source="userList"
            :row-selection="{ onChange: (selectedRowKeys, selectedRows) => handleSelectionChange(selectedRows) }"
            :row-key="'userId'" :scroll="{ y: '260px' }" :columns="columns">
            <template #bodyCell="{ column, record }">
               <template v-if="column.key === 'status'">
                  <dict-tag :options="sys_normal_disable" :value="record.status" />
               </template>
               <template v-else-if="column.key === 'createTime'">
                  <span>{{ parseTime(record.createTime) }}</span>
               </template>
            </template>
         </a-table>
      </a-row>
      <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum"
         v-model:limit="queryParams.pageSize" @pagination="getList" />
      <template #footer>
         <div class="dialog-footer">
            <a-button type="primary" @click="handleSelectUser">确 定</a-button>
            <a-button @click="visible = false">取 消</a-button>
         </div>
      </template>
   </a-modal>
</template>

<script setup name="SelectUser">
import { authUserSelectAll, unallocatedUserList } from "@/api/system/system/role.js";
import { normalizePage, pageRows } from "@/utils/page.js";
import { h } from "vue";
import { SearchOutlined, ReloadOutlined } from "@ant-design/icons-vue";

const props = defineProps({
   roleId: {
      type: [Number, String]
   }
});

const { proxy } = getCurrentInstance();
const { sys_normal_disable } = proxy.useDict("sys_normal_disable");

const userList = ref([]);
const visible = ref(false);
const total = ref(0);
const userIds = ref([]);

const columns = [
   { title: '用户名称', dataIndex: 'userName', key: 'userName', ellipsis: true },
   { title: '用户昵称', dataIndex: 'nickName', key: 'nickName', ellipsis: true },
   { title: '邮箱', dataIndex: 'email', key: 'email', ellipsis: true },
   { title: '手机', dataIndex: 'phonenumber', key: 'phonenumber', ellipsis: true },
   { title: '状态', dataIndex: 'status', key: 'status', align: 'center' },
   { title: '创建时间', dataIndex: 'createTime', key: 'createTime', align: 'center', width: 180 },
];

const queryParams = reactive({
   pageNum: 1,
   pageSize: 6,
   roleId: undefined,
   userName: undefined,
   phonenumber: undefined
});

// 显示弹框
function show() {
   queryParams.roleId = props.roleId;
   getList();
   visible.value = true;
}

/**选择行 */
function clickRow(row) {
   proxy.$refs["refTable"].toggleRowSelection(row);
}

// 多选框选中数据
function handleSelectionChange(selection) {
   userIds.value = selection.map(item => item.userId);
}

// 查询表数据
function getList() {
   unallocatedUserList(queryParams).then(res => {
      const page = normalizePage(res);
      total.value = page.total;
      userList.value = pageRows(page.rows, page.total, queryParams);
   });
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

const emit = defineEmits(["ok"]);
/** 选择授权用户操作 */
function handleSelectUser() {
   const roleId = queryParams.roleId;
   const uIds = userIds.value.join(",");
   if (uIds == "") {
      proxy.$modal.msgError("请选择要分配的用户");
      return;
   }
   authUserSelectAll({ roleId: roleId, userIds: uIds }).then(res => {
      proxy.$modal.msgSuccess(res.msg);
      visible.value = false;
      emit("ok");
   });
}

defineExpose({
   show,
});
</script>

