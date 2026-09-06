

package com.datamaster.neo4j.dto;

import lombok.Data;
import com.datamaster.neo4j.node.ActionExecutionNode;
import com.datamaster.neo4j.node.ObjectNode;
import com.datamaster.neo4j.node.TableNode;

import java.util.List;

/**
 * <P>
 * 用途：对象血缘聚合 DTO（Neo4j 侧：数据维度 + 决策维度）。
 * </p>
 * <p>
 * 版本维度与权限维度不落 Neo4j，由本体 Service 层基于
 * ONT_ACTION_EXECUTION 快照与统一权限入口实时派生后合并。
 *
 * @author: Sisyphus
 * @create: 2026-09-01
 **/
@Data
public class ObjectLineageDTO {

    /**
     * 当前语义对象节点（数据维度：由其 MATERIALIZES 支撑表承载）
     */
    private ObjectNode currentObject;

    /**
     * 支撑该对象的物理表（数据维度）
     */
    private List<TableNode> tables;

    /**
     * 作用于该对象的动作执行（决策维度）
     */
    private List<ActionExecutionNode> decisions;
}
