<template>
  <div class="row-lineage-object">
    <div class="obj-header">{{ store.conceptName || '数据行' }}</div>
    <div class="obj-body">
      <div class="obj-table">{{ store.tableName }}</div>
      <div class="obj-pk" v-if="pkEntries.length">
        <span v-for="[k, v] in pkEntries" :key="k" class="pk-tag">{{ k }} = {{ v }}</span>
      </div>
    </div>
  </div>
</template>

<script setup name="RowLineageObjectNode">
import { reactive, inject, onMounted, computed } from 'vue'

const store = reactive({ conceptName: '', tableName: '', primaryKey: null })
const getNode = inject('getNode')

const pkEntries = computed(() => {
  const pk = store.primaryKey
  if (!pk || typeof pk !== 'object') return []
  return Object.entries(pk)
})

function setupData(data) {
  store.conceptName = data.conceptName || ''
  store.tableName = data.tableName || ''
  store.primaryKey = data.primaryKey || null
}

onMounted(() => {
  const node = getNode()
  setupData(node.data)
  node.on('change:data', (d) => setupData(d))
})
</script>

<style lang="scss" scoped>
.row-lineage-object {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  border: 2px solid #722ed1;
  border-radius: 8px;
  overflow: hidden;
  background: linear-gradient(135deg, #fff 0%, #f9f8ff 100%);
  box-shadow: 0 4px 12px rgba(114, 46, 209, 0.15);
}

.obj-header {
  background: linear-gradient(135deg, #722ed1 0%, #531dab 100%);
  color: #fff;
  padding: 6px 10px;
  font-weight: 600;
  font-size: 13px;
  text-align: center;
  line-height: 20px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.obj-body {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding: 4px 10px;
}

.obj-table {
  font-size: 11px;
  color: #999;
  margin-bottom: 4px;
}

.obj-pk {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}

.pk-tag {
  font-size: 11px;
  background: #f3e8ff;
  color: #722ed1;
  border-radius: 3px;
  padding: 1px 6px;
  white-space: nowrap;
}
</style>
