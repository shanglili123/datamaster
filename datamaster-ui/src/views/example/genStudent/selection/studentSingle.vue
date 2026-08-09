<template>
  <a-modal
      title="学生-单选"
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

<script setup name="StudentSingle">
  import { listStudent } from "@/api/example/genStudent/student";
  import { ref } from "vue";
  const { proxy } = getCurrentInstance();

  const { sys_user_sex, message_level } = proxy.useDict('sys_user_sex', 'message_level');

  const dataList = ref([]);
  const loading = ref(true);
  const showSearch = ref(true);
  const total = ref(0);
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
  const { queryParams, form} = toRefs(data);

  // -------------------------------------------
  const visible = ref(false);
  // 定义单选数据
  const single = ref();
  // antd 单选选中行 key
  const currentRowKey = ref([]);

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
    type: 'radio',
    selectedRowKeys: currentRowKey,
    onChange: handleCurrentChange,
  };

  /** 单选选中事件 */
  function handleCurrentChange(keys, rows) {
    if (rows.length > 0) {
      single.value = rows[0];
    } else {
      single.value = undefined;
    }
  }

  /**
   * 设置当前行
   * @param {Object} row 行对象
   * @returns 更改选中对象
   */
  function setCurrentRow(row) {
    if (row) {
      let data = dataList.value.filter((item) => item.id == row.id);
      if (data.length > 0) {
        currentRowKey.value = [data[0].id];
        single.value = data[0];
      }
    }
  }

  /**
   * 打开选择框
   * @param {Array} val 选中的对象数组
   */
  function open(val) {
    visible.value = true;
    single.value = val;
    resetQuery();
    getList();
  }

  /**
   * 取消按钮
   * @description 取消按钮时，重置所有状态
   */
  function cancel() {
    queryParams.value.pageNum = 1;
    proxy.resetForm("queryRef");
    visible.value = false;
  }

  /**
   * 确定按钮
   * @description 确定按钮时，emit confirm 事件，以便父组件接收到选中的数据
   */
  function confirm() {
    if (!single.value) {
      proxy.$modal.msgWarning("请选择数据！");
      return;
    }
    emit("confirm", single.value);
    visible.value = false;
  }

  /** 查询字典类型列表 */
  function getList() {
    loading.value = true;
    listStudent(proxy.addDateRange(queryParams.value, daterangeCreateTime.value)).then(
        async (response) => {
          dataList.value = response.data.rows;
          total.value = response.data.total;
          loading.value = false;
          // 初始化及分页切换选中逻辑
          await nextTick();
          setCurrentRow(single.value);
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

