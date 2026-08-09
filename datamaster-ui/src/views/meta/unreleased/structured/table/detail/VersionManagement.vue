<template>
  <div class="version-management">
    <qt-wrap
      :columns="tableStroe.columns"
      :tableRef="tableRef"
      :config="{ fullContent: false, actions: { table: { search: false } } }"
    >
      <qt-table v-bind="tableStroe" ref="tableRef">
        <template #active-version="{ row }">
          <CheckCircleOutlined v-show="row.activeVersion == 'Y'" style="color: #52c41a" />
        </template>
        <template #handle="{ row }">
          <a-button type="link" :icon="h(EyeOutlined)" @click="handleDetailClick(row)">
            详情
          </a-button>
          <a-button type="link" :icon="h(EditOutlined)"> 恢复 </a-button>
        </template>
      </qt-table>
    </qt-wrap>

    <a-modal v-model:open="dialog.open" title="版本详情" width="800" destroy-on-close>
      <a-form :label-col="{ style: { width: '100px' } }">
        <a-form-item label="表名称：">
          <a-input
            v-model:value="dialog.row.tableName"
            placeholder="请输入表名称"
            disabled
          />
        </a-form-item>
        <a-form-item label="表注释：">
          <a-input
            v-model:value="dialog.row.tableComment"
            placeholder="请输入表注释"
            disabled
          />
        </a-form-item>
        <a-form-item label="版本号：">
          <a-input
            v-model:value="dialog.row.version"
            placeholder="请输入版本号"
            disabled
          />
        </a-form-item>
        <a-form-item label="变更类型：">
          <a-input
            v-model:value="dialog.row.updateType"
            placeholder="请输入变更类型"
            disabled
          />
        </a-form-item>
        <a-form-item label="变更说明：">
          <a-textarea
            v-model:value="dialog.row.updateMsg"
            placeholder="请输入变更说明"
            disabled
            :auto-size="{ minRows: 8 }"
            :maxlength="500"
            show-count
          />
        </a-form-item>
        <a-form-item label="当前版本：">
          <dict-tag
            :options="toValue(dicts.sys_yes_no)"
            :value="dialog.row.activeVersion"
          />
        </a-form-item>
        <a-form-item label="创建人：">
          <a-input
            v-model:value="dialog.row.name"
            placeholder="请输入修改人"
            disabled
          />
        </a-form-item>
        <a-form-item label="创建时间：">
          <a-input
            v-model:value="dialog.row.time"
            placeholder="请输入创建时间"
            disabled
          />
        </a-form-item>
      </a-form>
      <template #footer>
        <div class="dialog-footer">
          <a-button @click="dialog.open = false">关闭</a-button>
        </div>
      </template>
    </a-modal>
  </div>
</template>

<script setup name="VersionManagement">
import { reactive, toValue, getCurrentInstance, h } from "vue";
import { CheckCircleOutlined, EyeOutlined, EditOutlined } from '@ant-design/icons-vue';

const { proxy } = getCurrentInstance();
const dicts = proxy.useDict("sys_yes_no");

const tableStroe = reactive({
  columns: [
    {
      label: "编号",
      prop: "id",
      sortable: true,
      width: 90,
    },
    {
      label: "表名称",
      prop: "tableName",
      align: "left",
      showOverflowTooltip: {
        effect: "light",
      },
      minWidth: 140,
    },
    {
      label: "表注释",
      prop: "tableComment",
      align: "left",
      showOverflowTooltip: {
        effect: "light",
      },
      minWidth: 240,
    },
    {
      label: "版本号",
      prop: "version",
      width: 90,
    },
    {
      label: "变更类型",
      prop: "updateType",
      width: 90,
    },
    {
      label: "变更说明",
      prop: "updateMsg",
      minWidth: 240,
      align: "left",
      showOverflowTooltip: {
        effect: "light",
      },
    },
    {
      label: "当前版本",
      prop: "activeVersion",
      slot: "active-version",
      width: 90,
    },
    {
      label: "创建人",
      prop: "createBy",
      width: 90,
    },
    {
      label: "创建时间",
      prop: "createTime",
      sortable: true,
      width: 160,
      date: true,
    },
    {
      label: "操作",
      width: 240,
      fixed: "right",
      slot: "handle",
    },
  ],
  func: function () {
    return new Promise((resolve) => {
      setTimeout(() => {
        resolve({
          data: {
            rows: [
              {
                id: 1,
                version: "1.0.0",
                updateType: "新增",
                updateMsg:
                  "新增用户表字段，包括用户姓名、联系方式、注册时间等基础信息，优化数据结构设计",
                activeVersion: "N",
                createBy: "admin",
                createTime: "2021-09-01 10:00:00",
                tableName: "dataMaster_user",
                tableComment: "用户表",
              },
              {
                id: 2,
                version: "1.0.1",
                updateType: "删除",
                updateMsg:
                  "删除冗余字段，移除不再使用的email_backup和phone_backup字段，精简表结构提高性能",
                activeVersion: "N",
                createBy: "user001",
                createTime: "2021-09-02 14:30:00",
                tableName: "dataMaster_meta",
                tableComment: "用户表",
              },
              {
                id: 3,
                version: "1.0.2",
                updateType: "新增",
                updateMsg:
                  "添加索引优化，为user_name和create_time字段创建复合索引，提升查询效率约30%",
                activeVersion: "N",
                createBy: "admin",
                createTime: "2021-09-03 09:15:00",
                tableName: "dataMaster_meta",
                tableComment: "用户表",
              },
              {
                id: 4,
                version: "1.0.3",
                updateType: "回滚",
                updateMsg:
                  "回滚至1.0.1版本，撤销1.0.2版本的索引变更，解决因索引导致的数据插入性能问题",
                activeVersion: "N",
                createBy: "dev001",
                createTime: "2021-09-04 16:45:00",
                tableName: "dataMaster_meta",
                tableComment: "用户表",
              },
              {
                id: 5,
                version: "1.0.4",
                updateType: "新增",
                updateMsg:
                  "增加用户权限字段，添加role_id和permission_level字段，支持多级权限管理功能",
                activeVersion: "Y",
                createBy: "admin",
                createTime: "2021-09-05 11:20:00",
                tableName: "dataMaster_meta",
                tableComment: "用户表",
              },
            ],
            total: 5,
          },
        });
      }, 1000);
    });
  },
});

const dialog = reactive({
  open: false,
  row: {},
});

function handleDetailClick(row) {
  dialog.row = row;
  dialog.open = true;
}
</script>
