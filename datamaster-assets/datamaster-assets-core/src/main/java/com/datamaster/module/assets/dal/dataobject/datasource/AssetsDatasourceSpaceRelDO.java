package com.datamaster.module.assets.dal.dataobject.datasource;

import com.baomidou.mybatisplus.annotation.TableName;
import com.datamaster.common.core.domain.entity.DatasourceSpaceRelDO;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Deprecated
@Data
@SuperBuilder
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName(value = "AST_DATASOURCE_SPACE_REL")
public class AssetsDatasourceSpaceRelDO extends DatasourceSpaceRelDO {
}
