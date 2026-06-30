<template>
  <div class="ops-policy">
    <el-form :model="form" label-width="120px" v-loading="loading">
      <el-form-item label="失败即停">
        <el-switch v-model="form.failStopEnabled" />
        <span class="form-tip">任务失败后自动下线任务和调度，防止无效重跑</span>
      </el-form-item>
      <el-form-item label="AI托管">
        <el-switch v-model="form.aiManaged" @change="onAiManagedChange" />
        <span class="form-tip">由 AI 分析失败原因并决策恢复策略</span>
      </el-form-item>
      <el-form-item label="自动恢复">
        <el-switch v-model="form.autoRecoverEnabled" :disabled="!form.aiManaged" />
        <span class="form-tip">AI 判断可恢复时自动从失败节点重跑</span>
      </el-form-item>
      <el-form-item label="恢复次数">
        <el-input-number v-model="form.maxRecoverTimes" :min="1" :max="10" :disabled="!form.autoRecoverEnabled" />
        <span class="form-tip">达到上限后自动下线任务</span>
      </el-form-item>
      <el-form-item label="恢复策略">
        <el-select v-model="form.recoverStrategy" :disabled="!form.autoRecoverEnabled" style="width:200px">
          <el-option label="安全自动恢复" value="SAFE_AUTO" />
          <el-option label="只给建议" value="SUGGEST_ONLY" />
        </el-select>
      </el-form-item>
      <el-form-item label="通知用户">
        <el-input v-model="form.notifyUsers" placeholder="多个用户用逗号分隔" style="width:300px" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :loading="saving" @click="handleSave">保存策略</el-button>
        <el-button @click="handleReset">重置</el-button>
      </el-form-item>
    </el-form>
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
