

package com.datamaster.neo4j.rel;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.neo4j.core.schema.*;
import com.datamaster.neo4j.node.TaskNode;

/**
 * <P>
 * 用途:
 * </p>
 *
 * @author: FXB
 * @create: 2025-08-27 14:02
 **/
@RelationshipProperties
@Data
@Builder
public class TableToTaskRel {
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
    private TaskNode task;
}

