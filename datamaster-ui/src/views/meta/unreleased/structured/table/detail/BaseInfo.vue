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
            <a-form-item label="表注释" name="tableComment">
              <a-input
                v-model:value="editForm.tableComment"
                placeholder="请输入表注释"
              />
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item label="所属分层" name="belongingLayer">
              <a-select
                v-model:value="editForm.belongingLayer"
                placeholder="请选择所属分层"
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
          <a-col :span="24">
            <a-form-item label="描述" name="description">
              <a-textarea
                v-model:value="editForm.description"
                placeholder="请输入描述"
                :auto-size="{ minRows: 8 }"
                :maxlength="500"
                show-count
              />
            </a-form-item>
          </a-col>
        </a-row>

        <a-row :gutter="2">
          <a-col :span="8">
            <a-form-item label="存储类型" name="storageType">
              <a-input
                v-model:value="editForm.storageType"
                placeholder="请输入存储类型"
              />
            </a-form-item>
          </a-col>
        </a-row>

        <a-row :gutter="2">
          <a-col :span="8">
            <a-form-item label="是否主表" name="masterFlag">
              <a-radio-group v-model:value="editForm.masterFlag">
                <a-radio
                  v-for="dict in toValue(dicts.table_yes_no)"
                  :key="dict.value"
                  :value="dict.value"
                >
                  {{ dict.label }}
                </a-radio>
              </a-radio-group>
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item label="是否临时表" name="tempFlag">
              <a-radio-group v-model:value="editForm.tempFlag">
                <a-radio
                  v-for="dict in toValue(dicts.table_yes_no)"
                  :key="dict.value"
                  :value="dict.value"
                >
                  {{ dict.label }}
                </a-radio>
              </a-radio-group>
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
          <a-col :span="24">
            <div class="infotop-row border-top">
              <div class="infotop-row-lable">描述</div>
              <div class="infotop-row-value">
                {{ getFormatValue(detail.description) }}
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
              {{ getFormatValue(detail.dbName) }}
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
            <div class="infotop-row-lable">存储类型</div>
            <div class="infotop-row-value">
              {{ getFormatValue(detail.storageType) }}
            </div>
          </div>
        </a-col>

        <a-col :span="8">
          <div class="infotop-row border-top">
            <div class="infotop-row-lable">存储大小</div>
            <div class="infotop-row-value">
              {{ getFormatValue(detail.storageSize) }}
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
            <div class="infotop-row-lable">是否主表</div>
            <div class="infotop-row-value">
              <dict-tag
                :options="toValue(dicts.table_yes_no)"
                :value="detail.masterFlag"
              />
            </div>
          </div>
        </a-col>

        <a-col :span="8">
          <div class="infotop-row border-top">
            <div class="infotop-row-lable">是否临时表</div>
            <div class="infotop-row-value">
              <dict-tag
                :options="toValue(dicts.table_yes_no)"
                :value="detail.tempFlag"
              />
            </div>
          </div>
        </a-col>
      </a-row>
    </div>
  </div>
</template>

<script setup name="SynchronizeTaskBaseInfo">
import { getCurrentInstance, toValue, ref, reactive, watch } from "vue";
import { updateTable } from "@/api/cat/unreleased/table";

const { proxy } = getCurrentInstance();
const dicts = proxy.useDict(
  "datasource_type",
  "meta_dw_layers",
  "table_yes_no"
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
  dbId: null,
  tableName: null,
  tableComment: null,
  belongingLayer: null,
  belongingSystem: null,
  description: null,
  storageType: null,
  techLeader: null,
  techLeaderPhone: null,
  businessLeader: null,
  businessLeaderPhone: null,
  masterFlag: "1",
  tempFlag: "0",
  remark: null,
});

const rules = {
  storageType: [{ trigger: ["blur", "change"] }],
};

// 初始化编辑表单
function initEditForm() {
  if (!props.detail) return;

  editForm.id = props.detail.id;
  editForm.dbId = props.detail.dbId;
  editForm.tableName = props.detail.tableName;
  editForm.tableComment = props.detail.tableComment;
  editForm.belongingLayer = props.detail.dbRespVO?.belongingLayer;
  editForm.belongingSystem = props.detail.dbRespVO?.belongingSystem;
  editForm.description = props.detail.description;
  editForm.storageType = props.detail.storageType;
  editForm.techLeader = props.detail.techLeader;
  editForm.techLeaderPhone = props.detail.techLeaderPhone;
  editForm.businessLeader = props.detail.businessLeader;
  editForm.businessLeaderPhone = props.detail.businessLeaderPhone;
  editForm.masterFlag = props.detail.masterFlag || "1";
  editForm.tempFlag = props.detail.tempFlag || "0";
  editForm.remark = props.detail.remark;
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
    await updateTable(editForm);
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
