<template>
  <a-alert v-for="(error, key) in naming.optionErrors" :key="key" type="error" show-icon :message="error" style="margin-bottom:8px" />
  <a-button v-if="Object.keys(naming.optionErrors).length && !readonly" size="small" :loading="naming.optionsLoading" @click="naming.loadOptions()">重试加载治理选项</a-button>
  <a-row :gutter="20">
    <a-col :span="12">
      <a-form-item label="数仓分层" :required="!readonly">
        <a-select v-if="!readonly" :disabled="disabled || identityLocked" v-model:value="model.dataLayerId" allow-clear show-search option-filter-prop="label" placeholder="请选择分层" :loading="naming.optionsLoading" @change="naming.changeLayer()">
          <a-select-option v-for="item in naming.options.dataLayers" :key="item.id" :value="item.id" :label="item.name">{{ item.name }}</a-select-option>
        </a-select>
        <div v-else class="form-readonly">{{ naming.name('dataLayers', model.dataLayerId) || model.dataLayerName || model.dataLayerId || '-' }}</div>
      </a-form-item>
    </a-col>
    <a-col :span="12">
      <a-form-item label="表类型" :required="!readonly">
        <a-select v-if="!readonly" :disabled="disabled || identityLocked" v-model:value="model.tableType" placeholder="请选择表类型" @change="naming.changeType()">
          <a-select-option v-for="(name, value) in tableTypes" :key="value" :value="value">{{ name }}</a-select-option>
        </a-select>
        <div v-else class="form-readonly">{{ tableTypes[model.tableType] || '-' }}</div>
      </a-form-item>
    </a-col>
    <a-col :span="12">
      <a-form-item label="业务分类">
        <a-tree-select v-if="!readonly" :disabled="disabled" v-model:value="model.businessCategoryId" :tree-data="naming.options.businessCategories" :field-names="{ value: 'id', label: 'name', children: 'children' }" allow-clear show-search tree-node-filter-prop="name" placeholder="可选业务分类（资产标签）" @change="handleBusinessCategoryChange" />
        <div v-else class="form-readonly">{{ naming.name('businessCategories', model.businessCategoryId) || model.businessCategoryName || model.businessCategoryId || '-' }}</div>
      </a-form-item>
    </a-col>
    <a-col :span="12">
      <a-form-item :label="String(model.tableType) === '4' ? '主题域' : '数据域'" :required="!readonly">
        <a-select v-if="!readonly && String(model.tableType) !== '4'" :disabled="disabled || identityLocked" v-model:value="model.dataDomainId" allow-clear show-search option-filter-prop="label" placeholder="请选择数据域">
          <a-select-option v-for="item in naming.options.dataDomains" :key="item.id" :value="item.id" :label="item.name">{{ item.name }}</a-select-option>
        </a-select>
        <a-select v-else-if="!readonly" v-model:value="model.themeDomainId" allow-clear show-search option-filter-prop="label" :disabled="disabled || identityLocked || !model.dataLayerId" placeholder="请选择主题域">
          <a-select-option v-for="item in naming.options.themeDomains" :key="item.id" :value="item.id" :label="item.name">{{ item.name }}</a-select-option>
        </a-select>
        <div v-else class="form-readonly">{{ String(model.tableType) === '4' ? (naming.name('themeDomains', model.themeDomainId) || model.themeDomainName || model.themeDomainId || '-') : (naming.name('dataDomains', model.dataDomainId) || model.dataDomainName || model.dataDomainId || '-') }}</div>
      </a-form-item>
    </a-col>
  </a-row>
</template>

<script setup>
const props = defineProps({
  model: { type: Object, required: true },
  naming: { type: Object, required: true },
  readonly: { type: Boolean, default: false },
  disabled: { type: Boolean, default: false },
  identityLocked: { type: Boolean, default: false },
});
const handleBusinessCategoryChange = value => {
  // Keep an explicit null so updates can clear an existing classification without sending id=0.
  props.model.businessCategoryId = value === undefined || value === '' ? null : value;
  if (props.identityLocked) props.naming.refreshDomains();
  else props.naming.changeCategory();
};
const tableTypes = { '1': '明细表', '2': '汇总表', '3': '维度表', '4': '应用表' };
</script>
