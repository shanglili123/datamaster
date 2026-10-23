package com.datamaster.metadata.dal.mapper.lineage;

import com.datamaster.metadata.dal.dataobject.lineage.LineageRows.ObjectRow;
import com.datamaster.metadata.dal.dataobject.metadata.CatalogTableDO;
import com.datamaster.metadata.dal.dataobject.metadata.CatalogColumnDO;
import com.datamaster.module.assets.dal.dataobject.asset.AssetsAssetDO;
import org.apache.ibatis.annotations.*;
import java.util.List;

/** Read-only authorization/resolution queries; never opens a source connection. */
@Mapper
public interface LineageAccessMapper {
    String ASSET_SCOPE = " AND (EXISTS (SELECT 1 FROM ast_asset_space_rel ar WHERE ar.asset_id=a.id AND ar.del_flag='0' " +
            "AND ar.space_id=#{spaceId} AND ar.space_code=#{spaceCode}) OR EXISTS " +
            "(SELECT 1 FROM ast_datasource_space_rel dr WHERE dr.datasource_id=a.datasource_id " +
            "AND dr.space_id=#{spaceId} AND dr.space_code=#{spaceCode})) ";

    @Select("SELECT a.id,a.table_id,a.datasource_id,a.table_name FROM ast_asset a WHERE a.id=#{id} AND a.type='1' AND a.del_flag='0' " + ASSET_SCOPE)
    AssetsAssetDO asset(@Param("id") Long id, @Param("spaceId") Long spaceId, @Param("spaceCode") String spaceCode);

    @Select("SELECT a.id,a.table_id,a.datasource_id,a.table_name FROM ast_asset a " +
            "LEFT JOIN cat_table t ON t.id=a.table_id AND t.del_flag='0' " +
            "WHERE a.type='1' AND a.del_flag='0' AND a.datasource_id=#{o.datasourceId} " + ASSET_SCOPE +
            " AND ((t.id IS NOT NULL AND t.datasource_id=#{o.datasourceId} " +
            "AND t.db_name IS NOT DISTINCT FROM #{o.catalogName} AND t.schema_name IS NOT DISTINCT FROM #{o.schemaName} " +
            "AND t.table_name=#{o.objectName}) OR (t.id IS NULL AND CAST(#{o.catalogName} AS varchar) IS NULL AND CAST(#{o.schemaName} AS varchar) IS NULL " +
            "AND a.table_name=#{o.objectName})) ORDER BY a.id LIMIT 2")
    @Options(timeout = 2)
    List<AssetsAssetDO> assetsForObject(@Param("o") ObjectRow object, @Param("spaceId") Long spaceId,
                                        @Param("spaceCode") String spaceCode);
    @Select("SELECT a.id,a.table_id,a.datasource_id,a.table_name FROM ast_asset a " +
            "WHERE a.type='1' AND a.del_flag='0' AND a.table_id IS NULL AND a.datasource_id=#{datasourceId} " +
            "AND a.table_name=#{tableName} " + ASSET_SCOPE + " ORDER BY a.id LIMIT 2")
    @Options(timeout = 2)
    List<AssetsAssetDO> unlinkedAssets(@Param("datasourceId") Long datasourceId, @Param("tableName") String tableName,
                                      @Param("spaceId") Long spaceId, @Param("spaceCode") String spaceCode);

    @Select("SELECT id,datasource_id,db_name,schema_name,table_name,space_id,space_code,business_leader,responsible_dept " +
            "FROM cat_table WHERE id=#{id} AND del_flag='0'")
    CatalogTableDO metadata(Long id);

    @Select("SELECT id,datasource_id,db_name,schema_name,table_name,space_id,space_code,business_leader,responsible_dept " +
            "FROM cat_table WHERE datasource_id=#{o.datasourceId} AND db_name IS NOT DISTINCT FROM #{o.catalogName} " +
            "AND schema_name IS NOT DISTINCT FROM #{o.schemaName} AND table_name=#{o.objectName} " +
            "AND space_id=#{spaceId} AND space_code=#{spaceCode} AND del_flag='0' ORDER BY id LIMIT 2")
    @Options(timeout = 2)
    List<CatalogTableDO> metadataForObject(@Param("o") ObjectRow object, @Param("spaceId") Long spaceId,
                                            @Param("spaceCode") String spaceCode);

    @Select("SELECT count(*) FROM ast_datasource d WHERE d.id=#{datasourceId} AND d.del_flag='0' AND EXISTS " +
            "(SELECT 1 FROM ast_datasource_space_rel r WHERE r.datasource_id=d.id AND r.space_id=#{spaceId} AND r.space_code=#{spaceCode})")
    @Options(timeout = 2)
    int connectionInSpace(@Param("datasourceId") Long datasourceId, @Param("spaceId") Long spaceId,
                           @Param("spaceCode") String spaceCode);

    @Select("SELECT id,table_id,datasource_id,column_name,space_id,space_code,business_leader,responsible_dept " +
            "FROM cat_column WHERE table_id=#{tableId} AND datasource_id=#{datasourceId} AND column_name=#{columnName} " +
            "AND space_id=#{spaceId} AND space_code=#{spaceCode} AND del_flag='0' ORDER BY id LIMIT 2")
    @Options(timeout = 2)
    List<CatalogColumnDO> metadataColumns(@Param("tableId") Long tableId, @Param("datasourceId") Long datasourceId,
                                          @Param("columnName") String columnName, @Param("spaceId") Long spaceId,
                                          @Param("spaceCode") String spaceCode);

    @Select("SELECT count(*) FROM ast_asset_column c JOIN ast_asset a ON a.id=c.asset_id " +
            "WHERE c.asset_id=#{assetId} AND c.column_name=#{columnName} AND c.del_flag='0' AND a.type='1' AND a.del_flag='0' " + ASSET_SCOPE +
            " AND EXISTS (SELECT 1 FROM ast_asset_column_space_rel cr WHERE cr.asset_id=a.id AND cr.column_id=c.id " +
            "AND cr.space_id=#{spaceId} AND cr.space_code=#{spaceCode} AND cr.del_flag='0')")
    @Options(timeout = 2)
    int assetColumnVisible(@Param("assetId") Long assetId, @Param("columnName") String columnName,
                             @Param("spaceId") Long spaceId, @Param("spaceCode") String spaceCode);

    @Select("SELECT count(*) FROM system_role_dept WHERE role_id=#{roleId} AND dept_id=#{deptId}")
    @Options(timeout = 2)
    int roleDepartment(@Param("roleId") Long roleId, @Param("deptId") Long deptId);

    @Select("SELECT count(*) FROM system_dept WHERE dept_id=#{targetDept} AND " +
            "(dept_id=#{userDept} OR position(',' || CAST(#{userDept} AS text) || ',' IN ',' || ancestors || ',') > 0)")
    @Options(timeout = 2)
    int childDepartment(@Param("targetDept") Long targetDept, @Param("userDept") Long userDept);
}
