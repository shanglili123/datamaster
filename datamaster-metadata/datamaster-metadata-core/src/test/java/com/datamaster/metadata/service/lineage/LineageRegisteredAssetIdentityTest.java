package com.datamaster.metadata.service.lineage;

import com.datamaster.metadata.api.lineage.dto.LineageQueryReqDTO;
import com.datamaster.metadata.dal.dataobject.metadata.CatalogTableDO;
import com.datamaster.metadata.dal.mapper.lineage.LineageAccessMapper;
import com.datamaster.module.assets.api.datasource.dto.AssetsDatasourceRespDTO;
import com.datamaster.module.assets.api.service.asset.IAssetsDatasourceApiService;
import com.datamaster.module.assets.dal.dataobject.asset.AssetsAssetDO;
import org.junit.Test;
import java.lang.reflect.*;
import java.util.*;
import static org.junit.Assert.*;

public class LineageRegisteredAssetIdentityTest {
    @Test public void registeredPostgresAssetWithoutMetadataUsesConnectionCatalogAndDefaultPublic() throws Exception {
        Harness harness = new Harness();
        CatalogTableDO table = harness.guard.assetMetadata(harness.asset, harness.request);
        assertNotNull(table); assertNull(table.getId());
        assertEquals("warehouse", table.getDbName()); assertEquals("public", table.getSchemaName());
        assertEquals("orders", table.getTableName()); assertNull(harness.asset.getTableId());
    }

    @Test public void duplicateMetadataOrSqlShapedNamesCannotBeGuessed() throws Exception {
        Harness harness = new Harness(); harness.ambiguous = true;
        assertNull(harness.guard.assetMetadata(harness.asset, harness.request));
        harness.ambiguous = false; harness.asset.setTableName("other.orders");
        assertNull(harness.guard.assetMetadata(harness.asset, harness.request));
    }

    private static void inject(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name); field.setAccessible(true); field.set(target,value);
    }
    private static final class Harness {
        final LineageGuard guard = new LineageGuard();
        final AssetsAssetDO asset = new AssetsAssetDO();
        final LineageQueryReqDTO request = new LineageQueryReqDTO();
        boolean ambiguous;
        Harness() throws Exception {
            asset.setId(1L); asset.setDatasourceId(7L); asset.setTableName("orders");
            request.setSpaceId(2L); request.setSpaceCode("space");
            AssetsDatasourceRespDTO datasource = new AssetsDatasourceRespDTO();
            datasource.setId(7L); datasource.setDatasourceType("postgresql");
            datasource.setDatasourceConfig("{\"username\":\"test\",\"password\":\"test\",\"dbname\":\"warehouse\"}");
            inject(guard,"datasourceApi",Proxy.newProxyInstance(IAssetsDatasourceApiService.class.getClassLoader(),
                    new Class<?>[]{IAssetsDatasourceApiService.class},(proxy,method,args)-> {
                        if ("getDatasourceById".equals(method.getName())) return datasource;
                        throw new AssertionError("No remote database method may be called");
                    }));
            inject(guard,"access",Proxy.newProxyInstance(LineageAccessMapper.class.getClassLoader(),
                    new Class<?>[]{LineageAccessMapper.class},(proxy,method,args)-> {
                        if ("metadataForObject".equals(method.getName())) return ambiguous
                                ? Arrays.asList(new CatalogTableDO(),new CatalogTableDO()) : Collections.emptyList();
                        throw new AssertionError(method.getName());
                    }));
        }
    }
}
