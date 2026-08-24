<template>
    <a-modal v-model:open="visible" title="任务执行日志" class="medium-dialog task-log-modal" :width="1000" @close="handleClose">
        <div class="task-log-container" ref="containerRef">
            <a-spin :spinning="loading" class="task-log-spin">
                <div class="log-container">
                    <div class="log-toolbar">
                        <span>{{ logStatusText }}</span>
                        <div class="log-actions">
                            <a-switch v-model:checked="autoFollow" size="small" checked-children="跟随" />
                            <a-button type="link" @click="scrollLogToBottom">到底部</a-button>
                        </div>
                    </div>
                    <div class="log-scrollbar" ref="logScrollbarRef" @scroll.passive="handleLogScroll">
                        <pre class="log-text">{{ logContent }}</pre>
                    </div>
                </div>
            </a-spin>
        </div>
        <template #footer>
            <div style="text-align: right">
                <a-button @click="handleClose">关闭</a-button>
            </div>
        </template>
    </a-modal>
</template>

<script setup>
import { ref, onBeforeUnmount, nextTick, computed, watch } from "vue";
import { message } from "ant-design-vue";
import { getLogByTaskInstanceId } from "@/api/col/task/etlTask";

// 状态变量
const visible = ref(false);
const containerRef = ref(null);
const logScrollbarRef = ref(null);
const logContent = ref("");
const polling = ref(false);
const autoFollow = ref(true);
const nextLineNum = ref(0);
const logLimit = 2000;
let pollTimer = null;

const logStatusText = computed(() => {
    if (polling.value) return "日志读取中...";
    return logContent.value ? "日志读取完成" : "暂无日志";
});

const clearPollTimer = () => {
    if (pollTimer) {
        clearTimeout(pollTimer);
        pollTimer = null;
    }
};

const scrollLogToBottom = () => {
    autoFollow.value = true;
    nextTick(() => {
        const wrap = logScrollbarRef.value;
        if (wrap) {
            wrap.scrollTop = wrap.scrollHeight;
        }
    });
};

const handleLogScroll = (event) => {
    const wrap = event.target || logScrollbarRef.value;
    if (!wrap) return;
    autoFollow.value = wrap.scrollHeight - wrap.scrollTop - wrap.clientHeight < 24;
};

watch(logContent, (value, oldValue) => {
    if (autoFollow.value || value.length < oldValue.length) {
        scrollLogToBottom();
    }
});

// 轮询日志
const fetchLog = async (taskId) => {
    if (!polling.value) return;
    try {
        const res = await getLogByTaskInstanceId({
            taskInstanceId: taskId,
            skipLineNum: nextLineNum.value,
            limit: logLimit,
        });
        if (!polling.value) return;
        const { status, log, logContent: chunk, toLineNum, isEnd } = res.data || {};
        const text = chunk ?? log ?? "";
        if (text) {
            logContent.value += logContent.value && !logContent.value.endsWith("\n") ? `\n${text}` : text;
        }
        if (Number.isFinite(Number(toLineNum))) {
            nextLineNum.value = Math.max(nextLineNum.value, Number(toLineNum));
        } else if (text) {
            nextLineNum.value += text.split(/\r?\n/).length;
        }
        const s = Number(status);
        if ([5, 6, 7].includes(s) && isEnd !== false) {
            polling.value = false;
            return;
        }
    } catch (error) {
        polling.value = false;
        message.error("日志读取失败");
        return;
    }
    if (polling.value) {
        clearPollTimer();
        pollTimer = setTimeout(() => fetchLog(taskId), 1500);
    }
};
let loading = ref(false)
// 打开弹窗
const open = async (taskId) => {
    loading.value = true;
    clearPollTimer();
    nextLineNum.value = 0;
    logContent.value = "";
    autoFollow.value = true;
    visible.value = true;
    await nextTick();
    polling.value = true;
    await fetchLog(taskId);
    loading.value = false;

};

// 关闭弹窗
const handleClose = () => {
    visible.value = false;
    polling.value = false;
    nextLineNum.value = 0;
    clearPollTimer();
    logContent.value = "";
};

onBeforeUnmount(() => {
    polling.value = false;
    clearPollTimer();
});

defineExpose({ open });
</script>

<style scoped>
.task-log-container {
    height: 660px;
}

.log-container {
    height: 660px;
    display: flex;
    flex-direction: column;
    border: 1px solid #ebeef5;
    border-radius: 3px;
    background: #000;
    color: #0f0;
    font-family: monospace;
    overflow: hidden;
}

.log-toolbar {
    height: 34px;
    flex-shrink: 0;
    padding: 0 12px;
    display: flex;
    align-items: center;
    justify-content: space-between;
    border-bottom: 1px solid #1f2937;
    color: #cbd5e1;
    font-family: Arial, sans-serif;
    font-size: 12px;
}

.log-actions {
    display: flex;
    align-items: center;
    gap: 12px;
}

.log-scrollbar {
    flex: 1;
    min-height: 0;
    overflow-y: auto;
    overflow-x: hidden;
}

.log-text {
    margin: 0;
    padding: 12px;
    white-space: pre-wrap;
    word-wrap: break-word;
    word-break: break-all;
}
</style>

<style lang="scss">
.task-log-modal .ant-modal-body {
    overflow: hidden !important;
    max-height: none !important;
    padding: 12px 24px !important;
}
</style>

