<template>
    <div class="justify-between mb15">
        <a-row :gutter="15" class="btn-style">
            <a-col :span="1.5">
                <a-button type="primary" @click="handleAdd" @mousedown="(e) => e.preventDefault()">
                    <template #icon><PlusOutlined /></template>新增
                </a-button>
            </a-col>
        </a-row>
    </div>

    <!-- 表格部分 -->
    <a-table
      striped
      :scroll="{ y: '300px' }"
      :loading="loading"
      :data-source="dpCodeMapList"
      :row-key="(record, index) => index"
      :pagination="false"
      size="middle"
    >
        <a-table-column title="原始值" align="left" data-index="originalValue" ellipsis>
            <template #default="{ record }">
                {{ record.originalValue || '-' }}
            </template>
        </a-table-column>
        <a-table-column title="字典名" align="left" data-index="dictName" ellipsis>
            <template #default="{ record }">
                {{ record.dictName || '-' }}
            </template>
        </a-table-column>
        <a-table-column title="字典值" align="left" data-index="dictValue" ellipsis>
            <template #default="{ record }">
                {{ record.dictValue || '-' }}
            </template>
        </a-table-column>
        <a-table-column title="操作" align="center" :width="150" fixed="right">
            <template #default="{ record, index }">
                <!-- 修改时传递行索引，用于后续的 local 编辑 -->
                <a-button type="link" @click="handleUpdate(record, index)"
                  ><template #icon><EditOutlined /></template>修改</a-button
                >
                <a-button type="link" danger @click="handleDelete(index)"
                  ><template #icon><DeleteOutlined /></template>删除</a-button
                >
            </template>
        </a-table-column>
        <template #emptyText>
            <div class="emptyBg">
                <p>无数据</p>
            </div>
        </template>
    </a-table>

    <!-- 新增/修改对话框 -->
    <a-modal :title="title" v-model:open="open" :draggable="true" :destroy-on-close="true" :footer="null" :width="800">
        <a-form ref="dpCodeMapRef" :model="form" :rules="rules" :label-col="{ style: { width: '80px' } }">
            <a-row :gutter="20">
                <a-col :span="12">
                    <a-form-item label="原始值" name="originalValue">
                        <a-input v-model:value="form.originalValue" placeholder="请输入原始值" />
                    </a-form-item>
                </a-col>
                <a-col :span="12">
                    <a-form-item label="字典名" name="dictName">
                        <a-input v-model:value="form.dictName" placeholder="请输入字典名" />
                    </a-form-item>
                </a-col>
            </a-row>
            <a-row :gutter="20">
                <a-col :span="12">
                    <a-form-item label="字典值" name="dictValue">
                        <a-input v-model:value="form.dictValue" placeholder="代码值" />
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
</template>

<script setup>
import { PlusOutlined, EditOutlined, DeleteOutlined } from "@ant-design/icons-vue";
const { proxy } = getCurrentInstance();
const props = defineProps({
    row: { type: Object, default: () => ({}) },
});
const dpCodeMapRef = ref(null);
function cancel() {
    open.value = false;
    reset();
}

const dpCodeMapList = ref(props.row || []);

watch(
    () => props.row,
    (newValue) => {
        dpCodeMapList.value = newValue || []; // 如果新值是 undefined 或 null，则赋为空数组
    },
    { deep: true }
);


// 其他状态变量
const loading = ref(false);
const total = ref(dpCodeMapList.value.length);
const open = ref(false);
const title = ref('');
// 表单和验证规则
const data = reactive({
    oldOriginalValue: null,
    form: {
        originalValue: null,
        dictName: null,
        dictValue: null,
    },
    rules: {
        originalValue: [{ required: true, message: '原始值不能为空', trigger: 'change' }],
        dictName: [{ required: true, message: '代码名不能为空', trigger: 'change' }],
        dictValue: [{ required: true, message: '字典值不能为空', trigger: 'change' }]
    }
});

const { oldOriginalValue, form, rules } = toRefs(data);
const emit = defineEmits(["dpCodeMapList",]);
/** 表单重置 */
function reset() {
    form.value = { index: null, id: null, originalValue: null, dictName: null, dictValue: null };
    oldOriginalValue.value = null;
    // proxy.resetForm('dpCodeMapRef');
}

/** 新增按钮操作 */
function handleAdd() {
    reset();
    open.value = true;
    title.value = '新增';
}

/** 修改按钮操作 */
function handleUpdate(row, index) {
    reset();
    form.value = { ...row, index };
    oldOriginalValue.value = row.originalValue;
    open.value = true;
    title.value = '修改';
}

/** 删除按钮操作 */
function handleDelete(index) {
    proxy.$modal.confirm('是否确认删除该数据项？')
        .then(() => {
            dpCodeMapList.value.splice(index, 1);
            total.value = dpCodeMapList.value.length;
            proxy.$modal.msgSuccess('删除成功');
            emit('dpCodeMapList', dpCodeMapList.value);
        })
        .catch(() => { });
}

/** 提交按钮：新增或修改 */
function submitForm() {
    dpCodeMapRef.value.validate((valid) => {
        if (valid) {
            // 检查 originalValue 是否已经存在
            const isDuplicate = dpCodeMapList.value.some(item => item.originalValue === form.value.originalValue);
            if (!(oldOriginalValue.value !== null && oldOriginalValue.value === form.value.originalValue) && isDuplicate) {
                proxy.$modal.msgWarning('原始值已存在，不能新增');
                return; // 阻止继续执行
            }

            // 如果是修改操作
            if (form.value.index !== null && form.value.index !== undefined) {
                dpCodeMapList.value.splice(form.value.index, 1, { ...form.value });
                proxy.$modal.msgSuccess('修改成功');
            } else {
                dpCodeMapList.value.push({ ...form.value });
                proxy.$modal.msgSuccess('新增成功');
            }
            emit('dpCodeMapList', dpCodeMapList.value);

            open.value = false;
            total.value = dpCodeMapList.value.length;
        }
    });
}
</script>

