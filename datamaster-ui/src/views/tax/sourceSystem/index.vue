<template>
  <div class="app-container" ref="app-container">
    <qt-wrap :columns="tableStore.columns" :tableRef="tableRef">
      <template #search>
        <qt-search-bar
          v-bind="searchStore"
          :params="tableStore.params"
          @query="handleQueryClick"
          @reset="handleResetQueryClick"
        />
      </template>
      <template #actions-data>
        <a-button type="primary" @click="handleAdd">
          新增
        </a-button>
        <a-button
          danger
          :disabled="!store.rows.length"
          @click="handleDelete"
        >
          删除
        </a-button>
      </template>
      <qt-table v-bind="tableStore" ref="tableRef">
        <template #type="scope">
          <dict-tag :options="sys_source_system_type" :value="scope.row.type" />
        </template>
        <template #validFlag="scope">
          <a-switch
            v-model:checked="scope.row.validFlag"
            @change="() => handleStatusChange(scope.row)"
          />
        </template>
        <template #responsiblePerson="scope">
          {{ getUserLabel(scope.row.responsiblePerson) }}
        </template>
        <template #contactPerson="scope">
          {{ getUserLabel(scope.row.contactPerson) }}
        </template>
        <template #handle="{ row }">
          <a-button type="link" size="small" @click="handleUpdate(row)"
            >修改</a-button
          >
          <a-button
            type="link"
            danger
            size="small"
            @click="handleDelete(row)"
            :disabled="row.validFlag"
            >删除</a-button
          >
          <a-button type="link" size="small" @click="handleDetail(row)"
            >详情</a-button
          >
        </template>
      </qt-table>
    </qt-wrap>

    <!-- 添加或修改来源系统对话框 -->
    <a-modal
      :title="title"
      v-model:open="open"
      width="800px"
      draggable
      destroy-on-close
    >
      <a-form
        ref="sourceSystemRef"
        :model="form"
        :rules="rules"
        :label-col="{ style: { width: '80px' } }"
        @submit.prevent
      >
        <a-form-item label="系统名称" name="name">
          <a-input v-model:value="form.name" placeholder="请输入系统名称" />
        </a-form-item>

        <a-form-item label="系统类型" name="type">
          <a-select v-model:value="form.type" placeholder="请选择系统类型" allow-clear>
            <a-select-option
              v-for="dict in sys_source_system_type"
              :key="dict.value"
              :value="dict.value"
              >{{ dict.label }}</a-select-option
            >
          </a-select>
        </a-form-item>

        <a-form-item label="对接人" name="contactPerson">
          <a-select
            v-model:value="form.contactPerson"
            show-search
            allow-clear
            option-filter-prop="label"
            placeholder="请选择对接人"
          >
            <a-select-option
              v-for="item in userOptions"
              :key="item.value"
              :value="item.value"
              :label="item.label"
              >{{ item.label }}</a-select-option
            >
          </a-select>
        </a-form-item>
        <a-form-item label="排序" name="sortOrder">
          <a-input-number
            style="width: 100%"
            v-model:value="form.sortOrder"
            :min="0"
          />
        </a-form-item>
        <a-form-item label="状态" name="validFlag">
          <a-radio-group v-model:value="form.validFlag">
            <a-radio :value="false">禁用</a-radio>
            <a-radio :value="true">启用</a-radio>
          </a-radio-group>
        </a-form-item>
        <a-form-item label="描述" name="description">
          <a-textarea
            :maxlength="500"
            show-count
            v-model:value="form.description"
            placeholder="请输入描述"
            :auto-size="{ minRows: 2, maxRows: 4 }"
          />
        </a-form-item>

        <a-form-item label="备注" name="remark">
          <a-textarea
            :maxlength="500"
            show-count
            v-model:value="form.remark"
            placeholder="请输入备注"
            :auto-size="{ minRows: 2, maxRows: 4 }"
          />
        </a-form-item>
      </a-form>
      <template #footer>
        <div class="dialog-footer">
          <a-button @click="cancel">取 消</a-button>
          <a-button type="primary" @click="submitForm"
            >确 定</a-button
          >
        </div>
      </template>
    </a-modal>

    <!-- 来源系统详情对话框 -->
    <a-modal
      :title="title"
      v-model:open="openDetail"
      draggable
      destroy-on-close
    >
      <a-form
        ref="sourceSystemRef"
        :model="form"
        :label-col="{ style: { width: '90px' } }"
        class="column-form"
      >
        <a-form-item label="编号" name="id">
          <div class="form-readonly">
            {{ form.id }}
          </div>
        </a-form-item>
        <a-form-item label="系统名称" name="name">
          <div class="form-readonly">
            {{ form.name }}
          </div>
        </a-form-item>
        <a-form-item label="系统类型" name="type">
          <div class="form-readonly">
            {{ getDictLabel(sys_source_system_type, form.type) }}
          </div>
        </a-form-item>

        <a-form-item label="对接人" name="contactPersonName">
          <div class="form-readonly">
            {{ form.contactPersonName }}
          </div>
        </a-form-item>
        <a-form-item label="排序" name="sortOrder">
          <div class="form-readonly">
            {{ form.sortOrder }}
          </div>
        </a-form-item>
        <a-form-item label="状态" name="validFlag">
          <div class="form-readonly">
            {{ form.validFlag ? "启用" : "禁用" }}
          </div>
        </a-form-item>
        <a-form-item label="描述" name="description" class="row-full">
          <div class="form-readonly textarea">
            {{ form.description ?? "-" }}
          </div>
        </a-form-item>
        <a-form-item label="备注" name="remark" class="row-full">
          <div class="form-readonly textarea">
            {{ form.remark ?? "-" }}
          </div>
        </a-form-item>

        <a-form-item label="创建人" name="createBy">
          <div class="form-readonly">
            {{ form.createBy }}
          </div>
        </a-form-item>

        <a-form-item label="创建时间" name="createTime">
          <div class="form-readonly">
            {{ parseTime(form.createTime, "{y}-{m}-{d} {h}:{i}") || "-" }}
          </div>
        </a-form-item>

        <a-form-item label="更新人" name="updateBy">
          <div class="form-readonly">
            {{ form.updateBy }}
          </div>
        </a-form-item>

        <a-form-item label="更新时间" name="updateTime">
          <div class="form-readonly">
            {{ parseTime(form.updateTime, "{y}-{m}-{d} {h}:{i}") || "-" }}
          </div>
        </a-form-item>
      </a-form>
      <template #footer>
        <div class="dialog-footer">
          <a-button @click="openDetail = false">关闭 </a-button>
        </div>
      </template>
    </a-modal>

    <DeleteConfirmDialog
      ref="deleteConfirmDialog"
      @confirm-delete="handleDelete"
    />
  </div>
</template>

<script setup name="SourceSystem">
import {
  listSourceSystem,
  getSourceSystem,
  delSourceSystem,
  addSourceSystem,
  updateSourceSystem,
} from "@/api/tax/sourceSystem/sourceSystem.js";
import { deptUserTree } from "@/api/system/system/user.js";
import { getToken } from "@/utils/auth.js";
import DeleteConfirmDialog from "@/components/DeleteConfirmDialog";
import { reactive, ref, toRefs, getCurrentInstance, onMounted } from "vue";

const { proxy } = getCurrentInstance();
const { sys_source_system_type } = proxy.useDict("sys_source_system_type");

// 获取用户列表选项
const userOptions = ref([]);

// 加载用户列表
function loadUserOptions() {
  deptUserTree().then((response) => {
    const options = response.data.map((item) => ({
      label: item.nickName,
      value: item.userId,
      ...item,
    }));
    userOptions.value = options;
    const responsiblePersonItem = searchStore.items.find(
      (i) => i.prop === "responsiblePerson"
    );
    if (responsiblePersonItem?.component) {
      responsiblePersonItem.component.options = options;
    }

    const contactPersonItem = searchStore.items.find(
      (i) => i.prop === "contactPerson"
    );
    if (contactPersonItem?.component) {
      contactPersonItem.component.options = options;
    }
  });
}

// 根据用户ID获取用户名
function getUserLabel(userId) {
  if (!userId) return "-";
  const user = userOptions.value.find((u) => u.value === userId);
  return user ? user.label : userId;
}

// 根据字典值获取标签
function getDictLabel(dictOptions, value) {
  if (!value) return "-";
  const dict = dictOptions.find((d) => d.value === value);
  return dict ? dict.label : value;
}

const store = reactive({
  rows: [],
});

const tableRef = ref(null);
const tableStore = reactive({
  config: {
    sort: true,
    table: {
      stripe: true,
      rowKey: "id",
      defaultSort: { prop: "create_time", order: "desc" },
      onSelectionChange: function (rows) {
        store.rows = rows;
      },
    },
  },
  columns: [
    {
      type: "selection",
      width: 55,
    },
    { label: "编号", prop: "id", width: 60, sortable: true },
    { label: "系统名称", prop: "name", align: "left", width: 150 },
    {
      label: "系统类型",
      prop: "type",
      width: 120,
      slot: "type",
    },
    {
      label: "描述",
      prop: "description",
      align: "left",
      width: 200,
      showOverflowTooltip: true,
    },
    {
      label: "状态",
      prop: "validFlag",
      width: 80,
      slot: "validFlag",
    },
    {
      label: "排序",
      prop: "sortOrder",
      sortableKey: "sort_order",
      width: 80,
      sortable: true,
    },
    {
      label: "对接人",
      prop: "contactPersonName",
    },
    { label: "创建人", prop: "createBy", width: 120 },
    {
      label: "创建时间",
      prop: "createTime",
      width: 150,
      sortable: true,
      sortableKey: "create_time",
      date: true,
    },
    { label: "操作", width: 240, fixed: "right", slot: "handle" },
  ],
  func: listSourceSystem,
  params: {},
  events: {},
});

const searchStore = reactive({
  items: [
    {
      label: "系统名称",
      prop: "name",
      align: "left",
      component: { is: "input", placeholder: "请输入系统名称" },
    },
    {
      label: "系统类型",
      prop: "type",
      component: {
        is: "select",
        placeholder: "请选择系统类型",
        options: sys_source_system_type,
      },
    },
    {
      label: "状态",
      prop: "validFlag",
      component: {
        is: "select",
        placeholder: "请选择状态",
        options: [
          { value: true, label: "启用" },
          { value: false, label: "禁用" },
        ],
      },
    },
    {
      label: "对接人",
      prop: "contactPerson",
      component: {
        is: "select",
        placeholder: "请选择对接人",
        options: [],
      },
    },
  ],
});

const open = ref(false);
const openDetail = ref(false);
const title = ref("");

const data = reactive({
  form: {},
  rules: {
    name: [{ required: true, message: "系统名称不能为空", trigger: "blur" }],
    type: [{ required: true, message: "系统类型不能为空", trigger: "blur" }],
  },
});

const { form, rules } = toRefs(data);

// 点击查询
function handleQueryClick() {
  tableRef.value.getList();
}

// 重置查询
function handleResetQueryClick() {
  tableRef.value.resetQuery();
}

/** 改变启用状态值 */
function handleStatusChange(row) {
  const text = row.validFlag === true ? "启用" : "禁用";
  proxy.$modal
    .confirm('确认要"' + text + '","' + row.name + '"来源系统吗？')
    .then(function () {
      updateSourceSystem({ id: row.id, validFlag: row.validFlag }).then(
        (response) => {
          proxy.$modal.msgSuccess(text + "成功");
          tableRef.value.getList();
        }
      );
    })
    .catch(function () {
      row.validFlag = !row.validFlag;
    });
}

// 取消按钮
function cancel() {
  open.value = false;
  openDetail.value = false;
  reset();
}

// 表单重置
function reset() {
  form.value = {
    id: null,
    name: null,
    type: null,
    sortOrder: 0,
    description: null,
    validFlag: false,
    responsiblePerson: null,
    contactPerson: null,
    delFlag: null,
    createBy: null,
    creatorId: null,
    createTime: null,
    updateBy: null,
    updaterId: null,
    updateTime: null,
    remark: null,
  };
  proxy.resetForm("sourceSystemRef");
}

/** 新增按钮操作 */
function handleAdd() {
  reset();
  open.value = true;
  title.value = "新增来源系统";
}

/** 修改按钮操作 */
function handleUpdate(row) {
  reset();
  const _id = row.id;
  getSourceSystem(_id).then((response) => {
    delete response.data.createTime;
    delete response.data.updateTime;
    form.value = response.data;
    form.value.responsiblePerson = Number(response.data.responsiblePerson);
    form.value.contactPerson = Number(response.data.contactPerson);
    open.value = true;
    title.value = "修改来源系统";
  });
}

/** 详情按钮操作 */
function handleDetail(row) {
  reset();
  const _id = row.id;
  getSourceSystem(_id).then((response) => {
    form.value = response.data;
    openDetail.value = true;
    title.value = "来源系统详情";
  });
}
/** 提交按钮 */
function submitForm() {
  proxy.$refs["sourceSystemRef"]
    .validate()
    .then(() => {
      if (form.value.id != null) {
        updateSourceSystem(form.value)
          .then((response) => {
            proxy.$modal.msgSuccess("修改成功");
            open.value = false;
            tableRef.value.getList();
          })
          .catch((error) => {});
      } else {
        addSourceSystem(form.value)
          .then((response) => {
            proxy.$modal.msgSuccess("新增成功");
            open.value = false;
            tableRef.value.getList();
          })
          .catch((error) => {});
      }
    })
    .catch(() => {});
}
/** 删除按钮操作 */
function handleDelete(row) {
  const invalidIds = [];
  let _ids = null;
  if (row?.id) {
    _ids = row.id;
  } else {
    // _ids = store.rows.map((item) => item.id).join(",");
    store.rows.forEach((item) => {
      // 当 validFlag 为 false 时，记录 id
      if (item.validFlag === false) {
        invalidIds.push(item.id);
      }
    });
  }
  proxy.$modal
    .confirm(
      `可删除${invalidIds.length}个，不可删除${
        store.rows.length - invalidIds.length
      }个，是否删除可删部分`
    )
    .then(function () {
      return delSourceSystem(invalidIds);
    })
    .then(() => {
      tableRef.value.getList();
      proxy.$modal.msgSuccess("删除成功");
    })
    .catch(() => {
      // 用户取消删除操作
    });
}

// 初始化
loadUserOptions();
</script>
