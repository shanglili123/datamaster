<template>
  <LineageGraph source="asset" :center-id="form1.id" :center-name="form1.name || form1.tableName || ''" />
</template>

<script setup name="AssetLineage">
import LineageGraph from '@/components/LineageGraph/index.vue';

defineProps({
  form1: { type: Object, default: () => ({}) },
});
</script>
