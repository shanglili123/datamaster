

package com.datamaster.neo4j.dto;

import com.alibaba.fastjson.JSONObject;
import lombok.Data;
import com.datamaster.neo4j.node.TableNode;
import com.datamaster.neo4j.node.TaskNode;
import com.datamaster.neo4j.rel.TableToTaskRel;
import com.datamaster.neo4j.rel.TaskToTableRel;

import java.util.List;
import java.util.Map;

@Data
public class LineageDTO {

    /**
     * 当前表节点
     */
    private TableNode currentTable;

    /**
     * 所有Task节点
     */
    private List<TaskNode> tasks;
    /**
     * 所有Table节点
     */
    private List<TableNode> tables;

    /**
     * 节点关系
     */
    private List<JSONObject> rels;
}

