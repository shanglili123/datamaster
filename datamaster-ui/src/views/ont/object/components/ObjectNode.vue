<template>
    <div class="object-node">
        <div class="object-header">
            <span>{{ store.name }}</span>
        </div>
        <div class="object-body">
            <div class="object-code">{{ store.code }}</div>
            <div class="object-table">{{ store.table }}</div>
        </div>
    </div>
</template>

<script setup name="OntObjectNode">
    import { reactive, inject, onMounted } from 'vue';

    const store = reactive({
        name: '',
        code: '',
        table: ''
    });

    const getNode = inject('getNode');

    function setupData(data) {
        store.name = data.name;
        store.code = data.code;
        store.table = data.table;
    }

    onMounted(() => {
        const node = getNode();
        setupData(node.data);
        node.on('change:data', (data) => {
            setupData(data);
        });
    });
</script>

<style lang="scss" scoped>
    .object-node {
        display: flex;
        flex-direction: column;
        width: 100%;
        height: 100%;
        background: linear-gradient(135deg, #fff 0%, #f9f8ff 100%);
        border: 2px solid #722ed1;
        border-radius: 8px;
        box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
        transition: all 0.3s ease;
    }

    .object-node:hover {
        box-shadow: 0 8px 24px rgba(0, 0, 0, 0.15);
    }

    .object-header {
        background: linear-gradient(135deg, #722ed1 0%, #531dab 100%);
        color: white;
        padding: 6px 12px;
        font-weight: 600;
        font-size: 14px;
        text-align: center;
        border-radius: 6px 6px 0 0;
        line-height: 20px;

        span {
            overflow: hidden;
            text-overflow: ellipsis;
            white-space: nowrap;
        }
    }

    .object-body {
        flex: 1;
        display: flex;
        flex-direction: column;
        justify-content: center;
        padding: 4px 12px;
        border-radius: 0 0 6px 6px;
    }

    .object-code {
        font-size: 12px;
        color: #722ed1;
        font-weight: 500;
    }

    .object-table {
        font-size: 11px;
        color: #999;
        margin-top: 2px;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
    }
</style>