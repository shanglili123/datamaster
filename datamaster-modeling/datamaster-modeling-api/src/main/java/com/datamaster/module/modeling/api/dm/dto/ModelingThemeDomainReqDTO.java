

package com.datamaster.module.modeling.api.dm.dto;

import lombok.*;

/**
 * 主题域管理 DTO 对象 Modeling_THEME_DOMAIN
 *
 * @author FXB
 * @date 2026-03-24
 */
@Data
public class ModelingThemeDomainReqDTO {

    private static final long serialVersionUID = 1L;

    /** ID */
    private Long id;

    /** 层级编码 */
    private String code;

    /** 名称 */
    private String name;

    /** 英文缩写 */
    private String engName;

    /** 关联上级ID */
    private Long parentId;

    /** 负责人ID */
    private Long ownerUserId;

    /** 数仓分层ID */
    private Long dataLayerId;

    /** 描述 */
    private String description;

    /** 是否有效 */
    private Boolean validFlag;

    /** 删除标志 */
    private Boolean delFlag;


}
