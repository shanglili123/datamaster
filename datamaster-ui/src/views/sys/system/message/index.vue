<template>
    <div class="app-container">
        <div class="pagecont-top" v-show="showSearch">
            <a-form
                class="btn-style"
                :model="queryParams"
                ref="queryRef"
                layout="inline"
                :label-col="{ style: { width: '68px' } }"
            >
                <a-form-item label="消息类型" name="category">
                    <a-select
                        v-model:value="queryParams.category"
                        placeholder="消息类型"
                        allow-clear
                        class="el-form-input-width"
                    >
                        <a-select-option
                            v-for="dict in message_category"
                            :key="dict.value"
                            :value="dict.value">{{ dict.label }}</a-select-option>
                    </a-select>
                </a-form-item>

                <a-form-item label="创建时间">
                    <a-range-picker
                        class="el-form-input-width"
                        v-model:value="queryParams.dateRange"
                        valueFormat="YYYY-MM-DD"
                        start-placeholder="开始日期"
                        end-placeholder="结束日期"
                    ></a-range-picker>
                </a-form-item>
                <a-form-item>
                    <a-button
                        type="primary"
                        @click="handleQuery"
                        @mousedown="(e) => e.preventDefault()"
                    >
                        <i class="iconfont-mini icon-a-zu22377 mr5"></i>查询
                    </a-button>
                    <a-button
                        @click="resetQuery"
                        @mousedown="(e) => e.preventDefault()"
                    >
                        <i class="iconfont-mini icon-a-zu22378 mr5"></i>重置
                    </a-button>
                </a-form-item>
            </a-form>
        </div>
        <div  class="pagecont-bottom">

            <div class="justify-between mb15">
                <a-row :gutter="10" class="btn-style">
                    <a-col :span="1.5">
                        <a-button @click="readAllMsg">
                            <i class="iconfont-mini icon-a-zu22378 mr5"></i>全部设为已读
                        </a-button>
                    </a-col>
                </a-row>
                <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
            </div>

            <a-table striped :loading="loading" :data-source="msgList" :columns="tableColumns" :pagination="false">
                <template #bodyCell="{ column, record }">
                    <template v-if="column.key === 'category'">
                        <dict-tag :options="message_category" :value="record.category" />
                    </template>
                    <template v-else-if="column.key === 'hasRead'">
                        <a-tag :color="record.hasRead ? 'success' : 'error'">
                            {{ record.hasRead ? "已读" : "未读" }}
                        </a-tag>
                    </template>
                    <template v-else-if="column.key === 'actions'">
                        <a-button type="link" size="small" @click="handleView(record)">详情</a-button>
                        <a-button type="link" danger size="small" @click="deleteMsg(record.id)">删除</a-button>
                    </template>
                </template>
            </a-table>
            <pagination
                v-show="total > 0"
                :total="total"
                v-model:page="queryParams.pageNum"
                v-model:limit="queryParams.pageSize"
                @pagination="getList"
            />
        </div>

        <a-modal
            title="消息详情"
            v-model:open="openView"
            width="800px"
            destroy-on-close
            class="msg-dialog"
        >
            <a-form :label-col="{ style: { width: '100px' } }">
                <a-row :gutter="20">
                    <a-col :span="12">
                        <a-form-item label="消息标题：">
                            <div class="form-value-ifon">
                                {{ viewData.title }}
                            </div>
                        </a-form-item>
                    </a-col>

                    <a-col :span="12">
                        <a-form-item label="类型：">
                            <div class="form-value-ifon">
                                <dict-tag
                                    :options="message_category"
                                    :value="viewData.category"
                                />
                            </div>
                        </a-form-item>
                    </a-col>
                    <a-col :span="12">
                        <a-form-item label="是否已读：">
                            <div class="form-value-ifon">
                                <a-tag :color="viewData.hasRead ? 'success' : 'error'">
                                    {{ viewData.hasRead ? "已读" : "未读" }}
                                </a-tag>
                            </div>
                        </a-form-item>
                    </a-col>
                    <a-col :span="12">
                        <a-form-item label="消息内容：">
                            <div class="form-value-ifon">
                                {{ viewData.content }}
                            </div>
                        </a-form-item>
                    </a-col>
                    <a-col :span="24">
                        <a-form-item label="创建时间：">
                            <div class="form-value-ifon">
                                {{ viewData.createTime }}
                            </div>
                        </a-form-item>
                    </a-col>
                </a-row>
            </a-form>
            <template #footer>
                <div class="dialog-footer">
                    <a-button @click="openView = false">关 闭</a-button>
                </div>
            </template>
        </a-modal>
    </div>
</template>

<script setup name="Message">
import { getCurrentInstance, ref } from "vue";
import { message, Modal } from "ant-design-vue";
import useUserStore from "@/store/system/user";
import { normalizePage, pageRows } from "@/utils/page.js";
import {
    listMessage,
    delMessage,
    read,
    readAll,
    updateMessage
} from "@/api/system/system/message/message";

const tableColumns = [
    { title: '消息标题', dataIndex: 'title', align: 'center' },
    { title: '消息类型', key: 'category', align: 'center' },
    { title: '是否已读', key: 'hasRead', align: 'center' },
    { title: '消息内容', dataIndex: 'content', align: 'center' },
    { title: '创建时间', dataIndex: 'createTime', align: 'center' },
    { title: '操作', key: 'actions', align: 'center', fixed: 'right', width: 240 },
];

const openView = ref(false);
const viewData = ref({});
const userStore = useUserStore();
const queryParams = ref({
    dateRange: [],
    pageNum: 1,
    pageSize: 6,
    receiverId: userStore.userId,
});
const total = ref(0);

const loading = ref(false);
const showSearch = ref(true);
const msgList = ref([]);
const { proxy } = getCurrentInstance();
const { message_category } = proxy.useDict("message_category");

const handleQuery = () => {
    console.log(queryParams.value);
    getList()
};

const resetQuery = () => {
    queryParams.value = {};
};

const handleView = (e) => {
    e.hasRead = '1'
    e.delFlag = 1
    updateMessage(e);
    msgList.value = msgList.value.map(item => {
        if (e.id == item.id) {
            return { ...item, hasRead: '1' }; // 创建一个新对象，修改 hasRead
        }
        return item; // 保持其他项不变
    });
    openView.value = true;
    viewData.value = e;
};

const startTime = ref(null)
const endTime = ref(null)
const getList = () => {
    const redataMaster = {
        category: queryParams.value.category,
        receiverId: userStore.userId,
        pageNum: queryParams.value.pageNum,
        pageSize: queryParams.value.pageSize,
    }
    if(queryParams.value.dateRange && queryParams.value.dateRange.length > 0){
        redataMaster.startTime= queryParams.value.dateRange[0],
        redataMaster.endTime= queryParams.value.dateRange[1]
    }
    listMessage(redataMaster).then((response) => {
        const page = normalizePage(response);
        total.value = page.total;
        msgList.value = pageRows(page.rows, page.total, queryParams.value);
    });
};
getList();

/** 全部已读 */
function readAllMsg() {
    Modal.confirm({
        title: "提示",
        content: "确定全部设为已读吗？",
        okText: "确定",
        cancelText: "取消",
        onOk: async () => {
            await readAll();
            console.log('------设置为已读----')
            getList();
            message.success("操作成功");
        }
    });
}
/** 删除 */
function deleteMsg(id) {
    Modal.confirm({
        title: "提示",
        content: "确定删除改条消息吗？",
        okText: "确定",
        cancelText: "取消",
        onOk: async () => {
            await delMessage(id);
            getList();
            message.success("操作成功");
        }
    });
}

// /**
//  * 修改状态为已读
//  * @param id
//  */
// function updateMsg(row) {
//     row.hasRead = '1'
//     updateMessage(row);
// }
</script>

<style scoped lang="scss">
.msg-dialog {
    .ant-modal-body {
        height: 300px !important;
    }
}
</style>

