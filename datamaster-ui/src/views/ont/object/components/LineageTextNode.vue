<template>
  <div class="lineage-text-node" :class="'dim-' + (store.dimension || '')">
    <div class="ltn-dim">{{ store.dimLabel }}</div>
    <div class="ltn-title" :title="store.title">{{ store.title }}</div>
    <div class="ltn-sub" :title="store.sub">{{ store.sub }}</div>
    <div v-if="store.tag" class="ltn-tag">{{ store.tag }}</div>
  </div>
</template>

<script setup name="OntLineageTextNode">
import { reactive, inject, onMounted } from 'vue'

const store = reactive({
  dimension: '',   // decision / version / permission
  dimLabel: '',    // 维度中文标签
  title: '',
  sub: '',
  tag: ''
})

const getNode = inject('getNode')

function setupData(data) {
  store.dimension = data.dimension
  store.dimLabel = data.dimLabel
  store.title = data.title
  store.sub = data.sub
  store.tag = data.tag
}

onMounted(() => {
  const node = getNode()
  setupData(node.data)
  node.on('change:data', (data) => setupData(data))
})
</script>

<style lang="scss" scoped>
.lineage-text-node {
  display: flex;
  flex-direction: column;
  width: 100%;
  height: 100%;
  padding: 6px 10px;
  background: #fff;
  border: 1.5px solid #d9d9d9;
  border-radius: 6px;
  box-sizing: border-box;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);

  .ltn-dim {
    font-size: 11px;
    font-weight: 600;
    margin-bottom: 2px;
  }

  .ltn-title {
    font-size: 13px;
    font-weight: 600;
    color: #1f2d3d;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .ltn-sub {
    font-size: 11px;
    color: #666;
    line-height: 16px;
    overflow: hidden;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    word-break: break-all;
  }

  .ltn-tag {
    margin-top: auto;
    align-self: flex-start;
    font-size: 10px;
    line-height: 16px;
    padding: 0 6px;
    border-radius: 8px;
    color: #fff;
  }

  // 决策维度：绿
  &.dim-decision {
    border-color: #52c41a;
    .ltn-dim { color: #389e0d; }
    .ltn-tag { background: #52c41a; }
  }

  // 版本维度：橙
  &.dim-version {
    border-color: #fa8c16;
    .ltn-dim { color: #d46b08; }
    .ltn-tag { background: #fa8c16; }
  }

  // 权限维度：按状态色（绿=允许/红=拒绝），默认灰
  &.dim-permission {
    border-color: #8c8c8c;
    .ltn-dim { color: #595959; }
    .ltn-tag.perm-ok { background: #52c41a; }
    .ltn-tag.perm-deny { background: #ff4d4f; }
  }
}
</style>
