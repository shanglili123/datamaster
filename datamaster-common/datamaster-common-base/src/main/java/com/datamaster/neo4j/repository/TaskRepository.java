

package com.datamaster.neo4j.repository;

import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.datamaster.neo4j.node.TaskNode;
import com.datamaster.neo4j.rel.TaskToTableRel;

import java.util.List;
import java.util.Optional;

/**
 * <P>
 * 用途:
 * </p>
 *
 * @author: FXB
 * @create: 2025-08-27 14:11
 **/
@Repository
public interface TaskRepository extends Neo4jRepository<TaskNode, Long> {

    TaskNode findByTaskId(Long taskId);

    @Query("MATCH ()-[r {taskId:$taskId}]->() DELETE r")
    void deleteRelByTaskId(@Param("taskId") Long taskId);

    @Query("MATCH (ta:Task) DETACH DELETE ta")
    void deleteTask();

    @Query("MATCH (t:Table) DETACH DELETE t")
    void deleteTable();
}

