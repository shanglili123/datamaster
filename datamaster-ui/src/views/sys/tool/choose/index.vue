<template>
  <div class="app-container">
    <a-input
      v-model:value="input1"
      style="max-width: 300px; margin-right: 20px"
      placeholder="单选"
      class="input-with-select"
    >
      <template #addonAfter>
        <a-button :icon="h(SearchOutlined)" @click="radioShow" />
      </template>
    </a-input>

    <a-input
      v-model:value="input2"
      style="max-width: 300px"
      placeholder="多选"
      class="input-with-select"
    >
      <template #addonAfter>
        <a-button :icon="h(SearchOutlined)" @click="checkShow" />
      </template>
    </a-input>

    <!-- 单选  字典管理 -->
    <Current ref="dictRef1" @confirm="radioSubmit" />
    <!-- 多选  字典管理 -->
    <Selection ref="dictRef2" @confirm="checkSubmit" />
  </div>
</template>

<script setup name="ToolChoose">
import { h } from 'vue'
import Current from "./temp-current.vue";
import Selection from "./userTypeMultiple.vue";
import { SearchOutlined } from "@ant-design/icons-vue";
const dictRef1 = ref();
const dictRef2 = ref();
const input1 = ref("");
const input2 = ref("");
const radioVal = ref(null);
const checkVal = ref([]);

// 单选
function radioShow() {
  dictRef1.value.open(radioVal.value);
}

function radioSubmit(val) {
  radioVal.value = val;
  input1.value = val.dictName;
}

// 多选
function checkShow() {
  dictRef2.value.open(checkVal.value);
}
function checkSubmit(val) {
  checkVal.value = [...val];
  input2.value = val.map((item) => item.id);
}
</script>
