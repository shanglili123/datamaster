package com.datamaster.module.governance.api.cat;

/**
 * 类目通用查询接口
 */
public interface ITaxonomyCatService {

    /**
     * 根据类目类型及类目编码获取类目id
     *
     * @param catType 类目类型
     * @param catCode 类目编码
     * @return 类目ID
     */
    Long getCatIdByCatTypeAndCode(String catType, String catCode);

    /**
     * 根据类目表名及类目编码获取类目id（已废弃，请使用 getCatIdByCatTypeAndCode）
     *
     * @param tableName 旧表名（已弃用）
     * @param catCode   类目编码
     * @return 类目ID
     */
    @Deprecated
    Long getCatIdByTableNameAndCatCode(String tableName, String catCode);
}
