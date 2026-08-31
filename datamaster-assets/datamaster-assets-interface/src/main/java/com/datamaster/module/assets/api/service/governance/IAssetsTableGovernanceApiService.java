package com.datamaster.module.assets.api.service.governance;

import com.datamaster.module.assets.api.governance.dto.AssetsTableGovernanceReqDTO;
import com.datamaster.module.assets.api.governance.dto.AssetsTableGovernanceRespDTO;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface IAssetsTableGovernanceApiService {

    AssetsTableGovernanceRespDTO resolveTable(AssetsTableGovernanceReqDTO reqDTO);

    void checkTableAccess(AssetsTableGovernanceReqDTO reqDTO);

    /**
     * 查询结果脱敏公共方法（数据资产 scene=1、数据查询 scene=2、数据服务 scene=3 共用）
     * <p>
     * 前置隐藏列（敏感等级不足、脱敏规则无区间 → ACTION_HIDE）会被移除，
     * 区间替换列（ACTION_REPLACE）会被脱敏替换，白名单用户放行。
     *
     * @param assetId             资产ID，为空时原样返回，不影响查询链路
     * @param data                查询结果数据
     * @param userId              当前用户ID
     * @param userPermissionLevel 用户数据权限等级，可为空
     * @param scene               应用场景：1数据资产 2数据查询 3数据服务
     * @return 脱敏后的数据
     */
    List<Map<String, Object>> desensitizeResultData(Long assetId, List<Map<String, Object>> data,
                                                      Long userId, Long userPermissionLevel, String scene);

    /**
     * 获取前置隐藏列（供查询前字段构建时剔除，避免敏感字段进入 SQL）
     *
     * @param assetId             资产ID，为空时返回空集合
     * @param userId              当前用户ID（白名单判断用）
     * @param userPermissionLevel 用户数据权限等级，可为空
     * @param scene               应用场景：1数据资产 2数据查询 3数据服务
     * @return 需要隐藏的列名集合
     */
    Set<String> getDesensitizeHideColumns(Long assetId, Long userId, Long userPermissionLevel, String scene);
}