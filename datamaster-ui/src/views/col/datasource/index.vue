<template>
  <div class="app-container datasource-list-page" ref="app-container">

    <div class="pagecont-top" v-show="showSearch">
      <a-form
          class="btn-style"
          :model="queryParams"
          ref="queryRef"
          layout="inline"
          :label-col="{ style: { width: '45px' } }"
          v-show="showSearch"
          @submit.prevent
      >
        <a-form-item label="名称" name="datasourceName">
          <a-input
              v-model:value="queryParams.datasourceName"
              placeholder="请输入数据源名称"
              allow-clear
              style="width: 150px;"
              @pressEnter="handleQuery"
          />
        </a-form-item>
        <a-form-item label="类型" name="datasourceType">
          <a-select
              v-model:value="queryParams.datasourceType"
              placeholder="请选择数据源类型"
              allow-clear
              style="width: 150px;"
          >
            <a-select-option
                  v-for="dict in datasourceTypeOptions"
                :key="dict.value"
                :value="dict.value"
            >
              {{ dict.label }}
            </a-select-option>
          </a-select>
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
      <div class="data-action-btns">
        <a-button
            type="primary"
            @click="handleAdd"
            v-hasPermi="['ast:dataSource:add']"
            @mousedown="(e) => e.preventDefault()"
        >
          <i class="iconfont-mini icon-xinzeng mr5"></i>新增
        </a-button>
      </div>
      <div class="top-right-btn">
        <right-toolbar
            v-model:showSearch="showSearch"
            @queryTable="getList"
            :columns="columns"
        ></right-toolbar>
      </div>
    </div>

    <div>
      <a-table
          :loading="loading"
          :data-source="daDatasourceList"
          :columns="tableColumns"
          :scroll="tableScroll"
          :row-key="(record) => record.id"
          :pagination="false"
          @change="handleTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'id'">
            {{ record.id || "-" }}
          </template>
          <template v-else-if="column.key === 'datasourceName'">
            {{ record.datasourceName || "-" }}
          </template>
          <template v-else-if="column.key === 'description'">
            {{ record.description || "-" }}
          </template>
          <template v-else-if="column.key === 'datasourceType'">
            <dict-tag
                :options="datasourceTypeOptions"
                :value="record.datasourceType"
            />
          </template>
          <template v-else-if="column.key === 'createBy'">
            {{ record.createBy || "-" }}
          </template>
          <template v-else-if="column.key === 'createTime'">
            <span>{{
                parseTime(record.createTime, "{y}-{m}-{d} {h}:{i}")
              }}</span>
          </template>
          <template v-else-if="column.key === 'validFlag'">
            <a-switch
                v-model:checked="record.validFlag"
                @change="handleStatusChange(record)"
            >
            </a-switch>
          </template>
          <template v-else-if="column.key === 'operation'">
            <a-button
                type="link"
                :icon="h(ApiOutlined)"
                @click="handleTestConnection(record)"
                v-hasPermi="['ast:dataSource:edit']"
            >测试连接
            </a-button>

            <a-button
                type="link"
                :icon="h(EyeOutlined)"
                @click="handleDetail(record)"
                v-hasPermi="['ast:dataSource:edit']"
            >详情
            </a-button>
            <a-popover placement="bottom" :overlay-style="{ width: '100px' }" trigger="click">
              <template #content>
                <div class="butgdlist">
                  <a-button
                      type="link"
                      :icon="h(EditOutlined)"
                      @click="handleUpdate(record)"
                      v-hasPermi="['ast:dataSource:edit']"
                  >修改
                  </a-button>
                  <a-button
                      type="link"
                      danger
                      :icon="h(DeleteOutlined)"
                      @click="handleDelete(record)"
                      v-hasPermi="['ast:dataSource:remove']"
                  >删除
                  </a-button>
                </div>
              </template>
              <a-button
                  type="link"
                  :disabled="record.isAdminAddTo == false"
                  :icon="h(DownOutlined)"
              >
                <a-tooltip
                    :title="record.isAdminAddTo == false ? '暂无权限' : ''"
                    placement="top"
                >
                  更多
                </a-tooltip>
              </a-button>
            </a-popover>
          </template>
        </template>
        <template #emptyText>
          <div class="emptyBg">
            <img src="@/assets/system/images/no_data/noData.png" alt="" />
            <p>暂无记录</p>
          </div>
        </template>
      </a-table>

      <pagination
          v-show="total > 0"
          :total="total"
          v-model:page="queryParams.pageNum"
          v-model:limit="queryParams.pageSize"
          @pagination="getList"
      />
    </div>

    <!-- 新增或修改数据源对话框 -->
    <a-modal
        :title="title"
        v-model:open="open"
        width="1000px"
        :get-container="() => $refs['app-container']"
    >
      <template #header="{ close, titleId, titleClass }">
        <span role="heading" aria-level="2">
          {{ title }}
        </span>
      </template>
      <a-form
          ref="daDatasourceRef"
          :model="form"
          :rules="rules"
          :label-col="{ style: { width: '110px' } }"
          @submit.prevent
          :disabled="title == '数据源详情'"
      >
        <a-row :gutter="20">
          <a-col :span="12">
            <a-form-item label="数据源名称" name="datasourceName">
              <a-input
                  v-model:value="form.datasourceName"
                  placeholder="请输入数据源名称"
              />
            </a-form-item>
          </a-col>

          <a-col :span="12">
            <a-form-item label="数据源类型" name="datasourceType">
              <a-select
                  v-model:value="form.datasourceType"
                  placeholder="请选择数据源类型"
                  @change="handleDatasourceChange"
                  :disabled="form.id"
              >
                <a-select-option
                    v-for="dict in datasourceTypeOptions"
                    :key="dict.value"
                    :value="dict.value"
                >{{ dict.label }}</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="20" v-if="form.datasourceType !== 'OSS-ALIYUN'">
          <a-col :span="12">
            <a-form-item label="IP" name="ip">
              <a-input v-model:value="form.ip" placeholder="请输入IP" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="端口号" name="port">
              <a-input v-model:value="form.port" placeholder="请输入端口号" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-row
            :gutter="20"
            v-if="
            showCredentialFields(form.datasourceType)
          "
        >
          <a-col :span="12">
            <a-form-item label="账号" :name="isCredentialRequired(form.datasourceType) ? 'username' : ''">
              <a-input v-model:value="form.username" placeholder="请输入账号" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="密码" :name="isCredentialRequired(form.datasourceType) ? 'password' : ''">
              <a-input
                  type="password"
                  v-model:value="form.password"
                  placeholder="请输入密码"
                  v-if="title === '新增数据源'"
              />
              <a-input
                  type="password"
                  v-model:value="form.password"
                  placeholder="请输入密码"
                  v-if="title !== '新增数据源'"
              />
            </a-form-item>
          </a-col>
        </a-row>

        <template v-if="form.datasourceType === 'OSS-ALIYUN'">
          <a-row :gutter="20">
            <a-col :span="12">
              <a-form-item label="keyID" name="keyId">
                <a-input v-model:value="form.keyId" placeholder="请输入keyID" />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="keySecret" name="keySecret">
                <a-input
                    v-model:value="form.keySecret"
                    placeholder="请输入keySecret"
                    v-if="title === '新增数据源'"
                />
                <a-input
                    type="password"
                    v-model:value="form.keySecret"
                    placeholder="请输入keySecret"
                    v-if="title !== '新增数据源'"
                />
              </a-form-item>
            </a-col>
          </a-row>
          <a-row :gutter="20">
            <a-col :span="12">
              <a-form-item label="bucket" name="bucket">
                <a-input
                    v-model:value="form.bucket"
                    placeholder="请输入bucket，例如：test"
                />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="endpoint" name="endpoint">
                <a-input
                    v-model:value="form.endpoint"
                    placeholder="请输入endpoint,例如：oss-cn-beijing.aliyuncs.com"
                />
              </a-form-item>
            </a-col>
          </a-row>
          <a-row :gutter="20">
            <a-col :span="24">
              <a-form-item label="域名" name="domain">
                <a-input
                    v-model:value="form.domain"
                    placeholder="请输入域名,可空，例如test.oss-cn-beijing.aliyuncs.com"
                />
              </a-form-item>
            </a-col>
          </a-row>
        </template>
        <a-row
            :gutter="20"
            v-if="
            showDbNameField(form.datasourceType)
          "
        >
          <a-col :span="12" v-if="form.datasourceType !== null">
            <a-form-item label="数据库名称" :name="isDbNameRequired(form.datasourceType) ? 'dbname' : ''">
              <a-input
                  v-model:value="form.dbname"
                  placeholder="请输入数据库名称"
                  :disabled="form.id"
              />
            </a-form-item>
          </a-col>
          <a-col
              :span="12"
              v-if="
              form.datasourceType !== null &&
              (form.datasourceType == 'Oracle' ||
                form.datasourceType == 'Oracle11' ||
                form.datasourceType == 'Kingbase8' ||
                form.datasourceType == 'SQL_Server' ||
                form.datasourceType == 'SQL_Server2008' ||
                form.datasourceType == 'PostgreSQL')
            "
          >
            <a-form-item label="模式名称" name="sid">
              <a-input
                  v-model:value="form.sid"
                  placeholder="请输入模式名称"
                  :disabled="form.id"
              />
            </a-form-item>
          </a-col>
        </a-row>

        <a-row
            :gutter="20"
            v-if="
            form.datasourceType !== null &&
            (form.datasourceType === 'Kafka' || form.datasourceType === 'HDFS')
          "
        >
          <a-col :span="24">
            <a-form-item label="配置参数" name="config">
              <a-textarea
                  :auto-size="{ minRows: 2, maxRows: 4 }"
                  v-model:value="form.config"
                  :placeholder="
                  form.datasourceType === 'Kafka'
                    ? '例如: {&quot;security.protocol&quot;&colon;&quot;SASL_PLAINTEXT&quot;}'
                    : '例如: {&quot;kerberosKeytabFilePath&quot;&colon;&quot;/path/to/keytab/file&quot;}'
                "
              />
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="20">
          <a-col :span="24">
            <a-form-item label="描述" name="description">
              <a-textarea
                  :auto-size="{ minRows: 8 }"
                  v-model:value="form.description"
                  placeholder="请输入描述"
              />
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="20">
          <a-col :span="24">
            <a-form-item label="所属空间" name="spaceNameList">
              <a-input
                  style="width: 100%"
                  :value="Array.isArray(form.spaceNameList) ? form.spaceNameList.join(', ') : form.spaceNameList"
                  placeholder="当前空间"
                  disabled
              />
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="20">
          <a-col :span="12">
            <a-form-item label="状态" name="validFlag">
              <a-radio-group v-model:value="form.validFlag">
                <a-radio
                    v-for="dict in sys_disable"
                    :key="dict.value"
                    :value="dict.value === '1'"
                >
                  {{ dict.label }}
                </a-radio>
              </a-radio-group>
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>
      <template #footer>
        <div class="dialog-footer">
          <a-button size="small" @click="cancel">取 消</a-button>
          <a-button
              type="primary"
              size="small"
              :loading="btnLoading"
              @click="submitForm"
          >确 定</a-button
          >
        </div>
      </template>
    </a-modal>

    <!-- 详情 -->
    <a-modal
        :title="title"
        v-model:open="openDetail"
        width="1000px"
        :get-container="() => $refs['app-container']"
    >
      <template #header="{ close, titleId, titleClass }">
        <span role="heading" aria-level="2">
          {{ title }}
        </span>
      </template>
      <a-form
          ref="daDatasourceRef"
          :model="form"
          :rules="rules"
          :label-col="{ style: { width: '110px' } }"
      >
        <a-row :gutter="20">
          <a-col :span="12">
            <a-form-item label="数据源名称">
              <div class="form-readonly">
                {{ form.datasourceName || "-" }}
              </div>
            </a-form-item>
          </a-col>

          <a-col :span="12">
            <a-form-item label="数据源类型">
              <div>
                <dict-tag
                    :options="datasourceTypeOptions"
                    :value="form.datasourceType"
                />
              </div>
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="20" v-if="form.datasourceType !== 'OSS-ALIYUN'">
          <a-col :span="12">
            <a-form-item label="IP">
              <div class="form-readonly">
                {{ form.ip || "-" }}
              </div>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="端口号">
              <div class="form-readonly">
                {{ form.port || "-" }}
              </div>
            </a-form-item>
          </a-col>
        </a-row>
        <a-row
            :gutter="20"
            v-if="
            showCredentialFields(form.datasourceType)
          "
        >
          <a-col :span="12">
            <a-form-item label="账号">
              <div class="form-readonly">
                {{ form.username || "-" }}
              </div>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="密码">
              <div class="form-readonly">***********</div>
            </a-form-item>
          </a-col>
        </a-row>

        <template v-if="form.datasourceType === 'OSS-ALIYUN'">
          <a-row :gutter="20">
            <a-col :span="12">
              <a-form-item label="keyID">
                <div class="form-readonly">
                  {{ form.keyId || "-" }}
                </div>
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="keySecret">
                <div class="form-readonly">
                  {{ form.keyIkeySecretd || "-" }}
                </div>
              </a-form-item>
            </a-col>
          </a-row>
          <a-row :gutter="20">
            <a-col :span="12">
              <a-form-item label="bucket">
                <div class="form-readonly">
                  {{ form.bucket || "-" }}
                </div>
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="endpoint">
                <div class="form-readonly">
                  {{ form.endpoint || "-" }}
                </div>
              </a-form-item>
            </a-col>
          </a-row>
          <a-row :gutter="20">
            <a-col :span="24">
              <a-form-item label="域名">
                <div class="form-readonly">
                  {{ form.domain || "-" }}
                </div>
              </a-form-item>
            </a-col>
          </a-row>
        </template>
        <a-row
            :gutter="20"
            v-if="
            showDbNameField(form.datasourceType)
          "
        >
          <a-col :span="12" v-if="form.datasourceType !== null">
            <a-form-item label="数据库名称">
              <div class="form-readonly">
                {{ form.dbname || "-" }}
              </div>
            </a-form-item>
          </a-col>
          <a-col
              :span="12"
              v-if="
              form.datasourceType !== null &&
              (form.datasourceType == 'Oracle' ||
                form.datasourceType == 'Oracle11' ||
                form.datasourceType == 'Kingbase8' ||
                form.datasourceType == 'SQL_Server' ||
                form.datasourceType == 'SQL_Server2008' ||
                form.datasourceType == 'PostgreSQL')
            "
          >
            <a-form-item label="模式名称">
              <div class="form-readonly">
                {{ form.sid || "-" }}
              </div>
            </a-form-item>
          </a-col>
        </a-row>

        <a-row
            :gutter="20"
            v-if="
            form.datasourceType !== null &&
            (form.datasourceType === 'Kafka' || form.datasourceType === 'HDFS')
          "
        >
          <a-col :span="24">
            <a-form-item label="配置参数">
              <div class="form-readonly">
                {{ form.config || "-" }}
              </div>
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="20">
          <a-col :span="24">
            <a-form-item label="描述">
              <div class="form-readonly textarea">
                {{ form.description || "-" }}
              </div>
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="20">
          <a-col :span="24">
            <a-form-item label="所属空间">
              <div class="form-readonly">
                {{ form.spaceNameListStr || "-" }}
              </div>
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="20">
          <a-col :span="12">
            <a-form-item label="状态">
              <dict-tag
                  :options="sys_disable"
                  :value="form.validFlag ? '1' : '0'"
              />
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>
      <template #footer>
        <div class="dialog-footer">
          <a-button size="small" @click="cancel">关 闭</a-button>
        </div>
      </template>
    </a-modal>
  </div>
</template>

<script setup name="DppDataSource">
import {
  listDaDatasource,
  getDaDatasource,
  clientsTest,
  delDaDatasource,
  removeDppOrDa,
  addDaDatasource,
  updateDaDatasource,
  listDaDatasourceBySpaceCode,
  editDatasourceStatus,
} from "@/api/ast/dataSource/dataSource";
import { encrypt, isDecrypted } from "@/utils/aesEncrypt";
import { getToken } from "@/utils/auth.js";
import useUserStore from "@/store/system/user";
import { getDatasourceTypes } from "@/components/Datasource/utils";
import { h } from "vue";
import { ApiOutlined, DownOutlined, EditOutlined, EyeOutlined, DeleteOutlined } from "@ant-design/icons-vue";
const userStore = useUserStore();
const { proxy } = getCurrentInstance();
const { sys_disable } = proxy.useDict("sys_disable");
const daDatasourceList = ref([]);
const datasourceTypeOptions = computed(() => getDatasourceTypes("datasource"));

// 列显隐信息
const columns = ref([
  { key: 1, label: "编号", visible: true },
  { key: 2, label: "数据源名称", visible: true },
  { key: 3, label: "描述", visible: true },
  { key: 4, label: "数据源类型", visible: true },
  { key: 5, label: "创建人", visible: true },
  { key: 6, label: "创建时间", visible: true },
  { key: 7, label: "状态", visible: true },
  { key: 9, label: "操作", visible: true },
]);

const getColumnVisibility = (key) => {
  const column = columns.value.find((col) => col.key === key);
  // 如果没有找到对应列配置，默认显示
  if (!column) return true;
  // 如果找到对应列配置，根据visible属性来控制显示
  return column.visible;
};

const tableColumns = computed(() => [
  { key: "id", title: "编号", dataIndex: "id", width: 120, align: "center", ellipsis: true, hidden: !getColumnVisibility(1) },
  { key: "datasourceName", title: "数据源名称", dataIndex: "datasourceName", width: 250, align: "left", ellipsis: true, hidden: !getColumnVisibility(2) },
  { key: "description", title: "描述", dataIndex: "description", width: 240, align: "left", ellipsis: true, hidden: !getColumnVisibility(3) },
  { key: "datasourceType", title: "数据源类型", dataIndex: "datasourceType", width: 140, align: "center", hidden: !getColumnVisibility(4) },
  { key: "createBy", title: "创建人", dataIndex: "createBy", width: 120, align: "center", ellipsis: true, hidden: !getColumnVisibility(5) },
  {
    key: "createTime",
    title: "创建时间",
    dataIndex: "createTime",
    width: 160,
    align: "center",
    sorter: true,
    sorterKey: "createTime",
    defaultSortOrder: "descend",
    hidden: !getColumnVisibility(6),
  },
  { key: "validFlag", title: "状态", dataIndex: "validFlag", width: 100, align: "center", hidden: !getColumnVisibility(7) },
  {
    key: "operation",
    title: "操作",
    align: "center",
    className: "small-padding fixed-width",
    fixed: "right",
    width: 420,
    hidden: !getColumnVisibility(9),
  },
].filter((col) => !col.hidden));
// 列总宽超出容器时启用横向滚动，保证 fixed 列与内容完整展示
const tableScroll = computed(() => {
  const totalWidth = tableColumns.value.reduce(
    (sum, c) => sum + (typeof c.width === "number" ? c.width : 0),
    0
  );
  return totalWidth > 0 ? { x: totalWidth } : undefined;
});

function handleTableChange(pagination, filters, sorter) {
  if (!sorter || Array.isArray(sorter)) {
    return;
  }
  handleSortChange({
    prop: sorter.field,
    order: sorter.order === "ascend" ? "ascending" : sorter.order === "descend" ? "descending" : sorter.order,
  });
}

const open = ref(false);
const openDetail = ref(false);
const loading = ref(true);
const showSearch = ref(true);
const ids = ref([]);
const single = ref(true);
const multiple = ref(true);
const total = ref(0);
const title = ref("");
const defaultSort = ref({ prop: "createTime", order: "desc" });
const router = useRouter();
/*** 用户导入参数 */
const upload = reactive({
  // 是否显示弹出层（用户导入）
  open: false,
  // 弹出层标题（用户导入）
  title: "",
  // 是否禁用上传
  isUploading: false,
  // 是否更新已经存在的用户数据
  updateSupport: 0,
  // 设置上传的请求头部
  headers: { Authorization: "Bearer " + getToken() },
  // 上传的地址
  url: import.meta.env.VITE_APP_BASE_API + "/ast/daDatasource/importData",
});

const data = reactive({
  form: {
    spaceNameListStr: "-",
    spaceNameList: userStore.spaceName ? [userStore.spaceName] : [],
    spaceIdList: [],
    spaceList: [],
  },
  queryParams: {
    pageNum: 1,
    pageSize: 6,
    datasourceName: null,
    datasourceType: null,
    datasourceConfig: null,
    config: null,
    ip: null,
    port: null,
    listCount: null,
    syncCount: null,
    dataSize: null,
    description: null,
    createTime: null,
  },
  rules: {
    datasourceName: [
      { required: true, message: "数据源名称不能为空", trigger: "blur" },
    ],
    datasourceType: [
      { required: true, message: "数据源类型不能为空", trigger: "change" },
    ],
    datasourceConfig: [
      {
        required: true,
        message: "数据源配置(json字符串)不能为空",
        trigger: "blur",
      },
    ],
    ip: [
      { required: true, message: "IP不能为空", trigger: "blur" },
      {
        pattern: /^[^\u4e00-\u9fa5]+$/,
        message: "IP不能包含中文",
        trigger: "blur",
      },
    ],
    port: [
      { required: true, message: "端口号不能为空", trigger: "blur" },
      {
        pattern: /^\d{1,9}$/,
        message: "端口号必须为1-9位数字",
        trigger: "blur",
      },
    ],
    username: [{ required: true, message: "账号不能为空", trigger: "blur" }],
    password: [{ required: true, message: "密码不能为空", trigger: "blur" }],
    keyId: [{ required: true, message: "keyID不能为空", trigger: "blur" }],
    keySecret: [
      { required: true, message: "keySecret不能为空", trigger: "blur" },
    ],
    bucket: [{ required: true, message: "bucket不能为空", trigger: "blur" }],
    endpoint: [
      { required: true, message: "endpoint不能为空", trigger: "blur" },
    ],
    dbname: [
      { required: true, message: "数据库名称不能为空", trigger: "blur" },
      // {
      //   pattern: /^[^\u4e00-\u9fa5]+$/,
      //   message: '数据库名称不能包含中文',
      //   trigger: 'blur'
      // }
    ],
    sid: [{ required: true, message: "模式不能为空", trigger: "blur" }],
    description: [{ required: true, message: "描述不能为空", trigger: "blur" }],
    config: [
      {
        trigger: "blur",
        validator: (rule, value, callback) => {
          if (value === null || value === undefined || value === "") {
            callback();
            return;
          }
          var flag = false;
          if (typeof value === "string") {
            try {
              const obj = JSON.parse(value);
              if (typeof obj === "object" && obj) {
                flag = true;
              }
            } catch (e) {}
          }
          if (flag) {
            callback();
          } else {
            callback("不是一个正确的JSON格式");
          }
        },
      },
    ],
  },
});

const { queryParams, form, rules } = toRefs(data);
// 监听 id 变化
watch(
    () => userStore.spaceCode,
    (newCode) => {
      getList();
    },
    { immediate: true } // `immediate` 为 true 表示页面加载时也会立即执行一次 watch
);

watch(
    () => userStore.spaceName,
    (name) => {
      if (!form.value.id && name) {
        form.value.spaceNameList = [name];
      }
    }
);


// 数据源类型 change 事件
function handleDatasourceChange(type) {
  rules.value.username[0].required = isCredentialRequired(type);
  rules.value.password[0].required = isCredentialRequired(type);
  rules.value.dbname[0].required = isDbNameRequired(type);
  if (!showCredentialFields(type)) {
    form.value.username = null;
    form.value.password = null;
  }
  if (!showDbNameField(type)) {
    form.value.dbname = null;
  }
  nextTick(() => proxy.$refs.daDatasourceRef?.clearValidate(["username", "password", "dbname"]));
}

function showCredentialFields(type) {
  return !!type && !["Kafka", "HDFS", "OSS-ALIYUN", "Redis", "RabbitMQ"].includes(type);
}

function isCredentialRequired(type) {
  return showCredentialFields(type) && !["Hive", "MongoDB", "Elasticsearch"].includes(type);
}

function showDbNameField(type) {
  return !!type && !["Kafka", "HDFS", "FTP", "OSS-ALIYUN", "Redis", "RabbitMQ", "Elasticsearch"].includes(type);
}

function isDbNameRequired(type) {
  return showDbNameField(type) && type !== "MongoDB";
}

function normalizeConfigText(value) {
  if (value === null || value === undefined || value === "") {
    return null;
  }
  return typeof value === "string" ? value : JSON.stringify(value);
}

function normalizePageData(response) {
  const data = response?.data ?? response ?? {};
  if (Array.isArray(data)) {
    return { rows: data, total: data.length };
  }
  const rows = Array.isArray(data.rows)
    ? data.rows
    : Array.isArray(data.list)
      ? data.list
      : Array.isArray(data.records)
        ? data.records
        : Array.isArray(response?.rows)
          ? response.rows
          : [];
  const total = Number(data.total ?? data.totalCount ?? response?.total ?? rows.length);
  return { rows, total: Number.isNaN(total) ? rows.length : total };
}

/** 查询数据源列表 */
function getList() {
  loading.value = true;
  queryParams.value.spaceId = userStore.spaceId;
  queryParams.value.spaceCode = userStore.spaceCode;
  listDaDatasourceBySpaceCode(queryParams.value).then((response) => {
    const pageData = normalizePageData(response);
    daDatasourceList.value = pageData.rows;
    total.value = pageData.total;
    loading.value = false;
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
    spaceNameList: userStore.spaceName ? [userStore.spaceName] : [],
    spaceIdList: [],
    spaceList: [],
    datasourceName: null,
    datasourceType: null,
    datasourceConfig: null,
    ip: null,
    port: null,
    listCount: null,
    syncCount: null,
    dataSize: null,
    description: null,
    validFlag: false,
    createBy: null,
    creatorId: null,
    createTime: null,
    updateBy: null,
    updaterId: null,
    updateTime: null,
  };
  proxy.resetForm("daDatasourceRef");
}

/** 搜索按钮操作 */
function handleQuery() {
  queryParams.value.pageNum = 1;
  getList();
}

/** 重置按钮操作 */
function resetQuery() {
  proxy.resetForm("queryRef");
  handleQuery();
}

// 多选框选中数据
function handleSelectionChange(selection) {
  ids.value = selection.map((item) => item.id);
  single.value = selection.length != 1;
  multiple.value = !selection.length;
}

/** 排序触发事件 */
function handleSortChange(column, prop, order) {
  queryParams.value.orderByColumn = column.prop;
  queryParams.value.isAsc = column.order;
  getList();
}

/** 新增按钮操作 */
function handleAdd() {
  reset();
  form.value.isDaOrDpp = true;
  form.value.spaceList = [
    {
      spaceId: userStore.spaceId,
      spaceCode: userStore.spaceCode,
      dppAssigned: true,
    },
  ];
  form.value.spaceNameList = userStore.spaceName ? [userStore.spaceName] : [];
  open.value = true;
  title.value = "新增数据源";
}

/** 修改按钮操作 */
let old_password;

function handleUpdate(row, type) {
  reset();
  const _id = row.id || ids.value;
  loading.value = true;
  getDaDatasource(_id)
      .then((response) => {
        form.value = response.data;
        form.value.spaceIdList = form.value.spaceList.map(
            (item) => item.spaceId
        );
        form.value.spaceNameList = form.value.spaceList.map(
            (item) => item.spaceName
        );

        // 拆解 datasourceConfig
        if (form.value.datasourceConfig) {
          const config = JSON.parse(form.value.datasourceConfig);
          form.value.username = config.username;
          form.value.password = config.password;
          form.value.dbname = config.dbname;
          form.value.sid = config.sid;
          if (config.keyId) form.value.keyId = config.keyId;
          if (config.keySecret) form.value.keySecret = config.keySecret;
          if (config.bucket) form.value.bucket = config.bucket;
          if (config.endpoint) form.value.endpoint = config.endpoint;
          if (config.domain) form.value.domain = config.domain;
          form.value.config = normalizeConfigText(config.config);
        }
        form.value.spaceListOld = form.value.spaceIdList;
        open.value = true;
        if (type == 3) {
          title.value = "数据源详情";
        } else {
          old_password = form.value.password;
          title.value = "修改数据源";
        }
      })
      .finally(() => {
        loading.value = false; // 不管成功失败都结束loading
      });
}

/** 详情按钮操作 */
function handleDetail(row) {
  reset();
  const _id = row.id || ids.value;
  getDaDatasource(_id).then((response) => {
    form.value = response.data;
    form.value.spaceNameListStr = form.value.spaceList
        .map((item) => item.spaceName)
        .join(", ");
    if (form.value.datasourceConfig) {
      const config = JSON.parse(form.value.datasourceConfig);
      form.value.username = config.username;
      form.value.password = config.password;
      form.value.dbname = config.dbname;
      form.value.sid = config.sid;
      if (config.keyId) {
        form.value.keyId = config.keyId;
      }
      if (config.keySecret) {
        form.value.keySecret = config.keySecret;
      }
      if (config.bucket) {
        form.value.bucket = config.bucket;
      }
      if (config.endpoint) {
        form.value.endpoint = config.endpoint;
      }
      if (config.domain) {
        form.value.domain = config.domain;
      }
      form.value.config = normalizeConfigText(config.config);
    }
    openDetail.value = true;
    title.value = "数据源详情";
  });
}

/** 详情按钮操作 */
function handleTestConnection(row) {
  loading.value = true; // 开始加载
  reset();
  const _id = row.id || ids.value;
  clientsTest(_id)
      .then((response) => {
        console.log(response);
        proxy.$modal.msgSuccess(response.msg);
      })
      .finally(() => {
        loading.value = false; // 结束加载
      });
}
const btnLoading = ref(false);
/** 提交按钮 */
function submitForm() {
  proxy.$refs["daDatasourceRef"]
    .validate()
    .then(() => {
      btnLoading.value = true;
      if (!showCredentialFields(form.value.datasourceType)) {
        form.value.username = null;
        form.value.password = null;
      }
      if (!showDbNameField(form.value.datasourceType)) {
        form.value.dbname = null;
      }
      if (form.value.id != null) {
        if (
            form.value.password &&
            (old_password !== form.value.password || !isDecrypted(form.value.password))
        ) {
          form.value.password = encrypt(form.value.password);
        }
        form.value.datasourceConfig = JSON.stringify({
          username: form.value.username,
          password: form.value.password,
          dbname: form.value.dbname,
          sid: form.value.sid,
          keyId: form.value.keyId,
          keySecret: form.value.keySecret,
          bucket: form.value.bucket,
          endpoint: form.value.endpoint,
          domain: form.value.domain,
          config: form.value.config,
        });

        let spaceListOld = [];
        form.value.spaceListOld.forEach((item) => {
          if (!form.value.spaceList.includes(item)) {
            spaceListOld.push(item);
          }
        });
        form.value.spaceListOld = spaceListOld;
        updateDaDatasource(form.value)
            .then((response) => {
              proxy.$modal.msgSuccess("修改成功");
              open.value = false;
              getList();
            })
            .finally(() => {
              btnLoading.value = false;
            });
      } else {
        form.value.datasourceConfig = JSON.stringify({
          username: form.value.username,
          password: form.value.password ? encrypt(form.value.password) : form.value.password,
          dbname: form.value.dbname,
          sid: form.value.sid,
          keyId: form.value.keyId,
          keySecret: form.value.keySecret,
          bucket: form.value.bucket,
          endpoint: form.value.endpoint,
          domain: form.value.domain,
          config: form.value.config,
        });
        addDaDatasource(form.value)
            .then((response) => {
              proxy.$modal.msgSuccess("新增成功");
              open.value = false;
              getList();
            })
            .finally(() => {
              btnLoading.value = false;
            });
      }
    })
    .catch(() => {});
}

/** 删除按钮操作 */
function handleDelete(row) {
  const _ids = row.id || ids.value;
  proxy.$modal
      .confirm('是否确认删除数据源编号为"' + _ids + '"的数据项？')
      .then(function () {
        return removeDppOrDa(_ids, 1);
      })
      .then(() => {
        getList();
        proxy.$modal.msgSuccess("删除成功");
      })
      .catch(() => {});
}

/** 导出按钮操作 */
function handleExport() {
  proxy.download(
      "ast/dataSource/export",
      {
        ...queryParams.value,
      },
      `daDatasource_${new Date().getTime()}.xlsx`
  );
}

/** ---------------- 导入相关操作 -----------------**/
/** 导入按钮操作 */
function handleImport() {
  upload.title = "数据源导入";
  upload.open = true;
}

/** 下载模板操作 */
function importTemplate() {
  proxy.download(
      "system/user/importTemplate",
      {},
      `daDatasource_template_${new Date().getTime()}.xlsx`
  );
}

/** 提交上传文件 */
function submitFileForm() {
  proxy.$refs["uploadRef"].submit();
}

/**文件上传中处理 */
const handleFileUploadProgress = (event, file, fileList) => {
  upload.isUploading = true;
};

/** 文件上传成功处理 */
const handleFileSuccess = (response, file, fileList) => {
  upload.open = false;
  upload.isUploading = false;
  proxy.$refs["uploadRef"].handleRemove(file);
  proxy.$alert(
      "<div style='overflow: auto;overflow-x: hidden;max-height: 70vh;padding: 10px 20px 0;'>" +
      response.msg +
      "</div>",
      "导入结果",
      { dangerouslyUseHTMLString: true }
  );
  getList();
};

/** ---------------------------------**/

function routeTo(link, row) {
  if (link !== "" && link.indexOf("http") !== -1) {
    window.location.href = link;
    return;
  }
  if (link !== "") {
    if (link === router.currentRoute.value.path) {
      window.location.reload();
    } else {
      router.push({
        path: link,
        query: {
          id: row.id,
        },
      });
    }
  }
}

/** 改变启用状态值 */
function handleStatusChange(row) {
  const text = row.validFlag === true ? "启用" : "禁用";
  const status = row.validFlag === true ? 1 : 0;
  proxy.$modal
      .confirm("确认要" + text + ' "' + row.datasourceName + '" 数据源吗？')
      .then(function () {
        editDatasourceStatus(row.id, status).then((response) => {
          proxy.$modal.msgSuccess(text + "成功");
          getList();
        });
      })
      .catch(function () {
        row.validFlag = !row.validFlag;
      });
}

queryParams.value.orderByColumn = defaultSort.value.prop;
queryParams.value.isAsc = defaultSort.value.order;
getList();
</script>

<style scoped lang="scss">
.pagecont-top {
  display: flex !important;
  flex-wrap: nowrap !important;
  align-items: center !important;
  gap: 8px;

  .ant-form {
    display: flex !important;
    flex-wrap: nowrap !important;
    flex: 0 1 auto !important;

    .ant-form-item {
      display: inline-flex !important;
      flex-shrink: 0 !important;
      margin-bottom: 0 !important;
    }
  }

  .data-action-btns {
    margin-left: auto;
    display: flex;
    align-items: center;
    gap: 8px;
    flex-shrink: 0;
  }

  .top-right-btn {
    flex-shrink: 0;
  }
}
</style>
