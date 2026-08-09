<template>
  <div class="base-info">
    <div class="module-head">
      <span>基础信息</span>
      <a-button
        v-if="!isEditing"
        type="primary"
        link
        @click="handleEdit"
      >
        编辑
      </a-button>
      <template v-else>
        <a-button type="primary" link @click="handleSave" :loading="saving">
          保存
        </a-button>
        <a-button type="primary" link @click="handleCancel">
          取消
        </a-button>
      </template>
    </div>
    <div class="module-body infotop">
      <a-form
        v-if="isEditing"
        :model="editForm"
        :rules="rules"
        ref="formRef"
        :label-col="{ style: { width: '110px' } }"
      >
        <a-row :gutter="2">
          <a-col :span="8">
            <a-form-item label="所属分层" name="belongingLayer">
              <a-select
                v-model:value="editForm.belongingLayer"
                placeholder="请选择所属分层"
                disabled
              >
                <a-select-option
                  v-for="dict in toValue(dicts.meta_dw_layers)"
                  :key="dict.value"
                  :value="dict.value"
                >
                  {{ dict.label }}
                </a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item label="所属系统" name="belongingSystem">
              <a-input
                v-model:value="editForm.belongingSystem"
                placeholder="请输入所属系统"
                disabled
              />
            </a-form-item>
          </a-col>
        </a-row>

        <a-row :gutter="2">
          <a-col :span="8">
            <a-form-item label="字段注释" name="columnComment">
              <a-input
                v-model:value="editForm.columnComment"
                placeholder="请输入字段注释"
              />
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item label="字段类型" name="columnType">
              <a-input
                v-model:value="editForm.columnType"
                placeholder="字段类型"
                disabled
              />
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item label="字段长度" name="columnLength">
              <a-input
                v-model:value="editForm.columnLength"
                placeholder="字段长度"
                disabled
              />
            </a-form-item>
          </a-col>
        </a-row>

        <a-row :gutter="2">
          <a-col :span="8">
            <a-form-item label="字段精度" name="columnPrecision">
              <a-input
                v-model:value="editForm.columnPrecision"
                placeholder="字段精度"
                disabled
              />
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item label="字段小数位" name="columnScale">
              <a-input
                v-model:value="editForm.columnScale"
                placeholder="字段小数位"
                disabled
              />
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item label="是否必填" name="nullableFlag">
              <a-select
                v-model:value="editForm.nullableFlag"
                placeholder="请选择"
                disabled
              >
                <a-select-option
                  v-for="dict in toValue(dicts.table_yes_no)"
                  :key="dict.value"
                  :value="dict.value"
                >
                  {{ dict.label }}
                </a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
        </a-row>

        <a-row :gutter="2">
          <a-col :span="8">
            <a-form-item label="默认值" name="defaultValue">
              <a-input
                v-model:value="editForm.defaultValue"
                placeholder="默认值"
                disabled
              />
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item label="是否主键" name="pkFlag">
              <a-select
                v-model:value="editForm.pkFlag"
                placeholder="请选择"
                disabled
              >
                <a-select-option
                  v-for="dict in toValue(dicts.table_yes_no)"
                  :key="dict.value"
                  :value="dict.value"
                >
                  {{ dict.label }}
                </a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item label="是否外键" name="fkFlag">
              <a-select
                v-model:value="editForm.fkFlag"
                placeholder="请选择"
                disabled
              >
                <a-select-option
                  v-for="dict in toValue(dicts.table_yes_no)"
                  :key="dict.value"
                  :value="dict.value"
                >
                  {{ dict.label }}
                </a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
        </a-row>

        <a-row :gutter="2">
          <a-col :span="24">
            <a-form-item label="字段描述" name="description">
              <a-textarea
                v-model:value="editForm.description"
                placeholder="请输入字段描述"
                :auto-size="{ minRows: 8 }"
                :maxlength="500"
                show-count
              />
            </a-form-item>
          </a-col>
        </a-row>

        <a-row :gutter="2">
          <a-col :span="24">
            <a-form-item label="备注" name="remark">
              <a-textarea
                v-model:value="editForm.remark"
                placeholder="请输入备注"
                :auto-size="{ minRows: 8 }"
                :maxlength="500"
                show-count
              />
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>

      <template v-else>
        <a-row :gutter="2">
          <a-col :span="8">
            <div class="infotop-row border-top">
              <div class="infotop-row-lable">所属分层</div>
              <div class="infotop-row-value">
                <dict-tag
                  :options="toValue(dicts.meta_dw_layers)"
                  :value="detail.dbRespVO?.belongingLayer"
                />
              </div>
            </div>
          </a-col>

          <a-col :span="8">
            <div class="infotop-row border-top">
              <div class="infotop-row-lable">所属系统</div>
              <div class="infotop-row-value">
                {{ getFormatValue(detail.dbRespVO?.belongingSystem) }}
              </div>
            </div>
          </a-col>
        </a-row>

        <a-row :gutter="2">
          <a-col :span="8">
            <div class="infotop-row border-top">
              <div class="infotop-row-lable">更新人</div>
              <div class="infotop-row-value">
                {{ getFormatValue(detail.updateBy) }}
              </div>
            </div>
          </a-col>

          <a-col :span="8">
            <div class="infotop-row border-top">
              <div class="infotop-row-lable">更新时间</div>
              <div class="infotop-row-value">
                {{ getFormatValue(detail.updateTime) }}
              </div>
            </div>
          </a-col>

          <a-col :span="8">
            <div class="infotop-row border-top">
              <div class="infotop-row-lable">创建人</div>
              <div class="infotop-row-value">
                {{ getFormatValue(detail.createBy) }}
              </div>
            </div>
          </a-col>
        </a-row>

        <a-row :gutter="2">
          <a-col :span="8">
            <div class="infotop-row border-top">
              <div class="infotop-row-lable">创建时间</div>
              <div class="infotop-row-value">
                {{ getFormatValue(detail.createTime) }}
              </div>
            </div>
          </a-col>
        </a-row>

        <a-row :gutter="2">
          <a-col :span="24">
            <div class="infotop-row border-top">
              <div class="infotop-row-lable">备注</div>
              <div class="infotop-row-value">
                {{ getFormatValue(detail.remark) }}
              </div>
            </div>
          </a-col>
        </a-row>
      </template>
    </div>

    <div class="module-head">技术信息</div>
    <div class="module-body infotop">
      <a-row :gutter="2">
        <a-col :span="8">
          <div class="infotop-row border-top">
            <div class="infotop-row-lable">数据源名称</div>
            <div class="infotop-row-value">
              {{ getFormatValue(detail.dbRespVO?.dbName) }}
            </div>
          </div>
        </a-col>

        <a-col :span="8">
          <div class="infotop-row border-top">
            <div class="infotop-row-lable">IP</div>
            <div class="infotop-row-value">
              {{ getFormatValue(detail.dbRespVO?.ip) }}
            </div>
          </div>
        </a-col>

        <a-col :span="8">
          <div class="infotop-row border-top">
            <div class="infotop-row-lable">端口号</div>
            <div class="infotop-row-value">
              {{ getFormatValue(detail.dbRespVO?.port) }}
            </div>
          </div>
        </a-col>
      </a-row>

      <a-row :gutter="2">
        <a-col :span="8">
          <div class="infotop-row border-top">
            <div class="infotop-row-lable">账号</div>
            <div class="infotop-row-value">
              {{ getFormatValue(detail.username) }}
            </div>
          </div>
        </a-col>

        <a-col :span="8">
          <div class="infotop-row border-top">
            <div class="infotop-row-lable">字段类型</div>
            <div class="infotop-row-value">
              <dict-tag
                :options="toValue(dicts.column_type)"
                :value="detail.columnType"
              />
            </div>
          </div>
        </a-col>

        <a-col :span="8">
          <div class="infotop-row border-top">
            <div class="infotop-row-lable">字段长度</div>
            <div class="infotop-row-value">
              {{ getFormatValue(detail.columnLength) }}
            </div>
          </div>
        </a-col>
      </a-row>

      <a-row :gutter="2">
        <a-col :span="8">
          <div class="infotop-row border-top">
            <div class="infotop-row-lable">字段精度</div>
            <div class="infotop-row-value">
              {{ getFormatValue(detail.columnPrecision) }}
            </div>
          </div>
        </a-col>

        <a-col :span="8">
          <div class="infotop-row border-top">
            <div class="infotop-row-lable">字段小数位</div>
            <div class="infotop-row-value">
              {{ getFormatValue(detail.columnScale) }}
            </div>
          </div>
        </a-col>

        <a-col :span="8">
          <div class="infotop-row border-top">
            <div class="infotop-row-lable">是否必填</div>
            <div class="infotop-row-value">
              <dict-tag
                :options="toValue(dicts.table_yes_no)"
                :value="detail.nullableFlag"
              />
            </div>
          </div>
        </a-col>
      </a-row>

      <a-row :gutter="2">
        <a-col :span="8">
          <div class="infotop-row border-top">
            <div class="infotop-row-lable">默认值</div>
            <div class="infotop-row-value">
              {{ getFormatValue(detail.defaultValue) }}
            </div>
          </div>
        </a-col>

        <a-col :span="8">
          <div class="infotop-row border-top">
            <div class="infotop-row-lable">是否主键</div>
            <div class="infotop-row-value">
              <dict-tag
                :options="toValue(dicts.table_yes_no)"
                :value="detail.pkFlag"
              />
            </div>
          </div>
        </a-col>

        <a-col :span="8">
          <div class="infotop-row border-top">
            <div class="infotop-row-lable">是否外键</div>
            <div class="infotop-row-value">
              <dict-tag
                :options="toValue(dicts.table_yes_no)"
                :value="detail.fkFlag"
              />
            </div>
          </div>
        </a-col>
      </a-row>

      <a-row :gutter="2">
        <a-col :span="24">
          <div class="infotop-row border-top">
            <div class="infotop-row-lable">字段描述</div>
            <div class="infotop-row-value">
              {{ getFormatValue(detail.description) }}
            </div>
          </div>
        </a-col>
      </a-row>
    </div>

    <div class="module-head">业务信息</div>
    <div class="module-body infotop">
      <a-row :gutter="2">
        <a-col :span="8">
          <div class="infotop-row border-top">
            <div class="infotop-row-lable">标准数据元</div>
            <div class="infotop-row-value">
              {{ getFormatValue(detail.dataElemName) }}
            </div>
          </div>
        </a-col>
      </a-row>
    </div>
  </div>
</template>

<script setup name="SynchronizeTaskBaseInfo">
import { getCurrentInstance, toValue, ref, reactive, watch } from "vue";
import { updateColumn } from "@/api/cat/unreleased/column";

const { proxy } = getCurrentInstance();
const dicts = proxy.useDict(
  "datasource_type",
  "meta_dw_layers",
  "table_yes_no",
  "column_type"
);

const props = defineProps({
  detail: {
    type: Object,
    required: true,
  },
  type: {
    type: String,
    default: "",
  },
});

const emit = defineEmits(["update:detail"]);

const isEditing = ref(false);
const saving = ref(false);
const formRef = ref(null);

const editForm = reactive({
  id: null,
  tableId: null,
  dbId: null,
  datasourceId: null,
  columnName: null,
  columnComment: null,
  columnType: null,
  columnLength: null,
  columnPrecision: null,
  columnScale: null,
  defaultValue: null,
  pkFlag: null,
  fkFlag: null,
  nullableFlag: null,
  description: null,
  remark: null,
  belongingLayer: null,
  belongingSystem: null,
});

const rules = {
  columnComment: [{ trigger: ["blur", "change"] }],
  description: [{ trigger: ["blur", "change"] }],
  remark: [{ trigger: ["blur", "change"] }],
};

// 初始化编辑表单
function initEditForm() {
  if (!props.detail) return;

  editForm.id = props.detail.id;
  editForm.tableId = props.detail.tableId;
  editForm.dbId = props.detail.dbId;
  editForm.datasourceId = props.detail.datasourceId;
  editForm.columnName = props.detail.columnName;
  editForm.columnComment = props.detail.columnComment;
  editForm.columnType = props.detail.columnType;
  editForm.columnLength = props.detail.columnLength;
  editForm.columnPrecision = props.detail.columnPrecision;
  editForm.columnScale = props.detail.columnScale;
  editForm.defaultValue = props.detail.defaultValue;
  editForm.pkFlag = props.detail.pkFlag;
  editForm.fkFlag = props.detail.fkFlag;
  editForm.nullableFlag = props.detail.nullableFlag;
  editForm.description = props.detail.description;
  editForm.remark = props.detail.remark;
  editForm.belongingLayer = props.detail.dbRespVO?.belongingLayer;
  editForm.belongingSystem = props.detail.dbRespVO?.belongingSystem;
}

// 点击编辑
function handleEdit() {
  initEditForm();
  isEditing.value = true;
}

// 取消编辑
function handleCancel() {
  isEditing.value = false;
}

// 保存
async function handleSave() {
  if (formRef.value) {
    const valid = await formRef.value.validate().catch(() => false);
    if (!valid) return;
  }

  saving.value = true;
  try {
    await updateColumn(editForm);
    proxy.$modal.msgSuccess("修改成功！");
    isEditing.value = false;
    // 触发父组件刷新数据
    emit("update:detail");
  } catch (error) {
    console.error("保存失败:", error);
  } finally {
    saving.value = false;
  }
}

// 监听 detail 变化，更新编辑表单
watch(
  () => props.detail,
  () => {
    if (isEditing.value) {
      initEditForm();
    }
  },
  { deep: true }
);
</script>

<style lang="scss" scoped>
.module-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
</style>
