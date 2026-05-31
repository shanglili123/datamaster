

package com.datamaster.module.taxonomy.api.Tag.dto;

import lombok.Data;

/**
 * 标签管理 DTO 对象 TAX_TAG
 *
 * @author DATAMASTER
 * @date 2025-07-11
 */
@Data
public class TaxonomyTagRespDTO {

    private static final long serialVersionUID = 1L;

    /** ID */
    private Long id;

    /** 编码 */
    private String code;

    /** 名称 */
    private String name;

    /** 描述 */
    private String description;

    /** 类目编码 */
    private String catCode;

    /** 资产数量 */
    private Long aeestCount;

    /** 状态 */
    private String status;

    /** 扩展信息别名 */
    private String allas;

    /** 近义词 */
    private String nearSynonyms;

    /** 同义词 */
    private String synonyms;

    /** 是否有效 */
    private Boolean validFlag;

    /** 删除标志 */
    private Boolean delFlag;


}
