<template>
    <div class="app-container" ref="app-container">
        <div class="pagecont-top" v-show="showSearch" style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 15px;">
            <a-form
                class="btn-style"
                :model="queryParams"
                ref="queryRef"
                layout="inline"
                :label-col="{ style: { width: '45px' } }"
                style="flex-shrink: 0;"
            >
                <a-form-item label="名称" name="roleName">
                    <a-input
                        v-model:value="queryParams.roleName"
                        placeholder="请输入角色名称"
                        allow-clear
                        style="width: 140px"
                        @pressEnter="handleQuery"
                    />
                </a-form-item>
                <a-form-item label="权限" name="roleKey">
                    <a-input
                        v-model:value="queryParams.roleKey"
                        placeholder="请输入权限字符"
                        allow-clear
                        style="width: 140px"
                        @pressEnter="handleQuery"
                    />
                </a-form-item>
                <a-form-item label="状态" name="status">
                    <a-select
                        v-model:value="queryParams.status"
                        placeholder="角色状态"
                        allow-clear
                        style="width: 140px"
                    >
                        <a-select-option
                            v-for="dict in sys_normal_disable"
                            :key="dict.value"
                            :value="dict.value">{{ dict.label }}</a-select-option>
                    </a-select>
                </a-form-item>
                <a-form-item label="类型" name="spaceId">
                    <a-select
                        v-model:value="queryParams.spaceId"
                        placeholder="角色类型"
                        allow-clear
                        style="width: 140px"
                    >
                        <a-select-option :value="0">系统角色</a-select-option>
                        <a-select-option :value="1">空间角色</a-select-option>
                    </a-select>
                </a-form-item>
                <a-form-item label="时间">
                    <a-range-picker
                        v-model:value="dateRange"
                        valueFormat="YYYY-MM-DD"
                        :placeholder="['开始', '结束']"
                        :separator="'-'"
                        style="width: 200px"
                    ></a-range-picker>
                </a-form-item>
                <a-form-item>
                    <a-button
                        type="primary"
                        @click="handleQuery"
                        @mousedown="(e) => e.preventDefault()"
                    >
                        <i class="iconfont-mini icon-a-zu22377 mr5"></i>查询
                    </a-button>
                    <a-button :icon="h(ReloadOutlined)" @click="resetQuery">重置</a-button>
                </a-form-item>
            </a-form>
            <div style="display: flex; gap: 10px; align-items: center;">
                <a-button type="primary" :icon="h(PlusOutlined)" @click="handleAdd" v-hasPermi="['system:role:add']">新增</a-button>
                <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
            </div>
        </div>
        <div>
            <!-- 表格数据 -->
            <a-table
                height="60vh"
                :loading="loading"
                :data-source="roleList"
                :row-key="'roleId'"
                :row-selection="rowSelection"
                :columns="columns"
            >
                <template #bodyCell="{ column, record }">
                    <template v-if="column.key === 'roleType'">
                        <a-tag>{{ isSystemRoleType(record.spaceId) ? '系统角色' : '空间角色' }}</a-tag>
                    </template>
                    <template v-else-if="column.key === 'status'">
                        <a-switch
                            v-model:checked="record.status"
                            checked-value="0"
                            un-checked-value="1"
                            @change="handleStatusChange(record)"
                        ></a-switch>
                    </template>
                    <template v-else-if="column.key === 'createTime'">
                        <span>{{ parseTime(record.createTime) }}</span>
                    </template>
                    <template v-else-if="column.key === 'action'">
                        <!-- <a-tooltip title="修改" placement="top" v-if="record.roleId !== 1">
                <a-button type="link" :icon="h(EditOutlined)" @click="handleUpdate(record)" v-hasPermi="['system:role:edit']"></a-button>
              </a-tooltip>
              <a-tooltip title="删除" placement="top" v-if="record.roleId !== 1">
                <a-button type="link" danger :icon="h(DeleteOutlined)" @click="handleDelete(record)" v-hasPermi="['system:role:remove']"></a-button>
              </a-tooltip> -->
                        <!-- <a-tooltip title="数据权限" placement="top" v-if="record.roleId !== 1">
                <a-button type="link" :icon="h(CheckCircleOutlined)" @click="handleDataScope(record)" v-hasPermi="['system:role:edit']"></a-button>
              </a-tooltip> -->
                        <!-- <a-tooltip title="分配用户" placement="top" v-if="record.roleId !== 1">
                <a-button type="link" :icon="h(UserOutlined)" @click="handleAuthUser(record)" v-hasPermi="['system:role:edit']"></a-button>
              </a-tooltip> -->
                        <a-button
                            type="link"
                            :icon="h(EditOutlined)"
                            @click="handleUpdate(record)"
                            v-hasPermi="['system:role:edit']"
                            v-if="record.roleId !== 1"
                            >修改</a-button
                        >
                        <a-button
                            type="link"
                            danger
                            :icon="h(DeleteOutlined)"
                            @click="handleDelete(record)"
                            v-hasPermi="['system:role:remove']"
                            v-if="record.roleId !== 1 && record.roleId !== 3"
                            >删除</a-button
                        >
                        <a-popover
                            placement="bottom"
                            :width="150"
                            trigger="click"
                            v-if="record.roleId !== 1"
                        >
                            <a-button type="link" :icon="h(EyeOutlined)">更多</a-button>
                            <template #content>
                                <div style="width: 90px" class="butgdlist">
                                    <a-button
                                        style="padding-left: 14px"
                                        type="link"
                                        :icon="h(CheckCircleOutlined)"
                                        @click="handleDataScope(record)"
                                        v-hasPermi="['system:role:edit']"
                                        >数据权限</a-button
                                    >
                                    <a-button
                                        type="link"
                                        :icon="h(UserOutlined)"
                                        @click="handleAuthUser(record)"
                                        v-hasPermi="['system:role:edit']"
                                        >分配用户</a-button
                                    >
                                </div>
                            </template>
                        </a-popover>
                    </template>
                </template>
            </a-table>

            <pagination
                v-show="total > 0"
                :total="total"
                v-model:page="data.queryParams.pageNum"
                v-model:limit="data.queryParams.pageSize"
                @pagination="getList"
            />
        </div>

        <!-- 添加或修改角色配置对话框 -->
        <a-modal
            :title="title"
            v-model:open="open"
            width="800px"
            destroy-on-close
        >
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
                                    <a-tooltip
                                        title="控制器中定义的权限字符，如：@PreAuthorize(`@ss.hasRole('admin')`)"
                                        placement="top"
                                    >
                                        <!-- <InfoCircleOutlined style="color: #909399" /> -->
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
                            <a-input-number
                                style="width: 100%"
                                v-model:value="form.roleSort"
                                :min="0"
                            />
                        </a-form-item>
                    </a-col>
                    <a-col :span="12">
                        <a-form-item label="状态">
                            <a-radio-group v-model:value="form.status">
                                <a-radio
                                    v-for="dict in sys_normal_disable"
                                    :key="dict.value"
                                    :value="dict.value"
                                    >{{ dict.label }}</a-radio
                                >
                            </a-radio-group>
                        </a-form-item>
                    </a-col>
                </a-row>
                <a-row :gutter="20">
                    <a-col :span="12">
                        <a-form-item label="角色类型" name="spaceId">
                            <a-select
                                v-model:value="form.spaceId"
                                placeholder="请选择角色类型"
                                class="el-form-input-width"
                            >
                                <a-select-option :value="0">系统角色</a-select-option>
                                <a-select-option :value="1">空间角色</a-select-option>
                            </a-select>
                        </a-form-item>
                    </a-col>
                </a-row>
                <a-form-item label="菜单权限">
                    <a-checkbox
                        v-model:checked="menuExpand"
                        @change="(e) => handleCheckedTreeExpand(e.target.checked, 'menu')"
                        >展开/折叠</a-checkbox
                    >
                    <a-checkbox
                        v-model:checked="menuNodeAll"
                        @change="(e) => handleCheckedTreeNodeAll(e.target.checked, 'menu')"
                        >全选/全不选</a-checkbox
                    >
                    <a-checkbox
                        v-model:checked="form.menuCheckStrictly"
                        @change="(e) => handleCheckedTreeConnect(e.target.checked, 'menu')"
                        >父子联动</a-checkbox
                    >
                    <a-tree
                        class="tree-border"
                        :tree-data="menuOptions"
                        checkable
                        ref="menuRef"
                        :field-names="{ title: 'label', key: 'id', children: 'children' }"
                        :check-strictly="!form.menuCheckStrictly"
                    ></a-tree>
                </a-form-item>
                <a-form-item label="备注">
                    <a-textarea
                        v-model:value="form.remark"
                        placeholder="请输入内容"
                    ></a-textarea>
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
                        <a-select-option
                            v-for="item in dataScopeOptions"
                            :key="item.value"
                            :value="item.value"
                        >{{ item.label }}</a-select-option>
                    </a-select>
                </a-form-item>
                <a-form-item label="数据权限" v-show="form.dataScope == 2">
                    <a-checkbox
                        v-model:checked="deptExpand"
                        @change="(e) => handleCheckedTreeExpand(e.target.checked, 'dept')"
                        >展开/折叠</a-checkbox
                    >
                    <a-checkbox
                        v-model:checked="deptNodeAll"
                        @change="(e) => handleCheckedTreeNodeAll(e.target.checked, 'dept')"
                        >全选/全不选</a-checkbox
                    >
                    <a-checkbox
                        v-model:checked="form.deptCheckStrictly"
                        @change="(e) => handleCheckedTreeConnect(e.target.checked, 'dept')"
                        >父子联动</a-checkbox
                    >
                    <a-tree
                        class="tree-border"
                        :tree-data="deptOptions"
                        checkable
                        default-expand-all
                        ref="deptRef"
                        :field-names="{ title: 'label', key: 'id', children: 'children' }"
                        :check-strictly="!form.deptCheckStrictly"
                    ></a-tree>
                </a-form-item>
            </a-form>
            <template #footer>
                <div class="dialog-footer">
                    <a-button type="primary" @click="submitDataScope">确 定</a-button>
                    <a-button @click="cancelDataScope">取 消</a-button>
                </div>
            </template>
        </a-modal>
    </div>
</template>

<script setup name="Role">
    import {
        addRole,
        changeRoleStatus,
        dataScope,
        delRole,
        getRole,
        listRole,
        updateRole,
        deptTreeSelect
    } from '@/api/system/system/role.js';
    import {
        roleMenuTreeselect,
        treeselect as menuTreeselect
    } from '@/api/system/system/menu.js';
    import { normalizePage, pageRows } from "@/utils/page.js";
    import { h } from 'vue';
    import { CheckCircleOutlined, DeleteOutlined, EditOutlined, EyeOutlined, InfoCircleOutlined, PlusOutlined, ReloadOutlined, UserOutlined } from "@ant-design/icons-vue";

    const router = useRouter();
    const { proxy } = getCurrentInstance();
    const { sys_normal_disable } = proxy.useDict('sys_normal_disable');

    const roleList = ref([]);
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
            spaceId: undefined,
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

    const columns = [
        { title: '角色编号', dataIndex: 'roleId', key: 'roleId', align: 'center' },
        { title: '角色名称', dataIndex: 'roleName', key: 'roleName', align: 'center', ellipsis: true },
        { title: '权限字符', dataIndex: 'roleKey', key: 'roleKey', align: 'center', ellipsis: true },
        { title: '角色类型', key: 'roleType', align: 'center' },
        { title: '显示顺序', dataIndex: 'roleSort', key: 'roleSort', align: 'center' },
        { title: '状态', key: 'status', align: 'center' },
        { title: '创建时间', dataIndex: 'createTime', key: 'createTime', width: 160, align: 'center' },
        { title: '操作', key: 'action', align: 'center', className: 'small-padding fixed-width', fixed: 'right', width: 240 }
    ];

    const rowSelection = {
        onChange: (selectedRowKeys, selectedRows) => {
            handleSelectionChange(selectedRows);
        }
    };

    function normalizeRoleType(spaceId) {
        return Number(spaceId) === 0 ? 0 : 1;
    }

    function isSystemRoleType(spaceId) {
        return normalizeRoleType(spaceId) === 0;
    }

    /** 查询角色列表 */
    function getList() {
        loading.value = true;
        const params = proxy.addDateRange(queryParams.value, dateRange.value);
        console.log('查询参数:', JSON.parse(JSON.stringify(params)));
        listRole(params).then((response) => {
            const page = normalizePage(response);
            total.value = page.total;
            roleList.value = pageRows(page.rows, page.total, queryParams.value);
            loading.value = false;
            console.log('返回结果：总数=' + page.total + ', 行数=' + page.rows.length);
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
            .catch(() => {});
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
    function handleSelectionChange(selection) {
        ids.value = selection.map((item) => item.roleId);
        single.value = selection.length != 1;
        multiple.value = !selection.length;
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
        });
    }

    /** 所有部门节点数据 */
    function getDeptAllCheckedKeys() {
        // 目前被选中的部门节点
        let checkedKeys = deptRef.value.getCheckedKeys();
        // 半选中的部门节点
        let halfCheckedKeys = deptRef.value.getHalfCheckedKeys();
        checkedKeys.unshift.apply(checkedKeys, halfCheckedKeys);
        return checkedKeys;
    }

    /** 重置新增的表单以及其他数据  */
    function reset() {
        if (menuRef.value != undefined) {
            menuRef.value.setCheckedKeys([]);
        }
        menuExpand.value = false;
        menuNodeAll.value = false;
        deptExpand.value = true;
        deptNodeAll.value = false;
        form.value = {
            roleId: undefined,
            spaceId: 0,
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

    /** 添加角色 */
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
            form.value.spaceId = normalizeRoleType(form.value.spaceId);
            form.value.roleSort = Number(form.value.roleSort);
            open.value = true;
            nextTick(() => {
                roleMenu.then((res) => {
                    let checkedKeys = res.checkedKeys;
                    checkedKeys.forEach((v) => {
                        nextTick(() => {
                            menuRef.value.setChecked(v, true, false);
                        });
                    });
                });
            });
            title.value = '修改角色';
        });
    }

    /** 根据角色ID查询菜单树结构 */
    function getRoleMenuTreeselect(roleId) {
        return roleMenuTreeselect(roleId).then((response) => {
            menuOptions.value = response.menus;
            return response;
        });
    }

    /** 根据角色ID查询部门树结构 */
    function getDeptTree(roleId) {
        return deptTreeSelect(roleId).then((response) => {
            deptOptions.value = response.depts;
            return response;
        });
    }

    /** 树权限（展开/折叠）*/
    function handleCheckedTreeExpand(value, type) {
        if (type == 'menu') {
            let treeList = menuOptions.value;
            for (let i = 0; i < treeList.length; i++) {
                menuRef.value.store.nodesMap[treeList[i].id].expanded = value;
            }
        } else if (type == 'dept') {
            let treeList = deptOptions.value;
            for (let i = 0; i < treeList.length; i++) {
                deptRef.value.store.nodesMap[treeList[i].id].expanded = value;
            }
        }
    }

    /** 树权限（全选/全不选） */
    function handleCheckedTreeNodeAll(value, type) {
        if (type == 'menu') {
            menuRef.value.setCheckedNodes(value ? menuOptions.value : []);
        } else if (type == 'dept') {
            deptRef.value.setCheckedNodes(value ? deptOptions.value : []);
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
        let checkedKeys = menuRef.value.getCheckedKeys();
        // 半选中的菜单节点
        let halfCheckedKeys = menuRef.value.getHalfCheckedKeys();
        checkedKeys.unshift.apply(checkedKeys, halfCheckedKeys);
        return checkedKeys;
    }

    /** 提交按钮 */
    function submitForm() {
        proxy.$refs['roleRef'].validate((valid) => {
            if (valid) {
                form.value.spaceId = normalizeRoleType(form.value.spaceId);
                if (form.value.roleId != undefined) {
                    form.value.menuIds = getMenuAllCheckedKeys();
                    updateRole(form.value).then((response) => {
                        proxy.$modal.msgSuccess('修改成功');
                        open.value = false;
                        getList();
                    });
                } else {
                    form.value.menuIds = getMenuAllCheckedKeys();
                    addRole(form.value).then((response) => {
                        proxy.$modal.msgSuccess('新增成功');
                        open.value = false;
                        getList();
                    });
                }
            }
        });
    }

    /** 取消按钮 */
    function cancel() {
        open.value = false;
        reset();
    }

    /** 选择角色权限范围触发 */
    function dataScopeSelectChange(value) {
        if (value !== '2') {
            deptRef.value.setCheckedKeys([]);
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
                    nextTick(() => {
                        if (deptRef.value) {
                            deptRef.value.setCheckedKeys(res.checkedKeys);
                        }
                    });
                });
            });
            title.value = '分配数据权限';
        });
    }

    /** 提交按钮（数据权限） */
    function submitDataScope() {
        if (form.value.roleId != undefined) {
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

    getList();
</script>

<style scoped lang="scss">
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