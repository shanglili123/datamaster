<template>
    <div class="justify-between mb15">
        <a-row :gutter="15" class="btn-style">
            <a-col :span="1.5">
                <a-button
                    type="primary"
                    @click="handleAdd"
                    v-hasPermi="['genStudent:student:add']"
                    @mousedown="(e) => e.preventDefault()"
                >
                    <i class="iconfont-mini icon-xinzeng mr5"></i>新增
                </a-button>
            </a-col>
            <a-col :span="1.5">
                <a-button
                    @click="handleExport"
                    v-hasPermi="['genStudent:student:export']"
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
        :data-source="studentList"
        :columns="tableColumns"
        :pagination="false"
        row-key="id"
        :row-selection="{ selectedRowKeys, onChange: handleSelectionChange }"
        @change="handleTableChange"
        :locale="{ emptyText: emptyContent }"
    >
        <template #bodyCell="{ column, record }">
            <template v-if="column.dataIndex === 'pictureUrl'">
                <image-preview :src="record.pictureUrl" :width="50" :height="50" />
            </template>
            <template v-else-if="column.dataIndex === 'sex'">
                <dict-tag :options="sys_user_sex" :value="record.sex" />
            </template>
            <template v-else-if="column.dataIndex === 'hobby'">
                <dict-tag
                    :options="message_level"
                    :value="record.hobby ? record.hobby.split(',') : []"
                />
            </template>
            <template v-else-if="column.dataIndex === 'createTime'">
                <span>{{ parseTime(record.createTime, '{y}-{m}-{d}') }}</span>
            </template>
            <template v-else-if="column.key === 'actions'">
                <a-button
                    type="link"
                    :icon="h(EditOutlined)"
                    @click="handleUpdate(record)"
                    v-hasPermi="['genStudent:student:edit']"
                    >修改</a-button
                >
                <a-button
                    type="link"
                    danger
                    :icon="h(DeleteOutlined)"
                    @click="handleDelete(record)"
                    v-hasPermi="['genStudent:student:remove']"
                    >删除</a-button
                >
                <a-button
                    type="link"
                    :icon="h(EyeOutlined)"
                    @click="handleDetail(record)"
                    v-hasPermi="['genStudent:student:edit']"
                    >详情</a-button
                >
            </template>
            <template v-else>
                <span>{{ record[column.dataIndex] || '-' }}</span>
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

    <!-- 添加或修改学生对话框 -->
    <a-modal
        :title="title"
        v-model:open="open"
        width="800px"
        draggable
    >
        <a-form ref="studentRef" :model="form" :rules="rules" :label-col="{ style: { width: '80px' } }">
            <a-row :gutter="20">
                <a-col :span="12">
                    <a-form-item label="姓名" name="name">
                        <a-input v-model:value="form.name" placeholder="请输入姓名" />
                    </a-form-item>
                </a-col>
                <a-col :span="24">
                    <a-form-item label="学生照" name="pictureUrl">
                        <image-upload v-model="form.pictureUrl" />
                    </a-form-item>
                </a-col>
            </a-row>
            <a-row :gutter="20">
                <a-col :span="24">
                    <a-form-item label="教育经历">
                        <editor v-model="form.experience" :min-height="192" />
                    </a-form-item>
                </a-col>
                <a-col :span="12">
                    <a-form-item label="性别" name="sex">
                        <a-select v-model:value="form.sex" placeholder="请选择性别">
                            <a-select-option
                                v-for="dict in sys_user_sex"
                                :key="dict.value"
                                :value="parseInt(dict.value)"
                            >{{ dict.label }}</a-select-option>
                        </a-select>
                    </a-form-item>
                </a-col>
            </a-row>
            <a-row :gutter="20">
                <a-col :span="12">
                    <a-form-item label="年龄" name="age">
                        <a-input v-model:value="form.age" placeholder="请输入年龄" />
                    </a-form-item>
                </a-col>
                <a-col :span="12">
                    <a-form-item label="学号" name="studentNumber">
                        <a-input v-model:value="form.studentNumber" placeholder="请输入学号" />
                    </a-form-item>
                </a-col>
            </a-row>
            <a-row :gutter="20">
                <a-col :span="12">
                    <a-form-item label="班级" name="grade">
                        <a-input v-model:value="form.grade" placeholder="请输入班级" />
                    </a-form-item>
                </a-col>
                <a-col :span="12">
                    <a-form-item label="爱好" name="hobby">
                        <a-checkbox-group v-model:value="form.hobby">
                            <a-checkbox
                                v-for="dict in message_level"
                                :key="dict.value"
                                :value="dict.value"
                            >
                                {{ dict.label }}
                            </a-checkbox>
                        </a-checkbox-group>
                    </a-form-item>
                </a-col>
            </a-row>
            <a-row :gutter="20">
                <a-col :span="24">
                    <a-form-item label="备注" name="remark">
                        <a-input v-model:value="form.remark" type="textarea" placeholder="请输入内容" />
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

    <!-- 学生详情对话框 -->
    <a-modal
        :title="title"
        v-model:open="openDetail"
        width="800px"
        draggable
    >
        <a-form ref="studentRef" :model="form" :label-col="{ style: { width: '80px' } }">
            <a-row :gutter="20">
                <a-col :span="12">
                    <a-form-item label="姓名" name="name">
                        <div>
                            {{ form.name }}
                        </div>
                    </a-form-item>
                </a-col>
                <a-col :span="12">
                    <a-form-item label="学生照" name="pictureUrl">
                        <image-preview :src="form.pictureUrl" :width="50" :height="50" />
                    </a-form-item>
                </a-col>
            </a-row>
            <a-row :gutter="20">
                <a-col :span="12">
                    <a-form-item label="教育经历">
                        <div v-html="form.experience"></div>
                    </a-form-item>
                </a-col>
                <a-col :span="12">
                    <a-form-item label="性别" name="sex">
                        <dict-tag :options="sys_user_sex" :value="form.sex" />
                    </a-form-item>
                </a-col>
            </a-row>
            <a-row :gutter="20">
                <a-col :span="12">
                    <a-form-item label="年龄" name="age">
                        <div>
                            {{ form.age }}
                        </div>
                    </a-form-item>
                </a-col>
                <a-col :span="12">
                    <a-form-item label="学号" name="studentNumber">
                        <div>
                            {{ form.studentNumber }}
                        </div>
                    </a-form-item>
                </a-col>
            </a-row>
            <a-row :gutter="20">
                <a-col :span="12">
                    <a-form-item label="班级" name="grade">
                        <div>
                            {{ form.grade }}
                        </div>
                    </a-form-item>
                </a-col>
                <a-col :span="12">
                    <a-form-item label="爱好" name="hobby">
                        <dict-tag :options="message_level" :value="form.hobby" />
                    </a-form-item>
                </a-col>
            </a-row>
            <a-row :gutter="20">
                <a-col :span="24">
                    <a-form-item label="备注" name="remark">
                        <div>
                            {{ form.remark }}
                        </div>
                    </a-form-item>
                </a-col>
            </a-row>
        </a-form>
        <template #footer>
            <div class="dialog-footer">
                <a-button @click="cancel">关 闭</a-button>
            </div>
        </template>
    </a-modal>
</template>

<script setup name="ComponentOne">
    import {
        listStudent,
        getStudent,
        delStudent,
        addStudent,
        updateStudent
    } from '@/api/example/genStudent/student';
    import { h } from 'vue';
    import { EditOutlined, DeleteOutlined, EyeOutlined } from '@ant-design/icons-vue';

    const { proxy } = getCurrentInstance();
    const { sys_user_sex, message_level } = proxy.useDict('sys_user_sex', 'message_level');

    const studentList = ref([]);

    // 列显隐信息
    const columns = ref([
        { key: 0, label: 'ID', visible: true },
        { key: 1, label: '姓名', visible: true },
        { key: 2, label: '学生照', visible: true },
        { key: 3, label: '教育经历', visible: true },
        { key: 4, label: '性别', visible: true },
        { key: 5, label: '年龄', visible: true },
        { key: 6, label: '学号', visible: true },
        { key: 7, label: '班级', visible: true },
        { key: 8, label: '爱好', visible: true },
        { key: 9, label: '是否有效', visible: true },
        { key: 10, label: '删除标志', visible: true },
        { key: 11, label: '创建人', visible: true },
        { key: 12, label: '创建人id', visible: true },
        { key: 13, label: '创建时间', visible: true },
        { key: 14, label: '更新人', visible: true },
        { key: 15, label: '更新人id', visible: true },
        { key: 16, label: '更新时间', visible: true },
        { key: 17, label: '备注', visible: true }
    ]);

    const tableColumns = [
        { key: 'id', title: 'ID', dataIndex: 'id', align: 'center', hidden: !columns.value[0].visible },
        { key: 'name', title: '姓名', dataIndex: 'name', align: 'center', hidden: !columns.value[1].visible },
        { key: 'pictureUrl', title: '学生照', dataIndex: 'pictureUrl', align: 'center', width: 100, hidden: !columns.value[2].visible },
        { key: 'experience', title: '教育经历', dataIndex: 'experience', align: 'center', hidden: !columns.value[3].visible },
        { key: 'sex', title: '性别', dataIndex: 'sex', align: 'center', hidden: !columns.value[4].visible },
        { key: 'age', title: '年龄', dataIndex: 'age', align: 'center', hidden: !columns.value[5].visible },
        { key: 'studentNumber', title: '学号', dataIndex: 'studentNumber', align: 'center', hidden: !columns.value[6].visible },
        { key: 'grade', title: '班级', dataIndex: 'grade', align: 'center', hidden: !columns.value[7].visible },
        { key: 'hobby', title: '爱好', dataIndex: 'hobby', align: 'center', hidden: !columns.value[8].visible },
        { key: 'createBy', title: '创建人', dataIndex: 'createBy', align: 'center', hidden: !columns.value[11].visible },
        {
            key: 'createTime',
            title: '创建时间',
            dataIndex: 'createTime',
            align: 'center',
            width: 180,
            sorter: true,
            sorterKey: 'createTime',
            defaultSortOrder: 'descend',
            hidden: !columns.value[13].visible
        },
        { key: 'remark', title: '备注', dataIndex: 'remark', align: 'center', hidden: !columns.value[17].visible },
        {
            key: 'actions',
            title: '操作',
            align: 'center',
            className: 'small-padding fixed-width',
            fixed: 'right',
            width: 240
        }
    ];

    const emptyContent = h('div', { class: 'emptyBg' }, [
        h('img', { src: new URL('@/assets/system/images/no_data/noData.png', import.meta.url).href, alt: '' }),
        h('p', '暂无记录'),
    ]);

    const open = ref(false);
    const openDetail = ref(false);
    const loading = ref(true);
    const showSearch = ref(true);
    const ids = ref([]);
    const selectedRowKeys = ref([]);
    const single = ref(true);
    const multiple = ref(true);
    const total = ref(0);
    const title = ref('');
    const defaultSort = ref({ prop: 'createTime', order: 'desc' });

    const data = reactive({
        studentDetail: {},
        form: {},
        queryParams: {
            pageNum: 1,
            pageSize: 6,
            name: null,
            pictureUrl: null,
            experience: null,
            sex: null,
            age: null,
            studentNumber: null,
            grade: null,
            hobby: null,
            createTime: null
        },
        rules: {
            name: [{ required: true, message: '姓名不能为空', trigger: 'blur' }],
            age: [{ required: true, message: '年龄不能为空', trigger: 'blur' }],
            validFlag: [{ required: true, message: '是否有效不能为空', trigger: 'blur' }],
            delFlag: [{ required: true, message: '删除标志不能为空', trigger: 'blur' }],
            createTime: [{ required: true, message: '创建时间不能为空', trigger: 'blur' }],
            updateTime: [{ required: true, message: '更新时间不能为空', trigger: 'blur' }]
        }
    });

    const { queryParams, form, studentDetail, rules } = toRefs(data);

    /** 查询学生列表 */
    function getList() {
        loading.value = true;
        listStudent(queryParams.value).then((response) => {
            studentList.value = response.data.rows;
            total.value = response.data.total;
            loading.value = false;
        });
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
            name: null,
            pictureUrl: null,
            experience: null,
            sex: null,
            age: null,
            studentNumber: null,
            grade: null,
            hobby: [],
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
        proxy.resetForm('studentRef');
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

    /** 排序触发事件 */
    function handleTableChange(pagination, filters, sorter) {
        if (!sorter || Array.isArray(sorter)) {
            return;
        }
        handleSortChange({
            prop: sorter.field,
            order: sorter.order === "ascend" ? "ascending" : sorter.order === "descend" ? "descending" : sorter.order
        });
    }

    function handleSortChange(column) {
        queryParams.value.orderByColumn = column.prop;
        queryParams.value.isAsc = column.order;
        getList();
    }

    /** 新增按钮操作 */
    function handleAdd() {
        reset();
        open.value = true;
        title.value = '新增学生';
    }

    /** 修改按钮操作 */
    function handleUpdate(row) {
        reset();
        const _id = row.id || ids.value;
        getStudent(_id).then((response) => {
            form.value = response.data;
            form.value.hobby = form.value.hobby.split(',');
            open.value = true;
            title.value = '修改学生';
        });
    }

    /** 详情按钮操作 */
    function handleDetail(row) {
        reset();
        const _id = row.id || ids.value;
        getStudent(_id).then((response) => {
            form.value = response.data;
            form.value.hobby = form.value.hobby.split(',');
            openDetail.value = true;
            title.value = '学生详情';
        });
    }

    /** 提交按钮 */
    function submitForm() {
        proxy.$refs['studentRef'].validate().then(() => {
            form.value.hobby = form.value.hobby.join(',');
            if (form.value.id != null) {
                updateStudent(form.value)
                    .then((response) => {
                        proxy.$modal.msgSuccess('修改成功');
                        open.value = false;
                        getList();
                    })
                    .catch((error) => {
                        form.value.hobby = form.value.hobby.split(',').map(String);
                    });
            } else {
                addStudent(form.value)
                    .then((response) => {
                        proxy.$modal.msgSuccess('新增成功');
                        open.value = false;
                        getList();
                    })
                    .catch((error) => {
                        form.value.hobby = form.value.hobby.split(',').map(String);
                    });
            }
        }).catch(() => {});
    }

    /** 删除按钮操作 */
    function handleDelete(row) {
        const _ids = row.id || ids.value;
        proxy.$modal
            .confirm('是否确认删除学生编号为"' + _ids + '"的数据项？')
            .then(function () {
                return delStudent(_ids);
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
            'example/student/export',
            {
                ...queryParams.value
            },
            `student_${new Date().getTime()}.xlsx`
        );
    }

    queryParams.value.orderByColumn = defaultSort.value.prop;
    queryParams.value.isAsc = defaultSort.value.order;
    getList();
</script>

