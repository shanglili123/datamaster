<template>
  <a-modal
    v-model:open="visible"
    :title="title"
    width="800px"
    destroy-on-close
  >
    <a-form
      ref="formRef"
      :model="form"
      :rules="currentRules"
      :label-col="{ style: { width: '140px' } }"
      @submit.prevent
    >
      <a-row :gutter="20">
        <a-col :span="24">
          <a-form-item label="上级目录" name="parentId">
            <a-tree-select
              show-search
              v-model:value="form.parentId"
              :tree-data="treeOptions"
              :field-names="{ value: 'id', label: 'name', children: 'children' }"
              placeholder="请选择上级"
            />
          </a-form-item>
        </a-col>
        <a-col :span="24">
          <a-form-item :label="effectiveNameLabel" name="name">
            <a-input
              v-model:value="form.name"
              :placeholder="effectiveNamePlaceholder"
            />
          </a-form-item>
        </a-col>
      </a-row>
      <a-col :span="24">
        <a-form-item label="状态" name="validFlag">
          <a-radio-group v-model:value="form.validFlag">
            <a-radio :value="true">启用</a-radio>
            <a-radio :value="false">禁用</a-radio>
          </a-radio-group>
        </a-form-item>
      </a-col>
      <a-row :gutter="20">
        <a-col :span="24">
          <a-form-item label="排序" name="sortOrder">
            <a-input-number
              style="width: 100%"
              v-model:value="form.sortOrder"
              :min="0"
            />
          </a-form-item>
        </a-col>
      </a-row>
      <a-row :gutter="20">
        <a-col :span="24">
          <a-form-item label="描述" name="description">
            <a-textarea
              :maxlength="500"
              show-count
              placeholder="请输入描述"
              v-model:value="form.description"
              style="min-height: 192px"
            />
          </a-form-item>
        </a-col>
      </a-row>
      <a-row :gutter="20">
        <a-col :span="24">
          <a-form-item label="备注" name="remark">
            <a-textarea
              :maxlength="500"
              show-count
              placeholder="请输入备注"
              v-model:value="form.remark"
              style="min-height: 192px"
            />
          </a-form-item>
        </a-col>
      </a-row>
    </a-form>
    <template #footer>
      <div class="dialog-footer">
        <a-button @click="onCancel">取 消</a-button>
        <a-button type="primary" @click="onSubmit">确 定</a-button>
      </div>
    </template>
  </a-modal>
</template>

<script setup>
/**
 * CatEditDialog 组件使用说明
 *
 * 该组件用于目录（Category）的编辑和新增操作。
 * 不再使用 props 传递数据，而是通过 expose 出的 open 方法进行调用。
 *
 * 使用方法：
 * 1. 在父组件中引入并放置组件：
 *    <CatEditDialog ref="catEditDialogRef" @submit="handleDialogSubmit" />
 *
 * 2. 在 script 中定义 ref：
 *    const catEditDialogRef = ref();
 *
 * 3. 调用 open 方法打开弹窗：
 *    catEditDialogRef.value.open({
 *      title: "新增目录",          // 弹窗标题
 *      nameLabel: "目录名称",      // 名称字段的 label
 *      treeOptions: [...],        // 上级目录树形数据
 *      form: { ... },             // 表单初始数据（如果是修改，传入当前行数据；如果是新增，传入默认值或部分预设值）
 *      rules: { ... }             // (可选) 表单校验规则，如果不传则使用默认规则
 *    });
 *
 * 4. 监听 @submit 事件获取结果：
 *    const handleDialogSubmit = (formData) => {
 *      // 调用接口保存 formData
 *      // 保存成功后无需手动关闭弹窗，弹窗会在点击确定且校验通过后自动关闭（或者根据业务需求调整）
 *      // 注意：目前的实现是校验通过后自动关闭弹窗并 emit submit。
 *    };
 */

import { ref, reactive, computed, nextTick } from "vue";

const emit = defineEmits(["submit", "cancel"]);

const visible = ref(false);
const formRef = ref();

// 组件内部状态
const title = ref("");
const nameLabel = ref("目录名称");
const treeOptions = ref([]);
const customRules = ref(null);

// 默认表单数据
const defaultForm = {
  parentId: undefined,
  name: "",
  validFlag: true,
  sortOrder: 0,
  description: "",
  remark: "",
};

const form = ref({ ...defaultForm });

const effectiveNameLabel = computed(() => nameLabel.value);
const effectiveNamePlaceholder = computed(() => `请输入${nameLabel.value}`);

// 默认校验规则
const defaultRules = {
  name: [{ required: true, message: "名称不能为空", trigger: "blur" }],
  parentId: [{ required: true, message: "上级目录不能为空", trigger: "blur" }],
  code: [{ required: true, message: "编码不能为空", trigger: "blur" }],
};

// 计算最终使用的规则，优先使用传入的 customRules
const currentRules = computed(() => {
  if (customRules.value) {
    return customRules.value;
  }
  // 动态更新默认规则中的 message
  const rules = JSON.parse(JSON.stringify(defaultRules));
  if (rules.name && rules.name[0]) {
    rules.name[0].message = `${nameLabel.value}不能为空`;
  }
  return rules;
});

/**
 * 打开弹窗的方法
 * @param {Object} options 配置项
 */
const open = (options = {}) => {
  title.value = options.title || "编辑";
  nameLabel.value = "目录名称";
  treeOptions.value = options.treeOptions || [];
  customRules.value = options.rules || null;

  // 初始化表单数据
  // 如果传入了 form，则合并到 defaultForm 中（深拷贝避免引用问题）
  // 注意：这里假设 options.form 包含了需要回显的数据
  if (options.form) {
    form.value = JSON.parse(
      JSON.stringify({ ...defaultForm, ...options.form })
    );
  } else {
    form.value = JSON.parse(JSON.stringify(defaultForm));
  }

  visible.value = true;

  // 重置校验状态
  nextTick(() => {
    formRef.value?.clearValidate();
  });
};

const onCancel = () => {
  visible.value = false;
  emit("cancel");
};

const onSubmit = () => {
  formRef.value?.validate((valid) => {
    if (valid) {
      emit("submit", JSON.parse(JSON.stringify(form.value)));
      visible.value = false;
    }
  });
};

// 暴露 open 方法给父组件
defineExpose({
  open,
});
</script>
