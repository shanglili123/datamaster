package com.datamaster.module.catalog.controller.admin.metadata.vo;

import com.baomidou.mybatisplus.annotation.TableField;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import com.datamaster.common.core.page.PageParam;

import java.util.Date;
import java.util.List;

/**
 * 元数据检索统一返回实体
 * 用于 Catalog_DB / Catalog_TABLE / Catalog_COLUMN UNION ALL 查询
 */
@Data
@Schema(description = "元数据检索结果")
public class CatalogMetaSearchRespDTO extends PageParam {

    private static final long serialVersionUID = 1L;

    // ================== 基础标识 ==================

    @Schema(description = "元数据ID")
    private Long id;

    @Schema(description = "元数据类型(DB-1 / TABLE-2 / COLUMN-3)")
    private String type;

    private Long datasourceId;

    private String dbType;

    private String name;

    private String status;


    private Date updateTime;

    private CatalogDbRespVO mdDbDO;
    private CatalogTableRespVO mdTableRespVO;
    private CatalogColumnRespVO mdColumnDO;



    //查询参数
    @TableField(exist = false)
    private String keyword;

    //元数据类型(DB-1 / TABLE-2 / COLUMN-3)
    @TableField(exist = false)
    private List<String> types;

    //数据源类型
    @TableField(exist = false)
    private List<String> dbTypes;

    //时间
    @TableField(exist = false)
    private Date startTime;
    @TableField(exist = false)
    private Date endTime;

    /**
     * 是否在门户展示：0-不展示，1-展示
     */
    @Schema(description = "是否在门户展示：0-不展示，1-展示", example = "0")
    private String portalVisible;

}
