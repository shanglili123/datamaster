<template>
    <a-modal v-model:open="visible" title="问题数据详情" class="medium-dialog" @close="handleClose" :destroy-on-close="true">
        <a-descriptions v-if="parsedFields.length" :column="1" bordered :label-style="{ width: '300px', whiteSpace: 'nowrap' }">
            <a-descriptions-item v-for="(item, index) in parsedFields" :key="index" :label="item.name">
                {{ item.value }}
            </a-descriptions-item>
        </a-descriptions>
        <template #footer>
            <a-button @click="handleClose">关闭</a-button>
        </template>
    </a-modal>
</template>

<script setup>
import { ref, defineExpose, watch } from 'vue'

const visible = ref(false)
const detailData = ref(null)
const parsedFields = ref([])

function open(data, value) {
    console.log("🚀 ~ open ~ value:", value)
    detailData.value = data
    visible.value = true
    try {
        const json = JSON.parse(data.dataJsonStr || '{}')

        const labelMap = (value && Array.isArray(value.evaColumns))
            ? value.evaColumns.reduce((map, col) => {
                map[col.name.toLowerCase()] = col.label || col.name
                return map
            }, {})
            : {}

        parsedFields.value = Object.entries(json).map(([key, val]) => ({
            name: labelMap[key.toLowerCase()] || key,
            value: val,
        }))
    } catch (e) {
        parsedFields.value = []
    }
}


function handleClose() {
    visible.value = false
    detailData.value = null
    parsedFields.value = []
}

defineExpose({
    open,
    close: handleClose,
})
</script>
<style scoped lang="scss">
:deep(.ant-descriptions-item-label) {
    width: 300px !important;
    white-space: nowrap;
}
</style>

