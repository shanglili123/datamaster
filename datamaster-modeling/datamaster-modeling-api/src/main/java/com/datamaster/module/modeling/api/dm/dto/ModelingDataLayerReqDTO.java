

package com.datamaster.module.modeling.api.dm.dto;

import lombok.*;

/**
 * 数仓分层管理 DTO 对象 Modeling_DATA_LAYER
 *
 * @author FXB
 * @date 2026-03-24
 */
@Data
public class ModelingDataLayerReqDTO {

    private static final long serialVersionUID = 1L;

    /** ID */
    private Long id;

    /** 名称 */
    private String name;

    /** 英文缩写 */
    private String engName;

    /** 负责人ID */
    private Long ownerUserId;

    /** 分类 */
    private String category;

    /** 描述 */
    private String description;

    /** 是否有效 */
    private Boolean validFlag;

    /** 删除标志 */
    private Boolean delFlag;


}
