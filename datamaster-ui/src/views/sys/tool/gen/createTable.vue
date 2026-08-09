<template>
  <!-- 创建表 -->
  <a-modal title="创建表" v-model:open="visible" width="800px" draggable destroy-on-close>
    <span>创建表语句(支持多个建表语句)：</span>
    <a-textarea :rows="10" placeholder="请输入文本" v-model:value="content"></a-textarea>
    <template #footer>
      <div class="dialog-footer">
        <a-button @click="visible = false">取 消</a-button>
        <a-button type="primary" @click="handleImportTable">确 定</a-button>
      </div>
    </template>
  </a-modal>
</template>

<script setup>
import { createTable } from "@/api/system/tool/gen.js";

const visible = ref(false);
const content = ref("");
const { proxy } = getCurrentInstance();
const emit = defineEmits(["ok"]);

/** 显示弹框 */
function show() {
  visible.value = true;
}

/** 导入按钮操作 */
function handleImportTable() {
  if (content.value === "") {
    proxy.$modal.msgError("请输入建表语句");
    return;
  }
  createTable({ sql: content.value }).then(res => {
    proxy.$modal.msgSuccess(res.msg);
    if (res.code === 200) {
      visible.value = false;
      emit("ok");
    }
  });
}

defineExpose({
  show,
});
</script>
