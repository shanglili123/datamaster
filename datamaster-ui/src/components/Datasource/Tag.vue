<template>
    <a-tag :color="tag?.type === 'success' ? 'green' : tag?.type === 'warning' ? 'orange' : tag?.type === 'primary' ? 'blue' : undefined">{{ tag.label }}</a-tag>
</template>

<script setup name="DatasourceTag">
    import { computed } from 'vue';
    import { getDatasourceActive } from './utils';
    const props = defineProps({
        value: {
            type: String,
            required: true
        }
    });

    const tag = computed(() => {
        return getDatasourceActive(props.value);
    });
</script>
