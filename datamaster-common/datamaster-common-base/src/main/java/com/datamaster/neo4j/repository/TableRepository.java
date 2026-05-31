

package com.datamaster.neo4j.repository;

import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.datamaster.neo4j.dto.LineageDTO;
import com.datamaster.neo4j.node.TableNode;

import java.util.Optional;

/**
 * <P>
 * 用途:
 * </p>
 *
 * @author: FXB
 * @create: 2025-08-27 13:44
 **/
@Repository
public interface TableRepository extends Neo4jRepository<TableNode, Long> {
//    Optional<TableNode> findByTableNameAndDatasourceId(String tableName,Long datasourceId);

    Optional<TableNode> findByTableNameAndDatasourceHostPort(String tableName,String datasourceHostPort);

    @Query(value = "MATCH (currentTable:Table {datasourceHostPort: $datasourceHostPort,tableName: $tableName})  " +
            "  " +
            "OPTIONAL MATCH (currentTable)<-[r1:TASK_TO_TABLE]-(sourceTask:Task)  " +
            "OPTIONAL MATCH (sourceTask)<-[r2:TABLE_TO_TASK]-(sourceTable:Table)  " +
            "  " +
            "OPTIONAL MATCH (currentTable)-[r3:TABLE_TO_TASK]->(targetTask:Task)  " +
            "OPTIONAL MATCH (targetTask)-[r4:TASK_TO_TABLE]->(targetTable:Table)  " +
            "RETURN  " +
            "  currentTable,  " +
            "  collect(DISTINCT sourceTask) + collect(DISTINCT targetTask) AS tasks,  " +
            "  [currentTable] + collect(DISTINCT sourceTable) + collect(DISTINCT targetTable) AS tables")
    Optional<LineageDTO> findLineage(@Param("datasourceHostPort") String datasourceHostPort, @Param("tableName") String tableName);
}

