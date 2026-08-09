<template>
    <div class="app-container" ref="app-container">
    <!-- 使用公共页头组件 -->
    <PageHeader
      :queryParams="queryParams"
      :showSearch="showSearch"
      :showAddBtn="true"
      :addPermission="['tax:assetCat:add']"
      :showToggleBtn="true"
      :isExpandAll="isExpandAll"
      @query="handleQuery"
      @reset="resetQuery"
      @add="handleAdd"
      @toggle="toggleExpandAll"
      @queryTable="getList"
    >
      <!-- 核心：在 searchForm 插槽中填入当前页面特有的搜索项 -->
      <template #searchForm>
        <a-form-item label="数据资产目录名称" name="name">
          <a-input class="el-form-input-width" v-model:value="queryParams.name" placeholder="请输入数据资产目录名称" allow-clear
            @pressEnter="handleQuery"
/>
        </a-form-item>
        <a-form-item label="上级目录" name="code">
          <a-tree-select allow-clear show-search class="el-form-input-width" v-model:value="queryParams.code"
            :tree-data="attAssetCatOptions" :field-names="{ value: 'code', label: 'name', children: 'children' }"
            placeholder="请选择上级"
/>
        </a-form-item>
      </template>
    </PageHeader>

        <div class="pagecont-bottom">
            <a-table height="60vh" v-if="refreshTable" :loading="loading" :data-source="attAssetCatList" :row-key="'id'"
                :default-expand-all-rows="isExpandAll" :columns="columns"
>
                <template #bodyCell="{ column, record }">
                    <template v-if="column.key === 'validFlag'">
                        <a-switch v-model:checked="record.validFlag" @change="handleStatusChange(record)" />
                    </template>
                    <template v-else-if="column.key === 'action'">
                        <a-button type="link" @click="handleUpdate(record)" v-hasPermi="['tax:assetCat:edit']">修改</a-button>
                        <a-button type="link" @click="handleAdd(record)" v-hasPermi="['tax:assetCat:add']">新增</a-button>
                        <a-button type="link" danger @click="handleDelete(record)" v-hasPermi="['tax:assetCat:remove']">删除</a-button>
                    </template>
                </template>
            </a-table>
            <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum"
                v-model:limit="queryParams.pageSize" @pagination="getList"
/>
        </div>

        <!-- 新增或修改数据资产目录管理对话框 -->
        <a-modal :title="title" v-model:open="open" width="800px" destroy-on-close>
            <a-form ref="attAssetCatRef" :model="form" :rules="rules" :label-col="{ style: { width: '80px' } }">
                <a-row :gutter="20">
                    <a-col :span="12">
                        <a-form-item label="目录名称" name="name">
                            <a-input v-model:value="form.name" placeholder="请输入数据资产目录名称" />
                        </a-form-item>
                    </a-col>
                    <!--            <a-form-item label="类别排序" name="sortOrder">-->
                    <!--&lt;!&ndash;              <a-input v-model:value="form.sortOrder" placeholder="请输入类别排序" />&ndash;&gt;-->
                    <!--              <a-input-number v-model:value="form.sortOrder"  steps="1" :min="0"  placeholder="请输入类别排序" />-->
                    <!--            </a-form-item>-->
                    <a-col :span="12">
                        <a-form-item label="上级目录" name="parentId">
                            <a-tree-select allow-clear :disabled="form.id" v-model:value="form.parentId"
                                :tree-data="attAssetCatOptions" :field-names="{ value: 'id', label: 'name', children: 'children' }"
                                placeholder="请选择上级"
/>
                        </a-form-item>
                    </a-col>
                </a-row>
                <a-row :gutter="20">
                    <a-col :span="24">
                        <a-form-item label="描述">
                            <a-textarea placeholder="请输入描述" v-model:value="form.description"
                                :auto-size="{ minRows: 4, maxRows: 8 }"
/>
                        </a-form-item>
                    </a-col>
                </a-row>
                <a-row :gutter="20"> </a-row>
                <a-row :gutter="20">
                    <a-col :span="12">
                        <a-form-item label="排序" name="sortOrder">
                            <a-input-number style="width: 100%" v-model:value="form.sortOrder"
                                :min="0"
/>
                        </a-form-item>
                    </a-col>

                    <a-col :span="12">
                        <a-form-item label="状态" name="validFlag">
                            <a-radio-group v-model:value="form.validFlag">
                                <a-radio :value="true">启用</a-radio>
                                <a-radio :value="false">禁用</a-radio>
                            </a-radio-group>
                        </a-form-item>
                    </a-col>
                </a-row>

                <a-row :gutter="20">
                    <a-col :span="24">
                        <a-form-item label="备注">
                            <a-textarea placeholder="请输入备注" v-model:value="form.remark"
                                :auto-size="{ minRows: 4, maxRows: 8 }"
/>
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
    </div>
</template>

<script setup name="AssetCat">
import PageHeader from '@/components/Cat/PageHeader.vue';
import {
    listAttAssetCat,
    getAttAssetCat,
    delAttAssetCat,
    addAttAssetCat,
    updateAttAssetCat
} from '@/api/tax/cat/assetCat/assetCat.js';
import useUserStore from '@/store/system/user';
import { normalizePage, pageRows } from "@/utils/page.js";
import { parseTime } from "@/utils/anivia.js";
const userStore = useUserStore();
const { proxy } = getCurrentInstance();

const attAssetCatList = ref([]);
const attAssetCatOptions = ref([]);
const open = ref(false);
const loading = ref(true);
const showSearch = ref(true);
const title = ref('');
const isExpandAll = ref(false);
const total = ref(0);
const refreshTable = ref(true);

const columns = [
    { title: '数据资产目录名称', dataIndex: 'name', key: 'name', width: 200, ellipsis: true, customRender: ({ text }) => text || '-' },
    { title: '描述', dataIndex: 'description', key: 'description', width: 250, ellipsis: true, customRender: ({ text }) => text || '-' },
    { title: '排序', dataIndex: 'sortOrder', key: 'sortOrder', ellipsis: true },
    { title: '创建人', dataIndex: 'createBy', key: 'createBy', align: 'center', customRender: ({ text }) => text || '-' },
    { title: '创建时间', dataIndex: 'createTime', key: 'createTime', width: 180, align: 'center', customRender: ({ text }) => text ? parseTime(text, '{y}-{m}-{d} {h}:{i}') : '-' },
    { title: '状态', key: 'validFlag', align: 'center' },
    { title: '备注', dataIndex: 'remark', key: 'remark', ellipsis: true, customRender: ({ text }) => text || '-' },
    { title: '操作', key: 'action', align: 'center', fixed: 'right', width: 240 }
];

const data = reactive({
    form: {},
    queryParams: {
        pageNum: 1,
        pageSize: 6,
        name: null,
        parentId: null
    },
    rules: {
        name: [{ required: true, message: '数据资产目录名称不能为空', trigger: 'blur' }],
        parentId: [{ required: true, message: '上级目录不能为空', trigger: 'blur' }]
    }
});

const { queryParams, form, rules } = toRefs(data);

/** 查询数据资产目录管理列表 */
function getList() {
    loading.value = true;
    queryParams.value.spaceId = userStore.spaceId;
    queryParams.value.spaceCode = userStore.spaceCode;
    listAttAssetCat(queryParams.value).then((response) => {
        const page = normalizePage(response);
        const treeData = proxy.handleTree(page.rows, 'id');
        total.value = treeData.length;
        attAssetCatList.value = pageRows(treeData, total.value, queryParams.value);
        loading.value = false;
    });
}

watch(
    () => userStore.spaceId,
    () => {
        getList();
    }
);

/** 查询数据资产目录管理下拉树结构1 */

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
        parentId: null,
        sortOrder: 0,
        description: null,
        code: null,
        validFlag: true,
        delFlag: null,
        createBy: null,
        creatorId: null,
        createTime: null,
        updateBy: null,
        updaterId: null,
        updateTime: null,
        remark: null
    };
    proxy.resetForm('attAssetCatRef');
}

/** 搜索按钮操作 */
function handleQuery() {
    getList();
}
/** 改变启用状态值 */
function handleStatusChange(row) {
    const text = row.validFlag === true ? '启用' : '禁用';
    proxy.$modal
        .confirm('确认要"' + text + '","' + row.name + '"数据资产目录吗？')
        .then(function () {
            updateAttAssetCat({ id: row.id, validFlag: row.validFlag }).then((response) => {
                proxy.$modal.msgSuccess(text + '成功');
                getList();
            }).catch((err) => {
                row.validFlag = !row.validFlag;
            });
        })
        .catch(function () {
            row.validFlag = !row.validFlag;
        });
}

/** 重置按钮操作 */
function resetQuery() {
    proxy.resetForm('queryRef');
    handleQuery();
}

/** 新增按钮操作 */
function handleAdd(row) {
    reset();
    // getTreeselect();
    listAttAssetCat({ spaceId: userStore.spaceId, spaceCode: userStore.spaceCode }).then((response) => {
        attAssetCatOptions.value = [];
        const data = { id: 0, name: '顶级节点', children: [] };
        data.children = proxy.handleTree(response.data, 'id', 'parentId');
        attAssetCatOptions.value.push(data);
    });
    if (row != null && row.id) {
        form.value.parentId = row.id;
    } else {
        form.value.parentId = 0;
    }
    open.value = true;
    title.value = '新增数据资产目录';
}

/** 展开/折叠操作 */
function toggleExpandAll() {
    refreshTable.value = false;
    isExpandAll.value = !isExpandAll.value;
    nextTick(() => {
        refreshTable.value = true;
    });
}
function getDataTree() {
    listAttAssetCat({ spaceId: userStore.spaceId, spaceCode: userStore.spaceCode }).then((response) => {
        attAssetCatOptions.value = [];
        const data = { id: 0, name: '顶级节点', children: [] };
        data.children = proxy.handleTree(response.data, 'id', 'parentId');
        attAssetCatOptions.value.push(data);
    });
}

/** 修改按钮操作 */
async function handleUpdate(row) {
    reset();
    // await getTreeselect();
    const response = await listAttAssetCat({ spaceId: userStore.spaceId, spaceCode: userStore.spaceCode });
    attAssetCatOptions.value = [];
    // 过滤节点的计算属性
    const filteredDepts = response.data.filter((d) => {
        // 过滤条件：去掉目标部门ID或者祖先中包含目标部门ID的项
        return d.ID !== row.id && !d.parentId.toString().split(',').includes(row.id.toString());
    });
    const data = { id: 0, name: '顶级节点', children: [] };
    data.children = proxy.handleTree(filteredDepts, 'id', 'parentId');
    attAssetCatOptions.value.push(data);
    if (row != null) {
        form.value.parentId = row.parentId;
    }
    getAttAssetCat(row.id).then((response) => {
        //把createTime过滤掉
        delete response.data.createTime;
        delete response.data.updateTime;
        form.value = response.data;
        open.value = true;
        title.value = '修改数据资产目录';
    });
}

/** 提交按钮 */
function submitForm() {
    proxy.$refs['attAssetCatRef'].validate().then(() => {
        if (form.value.id != null) {
            updateAttAssetCat(form.value).then((response) => {
                proxy.$modal.msgSuccess('修改成功');
                open.value = false;
                getList();
            });
        } else {
            addAttAssetCat(form.value).then((response) => {
                proxy.$modal.msgSuccess('新增成功');
                open.value = false;
                getList();
            });
        }
    }).catch(() => {});
}

/** 删除按钮操作 */
function handleDelete(row) {
    proxy.$modal
        .confirm('是否确认删除数据资产目录管理编号为"' + row.name + '"的数据项？')
        .then(function () {
            return delAttAssetCat(row.id);
        })
        .then(() => {
            getList();
            proxy.$modal.msgSuccess('删除成功');
        })
        .catch(() => { });
}

getList();
getDataTree();
</script>

