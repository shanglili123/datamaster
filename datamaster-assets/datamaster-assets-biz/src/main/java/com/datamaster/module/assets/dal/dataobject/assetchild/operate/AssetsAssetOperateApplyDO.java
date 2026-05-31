package com.datamaster.module.assets.dal.dataobject.assetchild.operate;import com.baomidou.mybatisplus.annotation.*;import lombok.*;
import lombok.experimental.SuperBuilder;
import com.datamaster.common.core.domain.BaseEntity;import java.util.Date;
/** *  DO  DA_ASSET_OPERATE_APPLY * * @author DATAMASTER * @date 2025-05-09 */

@Data
@TableName(value = "AST_ASSET_OPERATE_APPLY")
// 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
// @KeySequence("DA_ASSET_OPERATE_APPLY_seq")
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)public class AssetsAssetOperateApplyDO extends BaseEntity {    @TableField(exist = false)    private static final long serialVersionUID = 1L;/** id */
    private Long assetId;    /** id */
    private Long datasourceId;    /**  */
    private String tableName;    /** / */
    private String tableComment;    /**  */
    private String operateType;    /** JSON */
    private String operateJson;    /**  */
    private Date operateTime;    /**  */
    private String executeFlag;    /**  */
    private Date executeTime;    /**  */
    private Boolean validFlag;    /**  */

@TableLogic    private Boolean delFlag;
}