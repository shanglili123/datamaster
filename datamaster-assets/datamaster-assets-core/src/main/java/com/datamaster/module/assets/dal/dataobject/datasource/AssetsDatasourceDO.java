package com.datamaster.module.assets.dal.dataobject.datasource;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.annotation.JSONField;
import com.datamaster.common.core.domain.entity.DatasourceDO;
import com.datamaster.common.database.constants.DbQueryProperty;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Deprecated
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName(value = "AST_DATASOURCE")
public class AssetsDatasourceDO extends DatasourceDO {

    @TableField(exist = false)
    private String spaceName;

    @TableField(exist = false)
    private Boolean adminAddTo;

    @JSONField(serialize = false)
    public String toJsonString() {
        return JSON.toJSONString(this);
    }

    @JSONField(serialize = false)
    public DbQueryProperty simplify() {
        return new DbQueryProperty(
                this.getDatasourceType(),
                this.getIp(),
                this.getPort(),
                this.getDatasourceConfig()
        );
    }
}
