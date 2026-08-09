<template>
  <div class="app-container" ref="app-container">
    <div class="pagecont-top" v-show="showSearch">
      <a-form class="btn-style" :model="queryParams" ref="queryForm" :layout="inline" v-show="showSearch" :label-col="{ style: { width: '68px' } }">
        <a-form-item label="名称" name="name">
          <a-input
            v-model:value="queryParams.name"
            placeholder="请输入名称"
            allow-clear
            class="el-form-input-width"
            @pressEnter="handleQuery"
          />
        </a-form-item>
        <a-form-item label="颁发者" name="issuer">
          <a-input
            v-model:value="queryParams.issuer"
            placeholder="请输入颁发者"
            allow-clear
            class="el-form-input-width"
            @pressEnter="handleQuery"
          />
        </a-form-item>
        <a-form-item label="所有者" name="possessor">
          <a-input
            v-model:value="queryParams.possessor"
            placeholder="请输入所有者"
            class="el-form-input-width"
            allow-clear
            @pressEnter="handleQuery"
          />
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
      <a-row :gutter="10" class="btn-style">
        <a-col :span="1.5">
          <a-button
            type="primary"
            :icon="h(PlusOutlined)"
            size="small"
            @click="handleAdd"
            v-hasPermi="['ca:cert:add']"
          >新增</a-button>
        </a-col>
      </a-row>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
      </div>

      <a-spin :spinning="loading">
        <a-table
          :data-source="certList"
          :columns="tableColumns"
          :pagination="false"
          striped
          :scroll="{ y: '60vh' }"
          :row-selection="{ type: 'checkbox', onChange: handleSelectionChange }"
          row-key="id"
          :locale="{ emptyText: emptyContent }"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.dataIndex === 'validTime'">
              {{ record.validTime }} 年
            </template>
            <template v-else-if="column.dataIndex === 'remark'">
              <span>{{ record.remark || '-' }}</span>
            </template>
            <template v-else-if="column.key === 'actions'">
              <a-button type="link" size="small" @click="downloadFiles(record)" v-hasPermi="['ca:cert:edit']">下载</a-button>
              <a-button type="link" danger size="small" @click="handleDelete(record)" v-hasPermi="['ca:cert:remove']">删除</a-button>
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

    <!-- 添加或修改证书对话框 -->
    <a-modal :title="title" v-model:open="open" width="800px" destroy-on-close>
      <a-form ref="form" :model="form" :rules="rules" :label-col="{ style: { width: '80px' } }">
        <a-row :gutter="20">
          <a-col :span="12">
            <a-form-item label="名称" name="name">
              <a-input v-model:value="form.name" placeholder="请输入名称" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="颁发主体" name="subjectId">
              <a-select v-model:value="form.subjectId" placeholder="请选择颁发主体" @change="subjectChange" :style="{ width: '100%' }">
                <a-select-option
                  v-for="item in subjectList"
                  :key="item.id"
                  :value="item.id">{{ item.name }}
                </a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="颁发者" name="issuer">
              <a-input v-model:value="form.issuer" disabled placeholder="请输入颁发者" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="所有者" name="possessor">
              <a-input v-model:value="form.possessor" placeholder="请输入所有者" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="有效期" name="validTime">
              <div style="display: flex; align-items: center; gap: 6px;">
                <a-input-number v-model:value="form.validTime" :max="30" :min="1" :controls="false" style="width: 100%" placeholder="请输入有效期" />
                <span>年</span>
              </div>
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item label="备注" name="remark">
              <a-textarea v-model:value="form.remark" placeholder="请输入内容" />
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>
      <template #footer>
        <div class="dialog-footer">
          <a-button @click="cancel">取 消</a-button>
          <a-button type="primary" @click="submitForm">确 定</a-button>
        </div>
      </template>
    </a-modal>
  </div>
</template>

<script>

import { listCert, getCert, delCert, addCert, updateCert } from "@/api/system/ca/cert.js";

import {listSubject} from "@/api/system/ca/subject.js";

import JSZip from "jszip";
import { PlusOutlined } from "@ant-design/icons-vue";
import { h } from 'vue';

const tableColumns = [
  { title: 'ID', dataIndex: 'id', align: 'center' },
  { title: '名称', dataIndex: 'name', align: 'center', ellipsis: true },
  { title: '主体名称', dataIndex: 'subjectName', align: 'center', ellipsis: true },
  { title: '颁发者', dataIndex: 'issuer', align: 'center', ellipsis: true },
  { title: '所有者', dataIndex: 'possessor', align: 'center', ellipsis: true },
  { title: '有效期', dataIndex: 'validTime', align: 'center' },
  { title: '生效时间', dataIndex: 'createTime', align: 'center', ellipsis: true },
  { title: '备注', dataIndex: 'remark', align: 'center', ellipsis: true },
  { title: '操作', key: 'actions', align: 'center', fixed: 'right', width: 240 },
];

const emptyContent = h('div', { class: 'emptyBg' }, [
  h('img', { src: new URL('@/assets/system/images/no_data/noData.png', import.meta.url).href, alt: '' }),
  h('p', '没有记录哦~'),
]);

export default {
  name: "Cert",
  data() {
    return {
      // 遮罩层
      loading: true,
      // 选中数组
      ids: [],
      // 非单个禁用
      single: true,
      // 非多个禁用
      multiple: true,
      // 显示搜索条件
      showSearch: true,
      // 总条数
      total: 0,
      // 主体列表
      subjectList: [],
      // 证书表格数据
      certList: [],
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      open: false,
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 6,
        name: null,
        subjectId: null,
        subjectName: null,
        certificate: null,
        privateKey: null,
        issuer: null,
        possessor: null,
        validTime: null,
        validFlag: null,
        creatorId: null,
      },
      // 表单参数
      form: {},
      // 表单校验
      rules: {
        name: [
          { required: true, message: "名称", trigger: "blur" }
        ],
        subjectId: [
          { required: true, message: "主体id不能为空", trigger: "change" }
        ],
        subjectName: [
          { required: true, message: "主体名称不能为空", trigger: "change" }
        ],
        issuer: [
          { required: true, message: "颁发者不能为空", trigger: "blur" }
        ],
        possessor: [
          { required: true, message: "所有者不能为空", trigger: "blur" }
        ],
        validTime: [
          { required: true, message: "有效期不能为空", trigger: "blur" },
        ],
      }
    };
  },
  created() {
    this.getList();
    this.getSubjectList();
  },
  methods: {
    /** 查询证书列表 */
    getList() {
      this.loading = true;
      listCert(this.queryParams).then(response => {
        this.certList = response.rows;
        this.total = response.total;
        this.loading = false;
      });
    },
    /** 查询主题列表 */
    getSubjectList() {
      listSubject({
        pageNum: 1,
        pageSize: 999999
      }).then(response => {
        this.subjectList = response.rows;
      });
    },
    subjectChange(e) {
      this.subjectList.forEach(item => {
        if (item.id === e) {
          this.form.subjectName = item.name;
          this.form.issuer = item.name;
        }
      });
    },
    // 取消按钮
    cancel() {
      this.open = false;
      this.reset();
    },
    // 表单重置
    reset() {
      this.form = {
        id: null,
        name: null,
        subjectId: null,
        subjectName: null,
        certificate: null,
        privateKey: null,
        issuer: null,
        possessor: null,
        validTime: null,
        validFlag: null,
        delFlag: null,
        createBy: null,
        creatorId: null,
        createTime: null,
        updateBy: null,
        updateTime: null,
        remark: null
      };
      this.resetForm("form");
    },
    /** 搜索按钮操作 */
    handleQuery() {
      this.queryParams.pageNum = 1;
      this.getList();
    },
    /** 重置按钮操作 */
    resetQuery() {
      this.resetForm("queryForm");
      this.handleQuery();
    },
    // 多选框选中数据
    handleSelectionChange(selectedRowKeys, selectedRows) {
      this.ids = selectedRows.map(item => item.id)
      this.single = selectedRows.length!==1
      this.multiple = !selectedRows.length
    },
    /** 新增按钮操作 */
    handleAdd() {
      this.reset();
      this.open = true;
      this.title = "新增证书";
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      this.reset();
      const id = row.id || this.ids
      getCert(id).then(response => {
        this.form = response.data;
        this.open = true;
        this.title = "修改证书";
      });
    },
    /** 提交按钮 */
    submitForm() {
      this.$refs["form"].validate().then(() => {
        if (this.form.id != null) {
          updateCert(this.form).then(response => {
            this.$modal.msgSuccess("修改成功");
            this.open = false;
            this.getList();
          });
        } else {
          addCert(this.form).then(response => {
            this.$modal.msgSuccess("新增成功");
            this.open = false;
            this.getList();
          });
        }
      }).catch(() => {});
    },
    /** 删除按钮操作 */
    handleDelete(row) {
      const ids = row.id || this.ids;
      this.$modal.confirm('是否确认删除证书编号为"' + ids + '"的数据项？').then(function() {
        return delCert(ids);
      }).then(() => {
        this.getList();
        this.$modal.msgSuccess("删除成功");
      }).catch(() => {});
    },
    async downloadFiles(row) {
      const zip = new JSZip();

      const files = [row.privateKey, row.certificate];

      for (let fileUrl of files) {
        const response = await fetch(fileUrl);
        const blob = await response.blob();

        // 自动获取文件名
        const fileName = fileUrl.substring(fileUrl.lastIndexOf('/') + 1);
        zip.file(fileName, blob);
      }

      zip.generateAsync({ type: 'blob' }).then(content => {
        saveAs(content, row.name + "_数字证书" + '.zip');
      });
    },
    /** 导出按钮操作 */
    handleExport() {
      this.download('ca/cert/export', {
        ...this.queryParams
      }, `cert_${new Date().getTime()}.xlsx`)
    }
  }
};
</script>

