

package com.datamaster.module.taxonomy.controller.admin.space.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import com.datamaster.common.core.page.PageParam;

import java.util.Date;

/**
 * 空间成员关系 Request VO 对象 TAX_SPACE_USER_REL
 *
 * @author DATAMASTER
 * @date 2025-02-11
 */
@Schema(description = "空间成员关系 Request VO")
@Data
public class TaxonomySpaceUserRelPageReqVO extends PageParam {

    private static final long serialVersionUID = 1L;
    @Schema(description = "ID", example = "")
    private Long id;
    @Schema(description = "空间 ID", example = "")
    private Long spaceId;

    @Schema(description = "用户 ID", example = "")
    private Long userId;

    @Schema(description = "用户名称", example = "")
    private String userName;

    @Schema(description = "用户昵称", example = "")
    private String nickName;

    @Schema(description = "手机号码", example = "")
    private String phoneNumber;

    @Schema(description = "起始时间", example = "")
    private Date startTime;

    @Schema(description = "结束时间", example = "")
    private Date endTime;

}
