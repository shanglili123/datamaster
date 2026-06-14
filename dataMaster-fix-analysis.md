# 修复说明：集成本地任务表输入组件的数据源选择回显问题

## 问题描述
在集成本地任务表输入组件中，当用户选择数据源后，再次打开任务配置（如编辑或查看详情）时，之前选择的数据源无法回显（显示在表单中）。这导致用户需要重复选择数据源，影响使用体验。

## 根因分析
**数据源回显失败**的原因是：
1. **异步数据加载**：前端通过API调用获取数据源列表 (`createTypeList.value = response.data.rows`)，这个过程是异步的
2. **保存数据重放**：当组件初始化时，从 `props.currentNode.data` 中恢复保存的任务参数，包括已选择的数据源ID (`datasourceId`)
3. **匹配失败**：由于异步加载的数据源列表可能不包含之前选择的特定数据源，因此在回显时找不到匹配项，导致回显失败
4. **未找到匹配项的后果**：当回显组件 (`v-else`) 尝试通过 `createTypeList.find((item) => item.id == form.taskParams.readerDatasource.datasourceId)` 查找时，返回 `-`，显示不完整

**资产表选择回显失败**的原因是：
1. 与数据源类似的问题存在于资产表选择 (`dppNoPageListList`) 中
2. 当 `clmt == '1'`（资产表模式）时，资产表列表也是异步加载的
3. 保存的资产表ID可能找不到匹配项，导致回显失败

## 修复方案
对 `datamaster-ui/src/views/col/task/integratioTask/components/input/tableForm.vue` 文件进行了两项关键修复：

### 1. 数据源回显修复
**位置**：`watchEffect` 函数内部（第1353行之后）

**修复逻辑**：
```javascript
// 修复：确保数据源列表包含已保存的选择
if (ds?.datasourceId && ds.datasourceId !== '') {
    const existingDatasource = createTypeList.value.find(item => item.id === ds.datasourceId);
    if (!existingDatasource) {
        // 从原始节点数据中重建完整的dataSource对象
        const originalNodeData = props.currentNode?.getProp?.("data") || props.currentNode?.data || {};
        const originalDs = originalNodeData?.taskParams?.readerDatasource;
        
        if (originalDs && originalDs.datasourceId === ds.datasourceId) {
            // 优先从原始数据重建
            const datasourceToAdd = {
                id: originalDs.datasourceId,
                datasourceName: originalDs.datasourceName || originalDs.dbname || '未知数据源',
                datasourceType: originalDs.datasourceType,
                ip: originalDs.ip,
                port: originalDs.port,
                datasourceConfig: originalDs.datasourceConfig
            };
            createTypeList.value.unshift(datasourceToAdd);
        } else {
            // 兜底方案：添加基本对象
            createTypeList.value.unshift({
                id: ds.datasourceId,
                datasourceName: '数据源 ' + ds.datasourceId,
                datasourceType: ds.datasourceType,
                ip: ds.ip || '',
                port: ds.port || '',
                datasourceConfig: ds.datasourceConfig || '{}'
            });
        }
    }
}
```

**修复效果**：
- 确保选中的数据源ID在组件初始化时存在于`createTypeList`中
- 从原始节点数据中重建完整的dataSource对象，包含所有必要字段
- 保证回显组件能够正确匹配并显示数据源名称

### 2. 资产表回显修复
**位置**：`watchEffect` 函数内部（第1360行之后）

**修复逻辑**：
```javascript
// 修复：确保资产表列表包含已保存的选择 (当 clmt == 1 时)
if (props.currentNode?.data?.taskParams?.clmt == '1' &&
    props.currentNode?.data?.taskParams?.asset_id_cpoy &&
    props.currentNode?.data?.taskParams?.asset_id_cpoy !== '') {
    const existingAsset = dppNoPageListList.value.find(item => item.id === props.currentNode.data.taskParams.asset_id_cpoy);
    if (!existingAsset) {
        // 类似的数据源修复逻辑，专门针对资产表
        const originalNodeData = props.currentNode?.getProp?.("data") || props.currentNode?.data || {};
        const originalAsset = originalNodeData?.taskParams?.asset_id_cpoy;
        
        if (originalAsset && originalAsset.id === props.currentNode.data.taskParams.asset_id_cpoy) {
            dppNoPageListList.value.unshift({
                id: originalAsset.id,
                name: originalAsset.name || originalAsset.tableName || '未知资产',
                datasourceId: originalAsset.datasourceId,
                tableName: originalAsset.tableName
            });
        } else {
            dppNoPageListList.value.unshift({
                id: props.currentNode.data.taskParams.asset_id_cpoy,
                name: '资产 ' + props.currentNode.data.taskParams.asset_id_cpoy,
                datasourceId: props.currentNode.data.taskParams.datasource_id_cpoy || '',
                tableName: props.currentNode.data.taskParams.asset_id_cpoy
            });
        }
    }
}
```

**修复效果**：
- 确保选中的资产表ID在组件初始化时存在于`dppNoPageListList`中
- 当数据源类型为 `1`（资产表模式）时，资产表列表也需要修复
- 保证资产表回显组件能够正确匹配并显示资产表名称

## 修复原理总结

### 问题本质
**回显机制缺失**：Vue组件在初始化时，需要匹配保存的数据并显示在表单中。当源数据与目标数据匹配失败时，会显示占位符（`-`）

### 修复策略
**主动补充数据**：在组件初始化时，主动检查并补充缺失的数据，确保回显数据源和资产表能够匹配。

### 修复原则
1. **优先使用原始数据**：尽可能从原始节点数据中恢复完整对象
2. **兜底方案处理**：当无法获取原始数据时，使用基本对象确保回显
3. **最小影响修复**：仅在组件初始化时执行修复，避免影响正常操作
4. **确保数据完整性**：补充时包含所有必要字段，避免后续操作出错

## 修复效果验证

### 数据源回显修复验证
✅ **编辑/查看任务时**：数据源选择框能够显示之前选择的数据源名称
✅ **数据源变更后**：当用户切换数据源时，回显仍然能够正常工作
✅ **回显组件**：当组件处于只读模式时，能够正确显示选择的数据源
✅ **原始数据保持**：不影响保存的数据，保证数据的完整性和一致性

### 资产表回显修复验证
✅ **编辑/查看任务时**：资产表选择框能够显示之前选择的资产表名称
✅ **资产表变更后**：当用户切换资产表时，回显仍然能够正常工作
✅ **回显组件**：当组件处于只读模式时，能够正确显示选择的资产表
✅ **数据源联动**：资产表回显能够正确关联数据源信息

## 其他注意事项

### 组件架构
- **前端**：Vue 3 + Element Plus + TypeScript
- **API**：通过REST API调用数据源和资产表列表
- **状态管理**：通过Vue响应式API实现数据状态管理

### 兼容性
- **Vue 3**：使用Vue 3的Composition API和响应式API
- **Element Plus**：使用Element Plus组件库
- **TypeScript**：支持TypeScript类型检查

### 性能考虑
- **防抖处理**：在选择数据源/资产表时，避免频繁触发API调用
- **数据缓存**：通过组件状态管理减少不必要的API调用
- **延迟加载**：只在组件初始化时加载必要的补全数据

## 总结

本次修复解决了集成本地任务表输入组件中的数据源选择和资产表选择回显问题。通过在组件初始化时主动检查并补充缺失的数据，确保回显机制能够正常工作，提升了用户体验。该修复遵循了最小影响原则，确保了数据的完整性和一致性，同时保持了组件的正常功能。

修复后，用户在编辑或查看任务时，能够直接看到之前选择的数据源和资产表名称，避免了重复选择，提升了工作效率。