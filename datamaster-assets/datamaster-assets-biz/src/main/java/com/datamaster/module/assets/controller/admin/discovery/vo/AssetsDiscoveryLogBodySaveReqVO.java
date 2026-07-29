package com.datamaster.module.assets.controller.admin.discovery.vo;
import com.baomidou.mybatisplus.annotation.TableField;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import com.datamaster.common.core.domain.BaseEntity;
import javax.validation.constraints.Size;
import java.util.Date;/** * - / Request VO  DA_DISCOVERY_LOG_BODY * * @author DATAMASTER * @date 2025-10-15 */
@Schema(description = "- / Request VO")
@Data
public class AssetsDiscoveryLogBodySaveReqVO extends BaseEntity {
    private static final long serialVersionUID = 1L;
@Schema(description = "; ", example = "2025-10-15 10:00:00")    private Date tm;
@Schema(description = "id", example = "123")    private Long taskId;
@Schema(description = "日志内容", example = "")    @Size(max = 4000, message = "日志内容长度不能超过4000个字符")    private String logContent;
@Schema(description = ";01", example = "1")    private Boolean validFlag;
@Schema(description = ";10", example = "0")    private Boolean delFlag;
@Schema(description = "描述", example = "")    @Size(max = 512, message = "描述长度不能超过512个字符")    private String description;
@Schema(description = "", example = "DISCOVERY_TASK_001")    @TableField(exist = false)    private String taskCode;
@Schema(description = "", example = "A")    @TableField(exist = false)    private String taskName;}
