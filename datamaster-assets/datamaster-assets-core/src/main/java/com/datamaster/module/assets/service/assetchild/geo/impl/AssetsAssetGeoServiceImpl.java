package com.datamaster.module.assets.service.assetchild.geo.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.common.exception.ServiceException;
import com.datamaster.common.utils.StringUtils;
import com.datamaster.common.utils.object.BeanUtils;
import com.datamaster.module.assets.controller.admin.assetchild.geo.vo.AssetsAssetGeoPageReqVO;
import com.datamaster.module.assets.controller.admin.assetchild.geo.vo.AssetsAssetGeoRespVO;
import com.datamaster.module.assets.controller.admin.assetchild.geo.vo.AssetsAssetGeoSaveReqVO;
import com.datamaster.module.assets.dal.dataobject.assetchild.geo.AssetsAssetGeoDO;
import com.datamaster.module.assets.dal.mapper.assetchild.geo.AssetsAssetGeoMapper;
import com.datamaster.module.assets.service.assetchild.geo.IAssetsAssetGeoService;
import com.datamaster.mybatis.core.query.LambdaQueryWrapperX;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * -Service
 *
 * @author DATAMASTER
 * @date 2025-04-14
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class AssetsAssetGeoServiceImpl  extends ServiceImpl<AssetsAssetGeoMapper,AssetsAssetGeoDO> implements IAssetsAssetGeoService {
    @Resource
    private AssetsAssetGeoMapper AssetsAssetGeoMapper;

    @Override
    public PageResult<AssetsAssetGeoDO> getAssetGeoPage(AssetsAssetGeoPageReqVO pageReqVO) {
        return AssetsAssetGeoMapper.selectPage(pageReqVO);
    }

    @Override
    public AssetsAssetGeoRespVO getAssetGeoByAssetId(Long assetId) {
        LambdaQueryWrapperX<AssetsAssetGeoDO> queryWrapperX = new LambdaQueryWrapperX<>();
        queryWrapperX.eqIfPresent(AssetsAssetGeoDO::getAssetId,assetId);
        AssetsAssetGeoDO AssetsAssetApiDO = AssetsAssetGeoMapper.selectOne(queryWrapperX);
        return BeanUtils.toBean(AssetsAssetApiDO, AssetsAssetGeoRespVO.class);
    }

    @Override
    public Long createAssetGeo(AssetsAssetGeoSaveReqVO createReqVO) {
        AssetsAssetGeoDO dictType = BeanUtils.toBean(createReqVO, AssetsAssetGeoDO.class);
        AssetsAssetGeoMapper.insert(dictType);
        return dictType.getId();
    }

    @Override
    public int updateAssetGeo(AssetsAssetGeoSaveReqVO updateReqVO) {
        // 相关校验

        // 更新数据资产-矢量
        AssetsAssetGeoDO updateObj = BeanUtils.toBean(updateReqVO, AssetsAssetGeoDO.class);
        return AssetsAssetGeoMapper.updateById(updateObj);
    }
    @Override
    public int removeAssetGeo(Collection<Long> idList) {
        // 批量删除数据资产-矢量
        return AssetsAssetGeoMapper.deleteBatchIds(idList);
    }

    @Override
    public AssetsAssetGeoDO getAssetGeoById(Long id) {
        return AssetsAssetGeoMapper.selectById(id);
    }

    @Override
    public List<AssetsAssetGeoDO> getAssetGeoList() {
        return AssetsAssetGeoMapper.selectList();
    }

    @Override
    public Map<Long, AssetsAssetGeoDO> getAssetGeoMap() {
        List<AssetsAssetGeoDO> AssetsAssetGeoList = AssetsAssetGeoMapper.selectList();
        return AssetsAssetGeoList.stream()
                .collect(Collectors.toMap(
                        AssetsAssetGeoDO::getId,
                        AssetsAssetGeoDO -> AssetsAssetGeoDO,
                        // 保留已存在的值
                        (existing, replacement) -> existing
                ));
    }

        /**
         * -
         *
         * @param importExcelList -
         * @param isUpdateSupport
         * @param operName
         * @return
         */
        @Override
        public String importAssetGeo(List<AssetsAssetGeoRespVO> importExcelList, boolean isUpdateSupport, String operName) {
            if (StringUtils.isNull(importExcelList) || importExcelList.size() == 0) {
                throw new ServiceException("");
            }

            int successNum = 0;
            int failureNum = 0;
            List<String> successMessages = new ArrayList<>();
            List<String> failureMessages = new ArrayList<>();

            for (AssetsAssetGeoRespVO respVO : importExcelList) {
                try {
                    AssetsAssetGeoDO AssetsAssetGeoDO = BeanUtils.toBean(respVO, AssetsAssetGeoDO.class);
                    Long AssetsAssetGeoId = respVO.getId();
                    if (isUpdateSupport) {
                        if (AssetsAssetGeoId != null) {
                            AssetsAssetGeoDO existingAssetGeo = AssetsAssetGeoMapper.selectById(AssetsAssetGeoId);
                            if (existingAssetGeo != null) {
                                AssetsAssetGeoMapper.updateById(AssetsAssetGeoDO);
                                successNum++;
                                successMessages.add("ID " + AssetsAssetGeoId + " -");
                            } else {
                                failureNum++;
                                failureMessages.add("ID " + AssetsAssetGeoId + " -");
                            }
                        } else {
                            failureNum++;
                            failureMessages.add("ID");
                        }
                    } else {
                        QueryWrapper<AssetsAssetGeoDO> queryWrapper = new QueryWrapper<>();
                        queryWrapper.eq("id", AssetsAssetGeoId);
                        AssetsAssetGeoDO existingAssetGeo = AssetsAssetGeoMapper.selectOne(queryWrapper);
                        if (existingAssetGeo == null) {
                            AssetsAssetGeoMapper.insert(AssetsAssetGeoDO);
                            successNum++;
                            successMessages.add("ID " + AssetsAssetGeoId + " -");
                        } else {
                            failureNum++;
                            failureMessages.add("ID " + AssetsAssetGeoId + " -");
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
