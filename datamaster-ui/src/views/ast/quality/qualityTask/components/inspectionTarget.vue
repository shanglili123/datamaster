<template>
    <!-- 稽查对象信息 新增修改弹窗 第二步 -->
    <a-modal v-model:open="dialogVisible" draggable class="dialog" :title="dialogTitle" destroy-on-close width="800px">
        <a-form ref="formRef" :model="form" :rules="formRules" :label-col="{ style: { width: '120px' } }" @submit.prevent>
            <a-row :gutter="20">
                <a-col :span="24">
                    <a-form-item label="稽查对象名称" name="name">
                        <a-input v-model:value="form.name" placeholder="请输入稽查对象名称" />
                    </a-form-item>
                </a-col>
            </a-row>
            <a-row :gutter="20">
                <a-col :span="24">
                    <a-form-item label="源数据库连接" name="datasourceId">
                        <a-select v-model:value="form.datasourceId" placeholder="请选择源数据库连接" show-search
                            @change="onDatasourceChange">
                            <a-select-option v-for="ds in datasourceOptions" :key="ds.id" :value="ds.id">{{
                                ds.datasourceName }}</a-select-option>
                        </a-select>
                    </a-form-item>
                </a-col>
            </a-row>

            <a-row :gutter="20">
                <a-col :span="24">
                    <a-form-item label="数据连接类型">
                        <a-input v-model:value="form.datasourceType" disabled placeholder="数据连接类型" />
                    </a-form-item>
                </a-col>
            </a-row>
            <a-row :gutter="20">
                <a-col :span="24">
                    <a-form-item label="数据连接实例">
                        <a-input v-model:value="form.dbname" disabled placeholder="数据连接实例" />
                    </a-form-item>
                </a-col>
            </a-row>
            <a-row :gutter="20">
                <a-col :span="24">

                    <a-form-item label="选择表" name="tableName">
                        <a-select v-model:value="form.tableName" show-search :loading="tableLoading" @change="onTableChange">
                            <a-select-option v-for="item in tableOptions" :key="item.tableName" :value="item.tableName">{{
                                item.tableName }}</a-select-option>
                        </a-select>
                    </a-form-item>
                </a-col>
            </a-row>
        </a-form>

        <template #footer>
            <div class="dialog-footer">
                <a-button @click="closeDialog">取消</a-button>
                <a-button type="primary" @click="saveData">确定</a-button>
            </div>
        </template>
    </a-modal>

    <a-modal title="Cron表达式生成器" v-model:open="cronDialogVisible" destroy-on-close>
        <crontab ref="crontabRef" :expression="expression" @hide="cronDialogVisible = false" @fill="crontabFill" />
    </a-modal>
</template>

<script setup>
import { ref, defineProps, defineEmits, getCurrentInstance } from 'vue'
import Crontab from '@/components/Crontab/index.vue'
import { getTablesByDataSourceId, getColumnByAssetId } from '@/api/col/task/index.js'
import { getDaDatasourceList } from '@/api/std/model/model'

const emit = defineEmits(['confirm'])
const { proxy } = getCurrentInstance()

const props = defineProps({
    title: { type: String, default: '表单标题' },
})

const dialogVisible = ref(false)
const cronDialogVisible = ref(false)

const formRef = ref()
const form = ref({})
const defaultForm = {
    name: "",
    datasourceId: "",
    datasourceType: "",
    datasourceName: "",
    datasourceConfig: "",
    ip: "",
    port: "",
    tableName: "",
    columnComment: "",
}

const resetForm = () => {
    Object.assign(form.value, JSON.parse(JSON.stringify(defaultForm)))
    tableOptions.value = []
    columnList.value = []
    formRef.value?.resetFields()
}

const formRules = {
    name: [{ required: true, message: '请输入稽查对象名称', trigger: 'change' }],
    datasourceId: [{ required: true, message: '请选择源数据库连接', trigger: 'change' }],
    tableName: [{ required: true, message: '请选择表', trigger: 'change' }]
}

function crontabFill(value) {
    form.value.cronExpression = value
}

const datasourceOptions = ref([])
const tableOptions = ref([])
const tableLoading = ref(false)
const columnList = ref([])

const loadDatasourceOptions = async () => {
    try {
        const res = await getDaDatasourceList()
        datasourceOptions.value = res.data
    } catch (error) {
        console.error('获取数据源失败:', error)
    }
}

const onDatasourceChange = async (id) => {
    const selected = datasourceOptions.value.find(item => item.id == id)
    if (selected) {
        form.value.datasourceType = selected.datasourceType
        form.value.datasourceName = selected.datasourceName
        form.value.datasourceConfig = selected.datasourceConfig
        let safeJson = JSON.parse(selected.datasourceConfig);
        form.value.dbname = safeJson.dbname

        console.log("🚀 ~ onDatasourceChange ~ selected:", selected)

    }
    form.value.tableName = ''
    tableOptions.value = []
    await loadTablesByDatasourceId(id)
}

const onTableChange = async (val) => {
    const selectedTable = tableOptions.value.find(item => item.tableName == val);
    if (selectedTable) {
        form.value.columnComment = selectedTable?.tableComment; // 保存中文名
        if (!form.value.name) {
            form.value.name = selectedTable?.tableComment;
        }
    } else {
        form.value.columnComment = '';
    }
};


const loadTablesByDatasourceId = async (id) => {
    tableLoading.value = true
    const res = await getTablesByDataSourceId({ datasourceId: id })
    if (res.code == '200') {
        tableOptions.value = res.data
    }
    tableLoading.value = false
}
let mode = ref()
const dialogTitle = ref('')
const openDialog = async (record, index) => {
    mode.value = index
    console.log("🚀 ~ openDialog ~ mode.value:", mode.value)
    dialogTitle.value = mode.value ? '修改稽查对象' : '新增稽查对象'
    await loadDatasourceOptions()
    resetForm()
    dialogVisible.value = true

    if (record && index) {
        const temp = JSON.parse(JSON.stringify(record))
        Object.assign(form.value, temp)
        if (temp.datasourceId) {
            await loadTablesByDatasourceId(temp.datasourceId)
        }
        if (temp.tableName) {
            await onTableChange()
        }
    }
}

const saveData = () => {
    formRef.value.validate().then(() => {
        emit('confirm', JSON.parse(JSON.stringify(form.value)), mode.value);
    }).catch(() => { })
}

const closeDialog = () => {
    dialogVisible.value = false
    resetForm()
}

defineExpose({ openDialog, closeDialog })
</script>

<style scoped lang="less">
.dialog-footer {
    text-align: right;
}
</style>

