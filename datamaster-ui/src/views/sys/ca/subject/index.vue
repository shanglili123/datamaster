<template>
  <div class="app-container" ref="app-container">
    <div class="pagecont-top" v-show="showSearch">
      <a-form class="btn-style" :model="queryParams" ref="queryForm" :layout="inline" :label-col="{ style: { width: '68px' } }">
        <a-form-item label="主体名称" name="name">
          <a-input
            v-model:value="queryParams.name"
            placeholder="请输入主体名称"
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
            @click="handleAdd"
            v-hasPermi="['ca:subject:add']"
          >新增</a-button>
        </a-col>
      </a-row>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
      </div>

      <a-spin :spinning="loading">
        <a-table
          :data-source="subjectList"
          :columns="tableColumns"
          :pagination="false"
          striped
          :scroll="{ y: '60vh' }"
          :row-selection="{ type: 'checkbox', onChange: handleSelectionChange }"
          row-key="id"
          :locale="{ emptyText: emptyContent }"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'actions'">
              <a-button type="link" size="small" @click="downloadFiles(record)" v-hasPermi="['ca:subject:remove']">下载</a-button>
              <a-button type="link" danger size="small" @click="handleDelete(record)" v-hasPermi="['ca:subject:remove']">删除</a-button>
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

    <!-- 添加或修改主体管理对话框 -->
    <a-modal :title="title" v-model:open="open" width="800px" destroy-on-close>
      <a-form ref="form" :model="form" :rules="rules" :label-col="{ style: { width: '80px' } }">
        <a-row :gutter="20">
          <a-col :span="12">
            <a-form-item label="主体名称" name="name">
              <a-input v-model:value="form.name" placeholder="请输入主体名称" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="通用名称" name="cn">
              <a-input v-model:value="form.cn" placeholder="请输入通用名称" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="部门名称" name="ou">
              <a-input v-model:value="form.ou" placeholder="请输入部门名称" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="组织名称" name="o">
              <a-input v-model:value="form.o" placeholder="请输入组织名称" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="城市名称" name="l">
              <a-input v-model:value="form.l" placeholder="请输入城市名称" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="省份名称" name="st">
              <a-input v-model:value="form.st" placeholder="请输入省名称" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="国家名称" name="c">
              <a-input v-model:value="form.c" placeholder="请输入国家" />
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

import { listSubject, getSubject, delSubject, addSubject, updateSubject } from "@/api/system/ca/subject.js";

import JSZip from 'jszip';

import {red} from "chalk";
import { PlusOutlined } from "@ant-design/icons-vue";
import { h } from 'vue';

const tableColumns = [
  { title: 'ID', dataIndex: 'id', align: 'center' },
  { title: '主体名称', dataIndex: 'name', align: 'center', ellipsis: true },
  { title: '通用名称', dataIndex: 'cn', align: 'center', ellipsis: true },
  { title: '组织部门', dataIndex: 'ou', align: 'center' },
  { title: '组织名称', dataIndex: 'o', align: 'center' },
  { title: '城市名称', dataIndex: 'l', align: 'center' },
  { title: '省名称', dataIndex: 'st', align: 'center' },
  { title: '国家', dataIndex: 'c', align: 'center' },
  { title: '操作', key: 'actions', align: 'center', fixed: 'right', width: 240 },
];

const emptyContent = h('div', { class: 'emptyBg' }, [
  h('img', { src: new URL('@/assets/system/images/no_data/noData.png', import.meta.url).href, alt: '' }),
  h('p', '没有记录哦~'),
]);

export default {
  name: "Subject",
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
      // 主体管理表格数据
      subjectList: [],
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      open: false,
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 6,
        name: null,
        cn: null,
        ou: null,
        o: null,
        l: null,
        st: null,
        c: null,
        certificate: null,
        privateKey: null,
        validFlag: null,
        creatorId: null,
      },
      // 表单参数
      form: {},
      // 表单校验
      rules: {
        name: [
          { required: true, message: "主体名称不能为空", trigger: "blur" }
        ],
        cn: [
          { required: true, message: "通用名称不能为空", trigger: "blur" }
        ],
        ou: [
          { required: true, message: "组织单位名称不能为空", trigger: "blur" }
        ],
        o: [
          { required: true, message: "组织名称不能为空", trigger: "blur" }
        ],
        l: [
          { required: true, message: "城市名称不能为空", trigger: "blur" }
        ],
        st: [
          { required: true, message: "省名称不能为空", trigger: "blur" }
        ],
        c: [
          { required: true, message: "国家不能为空", trigger: "blur" }
        ],
      }
    };
  },
  created() {
    this.getList();
  },
  methods: {
    red,
    /** 查询主体管理列表 */
    getList() {
      this.loading = true;
      listSubject(this.queryParams).then(response => {
        this.subjectList = response.rows;
        this.total = response.total;
        this.loading = false;
      });
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
        saveAs(content, row.name + "_根证书" + '.zip');
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
        cn: null,
        ou: null,
        o: null,
        l: null,
        st: null,
        c: null,
        certificate: null,
        privateKey: null,
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
      this.title = "新增主体";
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      this.reset();
      const id = row.id || this.ids
      getSubject(id).then(response => {
        this.form = response.data;
        this.open = true;
        this.title = "修改主体管理";
      });
    },
    /** 提交按钮 */
    submitForm() {
      this.$refs["form"].validate().then(() => {
        if (this.form.id != null) {
          updateSubject(this.form).then(response => {
            this.$modal.msgSuccess("修改成功");
            this.open = false;
            this.getList();
          });
        } else {
          addSubject(this.form).then(response => {
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
      this.$modal.confirm('是否确认删除主体管理编号为"' + ids + '"的数据项？').then(function() {
        return delSubject(ids);
      }).then(() => {
        this.getList();
        this.$modal.msgSuccess("删除成功");
      }).catch(() => {});
    },
    /** 导出按钮操作 */
    handleExport() {
      this.download('ca/subject/export', {
        ...this.queryParams
      }, `subject_${new Date().getTime()}.xlsx`)
    }
  }
};
</script>

