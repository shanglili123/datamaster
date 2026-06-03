

package com.datamaster.quality.service.datasource.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.datamaster.common.core.domain.AjaxResult;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.common.database.DataSourceFactory;
import com.datamaster.common.database.DbQuery;
import com.datamaster.common.database.constants.DbQueryProperty;
import com.datamaster.common.database.core.DbColumn;
import com.datamaster.common.database.core.DbTable;
import com.datamaster.common.database.exception.DataQueryException;
import com.datamaster.common.exception.ServiceException;
import com.datamaster.common.utils.StringUtils;
import com.datamaster.common.utils.object.BeanUtils;
import com.datamaster.module.assets.api.datasource.dto.AssetsDatasourceRespDTO;
import com.datamaster.mybatis.core.query.LambdaQueryWrapperX;
import com.datamaster.quality.controller.da.datasource.vo.DatasourcePageReqVO;
import com.datamaster.quality.controller.da.datasource.vo.DatasourceRespVO;
import com.datamaster.quality.controller.da.datasource.vo.DatasourceSaveReqVO;
import com.datamaster.quality.dal.dataobject.datasource.DatasourceDO;
import com.datamaster.quality.dal.mapper.datasource.DatasourceMapper;
import com.datamaster.quality.service.datasource.IDatasourceQualityService;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 数据源Service业务层处理
 *
 * @author lhs
 * @date 2025-01-21
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class DatasourceQualityServiceImpl extends ServiceImpl<DatasourceMapper, DatasourceDO> implements IDatasourceQualityService {
    @Resource
    private DatasourceMapper DatasourceMapper;

    @Autowired
    private DataSourceFactory dataSourceFactory;

    /**
     * 查询数据资产的数据源连接信息
     *
     * @param daAsset
     * @return
     */
    @Override
    public List<DatasourceDO> getDataSourceByAsset(DatasourceRespVO daAsset) {
        return DatasourceMapper.selectList();
    }

    @Override
    public PageResult<DatasourceDO> getDatasourcePage(DatasourcePageReqVO pageReqVO) {
        return DatasourceMapper.selectPage(pageReqVO);
    }


    @Override
    public List<DatasourceDO> getDatasourceList(DatasourcePageReqVO reqVO) {
        LambdaQueryWrapperX<DatasourceDO> DatasourceDOLambdaQueryWrapperX = new LambdaQueryWrapperX<>();
        DatasourceDOLambdaQueryWrapperX.likeIfPresent(DatasourceDO::getDatasourceName, reqVO.getDatasourceName())
                .like(StringUtils.isNotEmpty(reqVO.getDatasourceType()), DatasourceDO::getDatasourceType, reqVO.getDatasourceType())
                .eq(StringUtils.isNotEmpty(reqVO.getDatasourceConfig()), DatasourceDO::getDatasourceConfig, reqVO.getDatasourceConfig())
                .eq(StringUtils.isNotEmpty(reqVO.getIp()), DatasourceDO::getIp, reqVO.getIp());

        return DatasourceMapper.selectList(DatasourceDOLambdaQueryWrapperX);
    }

    @Override
    public Long createDatasource(DatasourceSaveReqVO createReqVO) {
        DatasourceDO dictType = BeanUtils.toBean(createReqVO, DatasourceDO.class);
        DatasourceMapper.insert(dictType);
        return dictType.getId();
    }

    @Override
    public int removeDatasource(Collection<Long> idList) {
        // 批量删除数据源
        return DatasourceMapper.deleteBatchIds(idList);
    }

    @Override
    public AssetsDatasourceRespDTO getDatasourceById(Long id) {
        AssetsDatasourceRespDTO dto = new AssetsDatasourceRespDTO();
        DatasourceDO DatasourceDO = DatasourceMapper.selectById(id);
        org.springframework.beans.BeanUtils.copyProperties(DatasourceDO, dto);
        return dto;
    }

    @Override
    public DatasourceDO getDatasourceDOById(Long id) {
        DatasourceDO DatasourceDO = DatasourceMapper.selectById(id);
        if (DatasourceDO == null) {
            return null;
        }
        return DatasourceDO;
    }

    @Override
    public DatasourceRespVO getDatasourceByIdSimple(Long id) {
        return BeanUtils.toBean(DatasourceMapper.selectById(id), DatasourceRespVO.class);
    }

    @Override
    public List<DatasourceDO> getDatasourceList() {
        return DatasourceMapper.selectList();
    }

    @Override
    public Map<Long, DatasourceDO> getDatasourceMap() {
        List<DatasourceDO> datasourceList = DatasourceMapper.selectList();
        return datasourceList.stream()
                .collect(Collectors.toMap(
                        DatasourceDO::getId,
                        DatasourceDO -> DatasourceDO,
                        // 保留已存在的值
                        (existing, replacement) -> existing
                ));
    }


    /**
     * 导入数据源数据
     *
     * @param importExcelList 数据源数据列表
     * @param isUpdateSupport 是否更新支持，如果已存在，则进行更新数据
     * @param operName        操作用户
     * @return 结果
     */
    @Override
    public String importDatasource(List<DatasourceRespVO> importExcelList, boolean isUpdateSupport, String operName) {
        if (StringUtils.isNull(importExcelList) || importExcelList.size() == 0) {
            throw new ServiceException("导入数据不能为空！");
        }

        int successNum = 0;
        int failureNum = 0;
        List<String> successMessages = new ArrayList<>();
        List<String> failureMessages = new ArrayList<>();

        for (DatasourceRespVO respVO : importExcelList) {
            try {
                DatasourceDO DatasourceDO = BeanUtils.toBean(respVO, DatasourceDO.class);
                Long datasourceId = respVO.getId();
                if (isUpdateSupport) {
                    if (datasourceId != null) {
                        DatasourceDO existingDatasource = DatasourceMapper.selectById(datasourceId);
                        if (existingDatasource != null) {
                            DatasourceMapper.updateById(DatasourceDO);
                            successNum++;
                            successMessages.add("数据更新成功，ID为 " + datasourceId + " 的数据源记录。");
                        } else {
                            failureNum++;
                            failureMessages.add("数据更新失败，ID为 " + datasourceId + " 的数据源记录不存在。");
                        }
                    } else {
                        failureNum++;
                        failureMessages.add("数据更新失败，某条记录的ID不存在。");
                    }
                } else {
                    QueryWrapper<DatasourceDO> queryWrapper = new QueryWrapper<>();
                    queryWrapper.eq("id", datasourceId);
                    DatasourceDO existingDatasource = DatasourceMapper.selectOne(queryWrapper);
                    if (existingDatasource == null) {
                        DatasourceMapper.insert(DatasourceDO);
                        successNum++;
                        successMessages.add("数据插入成功，ID为 " + datasourceId + " 的数据源记录。");
                    } else {
                        failureNum++;
                        failureMessages.add("数据插入失败，ID为 " + datasourceId + " 的数据源记录已存在。");
                    }
                }
            } catch (Exception e) {
                failureNum++;
                String errorMsg = "数据导入失败，错误信息：" + e.getMessage();
                failureMessages.add(errorMsg);
                log.error(errorMsg, e);
            }
        }
        StringBuilder resultMsg = new StringBuilder();
        if (failureNum > 0) {
            resultMsg.append("很抱歉，导入失败！共 ").append(failureNum).append(" 条数据格式不正确，错误如下：");
            resultMsg.append("<br/>").append(String.join("<br/>", failureMessages));
            throw new ServiceException(resultMsg.toString());
        } else {
            resultMsg.append("恭喜您，数据已全部导入成功！共 ").append(successNum).append(" 条。");
        }
        return resultMsg.toString();
    }


    @Override
    public AjaxResult clientsTest(Long id) {
        DbQuery dbQuery = this.buildDbQuery(id);
        if (dbQuery.valid()) {
            dbQuery.close();
            return AjaxResult.success("数据库连接成功");
        }
        dbQuery.close();
        return AjaxResult.error("数据库连接失败");

    }

    public DbQuery buildDbQuery(Long id) {
        DatasourceDO datasourceBy = this.getDatasourceDOById(id);
        if (datasourceBy == null) {
            throw new DataQueryException("数据源详情信息查询失败");
        }
        DbQueryProperty dbQueryProperty = new DbQueryProperty(
                datasourceBy.getDatasourceType(),
                datasourceBy.getIp(),
                datasourceBy.getPort(),
                datasourceBy.getDatasourceConfig()
        );
        return dataSourceFactory.createDbQuery(dbQueryProperty);
    }

    /**
     * @param id 数据源id
     * @return
     */
    @Override
    public List<DbTable> getDbTables(Long id) {
        DatasourceDO datasourceBy = this.getDatasourceDOById(id);
        if (datasourceBy == null) {
            throw new DataQueryException("数据源详情信息查询失败");
        }

        DbQueryProperty dbQueryProperty = new DbQueryProperty(datasourceBy.getDatasourceType()
                , datasourceBy.getIp(), datasourceBy.getPort(), datasourceBy.getDatasourceConfig());
        DbQuery dbQuery = dataSourceFactory.createDbQuery(dbQueryProperty);
        if (!dbQuery.valid()) {
            throw new DataQueryException("数据库连接失败");
        }
        List<DbTable> tables = dbQuery.getTables(dbQueryProperty);
        dbQuery.close();
        return tables;
    }

    /**
     * @param id        数据源id
     * @param tableName 表名称
     * @return
     */
    @Override
    public List<DbColumn> getDbTableColumns(Long id, String tableName) {
        if (StringUtils.isEmpty(tableName)) {
            throw new DataQueryException("表名不能为空");
        }

        DatasourceDO datasourceBy = this.getDatasourceDOById(id);
        if (datasourceBy == null) {
            throw new DataQueryException("数据源详情信息查询失败");
        }

        DbQueryProperty dbQueryProperty = new DbQueryProperty(datasourceBy.getDatasourceType()
                , datasourceBy.getIp(), datasourceBy.getPort(), datasourceBy.getDatasourceConfig());
        DbQuery dbQuery = dataSourceFactory.createDbQuery(dbQueryProperty);
        if (!dbQuery.valid()) {
            throw new DataQueryException("数据库连接失败");
        }
        List<DbColumn> tableColumns = dbQuery.getTableColumns(dbQueryProperty, tableName);
        dbQuery.close();

        return tableColumns;
    }



}
