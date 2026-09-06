<template>
    <div class="object-lineage-graph">
        <div class="graph-wrap" id="object_lineage_shape"></div>
        <TeleportContainer />
        <div class="graph-legend">
            <span class="lg-item"><i class="lg-dot data"></i>数据·支撑表</span>
            <span class="lg-item"><i class="lg-dot object"></i>对象</span>
            <span class="lg-item"><i class="lg-dot permission"></i>权限·访问摘要</span>
        </div>
    </div>
</template>

<script setup name="OntObjectLineageGraph">
    import { getTeleport, register } from '@antv/x6-vue-shape';
    import { createBaseGraph } from '@/views/meta/components/common';
    import { defineComponent, onMounted, h } from 'vue';
    import { Modal } from 'ant-design-vue';
    import OntObjectNode from './ObjectNode.vue';
    import LineageTextNode from './LineageTextNode.vue';

    const props = defineProps({
        currentObject: {
            type: Object,
            default: null
        },
        tables: {
            type: Array,
            default: () => []
        },
        permission: {
            type: Object,
            default: null
        }
    });

    const TeleportContainer = defineComponent(getTeleport());

    // 节点尺寸
    const W = 200;
    const OBJ_H = 96;
    const SUB_H = 64;
    const GAP_X = 60;
    const GAP_Y = 40;

    // 注册「语义对象」节点形状
    try {
        register({
            shape: 'OntObjectNode',
            width: 240,
            height: OBJ_H,
            component: OntObjectNode,
            ports: {
                groups: {
                    top: { position: 'top', attrs: { circle: { r: 4, magnet: true, stroke: '#722ed1', fill: '#fff' } } },
                    right: { position: 'right', attrs: { circle: { r: 4, magnet: true, stroke: '#722ed1', fill: '#fff' } } },
                    bottom: { position: 'bottom', attrs: { circle: { r: 4, magnet: true, stroke: '#722ed1', fill: '#fff' } } },
                    left: { position: 'left', attrs: { circle: { r: 4, magnet: true, stroke: '#722ed1', fill: '#fff' } } }
                },
                items: [
                    { id: 'top', group: 'top' },
                    { id: 'right', group: 'right' },
                    { id: 'bottom', group: 'bottom' },
                    { id: 'left', group: 'left' }
                ]
            }
        });
    } catch {
        // 已注册则忽略
    }

    // 注册权限摘要文本节点
    try {
        register({
            shape: 'LineageTextNode',
            width: W,
            height: SUB_H,
            component: LineageTextNode,
            ports: {
                groups: {
                    top: { position: 'top', attrs: { circle: { r: 4, magnet: true, stroke: '#8c8c8c', fill: '#fff' } } },
                    right: { position: 'right', attrs: { circle: { r: 4, magnet: true, stroke: '#8c8c8c', fill: '#fff' } } },
                    bottom: { position: 'bottom', attrs: { circle: { r: 4, magnet: true, stroke: '#8c8c8c', fill: '#fff' } } },
                    left: { position: 'left', attrs: { circle: { r: 4, magnet: true, stroke: '#8c8c8c', fill: '#fff' } } }
                },
                items: [
                    { id: 'top', group: 'top' },
                    { id: 'right', group: 'right' },
                    { id: 'bottom', group: 'bottom' },
                    { id: 'left', group: 'left' }
                ]
            }
        });
    } catch {
        // 已注册则忽略
    }

    function objId(obj) {
        return 'object_' + (obj && obj.conceptId != null ? obj.conceptId : 'x');
    }

    function buildShapes() {
        const nodes = [];
        const edges = [];
        const obj = props.currentObject;
        if (!obj) return { nodes, edges };

        const oid = objId(obj);
        const tables = (props.tables || []).filter(t => t && t.tableName);
        const permission = props.permission;

        // ===== 左列：支撑表（数据维度）=====
        const tableIds = [];
        let sY = 0;
        tables.forEach((table, i) => {
            const id = 'table_' + table.tableName + '_' + (table.datasourceHostPort || '') + '_' + i;
            tableIds.push(id);
            sY = i * (SUB_H + GAP_Y);
            nodes.push({
                id,
                shape: 'TableNode',
                width: W,
                height: SUB_H,
                data: { name: table.tableName, fields: [] },
                x: 0,
                y: sY
            });
            // 表 -> 对象（MATERIALIZES）
            edges.push({
                id: id + '_' + oid,
                shape: 'er-relationship',
                attrs: { line: { stroke: '#1890ff', targetMarker: { name: 'classic', size: 8 } } },
                source: { cell: id, port: 'right' },
                target: { cell: oid, port: 'left' }
            });
        });
        const objCx = (sY || 0) / 2;

        // ===== 中心：对象 =====
        const objX = W + GAP_X;
        nodes.push({
            id: oid,
            shape: 'OntObjectNode',
            width: 240,
            height: OBJ_H,
            data: {
                name: obj.conceptName || obj.tableName || '对象',
                code: obj.conceptCode || '',
                table: obj.tableName || ''
            },
            x: objX,
            y: objCx - OBJ_H / 2
        });

        // ===== 右侧：权限摘要 =====
        if (permission) {
            const permOk = permission.accessible !== false;
            const pid = 'permission';
            nodes.push({
                id: pid,
                shape: 'LineageTextNode',
                width: 260,
                height: 88,
                data: {
                    dimension: 'permission',
                    dimLabel: '权限',
                    title: permOk ? '允许访问' : '拒绝访问',
                    sub: permission.message || (permOk ? '当前空间可访问该对象支撑表' : '无权限摘要'),
                    tag: permOk ? (Array.isArray(permission.allowedColumns) && permission.allowedColumns.length ? '可查字段 ' + permission.allowedColumns.length + ' 个' : '全量放行') : '受限',
                    tagClass: permOk ? 'perm-ok' : 'perm-deny'
                },
                x: objX + 240 + GAP_X,
                y: objCx - 44
            });
            edges.push({
                id: oid + '_' + pid,
                shape: 'er-relationship',
                attrs: { line: { stroke: permOk ? '#52c41a' : '#ff4d4f', strokeDasharray: '5 5', targetMarker: { name: 'classic', size: 8 } } },
                source: { cell: oid, port: 'right' },
                target: { cell: pid, port: 'left' }
            });
        }

        return { nodes, edges };
    }

    onMounted(() => {
        const container = document.getElementById('object_lineage_shape');
        if (!container) return;
        const { graph } = createBaseGraph(container);
        const { nodes, edges } = buildShapes();
        graph.addNodes(nodes);
        graph.addEdges(edges);
        if (nodes.length) graph.centerContent();
        graph.zoomToFit({ padding: { top: 40, right: 40, bottom: 40, left: 40 } });
        const zoom = graph.zoom();
        if (zoom > 1) graph.zoomTo(1);

        // 节点点击：展示对应节点信息
        graph.on('node:click', ({ node }) => {
            const d = node.getData() || {};
            const shape = node.shape;
            if (shape === 'OntObjectNode') {
                Modal.info({
                    title: '对象',
                    width: 520,
                    content: h('div', {}, [
                        h('p', {}, ['名称：' + (d.name || '-')]),
                        h('p', {}, ['编码：' + (d.code || '-')]),
                        h('p', {}, ['支撑表：' + (d.table || '-')])
                    ]),
                    okText: '关闭'
                });
            } else if (shape === 'TableNode') {
                Modal.info({
                    title: '支撑表',
                    width: 520,
                    content: h('p', {}, ['表名：' + (d.name || '-')]),
                    okText: '关闭'
                });
            } else if (shape === 'LineageTextNode' && d.dimension === 'permission') {
                Modal.info({
                    title: '权限摘要',
                    width: 520,
                    content: h('div', {}, [
                        h('p', {}, ['状态：' + d.title]),
                        h('p', {}, ['说明：' + d.sub]),
                        h('p', {}, ['标签：' + (d.tag || '-')])
                    ]),
                    okText: '关闭'
                });
            }
        });
    });
</script>

<style lang="scss" scoped>
    .object-lineage-graph {
        width: 100%;
        height: 500px;
        display: flex;
        flex-direction: column;
        gap: 8px;

        .graph-wrap {
            flex: 1;
            min-height: 0;
            width: 100%;
        }

        .graph-legend {
            display: flex;
            flex-wrap: wrap;
            gap: 16px;
            padding: 8px 12px;
            background: #fafafa;
            border-radius: 6px;
            border: 1px solid #f0f0f0;

            .lg-item {
                display: inline-flex;
                align-items: center;
                gap: 6px;
                font-size: 12px;
                color: #595959;

                .lg-dot {
                    display: inline-block;
                    width: 10px;
                    height: 10px;
                    border-radius: 50%;

                    &.data { background: #1890ff; }
                    &.object { background: #722ed1; }
                    &.permission { background: #595959; }
                }
            }
        }

        ::v-deep(.x6-port-body) {
            opacity: 0;
            transition: opacity 0.2s ease;
        }

        ::v-deep(.x6-node) {
            &:hover .x6-port-body {
                opacity: 1;
            }
        }
    }
</style>
