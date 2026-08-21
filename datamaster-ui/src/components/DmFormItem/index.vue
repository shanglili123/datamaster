<template>
    <!-- 不带！的 -->
  <a-form-item v-bind="$attrs">
    <template #default="scope">
      <div class="default-wrap">
        <slot name="default" v-bind="scope || {}" />
      </div>
    </template>

    <template #label>
      <div class="label-wrap">
        <slot name="label">
          {{ $attrs.label }}
        </slot>
      </div>
    </template>
  </a-form-item>
</template>

<script setup name="QtFromItem">
import { computed } from "vue";

const props = defineProps({
  tip: {
    type: [String, Object],
    default: "",
  },
});

const isString = computed(() => {
  return typeof props.tip === "string";
});
</script>

<style lang="scss" scoped>
.default-wrap {
  width: 100%;
  position: relative;

  .tip-content {
    display: flex;
    align-items: center;
    gap: 2px;
    color: #888;
    font-size: 12px;
    line-height: 1.5;
    padding-top: 4px;
    white-space: wrap;
  }
}

.label-wrap {
  display: flex;
  align-items: center;
  gap: 2px;
}

::v-deep(.ant-form-item-explain-error) {
  padding-top: 6px;
}

.ant-form-item-has-error {
  padding-bottom: 16px;
  .tip-content {
    display: none;
  }
}
</style>
