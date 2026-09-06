package com.datamaster.module.ontology.controller.admin.objectinstance.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.Map;

/**
 * 对象实例行操作 Request VO
 *
 * <p>对象管理页在列表内直接对单条数据做新增/修改/删除：先 {@code /row/preview}
 * 提交（生成 SQL + dry-run + 按需建审批链），再 {@code /row/confirm} 审批通过并执行。
 * 与普通动作走同一套执行记录/审批/快照/血缘/Webhook 链路。</p>
 */
@Schema(description = "对象实例行操作 Request VO")
@Data
public class RowOperateReqVO {

    @Schema(description = "所属本体ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "本体ID不能为空")
    private Long ontologyId;

    @Schema(description = "概念ID（对象集）", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "概念ID不能为空")
    private Long conceptId;

    @Schema(description = "物理表绑定ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "表绑定ID不能为空")
    private Long tableBindingId;

    @Schema(description = "行操作类型：CREATE=新增 / UPDATE=修改 / DELETE=删除", requiredMode = Schema.RequiredMode.REQUIRED, example = "CREATE")
    @NotBlank(message = "行操作类型不能为空")
    private String actionType;

    @Schema(description = "执行记录ID（confirm 阶段必填，来自 preview 返回）", example = "1")
    private Long executionId;

    /**
     * 行数据（key = 属性编码 propertyCode）：
     * CREATE=整行新值；UPDATE=主键属性 + 改动属性；DELETE=主键属性。
     * 主键属性进 UPDATE/DELETE 的 WHERE，非主键有值列进 INSERT/UPDATE 的 SET。
     */
    @Schema(description = "行数据（key=属性编码 propertyCode）")
    private Map<String, Object> data;

    @Schema(description = "审批通过理由（内置动作被改为需审批时必填）", example = "数据核对无误，同意")
    private String approveReason;

    @Schema(description = "执行后是否触发 Webhook 回调（默认 true，走动作/本体绑定的 ONT_WEBHOOK 配置）", example = "true")
    private Boolean triggerWebhook;

    @Schema(description = "当前空间ID（权限校验上下文；前端自动注入）", example = "1")
    private Long spaceId;

    @Schema(description = "当前空间编码（权限校验上下文）", example = "space_demo")
    private String spaceCode;
}