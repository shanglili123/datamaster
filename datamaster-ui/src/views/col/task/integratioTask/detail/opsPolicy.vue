<template>
  <div class="ops-policy">
    <a-spin :spinning="loading">
      <a-form :model="form" :label-col="{ style: { width: '120px' } }">
        <a-form-item label="失败即停">
          <a-switch v-model:checked="form.failStopEnabled" />
          <span class="form-tip">任务失败后自动下线任务和调度，防止无效重跑</span>
        </a-form-item>
        <a-form-item label="AI托管">
          <a-switch v-model:checked="form.aiManaged" @change="onAiManagedChange" />
          <span class="form-tip">由 AI 分析失败原因并决策恢复策略</span>
        </a-form-item>
        <a-form-item label="自动恢复">
          <a-switch v-model:checked="form.autoRecoverEnabled" :disabled="!form.aiManaged" />
          <span class="form-tip">AI 判断可恢复时自动从失败节点重跑</span>
        </a-form-item>
        <a-form-item label="恢复次数">
          <a-input-number v-model:value="form.maxRecoverTimes" :min="1" :max="10" :disabled="!form.autoRecoverEnabled" />
          <span class="form-tip">达到上限后自动下线任务</span>
        </a-form-item>
        <a-form-item label="恢复策略">
          <a-select v-model:value="form.recoverStrategy" :disabled="!form.autoRecoverEnabled" style="width:200px">
            <a-select-option label="安全自动恢复" value="SAFE_AUTO" />
            <a-select-option label="只给建议" value="SUGGEST_ONLY" />
          </a-select>
        </a-form-item>
        <a-form-item label="通知用户">
          <a-input v-model:value="form.notifyUsers" placeholder="多个用户用逗号分隔" style="width:300px" />
        </a-form-item>
        <a-form-item>
          <a-button type="primary" :loading="saving" @click="handleSave">保存策略</a-button>
          <a-button @click="handleReset">重置</a-button>
        </a-form-item>
      </a-form>
    </a-spin>
  </div>
</template>

<script setup name="OpsPolicy">
import { ref, reactive, watch, getCurrentInstance } from "vue";
import { getTaskOpsPolicy, saveTaskOpsPolicy } from "@/api/col/taskOps.js";

const { proxy } = getCurrentInstance();
const props = defineProps({
  taskId: { type: [Number, String], default: null },
});

const loading = ref(false);
const saving = ref(false);

const form = reactive({
  taskId: null,
  failStopEnabled: false,
  aiManaged: false,
  autoRecoverEnabled: false,
  maxRecoverTimes: 1,
  recoverStrategy: "SAFE_AUTO",
  notifyUsers: "",
});

const defaultForm = { ...form };

function onAiManagedChange(val) {
  if (!val) {
    form.autoRecoverEnabled = false;
  }
}

async function loadPolicy() {
  if (!props.taskId) return;
  loading.value = true;
  try {
    const res = await getTaskOpsPolicy(props.taskId);
    if (res?.data) {
      form.taskId = props.taskId;
      form.failStopEnabled = !!res.data.failStopEnabled;
      form.aiManaged = !!res.data.aiManaged;
      form.autoRecoverEnabled = !!res.data.autoRecoverEnabled;
      form.maxRecoverTimes = res.data.maxRecoverTimes ?? 1;
      form.recoverStrategy = res.data.recoverStrategy || "SAFE_AUTO";
      form.notifyUsers = res.data.notifyUsers || "";
    } else {
      form.taskId = props.taskId;
    }
  } catch (e) {
    // no policy yet, use defaults
    form.taskId = props.taskId;
  } finally {
    loading.value = false;
  }
}

async function handleSave() {
  saving.value = true;
  try {
    await saveTaskOpsPolicy({
      taskId: props.taskId,
      failStopEnabled: form.failStopEnabled,
      aiManaged: form.aiManaged,
      autoRecoverEnabled: form.autoRecoverEnabled,
      maxRecoverTimes: form.maxRecoverTimes,
      recoverStrategy: form.recoverStrategy,
      notifyUsers: form.notifyUsers,
    });
    proxy.$modal.msgSuccess("运维策略已保存");
  } finally {
    saving.value = false;
  }
}

function handleReset() {
  loadPolicy();
}

watch(() => props.taskId, (val) => {
  if (val) loadPolicy();
}, { immediate: true });
</script>

<style lang="less" scoped>
.ops-policy {
  padding: 24px;
  max-width: 700px;
}
.form-tip {
  margin-left: 10px;
  font-size: 12px;
  color: #999;
}
</style>
