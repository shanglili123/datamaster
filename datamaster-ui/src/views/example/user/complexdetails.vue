<template>
    <div class="app-container" ref="app-container">
        <div class="pagecont-top" v-show="showSearch" style="padding-bottom: 15px">
            <div class="infotop">
                <div class="infotop-title mb15">小强</div>
                <a-row :gutter="20">
                    <a-col :span="8">
                        <div class="infotop-row border-top">
                            <div class="infotop-row-lable">学号</div>
                            <div class="infotop-row-value">{{ userform.xh }}</div>
                        </div>
                    </a-col>
                    <a-col :span="8">
                        <div class="infotop-row border-top">
                            <div class="infotop-row-lable">姓名</div>
                            <div class="infotop-row-value">{{ userform.name }}</div>
                        </div>
                    </a-col>
                    <a-col :span="8">
                        <div class="infotop-row border-top">
                            <div class="infotop-row-lable">性别</div>
                            <div class="infotop-row-value">{{ userform.sex }}</div>
                        </div>
                    </a-col>
                    <a-col :span="8">
                        <div class="infotop-row">
                            <div class="infotop-row-lable">年级</div>
                            <div class="infotop-row-value">
                                <a-tag color="blue">{{ userform.nj }}</a-tag>
                            </div>
                        </div>
                    </a-col>
                    <a-col :span="8">
                        <div class="infotop-row">
                            <div class="infotop-row-lable">班级名称</div>
                            <div class="infotop-row-value">{{ userform.bjmc }}</div>
                        </div>
                    </a-col>
                    <a-col :span="8">
                        <div class="infotop-row">
                            <div class="infotop-row-lable">出生日期</div>
                            <div class="infotop-row-value">{{ userform.csrq }}</div>
                        </div>
                    </a-col>
                    <a-col :span="8">
                        <div class="infotop-row">
                            <div class="infotop-row-lable">年龄</div>
                            <div class="infotop-row-value">{{ userform.eage }}</div>
                        </div>
                    </a-col>
                    <a-col :span="8">
                        <div class="infotop-row">
                            <div class="infotop-row-lable">备注</div>
                            <div class="infotop-row-value">{{ userform.bz }}</div>
                        </div>
                    </a-col>
                </a-row>

                <!-- <a-form-item>
          <a-button type="primary" @click="handleQuery" @mousedown="(e) => e.preventDefault()">
            <i class="iconfont-mini icon-a-zu22377 mr5"></i>查询
          </a-button>
          <a-button @click="resetQuery" @mousedown="(e) => e.preventDefault()">
            <i class="iconfont-mini icon-a-zu22378 mr5"></i>重置
          </a-button>
        </a-form-item> -->
            </div>
        </div>

        <div class="pagecont-bottom">
            <a-tabs v-model:activeKey="activeName" class="demo-tabs" @change="handleClick">
                <a-tab-pane :tab="'家长管理'" key="1">
                    <div class="justify-between mb15">
                        <a-row :gutter="15" class="btn-style">
                            <a-col :span="1.5">
                                <a-button
                                    type="primary"
                                    @click="handleAdd"
                                    v-hasPermi="['user:userType:add']"
                                    @mousedown="(e) => e.preventDefault()"
                                >
                                    <i class="iconfont-mini icon-xinzeng mr5"></i>新增
                                </a-button>
                            </a-col>
                            <a-col :span="1.5">
                                <a-button
                                    @click="handleExport"
                                    v-hasPermi="['user:userType:export']"
                                    @mousedown="(e) => e.preventDefault()"
                                >
                                    <i class="iconfont-mini icon-download-line mr5"></i>导出
                                </a-button>
                            </a-col>
                        </a-row>
                        <div class="justify-end top-right-btn">
                            <right-toolbar
                                v-model:showSearch="showSearch"
                                @queryTable="getList"
                                :columns="columns"
                            ></right-toolbar>
                        </div>
                    </div>
                    <a-table
                        striped
                        height="374px"
                        :loading="loading"
                        :data-source="userTypeList"
                        :columns="tableColumns"
                        row-key="id"
                        :row-selection="{ selectedRowKeys, onChange: handleSelectionChange }"
                        :locale="{ emptyText: '暂无记录' }"
                    >
                        <template #bodyCell="{ column, record }">
                            <template v-if="column.dataIndex === 'name'">
                                {{ record.name || '-' }}
                            </template>
                            <template v-else-if="column.dataIndex === 'validFlag'">
                                {{ record.validFlag || '-' }}
                            </template>
                            <template v-else-if="column.dataIndex === 'delFlag'">
                                {{ record.delFlag || '-' }}
                            </template>
                            <template v-else-if="column.dataIndex === 'createBy'">
                                {{ record.createBy || '-' }}
                            </template>
                            <template v-else-if="column.dataIndex === 'creatorId'">
                                {{ record.creatorId || '-' }}
                            </template>
                            <template v-else-if="column.dataIndex === 'createTime'">
                                <span>{{ parseTime(record.createTime, '{y}-{m}-{d}') }}</span>
                            </template>
                            <template v-else-if="column.dataIndex === 'updateBy'">
                                {{ record.updateBy || '-' }}
                            </template>
                            <template v-else-if="column.dataIndex === 'updaterId'">
                                {{ record.updaterId || '-' }}
                            </template>
                            <template v-else-if="column.dataIndex === 'updateTime'">
                                <span>{{ parseTime(record.updateTime, '{y}-{m}-{d}') }}</span>
                            </template>
                            <template v-else-if="column.dataIndex === 'remark'">
                                {{ record.remark || '-' }}
                            </template>
                            <template v-else-if="column.key === 'actions'">
                                <a-button
                                    type="link"
                                    :icon="h(EditOutlined)"
                                    @click="handleUpdate(record)"
                                    v-hasPermi="['user:userType:edit']"
                                >修改</a-button>
                                <a-button
                                    type="link"
                                    danger
                                    :icon="h(DeleteOutlined)"
                                    @click="handleDelete(record)"
                                    v-hasPermi="['user:userType:remove']"
                                >删除</a-button>
                            </template>
                        </template>
                    </a-table>

                    <pagination
                        v-show="total > 0"
                        :total="total"
                        v-model:page="queryParams.pageNum"
                        v-model:limit="queryParams.pageSize"
                        @pagination="getList"
                    />
                </a-tab-pane>
                <a-tab-pane :tab="'教师信息'" key="2">教师信息</a-tab-pane>
                <a-tab-pane :tab="'发展评价'" key="3">发展评价</a-tab-pane>
                <a-tab-pane :tab="'健康档案'" key="4">健康档案</a-tab-pane>
                <a-tab-pane :tab="'学期评估'" key="5">学期评估</a-tab-pane>
                <a-tab-pane :tab="'学期评价'" key="6">学期评价</a-tab-pane>
            </a-tabs>
        </div>

        <!-- 添加或修改用户类型对话框 -->
        <a-modal
            :title="title"
            v-model:open="open"
            width="800px"
            draggable
            destroy-on-close
        >
            <a-form ref="userTypeRef" :model="form" :rules="rules" :label-col="{ style: { width: '80px' } }">
                <a-row :gutter="20">
                    <a-col :span="12">
                        <a-form-item label="类型名称" name="name">
                            <a-input v-model:value="form.name" placeholder="请输入类型名称" />
                        </a-form-item>
                    </a-col>
                    <a-col :span="12">
                        <a-form-item label="是否有效" name="validFlag">
                            <a-input v-model:value="form.validFlag" placeholder="请输入是否有效" />
                        </a-form-item>
                    </a-col>
                </a-row>
                <a-row :gutter="20">
                    <a-col :span="12">
                        <a-form-item label="删除标志" name="delFlag">
                            <a-input v-model:value="form.delFlag" placeholder="请输入删除标志" />
                        </a-form-item>
                    </a-col>
                    <a-col :span="24">
                        <a-form-item label="备注" name="remark">
                            <a-textarea
                                v-model:value="form.remark"
                                placeholder="请输入内容"
                            />
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
    </div>
</template>

<script setup name="UserType">
    import {
        listUserType,
        getUserType,
        delUserType,
        addUserType,
        updateUserType
    } from '@/api/example/user/userType';
    import { h } from 'vue';
    import { EditOutlined, DeleteOutlined } from '@ant-design/icons-vue';

    const { proxy } = getCurrentInstance();

    const userTypeList = ref([]);

    const activeName = ref('1');

    const handleClick = (key) => {
        console.log(key);
    };

    // 列显隐信息
    const columns = ref([
        { key: 0, label: 'ID', visible: true },
        { key: 1, label: '类型名称', visible: true },
        { key: 2, label: '是否有效', visible: true },
        { key: 3, label: '删除标志', visible: true },
        { key: 4, label: '创建人', visible: true },
        { key: 5, label: '创建人id', visible: true },
        { key: 6, label: '创建时间', visible: true },
        { key: 7, label: '更新人', visible: true },
        { key: 8, label: '更新人id', visible: true },
        { key: 9, label: '更新时间', visible: true },
        { key: 10, label: '备注', visible: true }
    ]);

    const getColumnVisibility = (key) => {
        const column = columns.value.find((col) => col.key === key);
        // 如果没有找到对应列配置，默认显示
        if (!column) return true;
        // 如果找到对应列配置，根据visible属性来控制显示
        return column.visible;
    };

    const tableColumns = [
        { key: 'id', title: 'ID', dataIndex: 'id', align: 'center', hidden: !getColumnVisibility(0) },
        { key: 'name', title: '类型名称', dataIndex: 'name', align: 'center', hidden: !getColumnVisibility(1) },
        { key: 'validFlag', title: '是否有效', dataIndex: 'validFlag', align: 'center', hidden: !getColumnVisibility(2) },
        { key: 'delFlag', title: '删除标志', dataIndex: 'delFlag', align: 'center', hidden: !getColumnVisibility(3) },
        { key: 'createBy', title: '创建人', dataIndex: 'createBy', align: 'center', hidden: !getColumnVisibility(4) },
        { key: 'creatorId', title: '创建人id', dataIndex: 'creatorId', align: 'center', hidden: !getColumnVisibility(5) },
        {
            key: 'createTime',
            title: '创建时间',
            dataIndex: 'createTime',
            align: 'center',
            width: 180,
            sorter: true,
            sorterKey: 'createTime',
            defaultSortOrder: 'descend',
            hidden: !getColumnVisibility(6)
        },
        { key: 'updateBy', title: '更新人', dataIndex: 'updateBy', align: 'center', hidden: !getColumnVisibility(7) },
        { key: 'updaterId', title: '更新人id', dataIndex: 'updaterId', align: 'center', hidden: !getColumnVisibility(8) },
        {
            key: 'updateTime',
            title: '更新时间',
            dataIndex: 'updateTime',
            align: 'center',
            width: 180,
            sorter: true,
            sorterKey: 'updateTime',
            hidden: !getColumnVisibility(9)
        },
        { key: 'remark', title: '备注', dataIndex: 'remark', align: 'center', hidden: !getColumnVisibility(10) },
        {
            key: 'actions',
            title: '操作',
            align: 'center',
            className: 'small-padding fixed-width',
            fixed: 'right',
            width: 240,
            hidden: false
        }
    ];

    const open = ref(false);
    const loading = ref(true);
    const showSearch = ref(true);
    const ids = ref([]);
    const single = ref(true);
    const multiple = ref(true);
    const total = ref(0);
    const title = ref('');
    const selectedRowKeys = ref([]);

    const data = reactive({
        userform: {
            xh: '4564646',
            name: '小强',
            sex: '男',
            nj: '小班',
            bjmc: '火箭班',
            csrq: '2022-12-06',
            eage: '2岁',
            bz: ''
        },
        form: {},
        queryParams: {
            pageNum: 1,
            pageSize: 6,
            id: null,
            name: null,
            validFlag: null,
            delFlag: null,
            createBy: null,
            creatorId: null,
            createTime: null,
            updateBy: null,
            updaterId: null,
            updateTime: null,
            remark: null
        },
        rules: {}
    });

    const { queryParams, form, userform, rules } = toRefs(data);

    /** 查询用户类型列表 */
    function getList() {
        loading.value = true;
        // listUserType(queryParams.value).then(response => {
        let response = {
            data: {}
        };
        (response.data = {
            rows: [
                {
                    id: 1,
                    name: '测试模版',
                    content: '${test}测试模版',
                    category: 0,
                    msgLevel: 0,
                    validFlag: true,
                    delFlag: false,
                    createBy: '',
                    creatorId: 1,
                    createTime: '2024-11-01',
                    updateBy: null,
                    updaterId: 1,
                    updateTime: '2024-11-08',
                    remark: null
                },
                {
                    id: 2,
                    name: '测试',
                    content: '2',
                    category: 0,
                    msgLevel: 0,
                    validFlag: true,
                    delFlag: false,
                    createBy: 'admin',
                    creatorId: 1,
                    createTime: '2024-11-20',
                    updateBy: null,
                    updaterId: null,
                    updateTime: '2024-11-20',
                    remark: null
                }
            ],
            total: 2
        }),
            (userTypeList.value = response.data.rows);
        total.value = response.data.total;
        loading.value = false;
        // });
    }

    // 取消按钮
    function cancel() {
        open.value = false;
        reset();
    }

    // 表单重置
    function reset() {
        form.value = {
            id: null,
            name: null,
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
        proxy.resetForm('userTypeRef');
    }

    /** 搜索按钮操作 */
    function handleQuery() {
        queryParams.value.pageNum = 1;
        getList();
    }

    /** 重置按钮操作 */
    function resetQuery() {
        proxy.resetForm('queryRef');
        handleQuery();
    }

    // 多选框选中数据
    function handleSelectionChange(selectedKeys, selectedRows) {
        selectedRowKeys.value = selectedKeys;
        ids.value = selectedRows.map((item) => item.id);
        single.value = selectedRows.length != 1;
        multiple.value = !selectedRows.length;
    }

    /** 新增按钮操作 */
    function handleAdd() {
        reset();
        open.value = true;
        title.value = '新增用户类型';
    }

    /** 修改按钮操作 */
    function handleUpdate(row) {
        reset();
        const _id = row.id || ids.value;
        getUserType(_id).then((response) => {
            form.value = response.data;
            open.value = true;
            title.value = '修改用户类型';
        });
    }

    /** 提交按钮 */
    function submitForm() {
        proxy.$refs['userTypeRef'].validate().then(() => {
            if (form.value.id != null) {
                updateUserType(form.value).then((response) => {
                    proxy.$modal.msgSuccess('修改成功');
                    open.value = false;
                    getList();
                });
            } else {
                addUserType(form.value).then((response) => {
                    proxy.$modal.msgSuccess('新增成功');
                    open.value = false;
                    getList();
                });
            }
        }).catch(() => {});
    }

    /** 删除按钮操作 */
    function handleDelete(row) {
        const _ids = row.id || ids.value;
        proxy.$modal
            .confirm('是否确认删除用户类型编号为"' + _ids + '"的数据项？')
            .then(function () {
                return delUserType(_ids);
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
            'example/userType/export',
            {
                ...queryParams.value
            },
            `userType_${new Date().getTime()}.xlsx`
        );
    }

    getList();
</script>

