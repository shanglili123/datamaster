<template>
    <a-tab-pane v-bind="$attrs">
        <template #default="scope">
            <slot name="default" v-bind="scope" />
        </template>

        <template #tab>
            <div class="label-wrap">
                <slot name="label">
                    {{ $attrs.label }}
                </slot>
                <a-tooltip
                    :title="props.tip.content"
                    :placement="props.tip.placement || 'top'"
                    v-if="props.tip && Object.keys(props.tip).length"
                >
                    <InfoFilled class="tip-icon" />
                    <template #title v-if="props.tip.custom">
                        <div class="tip-content" v-html="props.tip.content"></div>
                    </template>
                </a-tooltip>
            </div>
        </template>
    </a-tab-pane>
</template>

<script setup name="QtTabPane">
    import { InfoCircleFilled as InfoFilled } from '@ant-design/icons-vue';

    const props = defineProps({
        tip: {
            type: Object
        }
    });
</script>

<style lang="scss" scoped>
    .label-wrap{
        display: flex;
        align-items: center;
        gap: 2px;
        .tip-icon{
             color: #888;
        }
    }
</style>
