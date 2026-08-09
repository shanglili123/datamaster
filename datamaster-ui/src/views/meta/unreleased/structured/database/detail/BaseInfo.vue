<template>
  <div class="base-info">
    <div class="module-head">基础信息</div>
    <div class="module-body infotop">
      <a-row :gutter="2">
        <a-col :span="8">
          <div class="infotop-row border-top">
            <div class="infotop-row-lable">所属分层</div>
            <div class="infotop-row-value">
              <dict-tag
                :options="toValue(dicts.meta_dw_layers)"
                :value="detail.belongingLayer"
              />
            </div>
          </div>
        </a-col>

        <a-col :span="8">
          <div class="infotop-row border-top">
            <div class="infotop-row-lable">所属系统</div>
            <div class="infotop-row-value">
              {{ getFormatValue(detail.belongingSystem) }}
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
              {{ getFormatValue(detail.ip) }}
            </div>
          </div>
        </a-col>

        <a-col :span="8">
          <div class="infotop-row border-top">
            <div class="infotop-row-lable">端口号</div>
            <div class="infotop-row-value">
              {{ getFormatValue(detail.port) }}
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
            <div class="infotop-row-lable">表数量</div>
            <div class="infotop-row-value">
              {{ getFormatValue(detail.tableCount) }}
            </div>
          </div>
        </a-col>

        <a-col :span="8">
          <div class="infotop-row border-top">
            <div class="infotop-row-lable">字段数量</div>
            <div class="infotop-row-value">
              {{ getFormatValue(detail.columnCount) }}
            </div>
          </div>
        </a-col>
      </a-row>

      <a-row :gutter="2">
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
        <!-- <a-col :span="8">
                    <div class="infotop-row border-top">
                        <div class="infotop-row-lable">安全等级</div>
                        <div class="infotop-row-value">
                            {{ getFormatValue(detail.safetyLevelName) }}
                        </div>
                    </div>
                </a-col> -->
      </a-row>
    </div>
  </div>
</template>

<script setup name="SynchronizeTaskBaseInfo">
import { getCurrentInstance, toValue } from "vue";
const { proxy } = getCurrentInstance();
const dicts = proxy.useDict("datasource_type", "meta_dw_layers");

const props = defineProps({
  detail: {
    type: Object,
    required: true,
  },
});
</script>
