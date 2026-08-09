<template>
    <!-- 数据预览的修改记录前后对比弹窗 -->
    <a-modal v-model:open="visible" class="dialog" width="1200px" destroy-on-close>
        <template #header="{ close, titleId, titleClass }">
            <span role="heading" aria-level="2">
                前后比对
            </span>
        </template>
        <div class="center">

            <CodeDiff :old-string="oldStrToCompare" :new-string="newStrToCompare" :context="10"
                output-format="side-by-side"
/>
        </div>
        <!-- <template #footer>
            <a-button type="primary" @click="cancel">确认</a-button>
            <a-button @click="rollBack" :disabled="loading"><HistoryOutlined />回滚</a-button>
        </template> -->
    </a-modal>
</template>

<script setup>
import { ref } from 'vue'
import { CodeDiff } from 'v-code-diff'

const visible = ref(false)
const oldStrToCompare = ref('')
const newStrToCompare = ref('')
const loading = ref(false)
const id = ref(null)

function show(diffId, oldData, newData) {
    id.value = diffId
    oldStrToCompare.value = JSON.stringify(oldData, null, 2)
    newStrToCompare.value = JSON.stringify(newData, null, 2)
    visible.value = true
}
function close() {
    visible.value = false
}
function cancel() {
    close()
}

defineExpose({ show })
</script>

<style scoped>
.center {
    max-height: 600px;
    overflow-y: auto;
    overflow-x: hidden;
}
</style>

