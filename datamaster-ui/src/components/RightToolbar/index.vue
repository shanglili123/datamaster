<template>
  <div class="top-right-btn" :style="style">
    <a-space>
      <a-tooltip title="刷新">
        <a-button shape="circle" @click="refresh()">
          <template #icon><ReloadOutlined /></template>
        </a-button>
      </a-tooltip>
      <a-tooltip title="隐藏列" v-if="columns">
        <a-button shape="circle" @click="showColumn()" v-if="showColumnsType == 'transfer'">
          <template #icon><MenuOutlined /></template>
        </a-button>
        <a-dropdown v-if="showColumnsType == 'checkbox'" :trigger="['click']">
          <a-button shape="circle">
            <template #icon><MenuOutlined /></template>
          </a-button>
          <template #overlay>
            <a-menu>
              <a-menu-item v-for="item in columns" :key="item.key">
                <a-checkbox :checked="item.visible" @change="checkboxChange($event, item.label)">
                  {{ item.label }}
                </a-checkbox>
              </a-menu-item>
            </a-menu>
          </template>
        </a-dropdown>
      </a-tooltip>
    </a-space>
    <a-modal :title="title" v-model:open="open" :footer="null">
      <a-transfer
        :titles="['显示', '隐藏']"
        v-model:target-keys="value"
        :data-source="columns"
        @change="dataChange"
      />
    </a-modal>
  </div>
</template>

<script setup>
import { ReloadOutlined, MenuOutlined } from '@ant-design/icons-vue'

const props = defineProps({
  showSearch: {
    type: Boolean,
    default: true,
  },
  columns: {
    type: Array,
  },
  search: {
    type: Boolean,
    default: true,
  },
  showColumnsType: {
    type: String,
    default: "checkbox",
  },
  gutter: {
    type: Number,
    default: 10,
  },
})

const emits = defineEmits(['update:showSearch', 'queryTable']);

const value = ref([]);
const title = ref("显示/隐藏");
const open = ref(false);

const style = computed(() => {
  const ret = {};
  if (props.gutter) {
    ret.marginRight = `${props.gutter / 2}px`;
  }
  return ret;
});

function refresh() {
  emits("queryTable");
}

function dataChange(nextTargetKeys, direction, moveKeys) {
  for (let item in props.columns) {
    const key = String(props.columns[item].key);
    props.columns[item].visible = !nextTargetKeys.includes(key);
  }
}

function showColumn() {
  open.value = true;
}

if (props.showColumnsType == 'transfer') {
  for (let item in props.columns) {
    if (props.columns[item].visible === false) {
      value.value.push(String(props.columns[item].key));
    }
  }
}

function checkboxChange(event, label) {
  props.columns.filter(item => item.label == label)[0].visible = event.target.checked;
}
</script>
