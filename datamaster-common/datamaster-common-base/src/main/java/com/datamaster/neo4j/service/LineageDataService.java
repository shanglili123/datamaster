

package com.datamaster.neo4j.service;

import com.alibaba.fastjson.JSONObject;
import org.neo4j.driver.*;
import org.neo4j.driver.Record;
import org.neo4j.driver.types.Relationship;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.datamaster.neo4j.config.Neo4jProperties;
import com.datamaster.neo4j.dto.LineageDTO;
import com.datamaster.neo4j.dto.ObjectLineageDTO;
import com.datamaster.neo4j.node.ActionExecutionNode;
import com.datamaster.neo4j.node.ObjectNode;
import com.datamaster.neo4j.node.TableNode;
import com.datamaster.neo4j.node.TaskNode;
import com.datamaster.neo4j.rel.TableToTaskRel;
import com.datamaster.neo4j.rel.TaskToTableRel;
import com.datamaster.neo4j.rel.ObjectToTableRel;
import com.datamaster.neo4j.repository.ActionExecutionRepository;
import com.datamaster.neo4j.repository.ObjectRepository;
import com.datamaster.neo4j.repository.TableRepository;
import com.datamaster.neo4j.repository.TaskRepository;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * <P>
 * 用途:
 * </p>
 *
 * @author: FXB
 * @create: 2025-08-27 13:46
 **/
@Service
@ConditionalOnProperty(prefix = "datamaster.lineage", name = "enabled", havingValue = "true")
public class LineageDataService {

    @Resource
    private TableRepository tableRepository;

    @Resource
    private TaskRepository taskRepository;

    @Resource
    private ActionExecutionRepository actionExecutionRepository;

    @Resource
    private ObjectRepository objectRepository;

    @Resource
    private Neo4jProperties neo4jProperties;


    @Transactional("neo4jTransactionManager")
    public LineageDTO lineage(String datasourceHostPort, String tableName) {
        LineageDTO lineageDto = tableRepository.findLineage(datasourceHostPort, tableName)
                .orElse(null);
        if (lineageDto == null) {
            return new LineageDTO();
        }

        //查询关系数据
        getRels(lineageDto, datasourceHostPort, tableName);
        return lineageDto;
    }

    /**
     * 查询关系数据
     *
     * @param lineageDto
     * @param datasourceHostPort
     * @param tableName
     */
    void getRels(LineageDTO lineageDto, String datasourceHostPort, String tableName) {
        // 创建驱动
        Driver driver = GraphDatabase.driver(neo4jProperties.getUri(),
                AuthTokens.basic(neo4jProperties.getUsername(), neo4jProperties.getPassword()));

        // 打开自动关闭的会话
        try (Session session = driver.session(SessionConfig.forDatabase("neo4j"))) {
            // 3. 执行 Cypher，返回一条记录
            Result result = session.run(
                    "MATCH (currentTable:Table {datasourceHostPort: $datasourceHostPort,tableName: $tableName})   " +
                            "OPTIONAL MATCH (currentTable)<-[r1:TASK_TO_TABLE]-(sourceTask:Task)   " +
                            "OPTIONAL MATCH (sourceTask)<-[r2:TABLE_TO_TASK]-(sourceTable:Table)   " +
                            "OPTIONAL MATCH (currentTable)-[r3:TABLE_TO_TASK]->(targetTask:Task)   " +
                            "OPTIONAL MATCH (targetTask)-[r4:TASK_TO_TABLE]->(targetTable:Table)   " +
                            "RETURN     " +
                            "collect(DISTINCT sourceTask) + collect(DISTINCT targetTask) AS tasks,   " +
                            "currentTable + collect(DISTINCT sourceTable) + collect(DISTINCT targetTable)  AS tables,   " +
                            "collect(DISTINCT r1) + collect(DISTINCT r2) +collect(DISTINCT r3) + collect(DISTINCT r4) AS  rels ",
                    Values.parameters("datasourceHostPort", datasourceHostPort, "tableName", tableName)
            );
            if (result.hasNext()) {
                Record row = result.single();
                List<Relationship> rels = row.get("rels").asList(v -> v.asRelationship());
                List<JSONObject> relsList = new ArrayList<>();
                for (Relationship rel : rels) {
                    JSONObject relObj = new JSONObject();
                    relObj.put("startNodeId", rel.startNodeId());
                    relObj.put("endNodeId", rel.endNodeId());
                    relObj.put("type", rel.type());
                    JSONObject properties = new JSONObject();
                    properties.put("taskId", rel.get("taskId").asLong());
                    properties.put("taskCode", rel.get("taskCode").asString());
                    properties.put("datasourceHostPort", rel.get("datasourceHostPort").asString());
                    properties.put("tableName", rel.get("tableName").asString());
                    relObj.put("properties", properties);
                    relsList.add(relObj);
                }
                lineageDto.setRels(relsList);
            }
        } finally {
            if (driver != null) {
                driver.close();
            }
        }
    }

    /**
     * 删除任务
     *
     * @param taskId
     */
    @Transactional("neo4jTransactionManager")
    public void deleteTask(Long taskId) {
        TaskNode oldTaskNode = taskRepository.findByTaskId(taskId);
        if (oldTaskNode != null) {
            taskRepository.delete(oldTaskNode);
        }
    }

    /**
     * 保存动作执行节点（动作血缘，可插拔）
     * <p>
     * 由 ActionExecutionServiceImpl 在 DML 执行成功/失败后调用；
     * 仅当 LINEAGE_ENABLED=true 时本服务存在，写入失败不影响动作执行主流程。
     *
     * @param node 动作执行节点
     */
    @Transactional("neo4jTransactionManager")
    public void saveActionExecution(ActionExecutionNode node) {
        if (node == null) {
            return;
        }
        actionExecutionRepository.save(node);
    }

    /**
     * 保存语义对象节点（对象血缘，可插拔）
     * <p>
     * upsert 语义：对象以 conceptId 为业务键，已存在则复用其节点（并沿用 MATERIALIZES 关系）。
     * 由本体对象实例层在查询/执行时调用；写入失败不影响业务主流程（调用方自行 try/catch）。
     *
     * @param node 语义对象节点；node.objectId=conceptId
     */
    @Transactional("neo4jTransactionManager")
    public void saveObject(ObjectNode node) {
        if (node == null) {
            return;
        }
        if (node.getConceptId() == null) {
            return;
        }
        objectRepository.findByConceptId(node.getConceptId()).ifPresent(existing -> {
            node.setId(existing.getId());
            node.setMaterializesRels(existing.getMaterializesRels());
        });
        objectRepository.save(node);
    }

    /**
     * 保存对象血缘「数据维度」：语义对象 + 支撑物理表（MATERIALIZES）。
     * <p>
     * 对象与表均按业务键 upsert（对象=conceptId，表=tableName+datasourceHostPort），
     * 避免每次查询重复建节点/关系。由本体对象实例层在实例查询时调用，写入失败不影响查询。
     *
     * @param conceptId         概念ID（对象业务键）
     * @param ontologyId        本体ID
     * @param conceptCode       概念编码（可空）
     * @param conceptName       概念名称（可空）
     * @param tableName         命中物理表名
     * @param datasourceHostPort 数据源ip:port
     * @param dbName            数据库名（可空）
     * @param sid               模式名（可空）
     */
    @Transactional("neo4jTransactionManager")
    public void saveObjectLineage(Long conceptId, Long ontologyId, String conceptCode, String conceptName,
                                  String tableName, String datasourceHostPort, String dbName, String sid) {
        if (conceptId == null || tableName == null) {
            return;
        }
        // 1. 表节点（按表名+数据源 upsert）
        TableNode table = tableRepository.findByTableNameAndDatasourceHostPort(tableName, datasourceHostPort)
                .orElseGet(() -> TableNode.builder()
                        .name(tableName)
                        .tableName(tableName)
                        .datasourceHostPort(datasourceHostPort)
                        .dbName(dbName)
                        .sid(sid)
                        .build());

        // 2. 对象节点（按概念ID upsert，沿用既有节点与关系）
        ObjectNode object = objectRepository.findByConceptId(conceptId)
                .orElseGet(ObjectNode::new);
        object.setObjectId(conceptId);
        object.setOntologyId(ontologyId);
        object.setConceptId(conceptId);
        object.setConceptCode(conceptCode);
        object.setConceptName(conceptName);
        object.setTableName(tableName);
        object.setDatasourceHostPort(datasourceHostPort);
        object.setDbName(dbName);
        object.setSid(sid);

        // 3. 挂 MATERIALIZES 关系（按目标表去重，避免重复边）
        List<ObjectToTableRel> rels = new ArrayList<>();
        if (object.getMaterializesRels() != null) {
            rels.addAll(object.getMaterializesRels());
        }
        boolean exists = rels.stream().anyMatch(r -> r.getTable() != null
                && Objects.equals(r.getTable().getTableName(), tableName)
                && Objects.equals(r.getTable().getDatasourceHostPort(), datasourceHostPort));
        if (!exists) {
            rels.add(ObjectToTableRel.builder().objectId(conceptId).table(table).build());
        }
        object.setMaterializesRels(rels);

        // 4. 落库
        tableRepository.save(table);
        objectRepository.save(object);
    }

    /**
     * 对象血缘读取（数据维度 + 决策维度）。
     * <p>
     * 版本/权限维度不落 Neo4j，由本体 Service 层基于 ONT_ACTION_EXECUTION 快照与
     * 统一权限入口实时派生后合并。图中无该对象时返回空 DTO。
     *
     * @param conceptId 本体概念ID
     * @return 对象血缘聚合（当前对象 + 支撑表 + 决策动作执行）
     */
    @Transactional("neo4jTransactionManager")
    public ObjectLineageDTO objectLineage(Long conceptId) {
        ObjectLineageDTO dto = objectRepository.findObjectLineage(conceptId)
                .orElse(null);
        if (dto == null) {
            return new ObjectLineageDTO();
        }
        return dto;
    }

    /**
     * 保存
     *
     * @param readerTableNodeList
     * @param writerTableNodeList
     * @param taskNode
     */
    @Transactional("neo4jTransactionManager")
    public void save(List<TableNode> readerTableNodeList, List<TableNode> writerTableNodeList, TaskNode taskNode) {
        TaskNode oldTaskNode = taskRepository.findByTaskId(taskNode.getTaskId());
        if (oldTaskNode != null) {
            taskRepository.delete(oldTaskNode);
        }

        for (TableNode writerTableNode : writerTableNodeList) {
            TableNode oldWriterTableNode = tableRepository.findByTableNameAndDatasourceHostPort(writerTableNode.getTableName(), writerTableNode.getDatasourceHostPort())
                    .orElse(null);
            if (oldWriterTableNode != null) {
                writerTableNode.setId(oldWriterTableNode.getId());
                writerTableNode.setTaskToTableRels(oldWriterTableNode.getTaskToTableRels());
                writerTableNode.setTableToTaskRels(oldWriterTableNode.getTableToTaskRels());
            }
            tableRepository.save(writerTableNode);
        }


        for (TableNode readerTableNode : readerTableNodeList) {
            TableNode oldReaderTableNode = tableRepository.findByTableNameAndDatasourceHostPort(readerTableNode.getTableName(), readerTableNode.getDatasourceHostPort())
                    .orElse(null);
            if (oldReaderTableNode != null) {
                readerTableNode.setId(oldReaderTableNode.getId());
                readerTableNode.setTableToTaskRels(Stream.concat(readerTableNode.getTableToTaskRels().stream(), oldReaderTableNode.getTableToTaskRels().stream())
                        .distinct()
                        .collect(Collectors.toList()));
            }
            tableRepository.save(readerTableNode);
        }
        taskRepository.save(taskNode);
    }

    /**
     * 保存节点信息
     *
     * @param tableNodeList
     */
    @Transactional("neo4jTransactionManager")
    public void saveTable(List<TableNode> tableNodeList) {
        for (TableNode writerTableNode : tableNodeList) {
            TableNode oldWriterTableNode = tableRepository.findByTableNameAndDatasourceHostPort(writerTableNode.getTableName(), writerTableNode.getDatasourceHostPort())
                    .orElse(null);
            if (oldWriterTableNode != null) {
                writerTableNode.setId(oldWriterTableNode.getId());
                writerTableNode.setTaskToTableRels(oldWriterTableNode.getTaskToTableRels());
                writerTableNode.setTableToTaskRels(oldWriterTableNode.getTableToTaskRels());
            }
            tableRepository.save(writerTableNode);
        }
    }

    @Transactional
    public void save() {
        // 1. 源表
        TableNode orders = tableRepository.save(
                TableNode.builder()
                        .name("user")
                        .tableName("sales_db.dwh.user")
//                        .datasourceId(1L)
                        .datasourceType("MySql")
                        .dbName("sales_db")
                        .sid("dwh")
                        .build());

        // 2. 任务1
        TaskNode task1 = taskRepository.save(
                TaskNode.builder()
                        .name("任务名称")
                        .taskId(3L)
                        .taskCode("4000000000")
                        .build());

        // 3. 目标表1
        TableNode toOrders = tableRepository.save(
                TableNode.builder()
                        .name("to_user")
                        .tableName("sales_db.dwh.to_user")
//                        .datasourceId(1L)
                        .datasourceType("MySql")
                        .dbName("sales_db")
                        .sid("dwh")
                        .build());
        /* 6. 建立关系（核心） */

        // orders -> task1
        orders.setTableToTaskRels(Arrays.asList(TableToTaskRel.builder()
                .taskId(3L)
                .taskCode("4000000000")
//                .datasourceId(1L)
                .tableName("sales_db.dwh.user")
                .task(task1).build()));
        tableRepository.save(orders);

        // task1 -> toOrders
        task1.setTaskToTableRels(Arrays.asList(TaskToTableRel.builder()
                .taskId(3L)
                .taskCode("4000000000")
//                .datasourceId(1L)
                .tableName("sales_db.dwh.to_user")
                .table(toOrders).build()));
        taskRepository.save(task1);
    }

    @Transactional("neo4jTransactionManager")
    public void deletdAll() {
        taskRepository.deleteTask();
        taskRepository.deleteTable();
    }

//    public static void main(String[] args) {
//        // 1. 创建驱动
//        Driver driver = GraphDatabase.driver("bolt://110.42.38.62:40053",
//                AuthTokens.basic("neo4j", "InC3tmU4bijT4vkl"));
//
//        // 2. 打开自动关闭的会话
//        try (Session session = driver.session(SessionConfig.forDatabase("neo4j"))) {
//            // 3. 执行 Cypher，返回一条记录
//            Record row = session.run(
//                    "MATCH (currentTable:Table {tableName: $tableName})   " +
//                            "OPTIONAL MATCH (currentTable)<-[r1:TASK_TO_TABLE]-(sourceTask:Task)   " +
//                            "   " +
//                            "OPTIONAL MATCH (sourceTask)<-[r2:TABLE_TO_TASK]-(sourceTable:Table)   " +
//                            "   " +
//                            "OPTIONAL MATCH (currentTable)-[r3:TABLE_TO_TASK]->(targetTask:Task)   " +
//                            "OPTIONAL MATCH (targetTask)-[r4:TASK_TO_TABLE]->(targetTable:Table)   " +
//                            "RETURN     " +
//                            "collect(DISTINCT sourceTask) + collect(DISTINCT targetTask) AS tasks,   " +
//                            "currentTable + collect(DISTINCT sourceTable) + collect(DISTINCT targetTable)  AS tables,   " +
//                            "collect(DISTINCT r1) + collect(DISTINCT r2) +collect(DISTINCT r3) + collect(DISTINCT r4) AS  rels ",
//                    Values.parameters("tableName", "sales_db.dwh.to_orders")
//            ).single();              // 明确只取一条，避免游标
//
//            List<Relationship> rels =
//                    row.get("rels").asList(v -> v.asRelationship());   // 或 Value::asRelationship
//
//            List<JSONObject> relsObj = new ArrayList<>();
//            for (Relationship rel : rels) {
//                JSONObject relObj = new JSONObject();
//                relObj.put("startNodeId", rel.startNodeId());
//                relObj.put("endNodeId", rel.endNodeId());
//                relObj.put("type", rel.type());
//                JSONObject properties = new JSONObject();
//                properties.put("taskId", rel.get("taskId").asLong());
//                properties.put("taskCode", rel.get("taskCode").asString());
//                properties.put("datasourceId", rel.get("datasourceId").asLong());
//                properties.put("tableName", rel.get("tableName").asString());
//                relObj.put("properties", properties);
//                relsObj.add(relObj);
//            }
//            System.out.println(JSONObject.toJSONString(relsObj));
//        }
//        driver.close();
//    }

}

