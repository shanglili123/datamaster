<template>
  <a-modal
      title="学生-多选"
      v-model:open="visible"
      width="1200px"
      draggable
      destroy-on-close
      @cancel="cancel"
  >
    <a-form
        class="btn-style"
        :model="queryParams"
        ref="queryRef"
        layout="inline"
        v-show="showSearch"
        :label-col="{ style: { width: '68px' } }"
    >
      <a-form-item label="姓名" name="name">
        <a-input
            style="width:240px"
            v-model:value="queryParams.name"
            placeholder="请输入姓名"
            allow-clear
            @pressEnter="handleQuery"
        />
      </a-form-item>
      <a-form-item label="性别" name="sex">
        <a-select style="width:240px" v-model:value="queryParams.sex" placeholder="请选择性别" allow-clear>
          <a-select-option
              v-for="dict in sys_user_sex"
              :key="dict.value"
              :value="dict.value"
          >
            {{ dict.label }}
          </a-select-option>
        </a-select>
      </a-form-item>
      <a-form-item label="年龄" name="age">
        <a-input
            style="width:240px"
            v-model:value="queryParams.age"
            placeholder="请输入年龄"
            allow-clear
            @pressEnter="handleQuery"
        />
      </a-form-item>
      <a-form-item label="学号" name="studentNumber">
        <a-input
            style="width:240px"
            v-model:value="queryParams.studentNumber"
            placeholder="请输入学号"
            allow-clear
            @pressEnter="handleQuery"
        />
      </a-form-item>
      <a-form-item label="班级" name="grade">
        <a-input
            style="width:240px"
            v-model:value="queryParams.grade"
            placeholder="请输入班级"
            allow-clear
            @pressEnter="handleQuery"
        />
      </a-form-item>
      <a-form-item label="创建时间" name="createTime">
        <a-date-picker style="width:240px"
                        allow-clear
                        v-model:value="queryParams.createTime"
                        valueFormat="YYYY-MM-DD"
                        placeholder="请选择创建时间">
        </a-date-picker>
      </a-form-item>
      <a-form-item>
        <a-button
            type="primary"
            @click="handleQuery"
            @mousedown="(e) => e.preventDefault()"
        >
          <i class="iconfont-mini icon-a-zu22377 mr5"></i>查询
        </a-button>
        <a-button @click="resetQuery" @mousedown="(e) => e.preventDefault()">
          <i class="iconfont-mini icon-a-zu22378 mr5"></i>重置
        </a-button>
      </a-form-item>
    </a-form>

    <a-table
        striped
        height="300px"
        :loading="loading"
        :data-source="dataList"
        :columns="tableColumns"
        :pagination="false"
        row-key="id"
        :row-selection="rowSelection"
        @rowClick="handleRowClick"
        :locale="{ emptyText: '暂无数据' }"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.dataIndex === 'pictureUrl'">
          <image-preview :src="record.pictureUrl" :width="50" :height="50"/>
        </template>
        <template v-else-if="column.dataIndex === 'sex'">
              <dict-tag :options="sys_user_sex" :value="record.sex"/>
        </template>
        <template v-else-if="column.dataIndex === 'hobby'">
              <dict-tag :options="message_level" :value="record.hobby ? record.hobby.split(',') : []"/>
        </template>
        <template v-else-if="column.dataIndex === 'createTime'">
          <span>{{ parseTime(record.createTime, '{y}-{m}-{d}') }}</span>
        </template>
        <template v-else>
          {{ record[column.dataIndex] || '-' }}
        </template>
      </template>
    </a-table>

    <pagination
        v-show="total > 0"
        :total="total"
        v-model:page="queryParams.pageNum"
        v-model:limit="queryParams.pageSize"
        @pagination="getList"
    />
    <template #footer>
      <div class="dialog-footer">
        <a-button size="small" @click="cancel">取 消</a-button>
        <a-button type="primary" size="small" @click="confirm">
          确 定
        </a-button>
      </div>
    </template>
  </a-modal>
</template>

<script setup name="StudentMultiple">
  import { listStudent } from "@/api/example/genStudent/student";
  import { ref } from "vue";
  const { proxy } = getCurrentInstance();

  const { sys_user_sex, message_level } = proxy.useDict('sys_user_sex', 'message_level');

  const dataList = ref([]);
  const loading = ref(true);
  const showSearch = ref(true);
  const total = ref(0);
  const dateRange = ref([]);
  const data = reactive({
    form: {},
    queryParams: {
      pageNum: 1,
      pageSize: 6,
      name: null,
      pictureUrl: null,
      experience: null,
      sex: null,
      age: null,
      studentNumber: null,
      grade: null,
      hobby: null,
      createTime: null,
    }
  });
  const { queryParams, form } = toRefs(data);

  // -------------------------------------------
  const visible = ref(false);
  // 定义多选数据
  const multiple = ref([]);
  // 定义上次勾选数据==用于对比删除
  const oldSelection = ref([]);
  // 是否分页切换
  const isAuto = ref(false);
  // antd 行选择 key
  const selectedRowKeys = ref([]);

  const emit = defineEmits(["open", "confirm", "cancel"]);

  const tableColumns = [
    { title: 'ID', dataIndex: 'id', align: 'center' },
    { title: '姓名', dataIndex: 'name', align: 'center' },
    { title: '学生照', dataIndex: 'pictureUrl', align: 'center', width: 100 },
    { title: '教育经历', dataIndex: 'experience', align: 'center' },
    { title: '性别', dataIndex: 'sex', align: 'center' },
    { title: '年龄', dataIndex: 'age', align: 'center' },
    { title: '学号', dataIndex: 'studentNumber', align: 'center' },
    { title: '班级', dataIndex: 'grade', align: 'center' },
    { title: '爱好', dataIndex: 'hobby', align: 'center' },
    { title: '创建人', dataIndex: 'createBy', align: 'center' },
    { title: '创建时间', dataIndex: 'createTime', align: 'center', width: 180 },
    { title: '备注', dataIndex: 'remark', align: 'center' },
  ];

  const rowSelection = {
    selectedRowKeys,
    preserveSelectedRowKeys: true,
    onChange: handleSelectionChange,
  };

  /** 多选框选中事件 */
  function handleSelectionChange(keys, selection) {
    selectedRowKeys.value = keys;
    // console.log(selection, "===handleSelectionChange");
    if (selection.length > 0) {
      // 如果选中值不是空值且少选了一个值
      if (oldSelection.value.length > selection.length) {
        oldSelection.value.forEach((item) => {
          let index = selection.findIndex((ece) => ece.id == item.id);
          if (index == -1) {
            multiple.value = multiple.value.filter(
                (ece) => item.id != ece.id
            );
          }
        });
      }
      if (multiple.value.length > 0) {
        selection.forEach((item) => {
          let index = multiple.value.findIndex(
              (ece) => ece.id == item.id
          );
          if (index == -1) {
            multiple.value.push(item);
          }
        });
      } else {
        multiple.value.push(...selection);
      }
    } else {
      // 如果不是分页导致的
      if (!isAuto.value) {
        // 如果选中值，取消到没有选择任何值
        oldSelection.value.forEach((item) => {
          let index = selection.findIndex((ece) => ece.id == item.id);
          if (index == -1) {
            multiple.value = multiple.value.filter(
                (ece) => item.id != ece.id
            );
          }
        });
      }
    }
    oldSelection.value = selection;
  }

  /** 行单机事件 */
  function handleRowClick(record) {
    // 检查当前行是否已经在 multiple 中
    const index = multiple.value.findIndex(item => item.id === record.id);

    // 如果行已经被选中，移除它
    if (index > -1) {
      multiple.value = multiple.value.filter(item => item.id !== record.id);
    } else {
      // 如果行未被选中，添加到 multiple 中
      multiple.value.push(record);
    }

    // 同步更新表格的选中状态（antd 通过 selectedRowKeys 控制）
    selectedRowKeys.value = multiple.value.map(item => item.id);
  }

  /**
   * 选中table的复选框
   * @param {Array} rows 选中的对象数组
   * @param {Boolean} ignoreSelectable 是否忽略可选
   */
  function setSelectionRow(rows) {
    // 选中数据
    if (rows.length > 0) {
      selectedRowKeys.value = rows.map((item) => item.id);
    }
  }

  function rest(){
    queryParams.value.pageNum = 1;
    proxy.resetForm("queryRef");
    oldSelection.value = []
  }

  /**
   * 打开选择框
   * @param {Array} val 选中的对象数组
   */
  function open(val) {
    if (!Array.isArray(val)) {
      val = [val];  // 将非可迭代值转化为数组
    }
    visible.value = true;
    multiple.value = [...val];
    getList();
  }

  /**
   * 取消按钮
   * @description 取消按钮时，重置所有状态
   */
  function cancel() {
    rest();
    visible.value = false;
  }

  /**
   * 确定按钮
   * @description 确定按钮时，emit confirm 事件，以便父组件接收到选中的数据
   */
  function confirm() {
    if (multiple.value.length == 0) {
      proxy.$modal.msgWarning("未选择数据！");
      return;
    }
    emit("confirm", [...multiple.value]);
    rest();
    visible.value = false;
  }

  /** 查询字典类型列表 */
  function getList() {
    loading.value = true;
    listStudent(proxy.addDateRange(queryParams.value, dateRange.value)).then(
        async (response) => {
          dataList.value = response.data.rows;
          total.value = response.data.total;
          loading.value = false;
          // 初始化及分页切换选中逻辑
          isAuto.value = true;
          await nextTick();
          setSelectionRow(multiple.value);
          isAuto.value = false;
        }
    );
  }

  /** 搜索按钮操作 */
  function handleQuery() {
    getList();
  }

  /** 重置按钮操作 */
  function resetQuery() {
    proxy.resetForm("queryRef");
    queryParams.value.pageNum = 1;
    handleQuery();
  }

  defineExpose({ open });
</script>

