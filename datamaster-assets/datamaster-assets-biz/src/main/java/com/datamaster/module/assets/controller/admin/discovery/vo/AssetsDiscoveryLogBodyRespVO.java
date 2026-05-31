package com.datamaster.module.assets.controller.admin.discovery.vo;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import com.datamaster.common.annotation.Excel;
import java.io.Serializable;
import java.util.Date;/** * - Response VO  DA_DISCOVERY_LOG_BODY * * @author DATAMASTER * @date 2025-10-15 */
@Schema(description = "- Response VO")
@Data
public class AssetsDiscoveryLogBodyRespVO implements Serializable {
    private static final long serialVersionUID = 1L;
@Excel(name = "")    @Schema(description = "; ", example = "2025-10-15 10:00:00")    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")    private Date tm;
@Excel(name = "id")    @Schema(description = "id", example = "123")    private Long taskId;
@Excel(name = "")    @Schema(description = "", example = "")    private String logContent;
@Excel(name = "")    @Schema(description = ";01", example = "1")    private Boolean validFlag;
@Excel(name = "")    @Schema(description = ";10", example = "0")    private Boolean delFlag;
@Excel(name = "")    @Schema(description = "", example = "admin")    private String createBy;
@Excel(name = "id")    @Schema(description = "id", example = "1")    private Long creatorId;
@Excel(name = "", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")    @Schema(description = "", example = "2025-10-15 10:05:00")    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")    private Date createTime;
@Excel(name = "")    @Schema(description = "", example = "admin")    private String updateBy;
@Excel(name = "id")    @Schema(description = "id", example = "1")    private Long updaterId;
@Excel(name = "", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")    @Schema(description = "", example = "2025-10-15 10:10:00")    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")    private Date updateTime;
@Excel(name = "")    @Schema(description = "", example = "")    private String remark;}
