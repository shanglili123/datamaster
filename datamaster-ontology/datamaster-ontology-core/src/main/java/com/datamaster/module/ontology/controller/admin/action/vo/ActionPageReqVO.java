package com.datamaster.module.ontology.controller.admin.action.vo;

import com.datamaster.common.core.page.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Schema(description = "动作分页查询 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class ActionPageReqVO extends PageParam {

    private static final long serialVersionUID = 1L;

    @Schema(description = "本体ID", example = "1")
    private Long ontologyId;

    @Schema(description = "动作名称", example = "创建客户")
    private String name;

    @Schema(description = "动作类型：CREATE/UPDATE/DELETE/QUERY/FUNCTION", example = "CREATE")
    private String actionType;
}
