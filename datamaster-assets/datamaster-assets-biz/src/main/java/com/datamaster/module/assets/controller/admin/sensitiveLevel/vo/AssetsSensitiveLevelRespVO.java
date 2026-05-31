package com.datamaster.module.assets.controller.admin.sensitiveLevel.vo;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import com.datamaster.common.annotation.Excel;
import java.io.Serializable;
import java.util.Date;/** *  Response VO  DA_SENSITIVE_LEVEL * * @author DATAMASTER * @date 2025-01-21 */
@Schema(description = " Response VO")
@Data
public class AssetsSensitiveLevelRespVO implements Serializable {
    private static final long serialVersionUID = 1L;
@Excel(name = "ID")    @Schema(description = "ID")    private Long id;
@Excel(name = "")    @Schema(description = "", example = "")    private String sensitiveLevel;
@Excel(name = "")    @Schema(description = "", example = "")    private String sensitiveRule;
@Excel(name = "")    @Schema(description = "", example = "")    private Long startCharLoc;
@Excel(name = "")    @Schema(description = "", example = "")    private Long endCharLoc;
@Excel(name = "")    @Schema(description = "", example = "")    private String maskCharacter;
@Excel(name = "")    @Schema(description = "", example = "")    private String onlineFlag;
@Excel(name = "")    @Schema(description = "", example = "")    private String description;
@Excel(name = "")    @Schema(description = "", example = "")    private Boolean validFlag;
@Excel(name = "")    @Schema(description = "", example = "")    private Boolean delFlag;
@Excel(name = "")    @Schema(description = "", example = "")    private String createBy;
@Excel(name = "id")    @Schema(description = "id", example = "")    private Long creatorId;
@Excel(name = "", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")    @Schema(description = "", example = "")    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")    private Date createTime;
@Excel(name = "")    @Schema(description = "", example = "")    private String updateBy;
@Excel(name = "id")    @Schema(description = "id", example = "")    private Long updaterId;
@Excel(name = "", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")    @Schema(description = "", example = "")    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")    private Date updateTime;
@Excel(name = "")    @Schema(description = "", example = "")    private String remark;}
