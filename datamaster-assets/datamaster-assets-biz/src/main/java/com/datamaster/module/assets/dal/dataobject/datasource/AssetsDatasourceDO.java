package com.datamaster.module.assets.dal.dataobject.datasource;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.annotation.JSONField;
import com.baomidou.mybatisplus.annotation.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;
import com.datamaster.common.core.domain.BaseEntity;
import com.datamaster.common.database.constants.DbQueryProperty;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 *  DO  DA_DATASOURCE
 *
 * @author lhs
 * @date 2025-01-21
 */
@Data
@TableName(value = "AST_DATASOURCE")
// 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
// @KeySequence("DA_DATASOURCE_seq")
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Slf4j
public class AssetsDatasourceDO extends BaseEntity {
    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

/**  */
    private String datasourceName;

    /**  */
    private String datasourceType;

    /** (json) */
    private String datasourceConfig;

    /**  */
    @TableField(exist = false)
    private List<AssetsDatasourceProjectRelDO> projectList;

    /**  */
    @TableField(exist = false)
    private String projectName;

    /**  */
    @TableField(exist = false)
    private Boolean isAdminAddTo;

    /** IP */
    private String ip;

    /**  */
    private Long port;

    /**  */
    private Long listCount;

    /**  */
    private Long syncCount;

    /**  */
    private Long DataSize;

    /**  */
    private String description;

    /**  */
    private Boolean validFlag;

    @TableLogic
    private Boolean delFlag;

    @JSONField(serialize = false)
    public String toJsonString() {
        // 默认 Fastjson：忽略 null 字段，字段顺序自动
        return JSON.toJSONString(this);
    }

    @JSONField(serialize = false)
    public DbQueryProperty simplify() {
        DbQueryProperty dbQueryProperty = new DbQueryProperty(
                this.getDatasourceType(),
                this.getIp(),
                this.getPort(),
                this.getDatasourceConfig()
        );
        return dbQueryProperty;
    }

}
