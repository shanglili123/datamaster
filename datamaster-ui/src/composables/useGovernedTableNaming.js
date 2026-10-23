import { computed, reactive, ref, watch } from 'vue';
import { listDataLayer } from '@/api/mdl/dataLayer/dataLayer';
import { listBusinessCategory } from '@/api/mdl/businessCategory/businessCategory';
import { listDataDomain, listDataDomainByCategoryId } from '@/api/mdl/dataDomain/dataDomain';
import { listThemeDomain } from '@/api/mdl/themeDomain/themeDomain';
import { previewTableName } from '@/api/std/model/model';

const rows = response => Array.isArray(response?.data)
  ? response.data : (response?.data?.rows || response?.data?.list || []);
const errorText = error => error?.response?.data?.msg || error?.message || '请求失败，请检查权限或稍后重试';
const hasId = value => value !== undefined && value !== null && value !== '';
const flatten = items => items.flatMap(item => [item, ...flatten(item.children || [])]);
const toTree = items => {
  const all = [...new Map(flatten(items).map(item => [String(item.id), { ...item, children: [] }])).values()];
  const byId = new Map(all.map(item => [String(item.id), item]));
  const roots = [];
  all.forEach(item => {
    const parent = byId.get(String(item.parentId));
    if (parent && parent !== item) parent.children.push(item);
    else roots.push(item);
  });
  return roots;
};

// Both forms use the server as the only authority for the final physical name.
export function useGovernedTableNaming(getModel, getBaseName, getDatasourceType = () => undefined) {
  const options = reactive({ dataLayers: [], businessCategories: [], dataDomains: [], themeDomains: [] });
  const optionErrors = reactive({});
  const optionsLoading = ref(false);
  const previewLoading = ref(false);
  const previewResult = ref(null);
  const previewError = ref('');
  let optionRequest = 0;
  let domainRequest = 0;
  let previewRequest = 0;

  const payload = computed(() => {
    const model = getModel() || {};
    const application = String(model.tableType) === '4';
    return {
      baseTableName: String(getBaseName() || '').trim(),
      tableType: model.tableType == null ? undefined : String(model.tableType),
      dataLayerId: model.dataLayerId,
      businessCategoryId: model.businessCategoryId === undefined || model.businessCategoryId === '' ? undefined : model.businessCategoryId,
      dataDomainId: application ? undefined : model.dataDomainId,
      themeDomainId: application ? model.themeDomainId : undefined,
      tableCase: model.tableCase || '2',
      datasourceType: getDatasourceType() || undefined,
    };
  });
  const signature = computed(() => JSON.stringify(payload.value));
  const resetPreview = () => {
    previewRequest++;
    previewResult.value = null;
    previewError.value = '';
    previewLoading.value = false;
  };
  watch(signature, resetPreview, { flush: 'sync' });

  const refreshDomains = async () => {
    const request = ++domainRequest;
    const model = getModel() || {};
    const categoryId = model.businessCategoryId;
    const layerId = model.dataLayerId;
    const responses = await Promise.allSettled([
      hasId(categoryId)
        ? listDataDomainByCategoryId({ businessCategoryId: categoryId, pageNum: 1, pageSize: 9999 })
        : listDataDomain({ pageNum: 1, pageSize: 9999 }),
      hasId(layerId) ? listThemeDomain({ dataLayerId: layerId, pageNum: 1, pageSize: 9999 }) : Promise.resolve({ data: [] }),
    ]);
    if (request !== domainRequest) return;
    ['dataDomains', 'themeDomains'].forEach((key, index) => {
      const result = responses[index];
      if (result.status === 'fulfilled') {
        options[key] = flatten(rows(result.value));
        delete optionErrors[key];
      } else {
        options[key] = [];
        optionErrors[key] = `${key === 'dataDomains' ? '数据域' : '主题域'}加载失败：${errorText(result.reason)}`;
      }
    });
  };
  const loadOptions = async () => {
    const request = ++optionRequest;
    optionsLoading.value = true;
    const results = await Promise.allSettled([
      listDataLayer({ pageNum: 1, pageSize: 9999 }),
      listBusinessCategory({ pageNum: 1, pageSize: 9999 }),
      refreshDomains(),
    ]);
    if (request !== optionRequest) return;
    ['dataLayers', 'businessCategories'].forEach((key, index) => {
      const result = results[index];
      if (result.status === 'fulfilled') {
        options[key] = key === 'businessCategories' ? toTree(rows(result.value)) : flatten(rows(result.value));
        delete optionErrors[key];
      } else {
        options[key] = [];
        optionErrors[key] = `${key === 'dataLayers' ? '数仓分层' : '业务分类'}加载失败：${errorText(result.reason)}`;
      }
    });
    optionsLoading.value = false;
  };
  const changeType = () => {
    const model = getModel();
    if (String(model.tableType) === '4') model.dataDomainId = undefined;
    else model.themeDomainId = undefined;
    refreshDomains();
  };
  const changeLayer = () => {
    getModel().themeDomainId = undefined;
    options.themeDomains = [];
    refreshDomains();
  };
  const changeCategory = () => {
    getModel().dataDomainId = undefined;
    options.dataDomains = [];
    refreshDomains(); // Clearing the category restores all data domains.
  };
  const preview = async () => {
    resetPreview();
    const value = payload.value;
    if (!/^[A-Za-z][A-Za-z0-9_]*$/.test(value.baseTableName)) previewError.value = '基础名称须以字母开头，仅含字母、数字和下划线';
    else if (!hasId(value.dataLayerId)) previewError.value = '请选择数仓分层';
    else if (!['1', '2', '3', '4'].includes(value.tableType)) previewError.value = '请选择表类型';
    else if (value.tableType === '4' && !hasId(value.themeDomainId)) previewError.value = '应用表必须选择主题域';
    else if (value.tableType !== '4' && !hasId(value.dataDomainId)) previewError.value = '明细、汇总、维度表必须选择数据域';
    if (previewError.value) return null;
    const request = ++previewRequest;
    const current = signature.value;
    previewLoading.value = true;
    try {
      const response = await previewTableName(value);
      if (request !== previewRequest || current !== signature.value) return null;
      if (!response?.data?.tableName) throw new Error(response?.msg || '服务端未返回最终表名');
      previewResult.value = response.data;
      return response.data;
    } catch (error) {
      if (request === previewRequest) previewError.value = `命名预览失败：${errorText(error)}`;
      return null;
    } finally {
      if (request === previewRequest) previewLoading.value = false;
    }
  };
  const cancel = () => {
    ++optionRequest;
    ++domainRequest;
    optionsLoading.value = false;
    resetPreview();
  };
  return reactive({
    options, optionErrors, optionsLoading, previewLoading, previewResult, previewError,
    payload, signature, preview, resetPreview, loadOptions, refreshDomains, changeType, changeLayer, changeCategory, cancel,
    name: (key, id) => flatten(options[key] || []).find(item => String(item.id) === String(id))?.name || '',
  });
}
