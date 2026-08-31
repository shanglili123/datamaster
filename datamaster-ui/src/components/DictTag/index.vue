<template>
  <div>
    <template v-for="(item, index) in options">
      <template v-if="values.includes(item.value)">
        <span
          v-if="(item.elTagType == 'default' || item.elTagType == '') && (item.elTagClass == '' || item.elTagClass == null)"
          :key="item.value"
          :index="index"
          :class="item.elTagClass"
        >{{ item.label + " " }}</span>
        <a-tag
          v-else
          :key="item.value + ''"
          :index="index"
          :color="antTagColor(item.elTagType)"
          :class="item.elTagClass"
        >{{ item.label + " " }}</a-tag>
      </template>
    </template>
  </div>
</template>

<script setup>
const props = defineProps({
  options: {
    type: Array,
    default: null,
  },
  value: [Number, String, Array],
  showValue: {
    type: Boolean,
    default: true,
  },
});

const values = computed(() => {
  if (props.value !== null && props.value !== undefined) {
    return Array.isArray(props.value) ? props.value : [String(props.value)];
  }
  return [];
});

function antTagColor(elTagType) {
  const map = {
    primary: 'blue',
    success: 'green',
    info: 'default',
    warning: 'orange',
    danger: 'red',
  };
  return map[elTagType] || 'default';
}
</script>
