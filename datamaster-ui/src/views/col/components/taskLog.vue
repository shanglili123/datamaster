<template>
    <a-modal v-model:open="visible" title="任务执行日志" class="medium-dialog task-log-modal" :width="1000" @close="handleClose" @cancel="handleClose">
        <div class="task-log-container" :class="{ 'stage-windows-enabled': showStageWindows }" ref="containerRef">
            <a-tabs v-if="showStageWindows" v-model:activeKey="activeTab" @change="handleTabChange">
                <a-tab-pane key="log" tab="日志" />
                <a-tab-pane key="stageWindows" tab="阶段增量窗口" />
            </a-tabs>
            <a-spin v-show="!showStageWindows || activeTab === 'log'" :spinning="loading" class="task-log-spin">
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
            <div v-if="showStageWindows && activeTab === 'stageWindows'" class="stage-windows-panel">
                <div class="stage-windows-toolbar">
                    <span>当前实例已固定的阶段增量窗口（起止值均包含；状态按整个工作流收尾更新）</span>
                    <a-button :loading="stageWindowsLoading" @click="fetchStageWindows">刷新</a-button>
                </div>
                <a-spin :spinning="stageWindowsLoading">
                    <a-alert v-if="stageWindowsError" type="error" show-icon message="阶段增量窗口读取失败">
                        <template #action>
                            <a-button size="small" :disabled="stageWindowsLoading" @click="fetchStageWindows">重试</a-button>
                        </template>
                    </a-alert>
                    <div v-else-if="stageWindowsLoading" class="stage-windows-loading">正在读取阶段增量窗口...</div>
                    <a-table v-else :columns="stageWindowColumns" :data-source="stageWindows"
                        :row-key="stageWindowRowKey" :pagination="false" :scroll="{ x: 1900, y: 470 }" size="small">
                        <template #bodyCell="{ column, record }">
                            <template v-if="column.key === 'empty'">
                                {{ record.empty === true ? '是' : record.empty === false ? '否' : '-' }}
                            </template>
                            <template v-else-if="column.key === 'status'">{{ stageWindowStatusLabel(record.status) }}</template>
                            <template v-else>{{ displayStageValue(record[column.dataIndex]) }}</template>
                        </template>
                        <template #emptyText>暂无阶段增量窗口</template>
                    </a-table>
                </a-spin>
            </div>
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
import { getStageWindows } from "@/api/col/instance/job";

const props = defineProps({
    showStageWindows: { type: Boolean, default: false },
});

const activeTab = ref("log");
const localInstanceId = ref();
const stageWindows = ref([]);
const stageWindowsLoading = ref(false);
const stageWindowsError = ref(false);
const stageWindowsLoaded = ref(false);
let openGeneration = 0;

const stageWindowColumns = [
    { title: "阶段", dataIndex: "stageKey", key: "stageKey", width: 180 },
    { title: "源节点 ID", dataIndex: "sourceNodeId", key: "sourceNodeId", width: 180 },
    { title: "来源表", dataIndex: "sourceTable", key: "sourceTable", width: 180 },
    { title: "游标字段", dataIndex: "cursorColumn", key: "cursorColumn", width: 160 },
    { title: "游标类型", dataIndex: "cursorType", key: "cursorType", width: 90 },
    { title: "起始值", dataIndex: "startValue", key: "startValue", width: 220 },
    { title: "结束值", dataIndex: "endValue", key: "endValue", width: 220 },
    { title: "空窗口", dataIndex: "empty", key: "empty", width: 80 },
    { title: "重放来源记录", dataIndex: "replayFromId", key: "replayFromId", width: 180 },
    { title: "状态", dataIndex: "status", key: "status", width: 160 },
];
const displayStageValue = (value) => value === null || value === undefined || value === "" ? "-" : String(value);
const stageWindowStatusLabel = (status) => ({
    PREPARED: "已固定，待流程收尾",
    SUCCESS: "已提交成功水位",
    FAILURE: "失败，保留重放窗口",
})[status] || displayStageValue(status);
const stageWindowRowKey = (record, index) => JSON.stringify([record.stageKey, record.sourceNodeId, index]);

const fetchStageWindows = async () => {
    if (!props.showStageWindows || !visible.value || activeTab.value !== "stageWindows" || stageWindowsLoading.value) return;
    const generation = openGeneration;
    const localId = localInstanceId.value;
    stageWindowsLoading.value = true;
    stageWindowsError.value = false;
    try {
        if (localId === null || localId === undefined || localId === "") throw new Error("Missing local instance ID");
        const res = await getStageWindows(localId);
        if (generation !== openGeneration || !visible.value) return;
        if (!Array.isArray(res.data)) throw new Error("Invalid stage windows response");
        stageWindows.value = res.data.filter((row) => row && typeof row === "object" && !Array.isArray(row));
        stageWindowsLoaded.value = true;
    } catch (error) {
        if (generation !== openGeneration || !visible.value) return;
        stageWindowsError.value = true;
    } finally {
        if (generation === openGeneration && visible.value) stageWindowsLoading.value = false;
    }
};

const handleTabChange = (key) => {
    activeTab.value = key;
    if (key === "stageWindows" && !stageWindowsLoaded.value) fetchStageWindows();
};

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
const fetchLog = async (taskId, generation = openGeneration) => {
    if (!polling.value || generation !== openGeneration) return;
    try {
        const res = await getLogByTaskInstanceId({
            taskInstanceId: taskId,
            skipLineNum: nextLineNum.value,
            limit: logLimit,
        });
        if (!polling.value || generation !== openGeneration) return;
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
        if (generation !== openGeneration || !polling.value) return;
        polling.value = false;
        message.error("日志读取失败");
        return;
    }
    if (polling.value && generation === openGeneration) {
        clearPollTimer();
        pollTimer = setTimeout(() => fetchLog(taskId, generation), 1500);
    }
};
let loading = ref(false)
// 打开弹窗
const open = async (taskId) => {
    const generation = ++openGeneration;
    localInstanceId.value = taskId;
    activeTab.value = "log";
    stageWindows.value = [];
    stageWindowsLoading.value = false;
    stageWindowsError.value = false;
    stageWindowsLoaded.value = false;
    loading.value = true;
    polling.value = false;
    clearPollTimer();
    nextLineNum.value = 0;
    logContent.value = "";
    autoFollow.value = true;
    visible.value = true;
    await nextTick();
    if (generation !== openGeneration || !visible.value) return;
    polling.value = true;
    await fetchLog(taskId, generation);
    if (generation === openGeneration && visible.value) loading.value = false;
};

// 关闭弹窗
const handleClose = () => {
    ++openGeneration;
    visible.value = false;
    localInstanceId.value = undefined;
    stageWindows.value = [];
    stageWindowsLoading.value = false;
    stageWindowsError.value = false;
    stageWindowsLoaded.value = false;
    loading.value = false;
    polling.value = false;
    nextLineNum.value = 0;
    clearPollTimer();
    logContent.value = "";
};

onBeforeUnmount(() => {
    ++openGeneration;
    polling.value = false;
    clearPollTimer();
});

defineExpose({ open });
</script>

<style scoped>
.task-log-container {
    height: 660px;
}

.stage-windows-enabled {
    display: flex;
    flex-direction: column;
}

.stage-windows-enabled .task-log-spin {
    flex: 1;
    min-height: 0;
}

.stage-windows-enabled .task-log-spin :deep(.ant-spin-container),
.stage-windows-enabled .log-container {
    height: 100%;
}

.stage-windows-panel {
    flex: 1;
    min-height: 0;
    overflow: auto;
}

.stage-windows-panel :deep(.ant-table-cell) {
    overflow-wrap: anywhere;
}

.stage-windows-toolbar {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 12px;
}

.stage-windows-loading {
    padding: 48px 0;
    text-align: center;
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

