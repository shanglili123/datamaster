package com.datamaster.module.assets.controller.admin.assetchild.operate.vo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import com.datamaster.common.core.domain.BaseEntity;
import javax.validation.constraints.Size;
import java.util.Date;/** *  / Request VO DA_ASSET_OPERATE_LOG * * @author lili.shang * @date 2025-05-09 */
@Schema(description = " Response VO")
@Data
public class AssetsAssetOperateLogSaveReqVO extends BaseEntity {
    private static final long serialVersionUID = 1L;
@Schema(description = "ID")    private Long id;
@Schema(description = "id", example = "")    private Long assetId;
@Schema(description = "id", example = "")    private Long datasourceId;
@Schema(description = "", example = "")    @Size(max = 256, message = "256")    private String tableName;
@Schema(description = "/", example = "")    @Size(max = 256, message = "/256")    private String tableComment;//1: 新增 2:修改 3:删除 4:导入
@Schema(description = "操作类型", example = "")    @Size(max = 256, message = "操作类型长度不能超过256个字符")    private String operateType;
@Schema(description = "", example = "")    private Date operateTime;
@Schema(description = "", example = "")    private Date executeTime;
@Schema(description = "(JSON)", example = "")    private String updateBefore;
@Schema(description = "(JSON)", example = "")    private String updateAfter;
@Schema(description = "", example = "")    private String fieldNames;
@Schema(description = "URL", example = "")    @Size(max = 256, message = "URL256")    private String fileUrl;
@Schema(description = "", example = "")    @Size(max = 256, message = "256")    private String fileName;//状态;1:执行中  2:失败  3:成功   4:回滚失败  5:回滚成功
@Schema(description = "状态", example = "")    @Size(max = 256, message = "状态长度不能超过256个字符")    private String status;
@Schema(description = "", example = "")    @Size(max = 256, message = "256")    private String remark;
@Schema(description = "JSON MD5", example = "")    private String updateWhereMd5;}
