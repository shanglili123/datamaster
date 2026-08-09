<template>
    <div class="justify-between mb15">
        <a-row :gutter="10" class="btn-style">
            <a-col :span="1.5">
                <a-button type="primary" :icon="h(PlusOutlined)">新增</a-button>
            </a-col>
            <a-col :span="1.5">
                <a-button :icon="h(SwitcherOutlined)" @click="toggleExpandAll"
                    >展开/折叠</a-button
                >
            </a-col>
        </a-row>
        <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </div>

    <a-table
        height="60vh"
        v-if="refreshTable"
        :loading="loading"
        :data-source="detailsList"
        :columns="tableColumns"
        :pagination="false"
        row-key="id"
        :default-expand-all-rows="isExpandAll"
        :children-column-name="'children'"
        :locale="{ emptyText: '暂无数据' }"
    >
        <template #bodyCell="{ column, record }">
            <template v-if="column.dataIndex === 'createTime'">
                <span>{{ parseTime(record.createTime, '{y}-{m}-{d}') }}</span>
            </template>
            <template v-else-if="column.key === 'actions'">
                <a-button type="link" :icon="h(EditOutlined)">修改</a-button>
                <a-button type="link" :icon="h(PlusOutlined)">新增</a-button>
                <a-button type="link" danger :icon="h(DeleteOutlined)">删除</a-button>
            </template>
            <template v-else>
                <span>{{ record[column.dataIndex] || '-' }}</span>
            </template>
        </template>
    </a-table>
    <!-- 添加或修改详情对话框 -->
</template>

<script setup name="ComponentTwo">
    import { h } from 'vue';
    import { PlusOutlined, SwitcherOutlined, EditOutlined, DeleteOutlined } from '@ant-design/icons-vue';

    const { proxy } = getCurrentInstance();

    const tableColumns = [
        { title: 'ID', dataIndex: 'id', align: 'center' },
        { title: '标题', dataIndex: 'title', align: 'left' },
        { title: '内容', dataIndex: 'content', align: 'center' },
        { title: '创建人', dataIndex: 'createBy', align: 'center' },
        { title: '创建时间', dataIndex: 'createTime', align: 'center', width: 180 },
        { title: '备注', dataIndex: 'remark', align: 'center' },
        { title: '操作', key: 'actions', align: 'center', className: 'small-padding fixed-width', width: 200 },
    ];

    const detailsList = ref([]);
    const open = ref(false);
    const loading = ref(true);
    const showSearch = ref(true);
    const title = ref('');
    const isExpandAll = ref(true);
    const refreshTable = ref(true);

    const props = defineProps(['bidId']);

    const data = reactive({
        form: {},
        queryParams: {
            parentId: null,
            bidId: null,
            title: null,
            content: null,
            createTime: null
        },
        rules: {
            parentId: [{ required: true, message: '节点不能为空', trigger: 'blur' }],
            title: [{ required: true, message: '标题不能为空', trigger: 'blur' }],
            validFlag: [{ required: true, message: '是否有效不能为空', trigger: 'blur' }],
            delFlag: [{ required: true, message: '删除标志不能为空', trigger: 'blur' }],
            createTime: [{ required: true, message: '创建时间不能为空', trigger: 'blur' }],
            updateTime: [{ required: true, message: '更新时间不能为空', trigger: 'blur' }]
        }
    });

    const { queryParams, form, rules } = toRefs(data);

    /** 查询详情列表 */
    function getList() {
        loading.value = true;
        let responseData = [
            {
                id: 1,
                parentId: 0,
                bidId: 13,
                title: '测试',
                content: '11',
                validFlag: true,
                delFlag: false,
                createBy: 'admin',
                creatorId: 1,
                createTime: '2024-12-16 12:08:41',
                updateBy: null,
                updaterId: null,
                updateTime: '2024-12-16 12:08:41',
                remark: '11'
            },
            {
                id: 2,
                parentId: 1,
                bidId: 13,
                title: '测试2',
                content: '2',
                validFlag: true,
                delFlag: false,
                createBy: 'admin',
                creatorId: 1,
                createTime: '2024-12-16 12:09:50',
                updateBy: null,
                updaterId: null,
                updateTime: '2024-12-16 12:09:50',
                remark: '2'
            }
        ];
        detailsList.value = proxy.handleTree(responseData, 'id', 'parentId');
        loading.value = false;
    }

    /** 查询详情下拉树结构 */
    function getTreeselect() {}

    // 取消按钮
    function cancel() {
        open.value = false;
        reset();
    }

    // 表单重置
    function reset() {
        form.value = {
            id: null,
            parentId: null,
            bidId: null,
            title: null,
            content: null,
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
        proxy.resetForm('bidDetailsRef');
    }

    /** 搜索按钮操作 */
    function handleQuery() {
        getList();
    }

    /** 重置按钮操作 */
    function resetQuery() {
        proxy.resetForm('queryRef');
        handleQuery();
    }

    /** 新增按钮操作 */
    function handleAdd(row) {
        reset();
        getTreeselect();
        if (row != null && row.id) {
            form.value.parentId = row.id;
        } else {
            form.value.parentId = 0;
        }
        open.value = true;
        title.value = '新增内容';
    }

    /** 展开/折叠操作 */
    function toggleExpandAll() {
        refreshTable.value = false;
        isExpandAll.value = !isExpandAll.value;
        nextTick(() => {
            refreshTable.value = true;
        });
    }

    /** 修改按钮操作 */
    async function handleUpdate(row) {
        reset();
        await getTreeselect();
        if (row != null) {
            form.value.parentId = row.parentId;
        }
        // (row.id).then(response => {
        //   form.value = response.data;
        //   open.value = true;
        //   title.value = "修改详情";
        // });
    }

    getList();
</script>

