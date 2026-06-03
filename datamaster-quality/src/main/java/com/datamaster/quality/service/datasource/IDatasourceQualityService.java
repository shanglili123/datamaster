

package com.datamaster.quality.service.datasource;

import com.baomidou.mybatisplus.extension.service.IService;
import com.datamaster.common.core.domain.AjaxResult;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.common.database.core.DbColumn;
import com.datamaster.common.database.core.DbTable;
import com.datamaster.module.assets.api.datasource.dto.AssetsDatasourceRespDTO;
import com.datamaster.quality.controller.da.datasource.vo.DatasourcePageReqVO;
import com.datamaster.quality.controller.da.datasource.vo.DatasourceRespVO;
import com.datamaster.quality.controller.da.datasource.vo.DatasourceSaveReqVO;
import com.datamaster.quality.dal.dataobject.datasource.DatasourceDO;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 数据源Service接口
 *
 * @author lhs
 * @date 2025-01-21
 */
public interface IDatasourceQualityService extends IService<DatasourceDO> {

    /**
     * 获得数据源分页列表
     *
     * @param pageReqVO 分页请求
     * @return 数据源分页列表
     */
    PageResult<DatasourceDO> getDatasourcePage(DatasourcePageReqVO pageReqVO);

    List<DatasourceDO> getDatasourceList(DatasourcePageReqVO pageReqVO);

    AssetsDatasourceRespDTO getDatasourceById(Long id);

    /**
     * 查询数据资产的数据源连接信息
     *
     * @param daAsset
     * @return
     */
    List<DatasourceDO> getDataSourceByAsset(DatasourceRespVO daAsset);


    /**
     * 创建数据源
     *
     * @param createReqVO 数据源信息
     * @return 数据源编号
     */
    Long createDatasource(DatasourceSaveReqVO createReqVO);

    /**
     * 删除数据源
     *
     * @param idList 数据源编号
     */
    int removeDatasource(Collection<Long> idList);



    /**
     * 获得数据源详情
     *
     * @param id 数据源编号
     * @return 数据源
     */
    DatasourceDO getDatasourceDOById(Long id);
    DatasourceRespVO getDatasourceByIdSimple(Long id);

    /**
     * 获得全部数据源列表
     *
     * @return 数据源列表
     */
    List<DatasourceDO> getDatasourceList();

    /**
     * 获得全部数据源 Map
     *
     * @return 数据源 Map
     */
    Map<Long, DatasourceDO> getDatasourceMap();


    /**
     * 导入数据源数据
     *
     * @param importExcelList 数据源数据列表
     * @param isUpdateSupport 是否更新支持，如果已存在，则进行更新数据
     * @param operName        操作用户
     * @return 结果
     */
    String importDatasource(List<DatasourceRespVO> importExcelList, boolean isUpdateSupport, String operName);


    AjaxResult clientsTest(Long id);

    /**
     * 获取数据库表信息
     *
     * @param id 数据源id
     * @return
     */
    List<DbTable> getDbTables(Long id);

    /**
     * 获取数据库
     * 表的字段信息
     *
     * @param id        数据源id
     * @param tableName 表名称
     * @return
     */
    List<DbColumn> getDbTableColumns(Long id, String tableName);
}
