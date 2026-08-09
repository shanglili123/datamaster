<template>
    <div class="pagecont-top" v-show="showSearch">
        <a-form class="btn-style" :model="queryParams" ref="queryRef" v-show="showSearch" layout="inline"
            :label-col="{ style: { width: '45px' } }">
            <a-form-item label="名称" name="roleName">
                <a-input v-model:value="queryParams.roleName" placeholder="请输入角色名称" allow-clear style="width: 130px;"
                    @pressEnter="handleQuery" />
            </a-form-item>
            <a-form-item label="权限" name="roleKey">
                <a-input v-model:value="queryParams.roleKey" placeholder="请输入权限字符" allow-clear style="width: 130px;"
                    @pressEnter="handleQuery" />
            </a-form-item>
            <a-form-item label="状态" name="status">
                <a-select v-model:value="queryParams.status" placeholder="角色状态" allow-clear style="width: 120px;">
                    <a-select-option v-for="dict in sys_normal_disable" :key="dict.value" :value="dict.value">
                        {{ dict.label }}
                    </a-select-option>
                </a-select>
            </a-form-item>
            <a-form-item label="时间">
                <a-range-picker style="width: 200px;" v-model:value="dateRange" valueFormat="YYYY-MM-DD"
                    :allow-clear="false" />
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
            <a-button type="primary" :icon="h(PlusOutlined)" @click="handleAdd"
                v-hasPermi="['col:space:role:add']">新增</a-button>
            <a-button danger :icon="h(DeleteOutlined)" :disabled="multiple" @click="handleDelete"
                v-hasPermi="['col:space:role:remove']">删除</a-button>
        </div>
        <div class="top-right-btn">
            <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
        </div>
    </div>

    <div>
        <!-- 表格数据 -->
        <a-table
            striped
            :loading="loading"
            :data-source="roleList"
            :columns="tableColumns"
            :pagination="false"
            row-key="roleId"
            :row-selection="{ selectedRowKeys, onChange: handleSelectionChange }"
        >
            <template #bodyCell="{ column, record }">
                <template v-if="column.dataIndex === 'createBy'">
                    {{ record.createBy || "-" }}
                </template>
                <template v-if="column.dataIndex === 'createTime'">
                    <span>{{ parseTime(record.createTime) }}</span>
                </template>
                <template v-if="column.dataIndex === 'status'">
                    <a-switch v-model:checked="record.status" checked-value="0" unchecked-value="1"
                        @change="handleStatusChange(record)"></a-switch>
                </template>
                <template v-if="column.key === 'actions'">
                    <a-button v-if="record.roleId !== 1 || record.builtInOrNot" type="link" size="small"
                        @click="handleUpdate(record)" v-hasPermi="['col:space:role:edit']">修改</a-button>
                    <a-button v-if="(record.roleId !== 1 && record.roleId !== 3) || record.builtInOrNot"
                        type="link" danger size="small" @click="handleDelete(record)"
                        v-hasPermi="['col:space:role:remove']">删除</a-button>
                </template>
            </template>
        </a-table>

        <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum"
            v-model:limit="queryParams.pageSize" @pagination="getList" />
    </div>

    <!-- 新增或修改角色配置对话框 -->
    <a-modal :title="title" v-model:open="open" width="800px" destroy-on-close>
        <a-form ref="roleRef" :model="form" :rules="rules" :label-col="{ style: { width: '100px' } }">
            <a-row :gutter="20">
                <a-col :span="12">
                    <a-form-item label="角色名称" name="roleName">
                        <a-input v-model:value="form.roleName" placeholder="请输入角色名称" />
                    </a-form-item>
                </a-col>
                <a-col :span="12">
                    <a-form-item name="roleKey">
                        <template #label>
                            <span>
                                <a-tooltip title="控制器中定义的权限字符，如：@PreAuthorize(`@ss.hasRole('admin')`)"
                                    placement="top">
                                    <InfoCircleOutlined style="color: #909399" />
                                </a-tooltip>
                                权限字符
                            </span>
                        </template>
                        <a-input v-model:value="form.roleKey" placeholder="请输入权限字符" />
                    </a-form-item>
                </a-col>
            </a-row>
            <a-row :gutter="20">
                <a-col :span="12">
                    <a-form-item label="角色顺序" name="roleSort">
                        <a-input-number style="width: 100%" v-model:value="form.roleSort" :min="0" />
                    </a-form-item>
                </a-col>
                <a-col :span="12">
                    <a-form-item label="状态">
                        <a-radio-group v-model:value="form.status">
                            <a-radio v-for="dict in sys_normal_disable" :key="dict.value" :value="dict.value">{{
                                dict.label
                                }}</a-radio>
                        </a-radio-group>
                    </a-form-item>
                </a-col>
            </a-row>
            <a-form-item label="菜单权限">
                <a-checkbox v-model:checked="menuExpand"
                    @change="(e) => handleCheckedTreeExpand(e.target.checked, 'menu')">展开/折叠</a-checkbox>
                <a-checkbox v-model:checked="menuNodeAll"
                    @change="(e) => handleCheckedTreeNodeAll(e.target.checked, 'menu')">全选/全不选</a-checkbox>
                <a-checkbox v-model:checked="form.menuCheckStrictly"
                    @change="(e) => handleCheckedTreeConnect(e.target.checked, 'menu')">父子联动</a-checkbox>
                <a-tree class="tree-border" :tree-data="menuOptions" checkable ref="menuRef"
                    :field-names="{ title: 'label', children: 'children', value: 'id' }"
                    :expanded-keys="menuExpandedKeys" :checked-keys="menuCheckedKeys"
                    :check-strictly="!form.menuCheckStrictly"
                    @expand="(keys) => (menuExpandedKeys = keys)"
                    @check="handleMenuTreeCheck" />
            </a-form-item>
            <a-form-item label="权限描述">
                <a-textarea v-model:value="form.remark" placeholder="请输入权限描述" />
            </a-form-item>
        </a-form>
        <template #footer>
            <div class="dialog-footer">
                <a-button @click="cancel">取 消</a-button>
                <a-button type="primary" @click="submitForm">确 定</a-button>
            </div>
        </template>
    </a-modal>

    <!-- 分配角色数据权限对话框 -->
    <a-modal :title="title" v-model:open="openDataScope" width="500px">
        <a-form :model="form" :label-col="{ style: { width: '80px' } }">
            <a-form-item label="角色名称">
                <a-input v-model:value="form.roleName" :disabled="true" />
            </a-form-item>
            <a-form-item label="权限字符">
                <a-input v-model:value="form.roleKey" :disabled="true" />
            </a-form-item>
            <a-form-item label="权限范围">
                <a-select v-model:value="form.dataScope" @change="dataScopeSelectChange">
                    <a-select-option v-for="item in dataScopeOptions" :key="item.value" :value="item.value">{{
                        item.label
                        }}</a-select-option>
                </a-select>
            </a-form-item>
            <a-form-item label="数据权限" v-show="form.dataScope == 2">
                <a-checkbox v-model:checked="deptExpand"
                    @change="(e) => handleCheckedTreeExpand(e.target.checked, 'dept')">展开/折叠</a-checkbox>
                <a-checkbox v-model:checked="deptNodeAll"
                    @change="(e) => handleCheckedTreeNodeAll(e.target.checked, 'dept')">全选/全不选</a-checkbox>
                <a-checkbox v-model:checked="form.deptCheckStrictly"
                    @change="(e) => handleCheckedTreeConnect(e.target.checked, 'dept')">父子联动</a-checkbox>
                <a-tree class="tree-border" :tree-data="deptOptions" checkable ref="deptRef"
                    :field-names="{ title: 'label', children: 'children', value: 'id' }"
                    :expanded-keys="deptExpandedKeys" :checked-keys="deptCheckedKeys"
                    :check-strictly="!form.deptCheckStrictly"
                    @expand="(keys) => (deptExpandedKeys = keys)"
                    @check="handleDeptTreeCheck" />
            </a-form-item>
        </a-form>
        <template #footer>
            <div class="dialog-footer">
                <a-button type="primary" @click="submitDataScope">确 定</a-button>
                <a-button @click="cancelDataScope">取 消</a-button>
            </div>
        </template>
    </a-modal>
</template>

<script setup name="Role">
import { h } from 'vue'
import {
    addRole,
    changeRoleStatus,
    dataScope,
    delRole,
    getRole,
    listRole,
    updateRole,
    deptTreeSelect
} from '@/api/tax/spaceUserRel/spaceUserRel.js';
import {
    roleMenuTreeselectDpp,
    treeselectDpp as menuTreeselect
} from '@/api/system/system/menu.js';
import useUserStore from '@/store/system/user';
import { normalizePage, pageRows } from "@/utils/page.js";
import { PlusOutlined, DeleteOutlined, InfoCircleOutlined } from '@ant-design/icons-vue';
const router = useRouter();
const { proxy } = getCurrentInstance();
const { sys_normal_disable } = proxy.useDict('sys_normal_disable');
const userStore = useUserStore();
const roleList = ref([]);
const selectedRowKeys = ref([]);
const tableColumns = [
    { title: '编号', dataIndex: 'roleId', align: 'center', width: 60 },
    { title: '角色名称', dataIndex: 'roleName', align: 'center', ellipsis: true },
    { title: '权限描述', dataIndex: 'remark', align: 'center', width: 250, ellipsis: true },
    { title: '权限字符', dataIndex: 'roleKey', align: 'center', ellipsis: true },
    { title: '显示顺序', dataIndex: 'roleSort', align: 'center' },
    { title: '创建人', dataIndex: 'createBy', align: 'center' },
    { title: '创建时间', dataIndex: 'createTime', align: 'center', width: 160 },
    { title: '状态', dataIndex: 'status', align: 'center' },
    { title: '操作', key: 'actions', align: 'center', fixed: 'right', width: 240 },
];
const open = ref(false);
const loading = ref(true);
const showSearch = ref(true);
const ids = ref([]);
const single = ref(true);
const multiple = ref(true);
const total = ref(0);
const title = ref('');
const dateRange = ref([]);
const menuOptions = ref([]);
const menuExpand = ref(false);
const menuNodeAll = ref(false);
const deptExpand = ref(true);
const deptNodeAll = ref(false);
const deptOptions = ref([]);
const openDataScope = ref(false);
const menuRef = ref(null);
const deptRef = ref(null);
const menuExpandedKeys = ref([]);
const menuCheckedKeys = ref([]);
const menuHalfCheckedKeys = ref([]);
const deptExpandedKeys = ref([]);
const deptCheckedKeys = ref([]);
const deptHalfCheckedKeys = ref([]);
/** 数据范围选项*/
const dataScopeOptions = ref([
    { value: '1', label: '全部数据权限' },
    { value: '2', label: '自定数据权限' },
    { value: '3', label: '本部门数据权限' },
    { value: '4', label: '本部门及以下数据权限' },
    { value: '5', label: '仅本人数据权限' }
]);

const data = reactive({
    form: {},
    queryParams: {
        pageNum: 1,
        pageSize: 6,
        spaceId: null,
        roleName: undefined,
        roleKey: undefined,
        status: undefined
    },
    rules: {
        roleName: [{ required: true, message: '角色名称不能为空', trigger: 'blur' }],
        roleKey: [{ required: true, message: '权限字符不能为空', trigger: 'blur' }],
        roleSort: [{ required: true, message: '角色顺序不能为空', trigger: 'blur' }]
    }
});

const { queryParams, form, rules } = toRefs(data);
// 监听 userStore 中的 spaceId 变化
watch(
    () => userStore.spaceId,
    (newValue, oldValue) => {
        if (newValue !== oldValue) {
            queryParams.value.spaceId = newValue;
            getList();
        }
    },
    { immediate: true }
);
/** 查询角色列表 */
function getList() {
    loading.value = true;
    if (queryParams.value.spaceId) {
        listRole(proxy.addDateRange(queryParams.value, dateRange.value)).then((response) => {
            const page = normalizePage(response);
            total.value = page.total;
            roleList.value = pageRows(page.rows, page.total, queryParams.value);
            loading.value = false;
        });
    }
}

/** 搜索按钮操作 */
function handleQuery() {
    queryParams.value.pageNum = 1;
    getList();
}

/** 重置按钮操作 */
function resetQuery() {
    dateRange.value = [];
    proxy.resetForm('queryRef');
    handleQuery();
}

/** 删除按钮操作 */
function handleDelete(row) {
    const roleIds = row.roleId || ids.value;
    proxy.$modal
        .confirm('是否确认删除角色编号为"' + roleIds + '"的数据项?')
        .then(function () {
            return delRole(roleIds);
        })
        .then(() => {
            getList();
            proxy.$modal.msgSuccess('删除成功');
        })
        .catch(() => { });
}

/** 导出按钮操作 */
function handleExport() {
    proxy.download(
        'system/role/export',
        {
            ...queryParams.value
        },
        `role_${new Date().getTime()}.xlsx`
    );
}

/** 多选框选中数据 */
function handleSelectionChange(selectedKeys, selectedRows) {
    selectedRowKeys.value = selectedKeys;
    ids.value = selectedRows.map((item) => item.roleId);
    single.value = selectedRows.length != 1;
    multiple.value = !selectedRows.length;
}

/** 角色状态修改 */
function handleStatusChange(row) {
    let text = row.status === '0' ? '启用' : '停用';
    proxy.$modal
        .confirm('确认要"' + text + '""' + row.roleName + '"角色吗?')
        .then(function () {
            return changeRoleStatus(row.roleId, row.status);
        })
        .then(() => {
            proxy.$modal.msgSuccess(text + '成功');
        })
        .catch(function () {
            row.status = row.status === '0' ? '1' : '0';
        });
}

/** 更多操作 */
function handleCommand(command, row) {
    switch (command) {
        case 'handleDataScope':
            handleDataScope(row);
            break;
        case 'handleAuthUser':
            handleAuthUser(row);
            break;
        default:
            break;
    }
}

/** 分配用户 */
function handleAuthUser(row) {
    router.push('/system/role-auth/user/' + row.roleId);
}

/** 查询菜单树结构 */
function getMenuTreeselect() {
    menuTreeselect().then((response) => {
        menuOptions.value = response.data;
        menuExpandedKeys.value = getAllNodeKeys(response.data);
    });
}

/** 所有部门节点数据 */
function getDeptAllCheckedKeys() {
    // 目前被选中的部门节点
    let checkedKeys = deptCheckedKeys.value.slice();
    // 半选中的部门节点
    let halfCheckedKeys = deptHalfCheckedKeys.value;
    checkedKeys.unshift.apply(checkedKeys, halfCheckedKeys);
    return checkedKeys;
}

/** 重置新增的表单以及其他数据  */
function reset() {
    menuCheckedKeys.value = [];
    menuHalfCheckedKeys.value = [];
    deptCheckedKeys.value = [];
    deptHalfCheckedKeys.value = [];
    menuExpand.value = false;
    menuNodeAll.value = false;
    deptExpand.value = true;
    deptNodeAll.value = false;
    form.value = {
        roleId: undefined,
        spaceId: undefined,
        roleName: undefined,
        roleKey: undefined,
        roleSort: 0,
        status: '0',
        menuIds: [],
        deptIds: [],
        menuCheckStrictly: true,
        deptCheckStrictly: true,
        remark: undefined
    };
    proxy.resetForm('roleRef');
}

/** 新增角色 */
function handleAdd() {
    reset();
    getMenuTreeselect();
    open.value = true;
    title.value = '新增角色';
}

/** 修改角色 */
function handleUpdate(row) {
    reset();
    const roleId = row.roleId || ids.value;
    const roleMenu = getRoleMenuTreeselect(roleId);
    getRole(roleId).then((response) => {
        form.value = response.data;
        form.value.roleSort = Number(form.value.roleSort);
        open.value = true;
        nextTick(() => {
            roleMenu.then((res) => {
                menuCheckedKeys.value = res.checkedKeys || [];
                menuHalfCheckedKeys.value = [];
            });
        });
        title.value = '修改角色';
    });
}

/** 根据角色ID查询菜单树结构 */
function getRoleMenuTreeselect(roleId) {
    return roleMenuTreeselectDpp(roleId).then((response) => {
        menuOptions.value = response.menus;
        menuExpandedKeys.value = getAllNodeKeys(response.menus);
        return response;
    });
}

/** 根据角色ID查询部门树结构 */
function getDeptTree(roleId) {
    return deptTreeSelect(roleId).then((response) => {
        deptOptions.value = response.depts;
        deptExpandedKeys.value = getAllNodeKeys(response.depts);
        return response;
    });
}

/** 递归收集树节点 id */
function getAllNodeKeys(treeData) {
    let keys = [];
    treeData.forEach((node) => {
        keys.push(node.id);
        if (node.children && node.children.length) {
            keys = keys.concat(getAllNodeKeys(node.children));
        }
    });
    return keys;
}

/** 树权限（展开/折叠）*/
function handleCheckedTreeExpand(value, type) {
    if (type == 'menu') {
        menuExpandedKeys.value = value ? getAllNodeKeys(menuOptions.value) : [];
    } else if (type == 'dept') {
        deptExpandedKeys.value = value ? getAllNodeKeys(deptOptions.value) : [];
    }
}

/** 树权限（全选/全不选） */
function handleCheckedTreeNodeAll(value, type) {
    if (type == 'menu') {
        menuCheckedKeys.value = value ? getAllNodeKeys(menuOptions.value) : [];
        menuHalfCheckedKeys.value = [];
    } else if (type == 'dept') {
        deptCheckedKeys.value = value ? getAllNodeKeys(deptOptions.value) : [];
        deptHalfCheckedKeys.value = [];
    }
}

/** 树权限（父子联动） */
function handleCheckedTreeConnect(value, type) {
    if (type == 'menu') {
        form.value.menuCheckStrictly = value ? true : false;
    } else if (type == 'dept') {
        form.value.deptCheckStrictly = value ? true : false;
    }
}

/** 所有菜单节点数据 */
function getMenuAllCheckedKeys() {
    // 目前被选中的菜单节点
    let checkedKeys = menuCheckedKeys.value.slice();
    // 半选中的菜单节点
    let halfCheckedKeys = menuHalfCheckedKeys.value;
    checkedKeys.unshift.apply(checkedKeys, halfCheckedKeys);
    return checkedKeys;
}

/** 菜单树勾选事件 */
function handleMenuTreeCheck(checkedKeys, info) {
    menuCheckedKeys.value = checkedKeys;
    menuHalfCheckedKeys.value = info.halfCheckedKeys || [];
}

/** 部门树勾选事件 */
function handleDeptTreeCheck(checkedKeys, info) {
    deptCheckedKeys.value = checkedKeys;
    deptHalfCheckedKeys.value = info.halfCheckedKeys || [];
}

/** 提交按钮 */
function submitForm() {
    proxy.$refs['roleRef'].validate().then(() => {
        if (form.value.roleId != undefined) {
            form.value.spaceId = userStore.spaceId;
            form.value.menuIds = getMenuAllCheckedKeys();
            updateRole(form.value).then((response) => {
                proxy.$modal.msgSuccess('修改成功');
                open.value = false;
                getList();
            });
        } else {
            form.value.menuIds = getMenuAllCheckedKeys();
            form.value.spaceId = userStore.spaceId;
            addRole(form.value).then((response) => {
                proxy.$modal.msgSuccess('新增成功');
                open.value = false;
                getList();
            });
        }
    }).catch(() => { });
}

/** 取消按钮 */
function cancel() {
    open.value = false;
    reset();
}

/** 选择角色权限范围触发 */
function dataScopeSelectChange(value) {
    if (value !== '2') {
        deptCheckedKeys.value = [];
        deptHalfCheckedKeys.value = [];
    }
}

/** 分配数据权限操作 */
function handleDataScope(row) {
    reset();
    const deptTreeSelect = getDeptTree(row.roleId);
    getRole(row.roleId).then((response) => {
        form.value = response.data;
        openDataScope.value = true;
        nextTick(() => {
            deptTreeSelect.then((res) => {
                deptCheckedKeys.value = res.checkedKeys || [];
                deptHalfCheckedKeys.value = [];
            });
        });
        title.value = '分配数据权限';
    });
}

/** 提交按钮（数据权限） */
function submitDataScope() {
    if (form.value.roleId != undefined) {
        form.value.spaceId = userStore.spaceId;
        form.value.deptIds = getDeptAllCheckedKeys();
        dataScope(form.value).then((response) => {
            proxy.$modal.msgSuccess('修改成功');
            openDataScope.value = false;
            getList();
        });
    }
}

/** 取消按钮（数据权限）*/
function cancelDataScope() {
    openDataScope.value = false;
    reset();
}
</script>

<style scoped lang="scss">
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

