<template>
    <el-select 
        v-bind="$attrs" 
        @change="handleDatasourceChange"
        :model-value="modelValue"
        @update:model-value="handleUpdateModelValue"
    >
        <el-option
            v-for="source in store.datasources"
            :key="source.id"
            :label="source.datasourceName"
            :value="source.id"
            :disabled="source.disabled"
        />
        
        <!--回echo fallback when selected value not in list-->
        <template v-if="modelValue && !store.datasources.some(item => item.id === modelValue)">
            <el-option 
                :value="modelValue"
                :label="modelValue + ' (回echo - 可能已被删除)'" 
                disabled
            />
        </template>
    </el-select>
</template>

<script setup name="DatasourceList">
    import { reactive, computed, onMounted, nextTick, watch } from 'vue';
    import { getDatasourceData, getAvailableDatasource } from './utils';
    import useUserStore from '@/store/system/user';

    const props = defineProps({
        flag: {
            type: String
        },
        data: {
            type: Array
        },
        project: {
            type: Boolean
        },
        // 新增回echo相关属性
        modelValue: {
            type: [String, Number],
            default: ''
        },
        savedDataSourceId: {
            type: [String, Number],
            default: ''
        },
        taskParams: {
            type: Object,
            default: () => ({})
        }
    });

    const emits = defineEmits(['change', 'update:modelValue']);

    const userStore = useUserStore();

    const store = reactive({
        types: [],
        datasources: []
    });

    // 计算当前选中的值
    const currentValue = computed(() => {
        return props.modelValue || props.savedDataSourceId || '';
    });

    function initDatasourceData() {
        if (props.data) {
            store.datasources = props.data;
            nextTick(() => perform回echo());
            return;
        }
        
        const params = {};
        params.pageNum = 1;
        params.pageSize = 9999;
        if (props.project) {
            params.projectId = userStore.projectId;
            params.projectCode = userStore.projectCode;
        }
        
        getDatasourceData(params).then((data) => {
            store.datasources = getAvailableDatasource(data, props.flag);
            nextTick(() => perform回echo());
        });
    }

    // 执行回echo逻辑
    function perform回echo() {
        if (!currentValue.value) return;
        
        const id = String(currentValue.value);
        let foundDatasource = store.datasources.find(item => item.id === id);
        
        // 尝试从taskParams获取回echo信息
        if (!foundDatasource && props.taskParams?.readerDatasource) {
            const ds = props.taskParams.readerDatasource;
            if (String(ds.datasourceId) === id) {
                foundDatasource = {
                    id: id,
                    datasourceName: ds.datasourceName || ds.dbname || '回echo数据源',
                    datasourceType: ds.datasourceType || '',
                    ip: ds.ip || '',
                    port: ds.port || '',
                    datasourceConfig: ds.datasourceConfig || '{}'
                };
            }
        }
        
        // 最后的兜底方案
        if (!foundDatasource) {
            foundDatasource = {
                id: id,
                datasourceName: '回echo数据源 ' + id,
                datasourceType: '',
                ip: '',
                port: '',
                datasourceConfig: '{}'
            };
        }
        
        // 添加到列表（避免重复）
        if (!store.datasources.some(item => item.id === id)) {
            store.datasources.unshift(foundDatasource);
            console.log('回echo: 执行回echo逻辑，数据源ID:', id, '名称:', foundDatasource.datasourceName);
        }
    }

    function handleUpdateModelValue(value) {
        emits('update:modelValue', value);
    }

    function handleDatasourceChange(id) {
        const source = store.datasources.find((item) => item.id === id);
        emits('change', id, source);
    }

    // 监听props变化，自动执行回echo
    watch(() => props.savedDataSourceId, (newVal) => {
        if (newVal) {
            nextTick(() => perform回echo());
        }
    });

    watch(() => props.taskParams, () => {
        if (props.taskParams?.readerDatasource?.datasourceId) {
            nextTick(() => perform回echo());
        }
    });

    onMounted(() => {
        initDatasourceData();
    });
</script>
