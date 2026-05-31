

package com.datamaster.module.modeling.api.dm.dto;

import lombok.*;

/**
 * 数仓分层-规范管理 DTO 对象 Modeling_DATA_LAYER_SPECIFICATION
 *
 * @author FXB
 * @date 2026-03-24
 */
@Data
public class ModelingDataLayerSpecificationRespDTO {

    private static final long serialVersionUID = 1L;

    /** ID */
    private Long id;

    /** 数仓分层ID */
    private Long dataLayerId;

    /** 表前缀 */
    private String prefixName;

    /** 业务大类英文缩写 */
    private String businessEngName;

    /** 负责人ID */
    private Long ownerUserId;

    /** 状态 */
    private String status;

    /** 描述 */
    private String description;

    /** 是否有效 */
    private Boolean validFlag;

    /** 删除标志 */
    private Boolean delFlag;


}
