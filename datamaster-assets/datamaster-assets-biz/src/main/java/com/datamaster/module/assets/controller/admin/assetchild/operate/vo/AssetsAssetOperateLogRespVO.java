package com.datamaster.module.assets.controller.admin.assetchild.operate.vo;
import com.baomidou.mybatisplus.annotation.TableField;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import com.datamaster.common.annotation.Excel;
import java.io.Serializable;
import java.util.Date;/** *  Response VO  DA_ASSET_OPERATE_LOG * * @author DATAMASTER * @date 2025-05-09 */
@Schema(description = " Response VO")
@Data
public class AssetsAssetOperateLogRespVO implements Serializable {
    private static final long serialVersionUID = 1L;
@Excel(name = "ID")    @Schema(description = "ID")    private Long id;
@Excel(name = "id")    @Schema(description = "id", example = "")    private Long assetId;
@Excel(name = "id")    @Schema(description = "id", example = "")    private Long datasourceId;
@Excel(name = "")    @Schema(description = "", example = "")    private String tableName;
@Excel(name = "/")    @Schema(description = "/", example = "")    private String tableComment;
@Excel(name = "")    @Schema(description = "", example = "")    private String operateType;
@Excel(name = "", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")    @Schema(description = "", example = "")    private Date operateTime;
@Excel(name = "", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")    @Schema(description = "", example = "")    private Date executeTime;
@Excel(name = "(JSON)")    @Schema(description = "(JSON)", example = "")    private String updateBefore;
@Excel(name = "(JSON)")    @Schema(description = "(JSON)", example = "")    private String updateAfter;
@Excel(name = "")    @Schema(description = "", example = "")    private String fieldNames;
@Excel(name = "URL")    @Schema(description = "URL", example = "")    private String fileUrl;
@Excel(name = "")    @Schema(description = "", example = "")    private String fileName;
@Excel(name = "")    @Schema(description = "", example = "")    private String status;
@Excel(name = "")    @Schema(description = "", example = "")    private Boolean validFlag;
@Excel(name = "")    @Schema(description = "", example = "")    private Boolean delFlag;
@Excel(name = "")    @Schema(description = "", example = "")    private String createBy;
@Excel(name = "id")    @Schema(description = "id", example = "")    private Long creatorId;
@Excel(name = "", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")    @Schema(description = "", example = "")    private Date createTime;
@Excel(name = "")    @Schema(description = "", example = "")    private String updateBy;
@Excel(name = "id")    @Schema(description = "id", example = "")    private Long updaterId;
@Excel(name = "", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")    @Schema(description = "", example = "")    private Date updateTime;
@Excel(name = "")    @Schema(description = "", example = "")    private String remark;
@Schema(description = "JSON MD5", example = "")    private String updateWhereMd5;    /**  */
@TableField(exist = false)    private String userName;    /**  */
@TableField(exist = false)    private String phoneNumber;    /**  */
@TableField(exist = false)    private String nickName;}
