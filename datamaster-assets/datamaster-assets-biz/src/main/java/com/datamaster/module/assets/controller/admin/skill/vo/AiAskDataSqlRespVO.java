package com.datamaster.module.assets.controller.admin.skill.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * Ask-data SQL generation response.
 */
@Schema(description = "问数SQL生成响应")
@Data
public class AiAskDataSqlRespVO {

    @Schema(description = "原始问题")
    private String question;

    @Schema(description = "生成的SQL")
    private String sql;

    @Schema(description = "SQL解释")
    private String explanation;

    @Schema(description = "引用的Skill列表")
    private List<AiSkillRespVO> referencedSkills;

    @Schema(description = "质量风险提示")
    private String qualityWarning;

    @Schema(description = "执行结果（如果执行了SQL）")
    private List<Map<String, Object>> executeResult;

    @Schema(description = "结果行数")
    private Integer rowCount;

    @Schema(description = "是否执行成功")
    private Boolean executeSuccess;

    @Schema(description = "执行错误信息")
    private String executeError;
}
