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
import com.datamaster.module.assets.controller.admin.datasource.vo.AssetsDatasourceProjectRelPageReqVO;
import com.datamaster.module.assets.controller.admin.datasource.vo.AssetsDatasourceProjectRelRespVO;
import com.datamaster.module.assets.controller.admin.datasource.vo.AssetsDatasourceProjectRelSaveReqVO;
import com.datamaster.module.assets.dal.dataobject.datasource.AssetsDatasourceProjectRelDO;
import com.datamaster.module.assets.dal.mapper.datasource.AssetsDatasourceProjectRelMapper;
import com.datamaster.module.assets.service.datasource.IAssetsDatasourceProjectRelService;

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
public class AssetsDatasourceProjectRelServiceImpl extends ServiceImpl<AssetsDatasourceProjectRelMapper, AssetsDatasourceProjectRelDO> implements IAssetsDatasourceProjectRelService {
    @Resource
    private AssetsDatasourceProjectRelMapper AssetsDatasourceProjectRelMapper;

    @Override
    public PageResult<AssetsDatasourceProjectRelDO> getDaDatasourceProjectRelPage(AssetsDatasourceProjectRelPageReqVO pageReqVO) {
        return AssetsDatasourceProjectRelMapper.selectPage(pageReqVO);
    }

    @Override
    public Long createDaDatasourceProjectRel(AssetsDatasourceProjectRelSaveReqVO createReqVO) {
        AssetsDatasourceProjectRelDO dictType = BeanUtils.toBean(createReqVO, AssetsDatasourceProjectRelDO.class);
        AssetsDatasourceProjectRelMapper.insert(dictType);
        return dictType.getId();
    }

    @Override
    public int updateDaDatasourceProjectRel(AssetsDatasourceProjectRelSaveReqVO updateReqVO) {
        // 相关校验

        // 更新数据源与项目关联关系
        AssetsDatasourceProjectRelDO updateObj = BeanUtils.toBean(updateReqVO, AssetsDatasourceProjectRelDO.class);
        return AssetsDatasourceProjectRelMapper.updateById(updateObj);
    }

    @Override
    public int removeDaDatasourceProjectRel(Collection<Long> idList) {
        // 批量删除数据源与项目关联关系
        return AssetsDatasourceProjectRelMapper.deleteBatchIds(idList);
    }

    @Override
    public AssetsDatasourceProjectRelDO getDaDatasourceProjectRelById(Long id) {
        return AssetsDatasourceProjectRelMapper.selectById(id);
    }

    @Override
    public List<AssetsDatasourceProjectRelDO> getDaDatasourceProjectRelList() {
        return AssetsDatasourceProjectRelMapper.selectList();
    }

    @Override
    public List<AssetsDatasourceProjectRelDO> getDaDatasourceProjectRelList(AssetsDatasourceProjectRelDO assetsDatasourceProjectRelDO) {
        LambdaQueryWrapper<AssetsDatasourceProjectRelDO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(assetsDatasourceProjectRelDO.getDatasourceId() != null, AssetsDatasourceProjectRelDO::getDatasourceId, assetsDatasourceProjectRelDO.getDatasourceId());
        queryWrapper.eq(assetsDatasourceProjectRelDO.getProjectId() != null, AssetsDatasourceProjectRelDO::getProjectId, assetsDatasourceProjectRelDO.getProjectId());
        queryWrapper.eq(StringUtils.isNotEmpty(assetsDatasourceProjectRelDO.getProjectCode()), AssetsDatasourceProjectRelDO::getProjectCode, assetsDatasourceProjectRelDO.getProjectCode());
        return AssetsDatasourceProjectRelMapper.selectList(queryWrapper);
    }

    @Override
    public List<AssetsDatasourceProjectRelDO> getJoinProjectAndDatasource(AssetsDatasourceProjectRelDO assetsDatasourceProjectRelDO) {
        MPJLambdaWrapper<AssetsDatasourceProjectRelDO> lambdaWrapper = new MPJLambdaWrapper();
        lambdaWrapper.selectAll(AssetsDatasourceProjectRelDO.class)
                .select("u.datasource_name as datasourceName,d.name as projectName")
                .leftJoin("AST_DATASOURCE u on t.DATASOURCE_ID = u.id")
                .leftJoin("TAX_PROJECT d on t.PROJECT_ID = d.id")
                .eq("u.del_flag", "0")
                .eq("d.del_flag", "0")
                .eq(assetsDatasourceProjectRelDO.getDatasourceId() != null, AssetsDatasourceProjectRelDO::getDatasourceId, assetsDatasourceProjectRelDO.getDatasourceId())
                .eq(assetsDatasourceProjectRelDO.getProjectId() != null, AssetsDatasourceProjectRelDO::getProjectId, assetsDatasourceProjectRelDO.getProjectId())
                .eq(StringUtils.isNotEmpty(assetsDatasourceProjectRelDO.getProjectCode()), AssetsDatasourceProjectRelDO::getProjectCode, assetsDatasourceProjectRelDO.getProjectCode());
        return AssetsDatasourceProjectRelMapper.selectList(lambdaWrapper);
    }

    @Override
    public Map<Long, AssetsDatasourceProjectRelDO> getDaDatasourceProjectRelMap() {
        List<AssetsDatasourceProjectRelDO> AssetsDatasourceProjectRelList = AssetsDatasourceProjectRelMapper.selectList();
        return AssetsDatasourceProjectRelList.stream()
                .collect(Collectors.toMap(
                        AssetsDatasourceProjectRelDO::getId,
                        AssetsDatasourceProjectRelDO -> AssetsDatasourceProjectRelDO,
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
    public String importDaDatasourceProjectRel(List<AssetsDatasourceProjectRelRespVO> importExcelList, boolean isUpdateSupport, String operName) {
        if (StringUtils.isNull(importExcelList) || importExcelList.size() == 0) {
            throw new ServiceException("");
        }

        int successNum = 0;
        int failureNum = 0;
        List<String> successMessages = new ArrayList<>();
        List<String> failureMessages = new ArrayList<>();

        for (AssetsDatasourceProjectRelRespVO respVO : importExcelList) {
            try {
                AssetsDatasourceProjectRelDO AssetsDatasourceProjectRelDO = BeanUtils.toBean(respVO, AssetsDatasourceProjectRelDO.class);
                Long AssetsDatasourceProjectRelId = respVO.getId();
                if (isUpdateSupport) {
                    if (AssetsDatasourceProjectRelId != null) {
                        AssetsDatasourceProjectRelDO existingDaDatasourceProjectRel = AssetsDatasourceProjectRelMapper.selectById(AssetsDatasourceProjectRelId);
                        if (existingDaDatasourceProjectRel != null) {
                            AssetsDatasourceProjectRelMapper.updateById(AssetsDatasourceProjectRelDO);
                            successNum++;
                            successMessages.add("ID " + AssetsDatasourceProjectRelId + " ");
                        } else {
                            failureNum++;
                            failureMessages.add("ID " + AssetsDatasourceProjectRelId + " ");
                        }
                    } else {
                        failureNum++;
                        failureMessages.add("ID");
                    }
                } else {
                    QueryWrapper<AssetsDatasourceProjectRelDO> queryWrapper = new QueryWrapper<>();
                    queryWrapper.eq("id", AssetsDatasourceProjectRelId);
                    AssetsDatasourceProjectRelDO existingDaDatasourceProjectRel = AssetsDatasourceProjectRelMapper.selectOne(queryWrapper);
                    if (existingDaDatasourceProjectRel == null) {
                        AssetsDatasourceProjectRelMapper.insert(AssetsDatasourceProjectRelDO);
                        successNum++;
                        successMessages.add("ID " + AssetsDatasourceProjectRelId + " ");
                    } else {
                        failureNum++;
                        failureMessages.add("ID " + AssetsDatasourceProjectRelId + " ");
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
