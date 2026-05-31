

package com.datamaster.neo4j.rel;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.RelationshipProperties;
import org.springframework.data.neo4j.core.schema.TargetNode;
import com.datamaster.neo4j.node.TableNode;

import java.util.Map;

/**
 * <P>
 * 用途:
 * </p>
 *
 * @author: FXB
 * @create: 2025-08-27 14:03
 **/
@RelationshipProperties
@Data
@Builder
public class TaskToTableRel {

    @Id
    @GeneratedValue
    private Long id;

    /**
     * 任务id
     */
    private Long taskId;

    /**
     * 任务编码
     */
    private String taskCode;

    /**
     * 全限定表名 例如：sales_db.dwh.orders
     */
    private String tableName;
//    /**
//     * 数据源 id
//     */
//    private Long datasourceId;

    /**
     * 数据源ip:port
     */
    private String datasourceHostPort;

    @TargetNode
    private TableNode table;
}

