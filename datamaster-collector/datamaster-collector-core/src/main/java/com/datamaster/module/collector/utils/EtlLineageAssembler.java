package com.datamaster.module.collector.utils;

import com.datamaster.common.utils.StringUtils;
import com.datamaster.neo4j.node.TableNode;
import com.datamaster.neo4j.node.TaskNode;
import com.datamaster.neo4j.rel.TableToTaskRel;
import com.datamaster.neo4j.rel.TaskToTableRel;
import lombok.Data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * <P>
 * 用途：ETL 任务发布成功后，将 ChunJun/FlinkX mainArgs 中的 reader/writer 组件
 * 转换为 Neo4j 血缘节点（TableNode/TaskNode 及表→任务/任务→表关系），
 * 供 LineageDataService 写库。血缘开关关闭（LINEAGE_ENABLED=false）时不会被调用。
 * </p>
 *
 * @author: FXB
 * @create: 2025-08-29 10:00
 **/
public class EtlLineageAssembler {

    /**
     * <p>
     * 从 mainArgs 构建血缘数据
     * </p>
     * <p>
     * 约定：
     * <ul>
     *   <li>readerList/writerList 中每一项的 parameter.connection.table 为表名</li>
     *   <li>无表名（querySql/topic/流式组件）的组件跳过，不建节点</li>
     *   <li>datasourceHostPort 使用 host:port 形式，与数据资产侧查询口径一致</li>
     * </ul>
     *
     * @param mainArgs ETL 任务参数（TaskConverter.buildEtlTaskParams 产物）
     * @param taskId   平台任务ID
     * @param taskCode DS 任务编码
     * @param taskName 任务名称
     * @param type     任务类型：1：离线任务 2：实时任务 3：数据开发任务 4：作业任务
     * @return 血缘数据；无有效表时返回 null
     */
    public static LineageBuildResult build(Map<String, Object> mainArgs, Long taskId, String taskCode, String taskName, String type) {
        if (mainArgs == null) {
            return null;
        }
        List<Map<String, Object>> readerList = castList(mainArgs.get("readerList"));
        List<Map<String, Object>> writerList = castList(mainArgs.get("writerList"));

        // 先建任务节点占位，供关系指向
        TaskNode taskNode = TaskNode.builder()
                .name(taskName)
                .taskId(taskId)
                .taskCode(taskCode)
                .type(type)
                .build();

        List<TableNode> readerTables = new ArrayList<>();
        for (Map<String, Object> item : readerList) {
            TableNode tableNode = buildTableNode(item);
            if (tableNode != null) {
                // 表 -> 任务
                tableNode.setTableToTaskRels(Collections.singletonList(TableToTaskRel.builder()
                        .taskId(taskId)
                        .taskCode(taskCode)
                        .tableName(tableNode.getTableName())
                        .datasourceHostPort(tableNode.getDatasourceHostPort())
                        .task(taskNode)
                        .build()));
                readerTables.add(tableNode);
            }
        }

        List<TableNode> writerTables = new ArrayList<>();
        List<TaskToTableRel> taskToTableRels = new ArrayList<>();
        for (Map<String, Object> item : writerList) {
            TableNode tableNode = buildTableNode(item);
            if (tableNode != null) {
                // 任务 -> 表
                taskToTableRels.add(TaskToTableRel.builder()
                        .taskId(taskId)
                        .taskCode(taskCode)
                        .tableName(tableNode.getTableName())
                        .datasourceHostPort(tableNode.getDatasourceHostPort())
                        .table(tableNode)
                        .build());
                writerTables.add(tableNode);
            }
        }
        taskNode.setTaskToTableRels(taskToTableRels);

        if (readerTables.isEmpty() && writerTables.isEmpty()) {
            return null;
        }
        return new LineageBuildResult(readerTables, writerTables, taskNode);
    }

    private static TableNode buildTableNode(Map<String, Object> item) {
        if (item == null) {
            return null;
        }
        Object parameterObj = item.get("parameter");
        if (!(parameterObj instanceof Map)) {
            return null;
        }
        Map<?, ?> parameter = (Map<?, ?>) parameterObj;
        Object connectionObj = parameter.get("connection");
        if (!(connectionObj instanceof Map)) {
            return null;
        }
        Map<?, ?> connection = (Map<?, ?>) connectionObj;
        Object tableNameObj = connection.get("table");
        // querySql/topic/流式组件 无 table，跳过
        if (tableNameObj == null || StringUtils.isBlank(String.valueOf(tableNameObj))) {
            return null;
        }
        String tableName = String.valueOf(tableNameObj);
        Object host = parameter.get("host");
        Object port = parameter.get("port");
        if (host == null || port == null) {
            return null;
        }
        return TableNode.builder()
                .name(tableName)
                .tableName(tableName)
                .datasourceHostPort(host + ":" + port)
                .datasourceType(parameter.get("dbType") == null ? null : String.valueOf(parameter.get("dbType")))
                .dbName(parameter.get("dbName") == null ? null : String.valueOf(parameter.get("dbName")))
                .sid(parameter.get("sid") == null ? null : String.valueOf(parameter.get("sid")))
                .build();
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> castList(Object obj) {
        if (!(obj instanceof List)) {
            return Collections.emptyList();
        }
        return (List<Map<String, Object>>) obj;
    }

    @Data
    public static class LineageBuildResult {
        private final List<TableNode> readerTables;
        private final List<TableNode> writerTables;
        private final TaskNode taskNode;

        public LineageBuildResult(List<TableNode> readerTables, List<TableNode> writerTables, TaskNode taskNode) {
            this.readerTables = readerTables;
            this.writerTables = writerTables;
            this.taskNode = taskNode;
        }
    }
}