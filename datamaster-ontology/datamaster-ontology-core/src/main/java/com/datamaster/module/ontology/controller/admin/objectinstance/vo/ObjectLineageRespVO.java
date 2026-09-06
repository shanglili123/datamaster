package com.datamaster.module.ontology.controller.admin.objectinstance.vo;

import com.datamaster.neo4j.node.ActionExecutionNode;
import com.datamaster.neo4j.node.ObjectNode;
import com.datamaster.neo4j.node.TableNode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 对象血缘 Response VO —— 对象血缘四维度聚合。
 *
 * <ul>
 *   <li>数据维度：currentObject + tables（对象-[MATERIALIZES]-&gt;表，含表级上/下游血缘）</li>
 *   <li>决策维度：decisions（ActionExecution-[DECISION_ACTION]-&gt;对象）</li>
 *   <li>版本维度：versions（由 ONT_ACTION_EXECUTION 的 beforeData/afterData 快照派生的时间线）</li>
 *   <li>权限维度：permission（复用资产统一权限入口实时计算）</li>
 * </ul>
 */
@Schema(description = "对象血缘 Response VO")
@Data
public class ObjectLineageRespVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "概念ID（对象类型ID）")
    private Long conceptId;

    @Schema(description = "物理表绑定ID")
    private Long tableBindingId;

    @Schema(description = "物理表名")
    private String tableName;

    @Schema(description = "数据维度：语义对象节点")
    private ObjectNode currentObject;

    @Schema(description = "数据维度：支撑该对象的物理表（含表级上/下游血缘）")
    private List<TableNode> tables;

    @Schema(description = "决策维度：作用于该对象的动作执行")
    private List<ActionExecutionNode> decisions;

    @Schema(description = "版本维度：对象快照时间线（由动作执行 before/after 快照派生）")
    private List<ObjectVersionVO> versions;

    @Schema(description = "权限维度：当前空间对该对象的访问摘要")
    private ObjectPermissionVO permission;
}
