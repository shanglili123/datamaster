<template>
  <div class="app-container" ref="app-container">
    <a-layout style="90%">
      <!-- 左侧可调整的部分 -->
      <a-layout-sider :style="{ width: `${leftWidth}px`, marginLeft: leftWidth == 0 ? '-15px' : '0px' }" class="left-pane">
        <div class="left-tree">
          <div class="head-container">
            <a-input v-model:value="deptName" placeholder="请输入部门名称" allow-clear
              style="margin-bottom: 20px">
              <template #prefix><SearchOutlined /></template>
            </a-input>
          </div>
          <div class="head-container">
            <a-tree :tree-data="deptOptions" :field-names="{ title: 'label', children: 'children', key: 'id' }"
              :filter-node-method="filterNode" ref="deptTreeRef" node-key="id" highlight-current default-expand-all
              @select="(selectedKeys, e) => handleNodeClick(e.node.dataRef)">
              <template #title="{ data, expanded, selected }">
                <span class="custom-tree-node">
                  <!-- 有子节点：文件夹图标 -->
                  <FolderOpenOutlined class="iconimg colorxz" v-if="expanded && data.children && data.children.length" />
                  <FolderOutlined class="iconimg colorxz" v-if="!expanded && data.children && data.children.length" />
                  <!-- 无子节点：文件图标 -->
                  <FileTextOutlined class="zjiconimg colorwxz"
                    v-show="!selected && (!data.children || data.children.length == 0)" />
                  <FileTextOutlined class="zjiconimg colorxz"
                    v-show="selected && (!data.children || data.children.length == 0)" />

                  <span class="treelable" @click="getNode(data)">{{ data.label }}</span>
                </span>
              </template>
            </a-tree>
          </div>
        </div>
      </a-layout-sider>
      <!-- 拖拽条 -->
      <div class="resize-bar" @mousedown="startResize">
        <div class="resize-handle-sx">
          <span class="zjsx"></span>
          <RightOutlined v-if="leftWidth == 0" @click.stop="toggleCollapse" class="collapse-icon" />
          <LeftOutlined v-else class="collapse-icon" @click.stop="toggleCollapse" />
        </div>
      </div>
      <!-- 右侧部分 -->
      <a-layout-content>
        <!--用户数据-->
        <div class="pagecont-top" v-show="showSearch">
          <a-form class="btn-style" :model="queryParams" ref="queryRef" layout="inline" :label-col="{ style: { width: '45px' } }">
            <a-form-item label="名称" name="userName">
              <a-input v-model:value="queryParams.userName" placeholder="请输入用户名称" allow-clear style="width: 150px"
                @pressEnter="handleQuery" />
            </a-form-item>
            <a-form-item label="手机" name="phonenumber">
              <a-input v-model:value="queryParams.phonenumber" placeholder="请输入手机号码" allow-clear style="width: 150px"
                @pressEnter="handleQuery" />
            </a-form-item>
            <a-form-item label="状态" name="status">
              <a-select v-model:value="queryParams.status" placeholder="用户状态" allow-clear style="width: 150px">
                <a-select-option v-for="dict in sys_normal_disable" :key="dict.value"
                  :value="dict.value">{{ dict.label }}</a-select-option>
              </a-select>
            </a-form-item>
            <a-form-item label="时间">
              <a-range-picker v-model:value="dateRange" valueFormat="YYYY-MM-DD"
                :placeholder="['开始', '结束']" :separator="'-'" style="width: 200px;"></a-range-picker>
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
          <div class="data-action-btns">
            <a-button type="primary" :icon="h(PlusOutlined)" @click="handleAdd" v-hasPermi="['system:user:add']">新增</a-button>
            <a-dropdown trigger="click" v-hasPermi="['system:user:import', 'system:user:export']">
              <a-button type="info">
                更多<DownOutlined style="font-size: 12px; margin-left: 4px;" />
              </a-button>
              <template #overlay>
                <a-menu>
                  <a-menu-item key="import" @click="handleImport" v-hasPermi="['system:user:import']">
                    <UploadOutlined />导入
                  </a-menu-item>
                  <a-menu-item key="export" @click="handleExport" v-hasPermi="['system:user:export']">
                    <DownloadOutlined />导出
                  </a-menu-item>
                </a-menu>
              </template>
            </a-dropdown>
          </div>
          <div class="top-right-btn">
            <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" :columns="columns"></right-toolbar>
          </div>
        </div>
        <div>
          <a-table
            striped
            :loading="loading"
            :data-source="userList"
            :pagination="false"
            :columns="tableColumns"
            :scroll="{ y: '58vh' }"
            row-key="userId"
            :row-selection="{ selectedRowKeys, onChange: handleSelectionChange }"
            :locale="{ emptyText: '暂无记录' }"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.dataIndex === 'deptName'">
                {{ record.dept?.deptName || '-' }}
              </template>
              <template v-if="column.dataIndex === 'status'">
                <a-switch v-model:checked="record.status" checked-value="0" un-checked-value="1"
                  @change="handleStatusChange(record)"></a-switch>
              </template>
              <template v-if="column.dataIndex === 'createTime'">
                <span>{{ parseTime(record.createTime) }}</span>
              </template>
              <template v-if="column.dataIndex === 'dataPermissionLevel'">
                <span>{{ dataPermissionLevelLabel(record.dataPermissionLevel) }}</span>
              </template>
              <template v-if="column.key === 'actions'">
                <a-button type="link" size="small" @click="handleUpdate(record)"
                  v-hasPermi="['system:user:edit']" v-if="record.userId !== 1">修改</a-button>
                <a-button type="link" danger size="small" @click="handleDelete(record)"
                  v-hasPermi="['system:user:remove']" v-if="record.userId !== 1">删除</a-button>
                <a-popover placement="bottom" :width="150" trigger="click" v-if="record.userId !== 1">
                  <a-button type="link" size="small">更多</a-button>
                  <template #content>
                    <div style="width: 90px" class="butgdlist">
                      <a-button type="link" size="small" @click="handleResetPwd(record)" v-hasPermi="['system:user:resetPwd']">重置密码</a-button>
                      <a-button type="link" size="small" @click="handleAuthRole(record)" v-hasPermi="['system:user:edit']">分配角色</a-button>
                    </div>
                  </template>
                </a-popover>
              </template>
            </template>
          </a-table>
          <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum"
            v-model:limit="queryParams.pageSize" @pagination="getList" />
        </div>
      </a-layout-content>
    </a-layout>

    <!-- 添加或修改用户配置对话框 -->
    <a-modal :title="title" v-model:open="open" width="800px" destroy-on-close>
      <a-form :model="form" :rules="rules" ref="userRef" :label-col="{ style: { width: '80px' } }">
        <a-row :gutter="20">
          <a-col :span="12">
            <a-form-item label="用户昵称" name="nickName">
              <a-input v-model:value="form.nickName" placeholder="请输入用户昵称" maxlength="30" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="归属部门" name="deptId">
              <a-tree-select v-model:value="form.deptId" :tree-data="deptOptions"
                :field-names="{ value: 'id', label: 'label', children: 'children' }" placeholder="请选择归属部门" />
              <!--                     <treeselect v-model="form.deptId" :options="deptOptions" :flat="true" :show-count="true" placeholder="请选择归属部门"-->
              <!--                                 noResultsText="暂无数据" :multiple="true"-->
              <!--                     />-->
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="20">
          <a-col :span="12">
            <a-form-item label="手机号码" name="phonenumber">
              <a-input v-model:value="form.phonenumber" placeholder="请输入手机号码" maxlength="11" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="邮箱" name="email">
              <a-input v-model:value="form.email" placeholder="请输入邮箱" maxlength="50" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="20">
          <a-col :span="12">
            <a-form-item v-if="form.userId == undefined" label="用户名称" name="userName">
              <a-input v-model:value="form.userName" placeholder="请输入用户名称" maxlength="30" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item v-if="form.userId == undefined" label="用户密码" name="password">
              <a-input v-model:value="form.password" placeholder="请输入用户密码" type="password" maxlength="20" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="20">
          <a-col :span="12">
            <a-form-item label="用户性别">
              <a-select v-model:value="form.sex" placeholder="请选择">
                <a-select-option v-for="dict in sys_user_sex" :key="dict.value"
                  :value="dict.value">{{ dict.label }}</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="状态">
              <a-radio-group v-model:value="form.status">
                <a-radio v-for="dict in sys_normal_disable" :key="dict.value" :value="dict.value">{{ dict.label }}
                </a-radio>
              </a-radio-group>
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="20">
          <a-col :span="12">
            <a-form-item label="岗位">
              <a-select v-model:value="form.postIds" mode="multiple" placeholder="请选择" class="selectlist">
                <a-select-option v-for="item in postOptions" :key="item.postId" :value="item.postId"
                  :disabled="item.status == 1">{{ item.postName }}</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="角色" name="roleIds">
            <a-select v-model:value="form.roleIds" mode="multiple" placeholder="请选择" class="selectlist">
                <a-select-option v-for="item in roleOptions" :key="item.roleId" :value="item.roleId"
                  :disabled="item.status == 1">{{ item.roleName }}</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="20">
          <a-col :span="12">
            <a-form-item label="数据权限" name="dataPermissionLevel">
              <a-select v-model:value="form.dataPermissionLevel" placeholder="请选择数据权限等级">
                <a-select-option v-for="item in dataPermissionLevelOptions" :key="item.value"
                  :value="item.value">{{ item.label }}</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="20">
          <a-col :span="24">
            <a-form-item label="备注">
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

    <!-- 用户导入对话框 -->
    <a-modal :title="upload.title" v-model:open="upload.open" width="800px" destroy-on-close>
      <a-upload-dragger ref="uploadRef" :max-count="1" accept=".xlsx, .xls" :headers="upload.headers"
        :action="upload.url + '?updateSupport=' + upload.updateSupport" :disabled="upload.isUploading"
        @progress="handleFileUploadProgress" @success="handleFileSuccess">
        <CloudUploadOutlined style="font-size: 42px; color: #4096ff" />
        <div class="ant-upload-text">将文件拖到此处，或<em>点击上传</em></div>
        <template #tip>
          <div class="ant-upload-tip text-center">
            <div class="ant-upload-tip">
              <a-checkbox v-model:checked="upload.updateSupport" />
              是否更新已经存在的用户数据
            </div>
            <span>仅允许导入xls、xlsx格式文件。</span>
            <a-link type="primary" style="font-size: 12px; vertical-align: baseline"
              @click="importTemplate">下载模板
            </a-link>
          </div>
        </template>
      </a-upload-dragger>
      <template #footer>
        <div class="dialog-footer">
          <a-button @click="upload.open = false">取 消</a-button>
          <a-button type="primary" @click="submitFileForm">确 定</a-button>
        </div>
      </template>
    </a-modal>
  </div>
</template>

<script setup name="User">
import { getToken } from "@/utils/auth.js";
import Cookies from "js-cookie";
import {
  changeUserStatus,
  listUser,
  resetUserPwd,
  delUser,
  getUser,
  updateUser,
  addUser,
  deptTreeSelect,
} from "@/api/system/system/user.js";
import { computed, nextTick, h } from "vue";
import {
  CloudUploadOutlined,
  DownloadOutlined,
  DownOutlined,
  FileTextOutlined,
  FolderOpenOutlined,
  FolderOutlined,
  LeftOutlined,
  PlusOutlined,
  RightOutlined,
  SearchOutlined,
  UploadOutlined,
} from "@ant-design/icons-vue";
import { normalizePage, pageRows } from "@/utils/page.js";
const router = useRouter();
const { proxy } = getCurrentInstance();
const { sys_normal_disable, sys_user_sex } = proxy.useDict(
  "sys_normal_disable",
  "sys_user_sex"
);
import store from "@/store";
import useUserStore from "@/store/system/user";
const userStore = useUserStore();
const userId = ref(userStore.id);
// 计算属性动态设置 phonenumber 规则
const phonenumberRules = computed(() => {
  const rules = [
    {
      pattern: /^1[3-9]\d{9}$/,
      message: "请输入正确的手机号码",
      trigger: "blur",
    },
  ];
  if (userId.value != 1) {
    rules.unshift({
      required: true,
      message: "联系人手机号不能为空",
      trigger: "blur",
    });
  }

  return rules;
});

// 表单校验规则
const rules = computed(() => ({
  userName: [
    { required: true, message: "用户名称不能为空", trigger: "blur" },
    {
      min: 2,
      max: 20,
      message: "用户名称长度必须介于 2 和 20 之间",
      trigger: "blur",
    },
  ],
  nickName: [{ required: true, message: "用户昵称不能为空", trigger: "blur" }],
  roleIds: [{ required: true, message: "角色不能为空", trigger: "change" }],
  deptId: [{ required: true, message: "归属部门不能为空", trigger: "change" }],
  password: [
    { required: true, message: "用户密码不能为空", trigger: "blur" },
    {
      min: 8,
      max: 20,
      message: "用户密码长度必须介于 8 和 20 之间",
      trigger: "blur",
    },
    {
      pattern: /^[^<>"'|\\]+$/,
      message: "不能包含非法字符：< > \" ' \\ |",
      trigger: "blur",
    },
    {
      validator: (rule, value, callback) => {
        const strengthRegex = {
          minLength: /^.{8,}$/,
          upperCase: /[A-Z]/,
          lowerCase: /[a-z]/,
          number: /\d/,
          specialChar: /[!@#$%^&*(),.?":{}|<>]/,
        };

        if (!strengthRegex.minLength.test(value)) {
          callback(new Error("密码长度必须至少 8 个字符"));
        } else if (!strengthRegex.upperCase.test(value)) {
          callback(new Error("密码必须包含至少一个大写字母"));
        } else if (!strengthRegex.lowerCase.test(value)) {
          callback(new Error("密码必须包含至少一个小写字母"));
        } else if (!strengthRegex.number.test(value)) {
          callback(new Error("密码必须包含至少一个数字"));
        } else if (!strengthRegex.specialChar.test(value)) {
          callback(new Error("密码必须包含至少一个特殊字符"));
        } else {
          callback();
        }
      },
      trigger: "blur",
    },
  ],
  email: [
    {
      type: "email",
      message: "请输入正确的邮箱地址",
      trigger: ["blur", "change"],
    },
  ],
  phonenumber: phonenumberRules.value,
}));
const userList = ref([]);
const open = ref(false);
const loading = ref(true);
const showSearch = ref(true);
const ids = ref([]);
const single = ref(true);
const multiple = ref(true);
const total = ref(0);
const title = ref("");
const dateRange = ref([]);
const deptName = ref("");
const deptOptions = ref(undefined);
const initPassword = ref(undefined);
const postOptions = ref([]);
const roleOptions = ref([]);

const leftWidth = ref(300); // 初始左侧宽度
const isResizing = ref(false); // 判断是否正在拖拽
let startX = 0; // 鼠标按下时的初始位置

// 数据权限等级选项
const dataPermissionLevelOptions = [
  { value: 1, label: '绝密' },
  { value: 2, label: '机密' },
  { value: 3, label: '秘密' },
  { value: 4, label: '内部' },
  { value: 5, label: '公开' },
];

function dataPermissionLevelLabel(level) {
  const numLevel = level === null || level === undefined ? null : Number(level);
  const opt = dataPermissionLevelOptions.find(item => item.value === numLevel);
  return opt ? opt.label : '未知';
}

const startResize = (event) => {
  isResizing.value = true;
  startX = event.clientX;
  // 使用 requestAnimationFrame 减少重绘频率
  document.addEventListener("mousemove", updateResize);
  document.addEventListener("mouseup", stopResize);
};

const updateResize = (event) => {
  if (isResizing.value) {
    const delta = event.clientX - startX; // 计算鼠标移动距离
    leftWidth.value += delta; // 修改左侧宽度
    startX = event.clientX; // 更新起始位置
    // 使用 requestAnimationFrame 来减少页面重绘频率
    requestAnimationFrame(() => { });
  }
};

const stopResize = () => {
  isResizing.value = false;
  document.removeEventListener("mousemove", updateResize);
  document.removeEventListener("mouseup", stopResize);
};
// 折叠展开
const toggleCollapse = () => {
  if (leftWidth.value === 0) {
    leftWidth.value = 300;
  } else {
    leftWidth.value = 0;
  }
  emit("update:leftWidth", leftWidth.value);
};
/*** 用户导入参数 */
const upload = reactive({
  // 是否显示弹出层（用户导入）
  open: false,
  // 弹出层标题（用户导入）
  title: "",
  // 是否禁用上传
  isUploading: false,
  // 是否更新已经存在的用户数据
  updateSupport: 0,
  // 设置上传的请求头部
  headers: { Authorization: "Bearer " + getToken() },
  // 上传的地址
  url: import.meta.env.VITE_APP_BASE_API + "/system/user/importData",
});
// 列显隐信息
const columns = ref([
  { key: 0, label: `用户编号`, visible: true },
  { key: 1, label: `用户名称`, visible: true },
  { key: 2, label: `用户名称`, visible: true },
  { key: 3, label: `部门`, visible: true },
  { key: 4, label: `手机号码`, visible: true },
  { key: 5, label: `状态`, visible: true },
  { key: 6, label: `创建时间`, visible: true },
]);

const selectedRowKeys = ref([]);
const tableColumns = computed(() => {
  const allCols = [
    { title: '用户编号', dataIndex: 'userId', align: 'center', colKey: 0 },
    { title: '用户名称', dataIndex: 'userName', align: 'center', ellipsis: true, colKey: 1 },
    { title: '用户昵称', dataIndex: 'nickName', align: 'center', ellipsis: true, colKey: 2 },
    { title: '部门', dataIndex: 'deptName', align: 'center', ellipsis: true, colKey: 3 },
    { title: '手机号码', dataIndex: 'phonenumber', align: 'center', width: 120, colKey: 4 },
    { title: '状态', dataIndex: 'status', align: 'center', colKey: 5 },
    { title: '创建时间', dataIndex: 'createTime', align: 'center', width: 160, colKey: 6 },
    { title: '数据权限', dataIndex: 'dataPermissionLevel', align: 'center' },
    { title: '操作', key: 'actions', align: 'center', fixed: 'right', width: 240 },
  ];
  return allCols.filter(col => col.colKey === undefined || columns.value[col.colKey]?.visible !== false);
});

const data = reactive({
  form: {},
  queryParams: {
    pageNum: 1,
    pageSize: 6,
    userName: undefined,
    phonenumber: undefined,
    status: undefined,
    deptId: undefined,
  },
});

const { queryParams, form } = toRefs(data);

/** 通过条件过滤节点  */
const filterNode = (value, data) => {
  if (!value) return true;
  return data.label.indexOf(value) !== -1;
};

/** 根据名称筛选部门树 */
watch(deptName, (val) => {
  proxy.$refs["deptTreeRef"].filter(val);
});

/** 查询部门下拉树结构 */
function getDeptTree() {
  deptTreeSelect().then((response) => {
    deptOptions.value = response.data;
  });
}

/** 查询用户列表 */
function getList() {
  loading.value = true;
  listUser(proxy.addDateRange(queryParams.value, dateRange.value)).then(
    (res) => {
      loading.value = false;
      const { rows, total: rowTotal } = normalizePage(res);
      total.value = rowTotal;
      userList.value = pageRows(rows, rowTotal, queryParams.value);
    }
  );
}

// // 自定义渲染内容的函数
// const renderContent = (h, { node }) => {
//    console.log(node.level,node.label,"===========node.level")
//   // 判断节点类型，选择不同的图标
// //   const icon = node.level === 1 ? 'el-icon-folder' : 'el-icon-document';
// //   return (
// //     <span>
// //     <i class={icon}></i>
// //       {node.label}
// //     </span>
// //   );
// };

/** 节点单击事件 */
function handleNodeClick(data) {
  queryParams.value.deptId = data.id;
  handleQuery();
}

function getNode(node) {
  console.log(node, "============node");
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
  queryParams.value.deptId = undefined;
  proxy.$refs.deptTreeRef.setCurrentKey(null);
  handleQuery();
}

/** 删除按钮操作 */
function handleDelete(row) {
  const userIds = row.userId || ids.value;
  proxy.$modal
    .confirm('是否确认删除用户编号为"' + userIds + '"的数据项？')
    .then(function () {
      return delUser(userIds);
    })
    .then(() => {
      getList();
      proxy.$modal.msgSuccess("删除成功");
    })
    .catch(() => { });
}

/** 导出按钮操作 */
function handleExport() {
  proxy.download(
    "system/user/export",
    {
      ...queryParams.value,
    },
    `user_${new Date().getTime()}.xlsx`
  );
}

/** 用户状态修改  */
function handleStatusChange(row) {
  let text = row.status === "0" ? "启用" : "停用";
  proxy.$modal
    .confirm('确认要"' + text + '""' + row.userName + '"用户吗?')
    .then(function () {
      return changeUserStatus(row.userId, row.status);
    })
    .then(() => {
      proxy.$modal.msgSuccess(text + "成功");
    })
    .catch(function () {
      row.status = row.status === "0" ? "1" : "0";
    });
}

/** 更多操作 */
function handleCommand(command, row) {
  switch (command) {
    case "handleResetPwd":
      handleResetPwd(row);
      break;
    case "handleAuthRole":
      handleAuthRole(row);
      break;
    default:
      break;
  }
}

/** 跳转角色分配 */
function handleAuthRole(row) {
  const userId = row.userId;
  router.push("/system/user-auth/role/" + userId);
}

/** 重置密码按钮操作 */
function handleResetPwd(row) {
  proxy
    .$prompt('请输入"' + row.userName + '"的新密码', "提示", {
      confirmButtonText: "确定",
      cancelButtonText: "取消",
      closeOnClickModal: false,
      inputType: 'password',
      inputPattern: /^.{8,20}$/, // 密码长度要求在 8 到 20 之间
      inputErrorMessage: "用户密码长度必须介于 8 和 20 之间",
      inputValidator: (value) => {
        // 校验密码包含的非法字符
        if (/<|>|"|'|\||\\/.test(value)) {
          return "不能包含非法字符：< > \" ' \\ |";
        }
        // 校验密码强度
        const strengthRegex = {
          upperCase: /[A-Z]/, // 至少一个大写字母
          lowerCase: /[a-z]/, // 至少一个小写字母
          number: /\d/, // 至少一个数字
          specialChar: /[!@#$%^&*(),.?":{}|<>]/, // 至少一个特殊字符
        };

        if (!strengthRegex.upperCase.test(value)) {
          return "密码必须包含至少一个大写字母";
        }
        if (!strengthRegex.lowerCase.test(value)) {
          return "密码必须包含至少一个小写字母";
        }
        if (!strengthRegex.number.test(value)) {
          return "密码必须包含至少一个数字";
        }
        if (!strengthRegex.specialChar.test(value)) {
          return "密码必须包含至少一个特殊字符";
        }
      },
    })
    .then(({ value }) => {
      resetUserPwd(row.userId, value).then((response) => {
        proxy.$modal.msgSuccess("修改成功，新密码是：" + value);
      });
    })
    .catch(() => {
      // 处理取消操作
    });
}

/** 选择条数  */
function handleSelectionChange(selectedKeys, selectedRows) {
  selectedRowKeys.value = selectedKeys;
  ids.value = selectedRows.map((item) => item.userId);
  single.value = selectedRows.length != 1;
  multiple.value = !selectedRows.length;
}

/** 导入按钮操作 */
function handleImport() {
  upload.title = "用户导入";
  upload.open = true;
}

/** 下载模板操作 */
function importTemplate() {
  proxy.download(
    "system/user/importTemplate",
    {},
    `user_template_${new Date().getTime()}.xlsx`
  );
}

/**文件上传中处理 */
const handleFileUploadProgress = (event, file, fileList) => {
  upload.isUploading = true;
};

/** 文件上传成功处理 */
const handleFileSuccess = (response, file, fileList) => {
  upload.open = false;
  upload.isUploading = false;
  proxy.$refs["uploadRef"].handleRemove(file);
  proxy.$alert(
    "<div style='overflow: auto;overflow-x: hidden;max-height: 70vh;padding: 10px 20px 0;'>" +
    response.msg +
    "</div>",
    "导入结果",
    { dangerouslyUseHTMLString: true }
  );
  getList();
};

/** 提交上传文件 */
function submitFileForm() {
  proxy.$refs["uploadRef"].submit();
}

/** 重置操作表单 */
function reset() {
  form.value = {
    userId: undefined,
    deptId: undefined,
    userName: undefined,
    nickName: undefined,
    password: undefined,
    phonenumber: undefined,
    email: undefined,
    sex: undefined,
    status: "0",
    remark: undefined,
    postIds: [],
    roleIds: [],
    dataPermissionLevel: 5,
  };
  proxy.resetForm("userRef");
}

/** 取消按钮 */
function cancel() {
  open.value = false;
  reset();
}

/** 新增按钮操作 */
function handleAdd() {
  reset();
  getUser().then((response) => {
    postOptions.value = response.posts;
    roleOptions.value = response.roles;
    open.value = true;
    title.value = "新增用户";
    form.value.password = initPassword.value;
  });
}

/** 修改按钮操作 */
function handleUpdate(row) {
  const userId = row.userId || ids.value;
  getUser(userId).then((response) => {
    const userData = response.data;
    if (userData.dataPermissionLevel !== null && userData.dataPermissionLevel !== undefined) {
      userData.dataPermissionLevel = Number(userData.dataPermissionLevel);
    } else {
      userData.dataPermissionLevel = 5;
    }
    form.value = userData;
    postOptions.value = response.posts;
    roleOptions.value = response.roles;
    form.value.postIds = response.postIds;
    form.value.roleIds = response.roleIds;
    title.value = "修改用户";
    form.value.password = "";
    nextTick(() => {
      open.value = true;
    });
  });
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs["userRef"].validate((valid) => {
    if (valid) {
      if (form.value.userId != undefined) {
        updateUser(form.value).then((response) => {
          proxy.$modal.msgSuccess("修改成功");
          open.value = false;
          getList();
        });
      } else {
        addUser(form.value).then((response) => {
          proxy.$modal.msgSuccess("新增成功");
          open.value = false;
          getList();
        });
      }
    }
  });
}

getDeptTree();
getList();
</script>
<style scoped lang="scss">
::v-deep {
  .selectlist .el-tag.el-tag--info {
    background: #f3f8ff !important;
    border: 0px solid #6ba7ff !important;
    color: #2666fb !important;
  }
}

.left-pane {
  background: transparent;
  overflow: hidden;
  transition: width 0s;
  /* 可以根据需要调整过渡时间 */
}

.app-container {
  margin: 13px 15px;

  .pagecont-bottom {
    flex: 1;
    min-height: calc(100vh - 250px);
    padding: 14px 16px;
    overflow: hidden;
    background: #ffffff;
    border: 1px solid #e8edf5;
    border-radius: 8px;
    box-shadow: 0 8px 22px rgba(31, 45, 61, 0.05);
  }
}

.el-main {
  padding: 2px 0px;
  // box-shadow: 1px 1px 3px rgba(0, 0, 0, .2);
}

.el-aside {
  padding: 2px 0;
  margin-bottom: 0px;
  background: transparent;
}

.custom-tree-node {
  display: flex;
  align-items: center;
  width: 100%;
  min-width: 0;
  padding: 0 10px;
}

.treelable {
  flex: 1;
  min-width: 0;
  margin-left: 8px;
  overflow: hidden;
  color: #3f4a5a;
  font-size: 13px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.zjiconimg {
  font-size: 12px;
}

.colorxz {
  color: var(--el-color-primary);
}

.colorwxz {
  color: #8aaadc;
}

.iconimg {
  font-size: 15px;
}

//上传附件样式调整
::v-deep {

  // .el-upload-list{
  //    display: flex;
  // }
  .el-upload-list__item {
    width: 100%;
    height: 25px;
  }
}

.resize-bar {
  cursor: ew-resize;
  width: 12px;
  height: 86vh;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: transparent;
}

.resize-handle-sx {
  width: 12px;
  text-align: center;
  position: relative;
  /* 必须加，用来定位 collapse-icon */
}

.zjsx {
  display: none;
  width: 5px;
  height: 50px;
  border-left: 1px solid #ccc;
  border-right: 1px solid #ccc;
}

.collapse-icon {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  /* 真正的居中 */
  font-size: 18px;
  color: #7f8da3;
  cursor: pointer;
  z-index: 10;
  padding: 5px 2px;
  background: #ffffff;
  border: 1px solid #e5eaf2;
  border-radius: 999px;
  box-shadow: 0 4px 12px rgba(31, 45, 61, 0.08);

  &:hover {
    color: var(--el-color-primary);
    border-color: #c9dcff;
  }
}

:deep(.left-tree .el-input__wrapper) {
  min-height: 34px;
  border-radius: 6px;
  background: #f8fafc;
  box-shadow: 0 0 0 1px #e2e8f0 inset;
}

:deep(.left-tree .el-tree) {
  --el-tree-node-hover-bg-color: #f6faff;
  background: transparent;
  color: #3f4a5a;

  .el-tree-node__content {
    height: 34px;
    margin: 2px 0;
    border-radius: 6px;
    transition: background-color 0.16s ease, color 0.16s ease;
  }

  &.el-tree--highlight-current .el-tree-node.is-current > .el-tree-node__content {
    background: #eef5ff;

    .custom-tree-node .treelable {
      color: var(--el-color-primary);
    }
  }
}

.pagecont-top {
  display: flex !important;
  flex-wrap: nowrap !important;
  align-items: center !important;
  gap: 8px;

  .el-form {
    display: flex !important;
    flex-wrap: nowrap !important;
    flex: 0 1 auto !important;

    .el-form-item {
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
