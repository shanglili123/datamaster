package com.datamaster.module.assets.service.assetchild.video.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.common.exception.ServiceException;
import com.datamaster.common.utils.StringUtils;
import com.datamaster.common.utils.object.BeanUtils;
import com.datamaster.module.assets.controller.admin.assetchild.video.vo.AssetsAssetVideoPageReqVO;
import com.datamaster.module.assets.controller.admin.assetchild.video.vo.AssetsAssetVideoReqVO;
import com.datamaster.module.assets.controller.admin.assetchild.video.vo.AssetsAssetVideoRespVO;
import com.datamaster.module.assets.controller.admin.assetchild.video.vo.AssetsAssetVideoSaveReqVO;
import com.datamaster.module.assets.dal.dataobject.assetchild.video.AssetsAssetVideoDO;
import com.datamaster.module.assets.dal.mapper.assetchild.video.AssetsAssetVideoMapper;
import com.datamaster.module.assets.service.assetchild.video.IAssetsAssetVideoService;
import com.datamaster.mybatis.core.query.LambdaQueryWrapperX;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
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
public class AssetsAssetVideoServiceImpl  extends ServiceImpl<AssetsAssetVideoMapper,AssetsAssetVideoDO> implements IAssetsAssetVideoService {
    @Resource
    private AssetsAssetVideoMapper AssetsAssetVideoMapper;

    @Override
    public PageResult<AssetsAssetVideoDO> getDaAssetVideoPage(AssetsAssetVideoPageReqVO pageReqVO) {
        return AssetsAssetVideoMapper.selectPage(pageReqVO);
    }

    @Override
    public AssetsAssetVideoRespVO getDaAssetVideoByAssetId(Long assetId) {
        LambdaQueryWrapperX<AssetsAssetVideoDO> queryWrapperX = new LambdaQueryWrapperX<>();
        queryWrapperX.eqIfPresent(AssetsAssetVideoDO::getAssetId,assetId);
        AssetsAssetVideoDO AssetsAssetApiDO = AssetsAssetVideoMapper.selectOne(queryWrapperX);
        return BeanUtils.toBean(AssetsAssetApiDO, AssetsAssetVideoRespVO.class);
    }

    @Override
    public Long createDaAssetVideo(AssetsAssetVideoSaveReqVO createReqVO) {
        AssetsAssetVideoDO dictType = BeanUtils.toBean(createReqVO, AssetsAssetVideoDO.class);
        AssetsAssetVideoMapper.insert(dictType);
        return dictType.getId();
    }

    @Override
    public int updateDaAssetVideo(AssetsAssetVideoSaveReqVO updateReqVO) {
        // 相关校验

        // 更新数据资产-视频数据
        AssetsAssetVideoDO updateObj = BeanUtils.toBean(updateReqVO, AssetsAssetVideoDO.class);
        return AssetsAssetVideoMapper.updateById(updateObj);
    }
    @Override
    public int removeDaAssetVideo(Collection<Long> idList) {
        // 批量删除数据资产-视频数据
        return AssetsAssetVideoMapper.deleteBatchIds(idList);
    }

    @Override
    public AssetsAssetVideoDO getDaAssetVideoById(Long id) {
        return AssetsAssetVideoMapper.selectById(id);
    }

    @Override
    public List<AssetsAssetVideoDO> getDaAssetVideoList() {
        return AssetsAssetVideoMapper.selectList();
    }

    @Override
    public Map<Long, AssetsAssetVideoDO> getDaAssetVideoMap() {
        List<AssetsAssetVideoDO> AssetsAssetVideoList = AssetsAssetVideoMapper.selectList();
        return AssetsAssetVideoList.stream()
                .collect(Collectors.toMap(
                        AssetsAssetVideoDO::getId,
                        AssetsAssetVideoDO -> AssetsAssetVideoDO,
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
        public String importDaAssetVideo(List<AssetsAssetVideoRespVO> importExcelList, boolean isUpdateSupport, String operName) {
            if (StringUtils.isNull(importExcelList) || importExcelList.size() == 0) {
                throw new ServiceException("");
            }

            int successNum = 0;
            int failureNum = 0;
            List<String> successMessages = new ArrayList<>();
            List<String> failureMessages = new ArrayList<>();

            for (AssetsAssetVideoRespVO respVO : importExcelList) {
                try {
                    AssetsAssetVideoDO AssetsAssetVideoDO = BeanUtils.toBean(respVO, AssetsAssetVideoDO.class);
                    Long AssetsAssetVideoId = respVO.getId();
                    if (isUpdateSupport) {
                        if (AssetsAssetVideoId != null) {
                            AssetsAssetVideoDO existingDaAssetVideo = AssetsAssetVideoMapper.selectById(AssetsAssetVideoId);
                            if (existingDaAssetVideo != null) {
                                AssetsAssetVideoMapper.updateById(AssetsAssetVideoDO);
                                successNum++;
                                successMessages.add("ID " + AssetsAssetVideoId + " -");
                            } else {
                                failureNum++;
                                failureMessages.add("ID " + AssetsAssetVideoId + " -");
                            }
                        } else {
                            failureNum++;
                            failureMessages.add("ID");
                        }
                    } else {
                        QueryWrapper<AssetsAssetVideoDO> queryWrapper = new QueryWrapper<>();
                        queryWrapper.eq("id", AssetsAssetVideoId);
                        AssetsAssetVideoDO existingDaAssetVideo = AssetsAssetVideoMapper.selectOne(queryWrapper);
                        if (existingDaAssetVideo == null) {
                            AssetsAssetVideoMapper.insert(AssetsAssetVideoDO);
                            successNum++;
                            successMessages.add("ID " + AssetsAssetVideoId + " -");
                        } else {
                            failureNum++;
                            failureMessages.add("ID " + AssetsAssetVideoId + " -");
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

    @Override
    public void queryServiceForwarding(HttpServletResponse response, AssetsAssetVideoReqVO AssetsAssetVideoReqVO) {

    }
}
