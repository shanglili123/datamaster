

package com.datamaster.metadata.controller.qa.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.datamaster.common.core.domain.BaseEntity;

import javax.validation.constraints.Size;
import java.util.Date;

/**
 * 质量探查日志 创建/修改 Request VO COL_QUALITY_LOG
 *
 * @author lili.shang
 * @date 2025-07-19
 */
@Schema(description = "质量探查日志 Response VO")
@Data
@NoArgsConstructor
public class QualityLogSaveReqVO extends BaseEntity {

    private static final long serialVersionUID = 1L;

    public QualityLogSaveReqVO(QualityTaskRespVO task) {
        this.name = task.getTaskName();
        this.qualityId = task.getId();
        this.startTime = new Date();
        // 创建日志即视为“进行中”（字典 quality_log_success_flag：0成功/1失败/2进行中），
        // 结束时由 updateQualityLog 更新为 0/1 并填写真实的 endTime。
        // 注意：success_flag/end_time 在 DB 中均为 NOT NULL 且无默认值，此处必须显式赋值。
        this.successFlag = "2";
        this.endTime = this.startTime;
    }

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "名称", example = "")
    @Size(max = 256, message = "名称长度不能超过256个字符")
    private String name;

    @Schema(description = "状态", example = "")
    @Size(max = 256, message = "状态长度不能超过256个字符")
    private String successFlag;

    @Schema(description = "开始时间", example = "")
    private Date startTime;

    @Schema(description = "结束时间", example = "")
    private Date endTime;

    @Schema(description = "任务id", example = "")
    private Long qualityId;

    @Schema(description = "评分", example = "")
    private Long score;

    @Schema(description = "问题数据", example = "")
    private Long problemData;

    @Schema(description = "描述", example = "")
    @Size(max = 256, message = "备注长度不能超过256个字符")
    private String description;


    private String path;

}
