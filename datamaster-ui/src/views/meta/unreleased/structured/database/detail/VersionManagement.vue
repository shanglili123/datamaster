<template>
    <div class="version-management">
        <qt-wrap
            :columns="tableStroe.columns"
            :tableRef="tableRef"
            :config="{ fullContent: false, actions: { table: { search: false } } }"
        >
            <qt-table v-bind="tableStroe" ref="tableRef">
                <template #active-version="{ row }">
                    <CheckCircleOutlined v-show="row.activeVersion == 'Y'" style="color: #52c41a" />
                </template>
                <template #handle="{ row }">
                    <a-button type="link" :icon="h(EyeOutlined)" @click="handleDetailClick(row)">
                        详情
                    </a-button>
                    <a-button type="link" :icon="h(EditOutlined)"> 恢复 </a-button>
                </template>
            </qt-table>
        </qt-wrap>

        <a-modal v-model:open="dialog.open" title="版本详情" width="800" destroy-on-close>
            <a-form :label-col="{ style: { width: '100px' } }">
                <a-form-item label="库名：">
                    <a-input v-model:value="dialog.row.dbName" placeholder="请输入库名" disabled />
                </a-form-item>
                <a-form-item label="版本号：">
                    <a-input v-model:value="dialog.row.version" placeholder="请输入版本号" disabled />
                </a-form-item>
                <a-form-item label="变更类型：">
                    <a-input
                        v-model:value="dialog.row.updateType"
                        placeholder="请输入变更类型"
                        disabled
                    />
                </a-form-item>
                <a-form-item label="变更说明：">
                    <a-textarea
                        v-model:value="dialog.row.updateMsg"
                        placeholder="请输入变更说明"
                        disabled
                        :auto-size="{ minRows: 8 }"
                        :maxlength="500"
                        show-count
                    />
                </a-form-item>
                <a-form-item label="当前版本：">
                    <dict-tag
                        :options="toValue(dicts.sys_yes_no)"
                        :value="dialog.row.activeVersion"
                    />
                </a-form-item>
                <a-form-item label="创建人：">
                    <a-input v-model:value="dialog.row.name" placeholder="请输入修改人" disabled />
                </a-form-item>
                <a-form-item label="创建时间：">
                    <a-input v-model:value="dialog.row.time" placeholder="请输入创建时间" disabled />
                </a-form-item>
            </a-form>
            <template #footer>
                <div class="dialog-footer">
                    <a-button @click="dialog.open = false">关闭</a-button>
                </div>
            </template>
        </a-modal>
    </div>
</template>

<script setup name="VersionManagement">
    import { reactive, toValue, getCurrentInstance, h } from 'vue';
    import { CheckCircleOutlined, EyeOutlined, EditOutlined } from '@ant-design/icons-vue';

    const { proxy } = getCurrentInstance();
    const dicts = proxy.useDict('sys_yes_no');

    const tableStroe = reactive({
        columns: [
            {
                label: '编号',
                prop: 'id',
                sortable: true,
                width: 90
            },
            {
                label: '库名',
                prop: 'dbName',
                showOverflowTooltip: {
                    effect: 'light'
                },
                minWidth: 140
            },
            {
                label: '版本号',
                prop: 'version',
                width: 90
            },
            {
                label: '变更类型',
                prop: 'updateType',
                width: 90
            },
            {
                label: '变更说明',
                prop: 'updateMsg',
                minWidth: 240,
                showOverflowTooltip: {
                    effect: 'light'
                }
            },
            {
                label: '当前版本',
                prop: 'activeVersion',
                slot: 'active-version',
                width: 90
            },
            {
                label: '创建人',
                prop: 'createBy',
                width: 90
            },
            {
                label: '创建时间',
                prop: 'createTime',
                sortable: true,
                width: 160,
                date: true
            },
            {
                label: '操作',
                width: 240,
                fixed: 'right',
                slot: 'handle'
            }
        ],
        func: function () {
            return new Promise((resolve) => {
                setTimeout(() => {
                    resolve({
                        data: {
                            rows: [
                                {
                                    id: 1,
                                    dbName: 'dataMaster_meta',
                                    version: '1.0.0',
                                    updateType: '新增',
                                    updateMsg:
                                        '新增用户表字段，包括用户姓名、联系方式、注册时间等基础信息，优化数据结构设计',
                                    activeVersion: 'Y',
                                    createBy: 'admin',
                                    createTime: '2021-09-01 10:00:00'
                                },
                                {
                                    id: 2,
                                    dbName: 'dataMaster_meta',
                                    version: '1.0.1',
                                    updateType: '修改',
                                    updateMsg: '修改表结构',
                                    activeVersion: 'N',
                                    createBy: 'admin',
                                    createTime: '2021-09-01 10:00:00'
                                },
                                {
                                    id: 3,
                                    dbName: 'dataMaster_meta',
                                    version: '1.0.2',
                                    updateType: '回滚',
                                    updateMsg: '回滚表结构',
                                    activeVersion: 'N',
                                    createBy: 'admin',
                                    createTime: '2021-09-01 10:00:00'
                                }
                            ],
                            total: 3
                        }
                    });
                }, 1000);
            });
        }
    });

    const dialog = reactive({
        open: false,
        row: {}
    });

    function handleDetailClick(row) {
        dialog.row = row;
        dialog.open = true;
    }
</script>
