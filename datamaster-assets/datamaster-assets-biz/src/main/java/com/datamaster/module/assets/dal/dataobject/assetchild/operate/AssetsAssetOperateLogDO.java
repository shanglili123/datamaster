package com.datamaster.module.assets.dal.dataobject.assetchild.operate;import com.baomidou.mybatisplus.annotation.*;import io.swagger.v3.oas.annotations.media.Schema;import lombok.*;
import lombok.experimental.SuperBuilder;
import com.datamaster.common.core.domain.BaseEntity;import java.util.Date;
/** *  DO  DA_ASSET_OPERATE_LOG * * @author DATAMASTER * @date 2025-05-09 */

@Data
@TableName(value = "AST_ASSET_OPERATE_LOG")
// 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
// @KeySequence("DA_ASSET_OPERATE_LOG_seq")
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)public class AssetsAssetOperateLogDO extends BaseEntity {    @TableField(exist = false)    private static final long serialVersionUID = 1L;/** id */
    private Long assetId;    /** id */
    private Long datasourceId;    /**  */
    private String tableName;    /** / */
    private String tableComment;    /**  */
    private String operateType;    /**  */
    private Date operateTime;    /**  */
    private Date executeTime;    /** (JSON) */
    private String updateBefore;    /** (JSON) */
    private String updateAfter;    /**  */
    private String fieldNames;    /** URL */
    private String fileUrl;    /**  */
    private String fileName;    /**  */
    private String status;    /**  */
    private Boolean validFlag;    /**  */

@TableLogic    private Boolean delFlag;    @Schema(description = "JSON MD5", example = "")    private String updateWhereMd5;    /**  */

@TableField(exist = false)    private String userName;    /**  */

@TableField(exist = false)    private String phoneNumber;    /**  */

@TableField(exist = false)    private String nickName;
}