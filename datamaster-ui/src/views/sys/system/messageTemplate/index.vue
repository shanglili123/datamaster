<template>
  <div class="app-container" ref="app-container">
    <div class="pagecont-top" v-show="showSearch">
      <a-form class="btn-style" :model="queryParams" ref="queryRef" layout="inline" :label-col="{ style: { width: '68px' } }">
        <a-form-item label="消息标题" name="title">
          <a-input
              class="el-form-input-width"
              v-model:value="queryParams.title"
              placeholder="请输入消息标题"
              allow-clear
              @pressEnter="handleQuery"
          />
        </a-form-item>
        <a-form-item label="消息类别" name="category">
          <a-select v-model:value="queryParams.category" placeholder="请选择" class="el-form-input-width">
            <a-select-option
                v-for="dict in message_category"
                :key="dict.value"
                :value="dict.value"
            >{{ dict.label }}</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="消息等级" name="msgLevel">
          <a-select v-model:value="queryParams.msgLevel" placeholder="请选择" class="el-form-input-width">
            <a-select-option
                v-for="dict in message_level"
                :key="dict.value"
                :value="dict.value"
            >{{ dict.label }}</a-select-option>
          </a-select>
        </a-form-item>

        <a-form-item>
          <a-button type="primary" @click="handleQuery" @mousedown="(e) => e.preventDefault()">
            <i class="iconfont-mini icon-a-zu22377 mr5"></i>查询
          </a-button>
          <a-button @click="resetQuery" @mousedown="e => e.preventDefault()">
            <i class="iconfont-mini icon-a-zu22378 mr5"></i>重置
          </a-button>
        </a-form-item>
      </a-form>
    </div>
    <div  class="pagecont-bottom">
      <div class="justify-between mb15">
        <a-row :gutter="15" class="justify-end btn-style">
          <a-col :span="1.5">
            <a-button
                type="primary"
                @click="handleAdd"
                v-hasPermi="['system:messageTemplate:add']"
                @mousedown="e => e.preventDefault()"
            >
              <i class="iconfont-mini icon-xinzeng mr5"></i>新增
            </a-button>
          </a-col>
        </a-row>
        <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
      </div>

      <a-spin :spinning="loading">
        <a-table
          :data-source="messageTemplateList"
          :columns="tableColumns"
          :pagination="false"
          striped
          :scroll="{ y: '60vh' }"
          :locale="{ emptyText: emptyContent }"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.dataIndex === 'category'">
              <dict-tag :options="message_category" :value="record.category" />
            </template>
            <template v-else-if="column.dataIndex === 'msgLevel'">
              <dict-tag :options="message_level" :value="record.msgLevel" />
            </template>
            <template v-else-if="column.dataIndex === 'createTime'">
              <span>{{ parseTime(record.createTime, '{y}-{m}-{d}') }}</span>
            </template>
            <template v-else-if="column.key === 'actions'">
              <a-button type="link" size="small" @click="handleUpdate(record)" v-hasPermi="['system:messageTemplate:edit']">修改</a-button>
              <a-button type="link" danger size="small" @click="handleDelete(record)" v-hasPermi="['system:messageTemplate:remove']">删除</a-button>
            </template>
            <template v-else>
              <span>{{ record[column.dataIndex] || '-' }}</span>
            </template>
          </template>
        </a-table>
      </a-spin>

      <pagination
        v-show="total>0"
        :total="total"
        v-model:page="queryParams.pageNum"
        v-model:limit="queryParams.pageSize"
        @pagination="getList"
      />
    </div>

    <!-- 添加或修改消息模板对话框 -->
    <a-modal :title="title" v-model:open="open" width="800px" destroy-on-close>
      <a-form ref="messageTemplateRef" :model="form" :rules="rules" :label-col="{ style: { width: '80px' } }">
        <a-row :gutter="20">
          <a-col :span="12">
            <a-form-item label="消息标题" name="title">
              <a-input v-model:value="form.title" placeholder="请输入消息标题" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="消息类别" name="category">
              <a-select v-model:value="form.category" placeholder="请选择">
                <a-select-option
                    v-for="dict in message_category"
                    :key="dict.value"
                    :value="parseInt(dict.value)"
                >{{ dict.label }}</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
        </a-row>
        <!-- <a-row :gutter="20">
          <a-col :span="24">
            <a-form-item label="消息类别" name="category">
              <a-select v-model:value="form.category" placeholder="请选择">
                <a-select-option
                    v-for="dict in message_category"
                    :key="dict.value"
                    :value="parseInt(dict.value)"
                >{{ dict.label }}</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
        </a-row> -->
        <a-row :gutter="20">
          <a-col :span="12">
            <a-form-item label="消息等级" name="level">
              <a-select v-model:value="form.msgLevel" placeholder="请选择">
                <a-select-option
                    v-for="dict in message_level"
                    :key="dict.value"
                    :value="parseInt(dict.value)"
                >{{ dict.label }}</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="20">
          <a-col :span="24">
            <a-form-item label="消息模板" name="content">
              <a-textarea v-model:value="form.content" placeholder="请输入内容"></a-textarea>
              <!--          <editor v-model="form.content" :min-height="192"/>-->
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="20">
          <a-col :span="24">
            <a-form-item label="备注" name="remark">
              <a-textarea v-model:value="form.remark" placeholder="请输入内容" />
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>
      <template #footer>
        <div class="dialog-footer">
          <a-button size="small" @click="cancel">取 消</a-button>
          <a-button type="primary" size="small" @click="submitForm">确 定</a-button>
        </div>
      </template>
    </a-modal>
  </div>
</template>

<script setup name="MessageTemplate">

import { listMessageTemplate, getMessageTemplate, delMessageTemplate, addMessageTemplate, updateMessageTemplate } from "@/api/system/system/message/messageTemplate";

import { normalizePage, pageRows } from "@/utils/page.js";
import { h } from 'vue';

const { proxy } = getCurrentInstance();
const { message_category, message_level } = proxy.useDict("message_category", "message_level");

const tableColumns = [
  { title: '模版ID', dataIndex: 'id', align: 'center' },
  { title: '消息标题', dataIndex: 'title', align: 'center' },
  { title: '消息模板内容', dataIndex: 'content', align: 'center', width: 180, ellipsis: true },
  { title: '消息类别', dataIndex: 'category', align: 'center' },
  { title: '消息等级', dataIndex: 'msgLevel', align: 'center' },
  { title: '创建人', dataIndex: 'createBy', align: 'center' },
  { title: '创建时间', dataIndex: 'createTime', align: 'center', width: 180 },
  { title: '备注', dataIndex: 'remark', align: 'center' },
  { title: '操作', key: 'actions', align: 'center', fixed: 'right', width: 240 },
];

const emptyContent = h('div', { class: 'emptyBg' }, [
  h('img', { src: new URL('@/assets/system/images/no_data/noData.png', import.meta.url).href, alt: '' }),
  h('p', '没有记录哦~'),
]);

const messageTemplateList = ref([]);
const open = ref(false);
const loading = ref(true);
const showSearch = ref(true);
const ids = ref([]);
const single = ref(true);
const multiple = ref(true);
const total = ref(0);
const title = ref("");

const data = reactive({
  form: {},
  queryParams: {
    pageNum: 1,
    pageSize: 6,
    title: null,
    category: null,
    msgLevel: null,
  },
  rules: {
    title: [
      { required: true, message: "消息标题不能为空", trigger: "blur" }
    ],
    content: [
      { required: true, message: "消息模板内容不能为空", trigger: "blur" }
    ],
    category: [
      { required: true, message: "消息类别不能为空", trigger: "blur" }
    ],
    msgLevel: [
      { required: true, message: "消息等级不能为空", trigger: "blur" }
    ],
  }
});

const { queryParams, form, rules } = toRefs(data);

/** 查询消息模板列表 */
function getList() {
  loading.value = true;
  listMessageTemplate(queryParams.value).then(response => {
    const page = normalizePage(response);
    total.value = page.total;
    messageTemplateList.value = pageRows(page.rows, page.total, queryParams.value);
    loading.value = false;
  });
}

// 取消按钮
function cancel() {
  open.value = false;
  reset();
}

// 表单重置
function reset() {
  form.value = {
    id: null,
    title: null,
    content: null,
    category: null,
    msgLevel: null,
    validFlag: null,
    delFlag: null,
    createBy: null,
    creatorId: null,
    createTime: null,
    updateBy: null,
    updaterId: null,
    updateTime: null,
    remark: null
  };
  proxy.resetForm("messageTemplateRef");
}

/** 搜索按钮操作 */
function handleQuery() {
  queryParams.value.pageNum = 1;
  getList();
}

/** 重置按钮操作 */
function resetQuery() {
  proxy.resetForm("queryRef");
  handleQuery();
}

/** 新增按钮操作 */
function handleAdd() {
  reset();
  open.value = true;
  title.value = "新增消息模板";
}

/** 修改按钮操作 */
function handleUpdate(row) {
  reset();
  const _id = row.id || ids.value
  getMessageTemplate(_id).then(response => {
    form.value = response.data;
    open.value = true;
    title.value = "修改消息模板";
  });
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs["messageTemplateRef"].validate().then(() => {
    if (form.value.id != null) {
      updateMessageTemplate(form.value).then(response => {
        proxy.$modal.msgSuccess("修改成功");
        open.value = false;
        getList();
      });
    } else {
      addMessageTemplate(form.value).then(response => {
        proxy.$modal.msgSuccess("新增成功");
        open.value = false;
        getList();
      });
    }
  }).catch(() => {});
}

/** 删除按钮操作 */
function handleDelete(row) {
  const _ids = row.id || ids.value;
  proxy.$modal.confirm('是否确认删除消息模板编号为"' + _ids + '"的数据项？').then(function() {
    return delMessageTemplate(_ids);
  }).then(() => {
    getList();
    proxy.$modal.msgSuccess("删除成功");
  }).catch(() => {});
}

/** 导出按钮操作 */
function handleExport() {
  proxy.download('system/messageTemplate/export', {
    ...queryParams.value
  }, `messageTemplate_${new Date().getTime()}.xlsx`)
}

getList();
</script>
