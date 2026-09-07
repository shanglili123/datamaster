<template>
  <div class="relation-jump">
    <a-button type="link" size="small" class="rj-toggle" @click="toggle">
      <template #icon>
        <DownOutlined v-if="expanded" />
        <RightOutlined v-else />
      </template>
      {{ relation.name || '关联' }}
    </a-button>

    <div v-if="expanded" class="rj-panel">
      <div v-if="loading" class="rj-loading"><a-spin size="small" /></div>
      <a-empty v-else-if="!resp" description="无关联对象" class="rj-empty" />
      <template v-else>
        <div class="rj-title">
          目标：{{ resp.conceptName || resp.tableName }}
          <span v-if="resp.total != null">（{{ resp.total }} 条）</span>
        </div>
        <a-table
          :columns="targetCols"
          :data-source="targetRows"
          :loading="loading"
          size="small"
          :pagination="false"
          row-key="__rjKey__"
          :scroll="{ x: 'max-content' }"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === '__rj_action__'">
              <template v-if="childRelations.length">
                <OntRelationJump
                  v-for="cr in childRelations"
                  :key="cr.id"
                  :ontology-id="ontologyId"
                  :concept-id="respConceptId"
                  :table-binding-id="respTableBindingId"
                  :row="record"
                  :relation="cr"
                  :space-id="spaceId"
                  :space-code="spaceCode"
                />
              </template>
              <span v-else class="rj-none">无</span>
            </template>
          </template>
        </a-table>
      </template>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { DownOutlined, RightOutlined } from '@ant-design/icons-vue'
import { queryRelatedObjects } from '@/api/ont/objectInstance'
import { listRelation } from '@/api/ont/relation'
import { listRelationColumn } from '@/api/ont/relationColumn'

defineOptions({ name: 'OntRelationJump' })

const props = defineProps({
  ontologyId: { type: [Number, String], required: true },
  // 当前（源）概念
  conceptId: { type: [Number, String], required: true },
  // 源概念的表绑定（用于定位关系关联字段 sourceConceptTableId）
  tableBindingId: { type: [Number, String], required: true },
  // 当前行（物理列键 → 值）
  row: { type: Object, required: true },
  // 当前跳转的关系
  relation: { type: Object, required: true },
  spaceId: { type: [Number, String], default: null },
  spaceCode: { type: String, default: '' }
})

const expanded = ref(false)
const loading = ref(false)
const resp = ref(null)
// 目标概念 / 目标表绑定（递归跳转的源）
const respConceptId = ref(null)
const respTableBindingId = ref(null)
// 目标概念自身的出向关系（懒加载，递归展开子行）
const childRelations = ref([])

// 本关系在当前源表上的字段绑定（sourceColumn → targetColumn / targetConceptTableId）
let colBinding = null
let metaPromise = null

async function ensureRelationMeta() {
  if (colBinding) return colBinding
  if (!metaPromise) {
    metaPromise = (async () => {
      const res = await listRelationColumn(props.relation.id)
      const list = res.data || []
      const rc = list.find(r => String(r.sourceConceptTableId) === String(props.tableBindingId)) || list[0] || null
      colBinding = rc
      if (rc) {
        respConceptId.value = props.relation.targetConceptId ?? null
        respTableBindingId.value = rc.targetConceptTableId ?? null
      }
      return rc
    })()
  }
  return metaPromise
}

async function loadChildRelations() {
  if (!respConceptId.value) return
  try {
    const res = await listRelation({ ontologyId: props.ontologyId, pageNum: 1, pageSize: 200 })
    const rows = (res.data && res.data.rows) || []
    childRelations.value = rows.filter(r =>
      String(r.sourceConceptId) === String(respConceptId.value)
    )
  } catch {
    childRelations.value = []
  }
}

function toggle() {
  expanded.value = !expanded.value
  if (expanded.value && !resp.value) load()
}

async function load() {
  const rc = await ensureRelationMeta().catch(() => null)
  if (!rc) {
    loading.value = false
    return
  }
  let val = props.row[rc.sourceColumn]
  if (val === undefined || val === null || val === '') {
    loading.value = false
    return
  }
  loading.value = true
  try {
    const r = await queryRelatedObjects({
      sourceConceptId: props.conceptId,
      sourceTableBindingId: props.tableBindingId,
      relationId: props.relation.id,
      sourceValues: [String(val)],
      pageNum: 1,
      pageSize: 20,
      spaceId: props.spaceId,
      spaceCode: props.spaceCode
    })
    resp.value = r.data || null
    loadChildRelations()
  } catch {
    resp.value = null
  } finally {
    loading.value = false
  }
}

// 目标表头：语义属性名 → 物理列（未映射退化为物理列名）
const targetCols = computed(() => {
  const d = resp.value
  if (!d) return []
  const map = {}
  ;(d.properties || []).forEach(p => {
    if (p.physicalColumnName) map[p.physicalColumnName] = p.propertyName || p.physicalColumnName
  })
  const cols = (d.columns || []).filter(c => map[c])
  const src = cols.length ? cols : (d.columns || [])
  const arr = src.map(c => ({
    title: map[c] || c,
    dataIndex: c,
    key: c,
    ellipsis: true
  }))
  arr.push({ title: '关联', dataIndex: '__rj_action__', key: '__rj_action__', width: 140, fixed: 'right' })
  return arr
})

const targetRows = computed(() =>
  ((resp.value && resp.value.rows) || []).map((r, i) => ({ ...r, __rjKey__: i }))
)
</script>

<style lang="scss" scoped>
.relation-jump {
  display: inline-flex;
  flex-direction: column;
}
.rj-toggle {
  padding: 0 4px;
}
.rj-panel {
  margin-top: 6px;
  border: 1px solid #e6f4ff;
  border-radius: 4px;
  background: #fafdff;
  padding: 8px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.rj-loading { padding: 8px; }
.rj-empty { margin: 4px 0; }
.rj-title {
  font-size: 12px;
  color: #1677ff;
  font-weight: 500;
}
.rj-none { color: #bbb; font-size: 12px; }
</style>
