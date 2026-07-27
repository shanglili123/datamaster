<template>
  <div class="base-info">
    <div class="module-head">
      <span>基础信息</span>
      <el-button
        v-if="!isEditing"
        type="primary"
        link
        @click="handleEdit"
      >
        编辑
      </el-button>
      <template v-else>
        <el-button type="primary" link @click="handleSave" :loading="saving">
          保存
        </el-button>
        <el-button type="primary" link @click="handleCancel">
          取消
        </el-button>
      </template>
    </div>
    <div class="module-body infotop">
      <el-form
        v-if="isEditing"
        :model="editForm"
        :rules="rules"
        ref="formRef"
        label-width="110"
      >
        <el-row :gutter="2">
          <el-col :span="8">
            <el-form-item label="表注释" prop="tableComment">
              <el-input
                v-model="editForm.tableComment"
                placeholder="请输入表注释"
              />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="所属分层" prop="belongingLayer">
              <el-select
                v-model="editForm.belongingLayer"
                placeholder="请选择所属分层"
              >
                <el-option
                  v-for="dict in toValue(dicts.meta_dw_layers)"
                  :key="dict.value"
                  :label="dict.label"
                  :value="dict.value"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="所属系统" prop="belongingSystem">
              <el-input
                v-model="editForm.belongingSystem"
                placeholder="请输入所属系统"
                disabled
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="2">
          <el-col :span="24">
            <el-form-item label="描述" prop="description">
              <el-input
                v-model="editForm.description"
                type="textarea"
                placeholder="请输入描述"
                :min-height="192"
                show-word-limit
                maxlength="500"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="2">
          <el-col :span="8">
            <el-form-item label="存储类型" prop="storageType">
              <el-input
                v-model="editForm.storageType"
                placeholder="请输入存储类型"
              />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="技术负责人" prop="techLeader">
              <el-tree-select
                v-model="editForm.techLeader"
                :data="userList"
                :props="{
                  value: 'userId',
                  label: 'nickName',
                  children: 'children',
                }"
                value-key="userId"
                placeholder="请选择技术负责人"
                check-strictly
                @change="handleTechLeaderChange"
              />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="技术负责人电话" prop="techLeaderPhone">
              <el-input
                v-model="editForm.techLeaderPhone"
                placeholder="请输入技术负责人电话"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="2">
          <el-col :span="8">
            <el-form-item label="是否主表" prop="masterFlag">
              <el-radio-group v-model="editForm.masterFlag">
                <el-radio
                  v-for="dict in toValue(dicts.table_yes_no)"
                  :key="dict.value"
                  :value="dict.value"
                >
                  {{ dict.label }}
                </el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="是否临时表" prop="tempFlag">
              <el-radio-group v-model="editForm.tempFlag">
                <el-radio
                  v-for="dict in toValue(dicts.table_yes_no)"
                  :key="dict.value"
                  :value="dict.value"
                >
                  {{ dict.label }}
                </el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="业务负责人" prop="businessLeader">
              <el-tree-select
                v-model="editForm.businessLeader"
                :data="userList"
                :props="{
                  value: 'userId',
                  label: 'nickName',
                  children: 'children',
                }"
                value-key="userId"
                placeholder="请选择业务负责人"
                check-strictly
                @change="handleBusinessLeaderChange"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="2">
          <el-col :span="8">
            <el-form-item label="业务负责人电话" prop="businessLeaderPhone">
              <el-input
                v-model="editForm.businessLeaderPhone"
                placeholder="请输入业务负责人电话"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="2">
          <el-col :span="24">
            <el-form-item label="备注" prop="remark">
              <el-input
                v-model="editForm.remark"
                type="textarea"
                placeholder="请输入备注"
                :min-height="192"
                show-word-limit
                maxlength="500"
              />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>

      <template v-else>
        <el-row :gutter="2">
          <el-col :span="8">
            <div class="infotop-row border-top">
              <div class="infotop-row-lable">所属分层</div>
              <div class="infotop-row-value">
                <dict-tag
                  :options="toValue(dicts.meta_dw_layers)"
                  :value="detail.dbRespVO?.belongingLayer"
                />
              </div>
            </div>
          </el-col>

          <el-col :span="8">
            <div class="infotop-row border-top">
              <div class="infotop-row-lable">所属系统</div>
              <div class="infotop-row-value">
                {{ getFormatValue(detail.dbRespVO?.belongingSystem) }}
              </div>
            </div>
          </el-col>
        </el-row>

        <el-row :gutter="2">
          <el-col :span="24">
            <div class="infotop-row border-top">
              <div class="infotop-row-lable">描述</div>
              <div class="infotop-row-value">
                {{ getFormatValue(detail.description) }}
              </div>
            </div>
          </el-col>
        </el-row>

        <el-row :gutter="2">
          <el-col :span="8">
            <div class="infotop-row border-top">
              <div class="infotop-row-lable">更新人</div>
              <div class="infotop-row-value">
                {{ getFormatValue(detail.updateBy) }}
              </div>
            </div>
          </el-col>

          <el-col :span="8">
            <div class="infotop-row border-top">
              <div class="infotop-row-lable">更新时间</div>
              <div class="infotop-row-value">
                {{ getFormatValue(detail.updateTime) }}
              </div>
            </div>
          </el-col>

          <el-col :span="8">
            <div class="infotop-row border-top">
              <div class="infotop-row-lable">创建人</div>
              <div class="infotop-row-value">
                {{ getFormatValue(detail.createBy) }}
              </div>
            </div>
          </el-col>
        </el-row>

        <el-row :gutter="2">
          <el-col :span="8">
            <div class="infotop-row border-top">
              <div class="infotop-row-lable">创建时间</div>
              <div class="infotop-row-value">
                {{ getFormatValue(detail.createTime) }}
              </div>
            </div>
          </el-col>
        </el-row>

        <el-row :gutter="2">
          <el-col :span="24">
            <div class="infotop-row border-top">
              <div class="infotop-row-lable">备注</div>
              <div class="infotop-row-value">
                {{ getFormatValue(detail.remark) }}
              </div>
            </div>
          </el-col>
        </el-row>
      </template>
    </div>

    <div class="module-head">技术信息</div>
    <div class="module-body infotop">
      <el-row :gutter="2">
        <el-col :span="8">
          <div class="infotop-row border-top">
            <div class="infotop-row-lable">数据源名称</div>
            <div class="infotop-row-value">
              {{ getFormatValue(detail.dbName) }}
            </div>
          </div>
        </el-col>

        <el-col :span="8">
          <div class="infotop-row border-top">
            <div class="infotop-row-lable">IP</div>
            <div class="infotop-row-value">
              {{ getFormatValue(detail.dbRespVO?.ip) }}
            </div>
          </div>
        </el-col>

        <el-col :span="8">
          <div class="infotop-row border-top">
            <div class="infotop-row-lable">端口号</div>
            <div class="infotop-row-value">
              {{ getFormatValue(detail.dbRespVO?.port) }}
            </div>
          </div>
        </el-col>
      </el-row>

      <el-row :gutter="2">
        <el-col :span="8">
          <div class="infotop-row border-top">
            <div class="infotop-row-lable">账号</div>
            <div class="infotop-row-value">
              {{ getFormatValue(detail.username) }}
            </div>
          </div>
        </el-col>

        <el-col :span="8">
          <div class="infotop-row border-top">
            <div class="infotop-row-lable">存储类型</div>
            <div class="infotop-row-value">
              {{ getFormatValue(detail.storageType) }}
            </div>
          </div>
        </el-col>

        <el-col :span="8">
          <div class="infotop-row border-top">
            <div class="infotop-row-lable">存储大小</div>
            <div class="infotop-row-value">
              {{ getFormatValue(detail.storageSize) }}
            </div>
          </div>
        </el-col>
      </el-row>

      <el-row :gutter="2">
        <el-col :span="8">
          <div class="infotop-row border-top">
            <div class="infotop-row-lable">技术负责人</div>
            <div class="infotop-row-value">
              {{ getFormatValue(detail.techLeaderName) }}
            </div>
          </div>
        </el-col>

        <el-col :span="8">
          <div class="infotop-row border-top">
            <div class="infotop-row-lable">技术负责人电话</div>
            <div class="infotop-row-value">
              {{ getFormatValue(detail.techLeaderPhone) }}
            </div>
          </div>
        </el-col>
      </el-row>
    </div>

    <div class="module-head">业务信息</div>
    <div class="module-body infotop">
      <el-row :gutter="2">
        <el-col :span="8">
          <div class="infotop-row border-top">
            <div class="infotop-row-lable">是否主表</div>
            <div class="infotop-row-value">
              <dict-tag
                :options="toValue(dicts.table_yes_no)"
                :value="detail.masterFlag"
              />
            </div>
          </div>
        </el-col>

        <el-col :span="8">
          <div class="infotop-row border-top">
            <div class="infotop-row-lable">是否临时表</div>
            <div class="infotop-row-value">
              <dict-tag
                :options="toValue(dicts.table_yes_no)"
                :value="detail.tempFlag"
              />
            </div>
          </div>
        </el-col>
        <el-col :span="8">
          <div class="infotop-row border-top">
            <div class="infotop-row-lable">业务负责人</div>
            <div class="infotop-row-value">
              {{ getFormatValue(detail.businessLeaderName) }}
            </div>
          </div>
        </el-col>
      </el-row>

      <el-row :gutter="2">
        <el-col :span="8">
          <div class="infotop-row border-top">
            <div class="infotop-row-lable">业务负责人电话</div>
            <div class="infotop-row-value">
              {{ getFormatValue(detail.businessLeaderPhone) }}
            </div>
          </div>
        </el-col>
      </el-row>
    </div>
  </div>
</template>

<script setup name="SynchronizeTaskBaseInfo">
import { getCurrentInstance, toValue, ref, reactive, watch } from "vue";
import { updateTable } from "@/api/cat/unreleased/table";
import { deptUserTree } from "@/api/system/system/user.js";

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
const userList = ref([]);

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
  techLeaderPhone: [{ trigger: ["blur", "change"] }],
  businessLeaderPhone: [{ trigger: ["blur", "change"] }],
};

// 获取用户列表
async function getUserList() {
  try {
    const res = await deptUserTree();
    userList.value = res.data || [];
  } catch (error) {
    console.error("获取用户列表失败:", error);
  }
}

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

// 技术负责人变化
function handleTechLeaderChange(userId) {
  const user = userList.value.find((item) => item.userId === userId);
  if (user) {
    editForm.techLeaderPhone = user.phonenumber;
  }
}

// 业务负责人变化
function handleBusinessLeaderChange(userId) {
  const user = userList.value.find((item) => item.userId === userId);
  if (user) {
    editForm.businessLeaderPhone = user.phonenumber;
  }
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

// 初始化
getUserList();
</script>

<style lang="scss" scoped>
.module-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
</style>
