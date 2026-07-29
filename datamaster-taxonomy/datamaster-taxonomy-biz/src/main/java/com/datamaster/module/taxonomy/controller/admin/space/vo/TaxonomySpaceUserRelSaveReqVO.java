

package com.datamaster.module.taxonomy.controller.admin.space.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import com.datamaster.common.core.domain.BaseEntity;

import javax.validation.constraints.Size;
import java.util.List;

/**
 * 空间成员关系 创建/修改 Request VO TAX_SPACE_USER_REL
 *
 * @author DATAMASTER
 * @date 2025-02-11
 */
@Schema(description = "空间成员关系 Request VO")
@Data
public class TaxonomySpaceUserRelSaveReqVO extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "空间 ID", example = "")
    private Long spaceId;

    @Schema(description = "用户 ID", example = "")
    private Long userId;

    @Schema(description = "描述", example = "")
    @Size(max = 256, message = "描述长度不能超过256个字符")
    private String description;

    @Schema(description = "用户 ID 集合", example = "")
    private List<Long> userIdList;

    @Schema(description = "角色 ID 集合", example = "")
    private List<Long> roleIdList;


}
