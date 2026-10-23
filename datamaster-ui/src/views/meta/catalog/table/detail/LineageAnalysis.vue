<template>
  <LineageGraph source="metadata" :center-id="detail.id" :center-name="detail.tableName || ''" />
</template>

<script setup name="LineageAnalysis">
import LineageGraph from '@/components/LineageGraph/index.vue';

// Metadata primary key, deliberately not detail.assetId.
defineProps({
  detail: { type: Object, default: () => ({}) },
});
</script>
