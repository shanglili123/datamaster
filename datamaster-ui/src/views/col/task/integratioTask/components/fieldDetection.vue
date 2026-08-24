<template>
    <a-modal v-model:open="visible" :draggable="true" title="字段冲突处理" :closable="false" :destroy-on-close="true"
        :footer="null" class="MessageBox" :width="600">
        <div style="padding: 10px 0;">
            已有 {{ existingFields.length }} 个字段，检测到
            {{ Math.max(0, newFields.length - existingFields.length) }} 个新字段，如何处理？
        </div>
        <template #footer>
            <a-button type="warning" @click="handleClick('addNewOnly')">增加新的</a-button>
            <a-button type="primary" @click="handleClick('addAll')">增加所有</a-button>
            <a-button type="primary" danger @click="handleClick('clearAndAddAll')">清除并增加所有</a-button>
            <a-button @click="onCancel">取消</a-button>
        </template>
    </a-modal>
</template>

<script setup>
const props = defineProps({
    modelValue: Boolean,
    existingFields: Array,
    newFields: Array
})

const emit = defineEmits(['update:modelValue', 'resolve'])

const visible = ref(props.modelValue)

watch(() => props.modelValue, val => {
    visible.value = val
})

watch(visible, val => {
    emit('update:modelValue', val)
})

const isAddNewOnlyDisabled = computed(() => {
    if (!props.existingFields || !props.newFields) return true
    const existingNames = props.existingFields.map(f => f.columnName)
    return props.newFields.every(f => existingNames.includes(f.columnName))
})

const isAddAllDisabled = computed(() => {
    return !props.newFields || props.newFields.length === 0
})

const handleClick = (actionType) => {
    emit('resolve', { action: actionType })
    visible.value = false
}

const onCancel = () => {
    emit('resolve', { action: 'cancel' })
    visible.value = false
}
</script>

