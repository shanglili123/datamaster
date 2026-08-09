<template>
  <!--  组合字段去重  -->
  <a-form ref="formRef" :model="form" :label-col="{ style: { width: '130px' } }" :disabled="falg">
    <div class="deduplication-config">
      <div class="justify-between mb15">
        <a-row :gutter="15" class="btn-style">
          <a-col :span="1.5">
            <a-button type="primary" @click="addtypecolumns">
              <i class="iconfont-mini icon-xinzeng mr5"></i>新增排序字段
            </a-button>
          </a-col>
        </a-row>
      </div>
      <a-table
        :data-source="form.stringValue"
        striped
        style="width: 100%"
        row-key="sort"
        :pagination="false"
        :columns="tableColumns"
        ref="dragTable"
      >
        <template #bodyCell="{ column, record, index }">
          <template v-if="column.key === 'index'">
            <div
              class="allowDrag"
              style="
                cursor: move;
                display: flex;
                justify-content: center;
                align-items: center;
              "
            >
              <ControlOutlined />
              <span style="margin-left: 4px">{{ index + 1 }}</span>
            </div>
          </template>
          <template v-else-if="column.key === 'columns'">
            <template v-if="!falg">
              <a-select
                v-model:value="record.columns"
                placeholder="请选择清洗字段"
                allow-clear
              >
                <a-select-option
                  v-for="dict in inputFields"
                  :key="dict.columnName"
                  :label="dict.label"
                  :value="dict.columnName"
                  :disabled="iscolumnsDisabled(dict.columnName, record.id)"
                />
              </a-select>
            </template>
            <div v-else class="form-readonly">
              {{
                inputFields.find((d) => d.columnName === record.columns)?.label ||
                record.columns ||
                "-"
              }}
            </div>
          </template>

          <template v-else-if="column.key === 'type'">
            <template v-if="!falg">
              <a-select v-model:value="record.type" placeholder="请选择" size="default">
                <a-select-option label="升序" value="1"></a-select-option>
                <a-select-option label="降序" value="0"></a-select-option>
              </a-select>
            </template>
            <div v-else class="form-readonly">
              {{ record.type === "1" ? "升序" : record.type === "0" ? "降序" : "-" }}
            </div>
          </template>
          <template v-else-if="column.key === 'actions' && !falg">
            <a-button
              type="link"
              size="small"
              danger
              @click="handleDeletetypecolumns(index)"
              ><template #icon><DeleteOutlined /></template>删除</a-button
            >
          </template>
        </template>
        <template #empty>
          <div class="emptyBg">
            <p>无数据</p>
          </div>
        </template>
      </a-table>
    </div>
    <a-form-item label="去重策略" name="handleType" style="margin-top: 20px">
      <a-radio-group
        v-model:value="form.handleType"
        class="strategy-radio-group"
        :disabled="falg"
      >
        <a-radio :value="'1'" class="radio-item">
          <span class="radio-label">保留首条记录</span>
        </a-radio>
        <p class="strategy-0ription">
          系统将根据去重条件保留满足去重规则记录中的第一条记录。
        </p>
        <a-radio :value="'2'" class="radio-item">
          <span class="radio-label">保留最新记录</span>
        </a-radio>
        <p class="strategy-0ription">
          系统将根据去重条件保留满足去重规则记录中的最新记录。
        </p>
      </a-radio-group>
    </a-form-item>
  </a-form>
</template>

<script setup name="columnsCombiner">
import { message } from 'ant-design-vue'
import { DeleteOutlined, ControlOutlined } from "@ant-design/icons-vue";
import Sortable from "sortablejs";
const props = defineProps({
  form: Object,
  inputFields: Array,
  falg: Boolean,
});
const form = reactive({ ...props.form });
const exposedcolumnss = ["stringValue", "handleType"];
const data = Object.fromEntries(exposedcolumnss.map((key) => [key, form[key]])); // 添加排序字段，默认排序顺序为降序

let dragTable = ref(null);
let sortableInstance = null;
const tableColumns = computed(() => {
  const cols = [
    { title: '序号', key: 'index', width: 80, align: 'left' },
    { title: '字段名称', key: 'columns', align: 'left' },
    { title: '排序顺序', key: 'type', align: 'left' },
  ];
  if (!props.falg) {
    cols.push({ title: '操作', key: 'actions', align: 'center', width: 100 });
  }
  return cols;
});
function setSort() {
  nextTick(() => {
    const tbody = dragTable.value?.$el.querySelector(
      ".ant-table-tbody"
    );
    if (!tbody) {
      console.warn("tbody 找不到，拖拽初始化失败");
      return;
    }

    if (sortableInstance) {
      sortableInstance.destroy();
    }

    sortableInstance = Sortable.create(tbody, {
      handle: ".allowDrag",
      animation: 150,
      onEnd: (evt) => {
        const movedItem = form.stringValue.splice(evt.oldIndex, 1)[0];
        form.stringValue.splice(evt.newIndex, 0, movedItem);
        console.log(
          "拖拽后顺序:",
          form.stringValue.map((f) => f.sort)
        );
      },
    });
  });
}
const addtypecolumns = () => {
  form.stringValue.push({
    sort: form.stringValue.length,
    columns: "", // 字段名称
    type: "0", // 默认降序
  });
  setSort();
};
// 删除排序字段
const handleDeletetypecolumns = (index) => {
  form.stringValue.splice(index, 1);
  setSort();
};
// 判断字段是否已被其他行选择，禁用重复选项
const iscolumnsDisabled = (columnsName, currentRowId) => {
  return form.stringValue.some(
    (item) => item.columns === columnsName && item.id !== currentRowId
  );
};
const formRef = ref(null);
function validate() {
  return new Promise((resolve) => {
    formRef.value.validate((valid) => {
      if (!valid) {
        resolve({ valid: false });
        return;
      }

      // 如果没有添加排序字段，直接通过
      if (!form.stringValue || form.stringValue.length === 0) {
        resolve({
          valid: true,
          data,
        });
        return;
      }

      // 校验每个字段名称非空
      for (const item of form.stringValue) {
        if (!item.columns) {
          message.error("排序字段名称不能为空");
          resolve({ valid: false });
          return;
        }
      }

      // 校验字段名称不重复
      const columnss = form.stringValue.map((item) => item.columns);
      const hasDuplicate = new Set(columnss).size !== columnss.length;
      if (hasDuplicate) {
        message.error("排序字段名称不能重复");
        resolve({ valid: false });
        return;
      }

      // 只有数组有值才更新 sort
      if (form.stringValue && form.stringValue.length > 0) {
        form.stringValue.forEach((item, index) => {
          item.sort = index + 1;
        });
      }

      resolve({
        valid: true,
        data,
      });
    });
  });
}

setSort();
defineExpose({ validate });
</script>

<style scoped lang="scss">
.deduplication-config {
  padding-left: 57px;
}
.form-readonly {
  min-height: 32px;
  line-height: 32px;
}
</style>

