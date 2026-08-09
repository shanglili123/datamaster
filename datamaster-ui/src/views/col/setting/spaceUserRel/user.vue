<template>
  <div class="pagecont-top" v-show="showSearch">
    <a-form class="btn-style" :model="queryParams" ref="queryRef" layout="inline" v-show="showSearch"
      :label-col="{ style: { width: '68px' } }" @submit.prevent>
      <a-form-item label="用户姓名" name="nickName">
        <a-input class="el-form-input-width" v-model:value="queryParams.nickName" placeholder="请输入用户姓名" allow-clear
          @pressEnter="handleQuery" />
      </a-form-item>
      <a-form-item label="手机号码" name="phoneNumber">
        <a-input v-model:value="queryParams.phoneNumber" placeholder="请输入手机号码" allow-clear class="el-form-input-width"
          @pressEnter="handleQuery" />
      </a-form-item>
      <a-form-item label="创建时间">
        <a-range-picker @change="handleDateChange" class="el-form-input-width" v-model:value="createTime"
          valueFormat="YYYY-MM-DD" />
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
      <a-row :gutter="15" class="btn-style">
        <a-col :span="1.5">
          <a-button type="primary" @click="handleAdd" v-hasPermi="['col:spaceUserRel:add']"
            @mousedown="(e) => e.preventDefault()">
            <i class="iconfont-mini icon-xinzeng mr5"></i>新增
          </a-button>
        </a-col>
        <a-col :span="1.5">
          <a-button danger :disabled="multiple" @click="handleDelete"
            v-hasPermi="['col:spaceUserRel:remove']" @mousedown="(e) => e.preventDefault()">
            <i class="iconfont-mini icon-shanchu-huise mr5"></i>移除
          </a-button>
        </a-col>
      </a-row>
      <div class="justify-end top-right-btn">
        <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
      </div>
    </div>
    <a-table striped :loading="loading" :data-source="spaceUserRelList" :columns="mainTableColumns"
      row-key="id" :pagination="false"
      :row-selection="{ selectedRowKeys: selectedRowKeys, onChange: onMainSelectionChange }"
      @change="handleTableChange"
      :locale="{ emptyText: emptyContent }">
      <template #bodyCell="{ column, record }">
        <template v-if="column.dataIndex === 'userId'">
          {{ record.userId || '-' }}
        </template>
        <template v-else-if="column.dataIndex === 'nickName'">
          {{ record.nickName || '-' }}
        </template>
        <template v-else-if="column.dataIndex === 'roleStr'">
          {{ record.roleStr || '-' }}
        </template>
        <template v-else-if="column.dataIndex === 'deptName'">
          {{ record.deptName || '-' }}
        </template>
        <template v-else-if="column.dataIndex === 'phoneNumber'">
          {{ record.phoneNumber || '-' }}
        </template>
        <template v-else-if="column.dataIndex === 'createBy'">
          {{ record.createBy || "-" }}
        </template>
        <template v-else-if="column.dataIndex === 'createTime'">
          <span>{{ parseTime(record.createTime, "{y}-{m}-{d} {h}:{i}") || "-" }}</span>
        </template>
        <template v-else-if="column.key === 'actions'">
          <a-button type="link" size="small" @click="handleUpdate(record)"
            v-hasPermi="['col:spaceUserRel:edit']">修改</a-button>
          <a-button type="link" danger size="small" @click="handleDelete(record)"
            v-hasPermi="['col:spaceUserRel:remove']">移除</a-button>
        </template>
      </template>
    </a-table>

    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum"
      v-model:limit="queryParams.pageSize" @pagination="getList" />
  </div>

  <!-- 新增或修改空间与用户关联关系对话框 -->
  <a-modal :title="title" v-model:open="open" class="warn-dialog-23012" width="700px" draggable>
    <template #header="{ close, titleId, titleClass }">
      <span role="heading" aria-level="2" class="ant-modal-title">
        {{ title }}
      </span>
    </template>
    <a-form ref="spaceUserRelRef" :model="form" :rules="rules" :label-col="{ style: { width: '80px' } }"
      @submit.prevent>
      <a-row :gutter="20">
        <a-col :span="24" v-if="form.id == null">
          <div class="hint-div">
            <InfoCircleOutlined style="color: #2A7BFD" />
            <span>
              如需添加新用户，请先点击``<a href="/system/user" style="color: #2a7bfd">用户管理</a>``进行添加。
            </span>
          </div>
        </a-col>
        <a-col :span="24" v-if="form.id == null">
          <a-form-item label="系统用户" name="userNameList">
            <a-input style="width: 76%" v-model:value="form.userNameList" placeholder="请选择用户" disabled>
            </a-input>
            <a-button style="margin-left: 12px" type="primary" @click="getListUser">选择用户</a-button>
          </a-form-item>
        </a-col>
        <a-col :span="24" v-if="form.id != null">
          <a-form-item label="系统用户" name="nickName">
            <a-input v-model:value="form.nickName" placeholder="请选择用户" disabled>
            </a-input>
          </a-form-item>
        </a-col>
        <a-col :span="24">
          <a-form-item label="用户角色" name="roleIdList">
            <a-checkbox-group v-model:value="form.roleIdList" class="checkbox-vertical">
              <div v-for="item in roleList" :key="item.roleId" style="margin-bottom: 15px;height: 40px;">
                <a-checkbox :value="item.roleId">
                  {{ item.roleName }}
                </a-checkbox>
                <p
                  style="display: flex;align-items: center;line-height:1;font-size: 12px;color: #888;margin-left: 23px;margin-top: 10px;">
                  {{ item.remark }}</p>
              </div>
            </a-checkbox-group>
          </a-form-item>
        </a-col>
      </a-row>
    </a-form>
    <template #footer>
      <div class="dialog-footer">
        <a-button size="small" @click="cancel">取 消</a-button>
        <a-button type="primary" size="small" @click="submitForm">确 定</a-button>
      </div>
    </template>
  </a-modal>

  <a-modal title="用户选择" v-model:open="openTwo" width="1000px" class="user-select-tatble" draggable>
    <template>
      <span role="heading" aria-level="2" class="ant-modal-title"> 用户选择 </span>
    </template>
    <!--用户数据-->
    <a-form class="btn-style" :model="queryParamsUser" ref="queryRef" layout="inline"
      :label-col="{ style: { width: '68px' } }">
      <a-form-item label="登录账号" name="userName">
        <a-input v-model:value="queryParamsUser.userName" placeholder="请输入登录账号" allow-clear class="el-form-input-width"
          @pressEnter="handleQueryUser" />
      </a-form-item>
      <a-form-item label="手机号码" name="phonenumber">
        <a-input v-model:value="queryParamsUser.phonenumber" placeholder="请输入手机号码" allow-clear
          class="el-form-input-width" @pressEnter="handleQueryUser" />
      </a-form-item>
      <a-form-item>
        <a-button type="primary" @click="handleQueryUser" @mousedown="(e) => e.preventDefault()">
          <i class="iconfont-mini icon-a-zu22377 mr5"></i>查询
        </a-button>
        <a-button @click="resetQueryUser" @mousedown="(e) => e.preventDefault()">
          <i class="iconfont-mini icon-a-zu22378 mr5"></i>重置
        </a-button>
      </a-form-item>
    </a-form>
    <a-table ref="userTableRef" striped :loading="loadingUser" :data-source="userList" :columns="userTableColumns"
      row-key="userId" :pagination="false"
      :row-selection="{ selectedRowKeys: selectedUserRowKeys, onChange: onUserSelectionChange }">
      <template #bodyCell="{ column, record }">
        <template v-if="column.dataIndex === 'createBy'">
          {{ record.createBy || "-" }}
        </template>
        <template v-else-if="column.dataIndex === 'createTime'">
          <span>{{ parseTime(record.createTime, "{y}-{m}-{d} {h}:{i}") || "-" }}</span>
        </template>
      </template>
    </a-table>
    <pagination v-show="totalUser > 0" :total="totalUser" v-model:page="queryParamsUser.pageNum"
      v-model:limit="queryParamsUser.pageSize" @pagination="getListUser" />
    <template #footer>
      <div class="dialog-footer">
        <a-button size="small" @click="openTwo = false">取 消</a-button>
        <a-button type="primary" size="small" @click="submitFormUser">确 定</a-button>
      </div>
    </template>
  </a-modal>
</template>

<script setup name="SpaceUserRel">
import { listUser } from '@/api/system/system/user.js';
import {
  listSpaceUserRel,
  getSpaceUserRel,
  delSpaceUserRel,
  addSpaceUserRel,
  updateSpaceUserRel,
  editUserListAndRoleList,
  listRole,
  getSpaceUserRoleDetail,
  addUserListAndRoleList
} from '@/api/tax/spaceUserRel/spaceUserRel';
import { getToken } from '@/utils/auth.js';
import useUserStore from '@/store/system/user';
import { checkSpaceManagePermission, noSpaceUser } from '@/api/tax/space/space.js';
import { ref, h, computed } from 'vue';
import { InfoCircleOutlined } from '@ant-design/icons-vue';
import { normalizePage, pageRows } from "@/utils/page.js";
const { proxy } = getCurrentInstance();
const { sys_normal_disable, sys_user_sex } = proxy.useDict(
  'sys_normal_disable',
  'sys_user_sex'
);
const emptyContent = h('div', { class: 'emptyBg' }, [
  h('img', { src: new URL('@/assets/system/images/no_data/noData.png', import.meta.url).href, alt: '' }),
  h('p', '暂无记录'),
]);

const mainTableColumns = computed(() => {
  const cols = [
    { title: '编号', dataIndex: 'userId', align: 'center', width: 80 },
    { title: '用户姓名', dataIndex: 'nickName', align: 'center' },
    { title: '角色', dataIndex: 'roleStr', align: 'center', ellipsis: true },
    { title: '部门', dataIndex: 'deptName', align: 'center' },
    { title: '手机号', dataIndex: 'phoneNumber', align: 'center' },
    { title: '创建人', dataIndex: 'createBy', align: 'center' },
  ];
  if (getColumnVisibility(14)) {
    cols.push({ title: '创建时间', dataIndex: 'createTime', align: 'center', width: 150, sorter: true, key: 'create_time' });
  }
  cols.push({ title: '操作', key: 'actions', align: 'center', fixed: 'right', width: 240 });
  return cols;
});

const userTableColumns = [
  { title: '编号', dataIndex: 'userId', align: 'center', width: 80 },
  { title: '登录账号', dataIndex: 'userName', align: 'center', ellipsis: true },
  { title: '用户姓名', dataIndex: 'nickName', align: 'center', ellipsis: true },
  { title: '部门', dataIndex: 'dept.deptName', align: 'center', width: 180, ellipsis: true },
  { title: '手机号码', dataIndex: 'phonenumber', align: 'center', width: 180 },
  { title: '创建人', dataIndex: 'createBy', align: 'left', ellipsis: true },
  { title: '创建时间', dataIndex: 'createTime', align: 'center', width: 150 },
];

const selectedRowKeys = ref([]);
const selectedUserRowKeys = ref([]);

const spaceUserRelList = ref([]);
const size = (ref < 'default') | 'large' | ('small' > 'default');
const value1 = ref('');
const value2 = ref('');
const activeName = ref('first');
// 列显隐信息
const columns = ref([
  { key: 0, label: 'ID', visible: true },
  { key: 2, label: '用户ID', visible: true },
  { key: 7, label: '创建时间', visible: true }
]);
const userList = ref([]);
const getColumnVisibility = (key) => {
  const column = columns.value.find((col) => col.key === key);
  // 如果没有找到对应列配置，默认显示
  if (!column) return true;
  // 如果找到对应列配置，根据visible属性来控制显示
  return column.visible;
};
const userStore = useUserStore();
const open = ref(false);
const openTwo = ref(false);
const openDetail = ref(false);
const loading = ref(true);
const loadingUser = ref(true);
const showSearch = ref(true);
const ids = ref([]);
const idsUser = ref([]);
const userName = ref([]);
const single = ref(true);
const multiple = ref(true);
const total = ref(0);
const totalUser = ref(0);
const title = ref('');
const defaultSort = ref({ prop: 'createTime', order: 'desc' });
const router = useRouter();
const roleList = ref([]);
/*** 用户导入参数 */
const upload = reactive({
  // 是否显示弹出层（用户导入）
  open: false,
  // 弹出层标题（用户导入）
  title: '',
  // 是否禁用上传
  isUploading: false,
  // 是否更新已经存在的用户数据
  updateSupport: 0,
  // 设置上传的请求头部
  headers: { Authorization: 'Bearer ' + getToken() },
  // 上传的地址
  url: import.meta.env.VITE_APP_BASE_API + '/tax/spaceUserRel/importData'
});
const createTime = ref(null);
const data = reactive({
  form: {
    userIdList: [],
    userNameList: [],
    roleIdList: []
  },
  queryParams: {
    pageNum: 1,
    pageSize: 6,
    spaceId: null,
    userId: null,
    createTime: null,
    endTime: null,
    startTime: null
  },
  queryParamsUser: {
    pageNum: 1,
    pageSize: 6,
    spaceId: null,
    userName: undefined,
    phoneNumber: undefined,
    status: undefined,
    deptId: undefined
  },
  rules: {
    userNameList: [{ required: true, message: '请选择用户', trigger: 'change' }],
    roleIdList: [{ required: true, message: '请选择用户角色', trigger: 'change' }]
  }
});

const { queryParams, queryParamsUser, form, rules } = toRefs(data);
let canManageSpaceUsers = ref(false);
// 监听 userStore 中的 spaceId 变化
watch(
  () => userStore.spaceId,
  (newValue, oldValue) => {
    if (newValue !== oldValue) {
      console.log(userStore.spaceCode, 'userStore.spaceCode');

      queryParams.value.spaceId = newValue;
      queryParamsUser.value.spaceId = newValue;
      getList();
    }
  },
  { immediate: true }
);
function handleDateChange(value) {
  queryParams.value.startTime = value[0];
  queryParams.value.endTime = value[1];
}
/** 查询空间与用户关联关系列表 */
function getList() {
  loading.value = true;
  if (queryParams.value.spaceId) {
    listSpaceUserRel(queryParams.value).then((response) => {
      const page = normalizePage(response);
      total.value = page.total;
      spaceUserRelList.value = pageRows(page.rows, page.total, queryParams.value);
      loading.value = false;
    });
    checkSpaceManagePermission(queryParams.value.spaceId).then((response) => {
      canManageSpaceUsers.value = response.data;
    });
  }
}

function getListUser() {
  loadingUser.value = true;
  noSpaceUser(queryParamsUser.value).then((response) => {
    const page = normalizePage(response);
    userList.value = pageRows(page.rows, page.total, queryParamsUser.value);
    openTwo.value = true;
    totalUser.value = page.total;
    loadingUser.value = false;
    console.log(userList.value, 'userList');

    // 在表格加载完成后，设置之前选中的用户
    nextTick(() => {
      selectedUserRowKeys.value = userList.value
        .filter((user) => form.value.userIdList.includes(user.userId))
        .map((user) => user.userId);
    });
  });
}
/** 搜索按钮操作 */
function handleQueryUser() {
  queryParamsUser.value.pageNum = 1;
  getListUser();
}
/** 重置按钮操作 */
function resetQueryUser() {
  queryParamsUser.value = {
    pageNum: 1,
    pageSize: 6,
    spaceId: userStore.spaceId,
    userName: undefined,
    phoneNumber: undefined,
    status: undefined,
    deptId: undefined
  };
  handleQueryUser();
}
/** 提交按钮操作 */
function submitFormUser() {
  form.value.userIdList = idsUser.value;
  form.value.userNameList = userName.value;
  openTwo.value = false;
}
// 多选框选中数据
function onUserSelectionChange(keys, rows) {
  selectedUserRowKeys.value = keys;
  idsUser.value = keys;
  userName.value = rows.map((item) => item.nickName);
}
function handleSelectionChangeUser(selection) {
  idsUser.value = selection.map((item) => item.userId);
  userName.value = selection.map((item) => item.nickName);
}
function getRoleList() {
  if (queryParams.value.spaceId) {
    listRole(queryParams.value).then((response) => {
      roleList.value = normalizePage(response).rows;
      console.log(roleList.value, 'roleList');
    });
  }
}
// 取消按钮
function cancel() {
  open.value = false;
  openDetail.value = false;
  reset();
}

// 表单重置
function reset() {
  form.value = {
    id: null,
    spaceId: null,
    userId: null,
    userIdList: [],
    userName: null,
    userNameList: [],
    roleIdList: [],
    validFlag: null,
    delFlag: null,
    createBy: null,
    creatorId: null,
    createTime: null,
    updateBy: null,
    updaterId: null,
    updateTime: null,
    remark: null
  };
  proxy.resetForm('spaceUserRelRef');
}

/** 搜索按钮操作 */
function handleQuery() {
  queryParams.value.pageNum = 1;
  getList();
}

/** 重置按钮操作 */
function resetQuery() {
  createTime.value = null;
  queryParams.value = {
    pageNum: 1,
    pageSize: 6,
    spaceId: userStore.spaceId,
    userId: null,
    createTime: null,
    endTime: null,
    startTime: null
  };
  // proxy.resetForm('queryRef');
  handleQuery();
}

// 多选框选中数据
function onMainSelectionChange(keys, rows) {
  selectedRowKeys.value = keys;
  ids.value = keys;
  single.value = rows.length != 1;
  multiple.value = !rows.length;
}
function handleSelectionChange(selection) {
  ids.value = selection.map((item) => item.id);
  single.value = selection.length != 1;
  multiple.value = !selection.length;
}

/** 排序触发事件 */
function handleSortChange(column, prop, order) {
  queryParams.value.orderByColumn = column.prop;
  queryParams.value.isAsc = column.order;
  getList();
}
function handleTableChange(pagination, filters, sorter) {
  if (sorter && sorter.field) {
    queryParams.value.orderByColumn = sorter.field;
    queryParams.value.isAsc = sorter.order === 'ascend' ? 'ascending' : 'descending';
    getList();
  }
}

/** 新增按钮操作 */
function handleAdd() {
  getRoleList();
  reset();
  open.value = true;
  title.value = '新增空间成员';
}

/** 修改按钮操作 */
function handleUpdate(row) {
  reset();
  const _id = row.id || ids.value;
  getRoleList();
  getSpaceUserRoleDetail(_id).then((response) => {
    form.value = response.data;
    console.log(form.value, 'form');
    open.value = true;
    title.value = '修改空间成员';
  });
}

/** 详情按钮操作 */
function handleDetail(row) {
  reset();
  const _id = row.id || ids.value;
  getSpaceUserRel(_id).then((response) => {
    form.value = response.data;
    openDetail.value = true;
    title.value = '空间与用户关联关系详情';
  });
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs['spaceUserRelRef'].validate().then(() => {
    if (form.value.id != null) {
      editUserListAndRoleList(form.value)
        .then((response) => {
          proxy.$modal.msgSuccess('修改成功');
          open.value = false;
          getList();
        })
        .catch((error) => { });
    } else {
      // 新增时增加额外验证
      if (!form.value.userIdList || form.value.userIdList.length === 0) {
        proxy.$modal.msgWarning('未选择用户，请选择用户后重试');
        return;
      }
      form.value.spaceId = userStore.spaceId;
      addUserListAndRoleList(form.value)
        .then((response) => {
          proxy.$modal.msgSuccess('新增成功');
          open.value = false;
          getList();
        })
        .catch((error) => { });
    }
  }).catch(() => { });
}

/** 删除按钮操作 */
function handleDelete(row) {
  const _ids = row.id || ids.value;
  const _userId =
    row.userId ||
    spaceUserRelList.value
      .filter((item) => ids.value.includes(item.id))
      .map((item) => item.userId);
  proxy.$modal
    .confirm('是否确认移除编号为"' + _userId + '"的数据项？')
    .then(function () {
      return delSpaceUserRel(_ids);
    })
    .then(() => {
      getList();
      proxy.$modal.msgSuccess('移除成功');
    })
    .catch(() => { });
}

/** 导出按钮操作 */
function handleExport() {
  proxy.download(
    'tax/spaceUserRel/export',
    {
      ...queryParams.value
    },
    `SpaceUserRel_${new Date().getTime()}.xlsx`
  );
}

/** ---------------- 导入相关操作 -----------------**/
/** 导入按钮操作 */
function handleImport() {
  upload.title = '空间与用户关联关系导入';
  upload.open = true;
}

/** 下载模板操作 */
function importTemplate() {
  proxy.download(
    'system/user/importTemplate',
    {},
    `SpaceUserRel_template_${new Date().getTime()}.xlsx`
  );
}

/** 提交上传文件 */
function submitFileForm() {
  proxy.$refs['uploadRef'].submit();
}

/**文件上传中处理 */
const handleFileUploadProgress = (event, file, fileList) => {
  upload.isUploading = true;
};

/** 文件上传成功处理 */
const handleFileSuccess = (response, file, fileList) => {
  upload.open = false;
  upload.isUploading = false;
  proxy.$refs['uploadRef'].handleRemove(file);
  proxy.$alert(
    "<div style='overflow: auto;overflow-x: hidden;max-height: 70vh;padding: 10px 20px 0;'>" +
    response.msg +
    '</div>',
    '导入结果',
    { dangerouslyUseHTMLString: true }
  );
  getList();
};
/** ---------------------------------**/

function routeTo(link, row) {
  if (link !== '' && link.indexOf('http') !== -1) {
    window.location.href = link;
    return;
  }
  if (link !== '') {
    if (link === router.currentRoute.value.path) {
      window.location.reload();
    } else {
      router.push({
        path: link,
        query: {
          id: row.id
        }
      });
    }
  }
}
</script>
<style lang="scss" scoped>
.hint-div {
  margin: 0px 0px 20px 12px;
  /* border-top: 1px solid rgba(204, 204, 204, 0.5); */
  border-right: 1px solid rgba(204, 204, 204, 0.5);
  border-bottom: 1px solid #e5f1f8;
  border-left: 1px solid #e5f1f8;
  border-radius: 8px;
  background-color: #ecf5ff;
  padding: 10px;
  box-shadow: -1px 1px 2px #e5f1f8;
  display: flex;
  align-items: center;

  span {
    margin-left: 5px;
  }
}
</style>
<style scoped lang="scss">
.app-container {
  .pagecont-bottom {
    min-height: calc(100vh - 240px);
  }
}
</style>
<style lang="scss">
.warn-dialog-23012 {
  .ant-modal-body {
    overflow: auto;
    height: 500px !important;
    padding: 20px 40px !important;
  }
}

.user-select-tatble {
  .ant-modal-body {
    height: 600px !important;
  }
}

.checkbox-vertical {
  display: flex;
  flex-direction: column;
  /* 竖排 */
  margin-top: 8px;
}

.checkbox-vertical .ant-checkbox {
  display: block;
  margin-bottom: 0px;
  height: 15px !important;
}
</style>

