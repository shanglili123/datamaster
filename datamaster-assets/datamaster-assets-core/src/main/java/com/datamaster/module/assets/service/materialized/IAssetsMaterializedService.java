package com.datamaster.module.assets.service.materialized;

import com.datamaster.module.assets.controller.admin.materialized.vo.AssetsMaterializedReqVO;

/**
 * 资产物化Service接口
 *
 * @author DATAMASTER
 * @date 2026-08-01
 */
public interface IAssetsMaterializedService {

    /**
     * 物化建表（资产侧编排：组装 DbColumn -> 表已存在检查 -> mgmt 建表 -> 登记资产 -> 回写 assetId）
     *
     * @param req 物化请求
     * @return 结果
     */
    Long createMaterializedTable(AssetsMaterializedReqVO req);

}
