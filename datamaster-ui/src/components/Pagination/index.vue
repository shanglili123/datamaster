<template>
  <div :class="{ 'hidden': hidden }" class="pagination-container">
    <a-pagination
      v-model:current="currentPage"
      v-model:pageSize="pageSize"
      :total="total"
      :pageSizeOptions="pageSizeOptions"
      :showSizeChanger="true"
      :showQuickJumper="true"
      :showTotal="(total) => `共 ${total} 条`"
      @change="handleChange"
    />
  </div>
</template>

<script setup>
import { scrollTo } from '@/utils/scroll-to'

const props = defineProps({
  total: {
    required: true,
    type: Number
  },
  page: {
    type: Number,
    default: 1
  },
  limit: {
    type: Number,
    default: 6
  },
  pageSizes: {
    type: Array,
    default() {
      return [4, 6, 8, 10, 20, 30, 50]
    }
  },
  autoScroll: {
    type: Boolean,
    default: true
  },
  hidden: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits();
const currentPage = computed({
  get() {
    return props.page
  },
  set(val) {
    emit('update:page', val)
  }
})
const pageSize = computed({
  get() {
    return props.limit
  },
  set(val){
    emit('update:limit', val)
  }
})

const pageSizeOptions = computed(() => {
  return props.pageSizes.map(String)
})

function handleChange(page, size) {
  if (size !== pageSize.value) {
    if (page * size > props.total) {
      page = 1
    }
  }
  emit('pagination', { page, limit: size })
  if (props.autoScroll) {
    scrollTo(0, 800)
  }
}
</script>

<style scoped>
.pagination-container {
  background: #fff;
  padding: 32px 16px;
  display: flex;
  justify-content: flex-end;
}
.pagination-container.hidden {
  display: none;
}
</style>
