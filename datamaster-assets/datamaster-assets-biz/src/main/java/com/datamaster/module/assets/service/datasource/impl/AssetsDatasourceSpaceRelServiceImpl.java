package com.datamaster.module.assets.service.datasource.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.yulichang.wrapper.MPJLambdaWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.common.exception.ServiceException;
import com.datamaster.common.utils.StringUtils;
import com.datamaster.common.utils.object.BeanUtils;
import com.datamaster.module.assets.controller.admin.datasource.vo.AssetsDatasourceSpaceRelPageReqVO;
import com.datamaster.module.assets.controller.admin.datasource.vo.AssetsDatasourceSpaceRelRespVO;
import com.datamaster.module.assets.controller.admin.datasource.vo.AssetsDatasourceSpaceRelSaveReqVO;
import com.datamaster.module.assets.dal.dataobject.datasource.AssetsDatasourceSpaceRelDO;
import com.datamaster.module.assets.dal.mapper.datasource.AssetsDatasourceSpaceRelMapper;
import com.datamaster.module.assets.service.datasource.IAssetsDatasourceSpaceRelService;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service
 *
 * @author DATAMASTER
 * @date 2025-03-13
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class AssetsDatasourceSpaceRelServiceImpl extends ServiceImpl<AssetsDatasourceSpaceRelMapper, AssetsDatasourceSpaceRelDO> implements IAssetsDatasourceSpaceRelService {
    @Resource
    private AssetsDatasourceSpaceRelMapper AssetsDatasourceSpaceRelMapper;

    @Override
    public PageResult<AssetsDatasourceSpaceRelDO> getDatasourceSpaceRelPage(AssetsDatasourceSpaceRelPageReqVO pageReqVO) {
        return AssetsDatasourceSpaceRelMapper.selectPage(pageReqVO);
    }

    @Override
    public Long createDatasourceSpaceRel(AssetsDatasourceSpaceRelSaveReqVO createReqVO) {
        AssetsDatasourceSpaceRelDO dictType = BeanUtils.toBean(createReqVO, AssetsDatasourceSpaceRelDO.class);
        AssetsDatasourceSpaceRelMapper.insert(dictType);
        return dictType.getId();
    }

    @Override
    public int updateDatasourceSpaceRel(AssetsDatasourceSpaceRelSaveReqVO updateReqVO) {
        // 相关校验

        // 更新数据源与空间关联关系
        AssetsDatasourceSpaceRelDO updateObj = BeanUtils.toBean(updateReqVO, AssetsDatasourceSpaceRelDO.class);
        return AssetsDatasourceSpaceRelMapper.updateById(updateObj);
    }

    @Override
    public int removeDatasourceSpaceRel(Collection<Long> idList) {
        // 批量删除数据源与空间关联关系
        return AssetsDatasourceSpaceRelMapper.deleteBatchIds(idList);
    }

    @Override
    public AssetsDatasourceSpaceRelDO getDatasourceSpaceRelById(Long id) {
        return AssetsDatasourceSpaceRelMapper.selectById(id);
    }

    @Override
    public List<AssetsDatasourceSpaceRelDO> getDatasourceSpaceRelList() {
        return AssetsDatasourceSpaceRelMapper.selectList();
    }

    @Override
    public List<AssetsDatasourceSpaceRelDO> getDatasourceSpaceRelList(AssetsDatasourceSpaceRelDO assetsDatasourceSpaceRelDO) {
        LambdaQueryWrapper<AssetsDatasourceSpaceRelDO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(assetsDatasourceSpaceRelDO.getDatasourceId() != null, AssetsDatasourceSpaceRelDO::getDatasourceId, assetsDatasourceSpaceRelDO.getDatasourceId());
        queryWrapper.eq(assetsDatasourceSpaceRelDO.getSpaceId() != null, AssetsDatasourceSpaceRelDO::getSpaceId, assetsDatasourceSpaceRelDO.getSpaceId());
        queryWrapper.eq(StringUtils.isNotEmpty(assetsDatasourceSpaceRelDO.getSpaceCode()), AssetsDatasourceSpaceRelDO::getSpaceCode, assetsDatasourceSpaceRelDO.getSpaceCode());
        return AssetsDatasourceSpaceRelMapper.selectList(queryWrapper);
    }

    @Override
    public List<AssetsDatasourceSpaceRelDO> getJoinSpaceAndDatasource(AssetsDatasourceSpaceRelDO assetsDatasourceSpaceRelDO) {
        MPJLambdaWrapper<AssetsDatasourceSpaceRelDO> lambdaWrapper = new MPJLambdaWrapper();
        lambdaWrapper.selectAll(AssetsDatasourceSpaceRelDO.class)
                .select("u.datasource_name as datasourceName,d.name as spaceName")
                .leftJoin("AST_DATASOURCE u on t.DATASOURCE_ID = u.id")
                .leftJoin("TAX_SPACE d on t.SPACE_ID = d.id")
                .eq("u.del_flag", "0")
                .eq("d.del_flag", "0")
                .eq(assetsDatasourceSpaceRelDO.getDatasourceId() != null, AssetsDatasourceSpaceRelDO::getDatasourceId, assetsDatasourceSpaceRelDO.getDatasourceId())
                .eq(assetsDatasourceSpaceRelDO.getSpaceId() != null, AssetsDatasourceSpaceRelDO::getSpaceId, assetsDatasourceSpaceRelDO.getSpaceId())
                .eq(StringUtils.isNotEmpty(assetsDatasourceSpaceRelDO.getSpaceCode()), AssetsDatasourceSpaceRelDO::getSpaceCode, assetsDatasourceSpaceRelDO.getSpaceCode());
        return AssetsDatasourceSpaceRelMapper.selectList(lambdaWrapper);
    }

    @Override
    public Map<Long, AssetsDatasourceSpaceRelDO> getDatasourceSpaceRelMap() {
        List<AssetsDatasourceSpaceRelDO> AssetsDatasourceSpaceRelList = AssetsDatasourceSpaceRelMapper.selectList();
        return AssetsDatasourceSpaceRelList.stream()
                .collect(Collectors.toMap(
                        AssetsDatasourceSpaceRelDO::getId,
                        AssetsDatasourceSpaceRelDO -> AssetsDatasourceSpaceRelDO,
                        // 保留已存在的值
                        (existing, replacement) -> existing
                ));
    }

    /**
     *
     *
     * @param importExcelList
     * @param isUpdateSupport
     * @param operName
     * @return
     */
    @Override
    public String importDatasourceSpaceRel(List<AssetsDatasourceSpaceRelRespVO> importExcelList, boolean isUpdateSupport, String operName) {
        if (StringUtils.isNull(importExcelList) || importExcelList.size() == 0) {
            throw new ServiceException("");
        }

        int successNum = 0;
        int failureNum = 0;
        List<String> successMessages = new ArrayList<>();
        List<String> failureMessages = new ArrayList<>();

        for (AssetsDatasourceSpaceRelRespVO respVO : importExcelList) {
            try {
                AssetsDatasourceSpaceRelDO AssetsDatasourceSpaceRelDO = BeanUtils.toBean(respVO, AssetsDatasourceSpaceRelDO.class);
                Long AssetsDatasourceSpaceRelId = respVO.getId();
                if (isUpdateSupport) {
                    if (AssetsDatasourceSpaceRelId != null) {
                        AssetsDatasourceSpaceRelDO existingDatasourceSpaceRel = AssetsDatasourceSpaceRelMapper.selectById(AssetsDatasourceSpaceRelId);
                        if (existingDatasourceSpaceRel != null) {
                            AssetsDatasourceSpaceRelMapper.updateById(AssetsDatasourceSpaceRelDO);
                            successNum++;
                            successMessages.add("ID " + AssetsDatasourceSpaceRelId + " ");
                        } else {
                            failureNum++;
                            failureMessages.add("ID " + AssetsDatasourceSpaceRelId + " ");
                        }
                    } else {
                        failureNum++;
                        failureMessages.add("ID");
                    }
                } else {
                    QueryWrapper<AssetsDatasourceSpaceRelDO> queryWrapper = new QueryWrapper<>();
                    queryWrapper.eq("id", AssetsDatasourceSpaceRelId);
                    AssetsDatasourceSpaceRelDO existingDatasourceSpaceRel = AssetsDatasourceSpaceRelMapper.selectOne(queryWrapper);
                    if (existingDatasourceSpaceRel == null) {
                        AssetsDatasourceSpaceRelMapper.insert(AssetsDatasourceSpaceRelDO);
                        successNum++;
                        successMessages.add("ID " + AssetsDatasourceSpaceRelId + " ");
                    } else {
                        failureNum++;
                        failureMessages.add("ID " + AssetsDatasourceSpaceRelId + " ");
                    }
                }
            } catch (Exception e) {
                failureNum++;
                String errorMsg = "" + e.getMessage();
                failureMessages.add(errorMsg);
                log.error(errorMsg, e);
            }
        }
        StringBuilder resultMsg = new StringBuilder();
        if (failureNum > 0) {
            resultMsg.append(" ").append(failureNum).append(" ");
            resultMsg.append("<br/>").append(String.join("<br/>", failureMessages));
            throw new ServiceException(resultMsg.toString());
        } else {
            resultMsg.append(" ").append(successNum).append(" ");
        }
        return resultMsg.toString();
    }
}
