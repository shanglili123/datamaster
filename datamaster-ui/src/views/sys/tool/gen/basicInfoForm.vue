<template>
  <a-form ref="basicInfoForm" :model="info" :rules="rules" :label-col="{ style: { width: '150px' } }">
    <a-row>
      <a-col :span="12">
        <a-form-item label="表名称" name="tableName">
          <a-input placeholder="请输入仓库名称" v-model:value="info.tableName" />
        </a-form-item>
      </a-col>
      <a-col :span="12">
        <a-form-item label="表描述" name="tableComment">
          <a-input placeholder="请输入" v-model:value="info.tableComment" />
        </a-form-item>
      </a-col>
      <a-col :span="12">
        <a-form-item label="实体类名称" name="className">
          <a-input placeholder="请输入" v-model:value="info.className" />
        </a-form-item>
      </a-col>
      <a-col :span="12">
        <a-form-item label="作者" name="functionAuthor">
          <a-input placeholder="请输入" v-model:value="info.functionAuthor" />
        </a-form-item>
      </a-col>
      <a-col :span="24">
        <a-form-item label="备注" name="remark">
          <a-textarea :rows="3" v-model:value="info.remark"></a-textarea>
        </a-form-item>
      </a-col>
    </a-row>
  </a-form>
</template>

<script setup>
defineProps({
  info: {
    type: Object,
    default: null
  }
});

// 表单校验
const rules = ref({
  tableName: [{ required: true, message: "请输入表名称", trigger: "blur" }],
  tableComment: [{ required: true, message: "请输入表描述", trigger: "blur" }],
  className: [{ required: true, message: "请输入实体类名称", trigger: "blur" }],
  functionAuthor: [{ required: true, message: "请输入作者", trigger: "blur" }]
});
</script>
