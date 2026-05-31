

package com.datamaster.module.taxonomy.controller.admin.project.vo;

import lombok.Data;
import com.datamaster.common.core.domain.entity.SysUser;

/**
 * <P>
 * 用途:
 * </p>
 *
 * @author: FXB
 * @create: 2025-09-01 17:43
 **/
@Data
public class TaxonomySysUserReqVO extends SysUser {

    private Long projectId;
}
