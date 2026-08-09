package com.datamaster.module.assets.service.assetchild.files.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.common.exception.ServiceException;
import com.datamaster.common.utils.StringUtils;
import com.datamaster.common.utils.object.BeanUtils;
import com.datamaster.module.assets.controller.admin.assetchild.files.vo.AssetsAssetFilesPageReqVO;
import com.datamaster.module.assets.controller.admin.assetchild.files.vo.AssetsAssetFilesRespVO;
import com.datamaster.module.assets.controller.admin.assetchild.files.vo.AssetsAssetFilesSaveReqVO;
import com.datamaster.module.assets.dal.dataobject.assetchild.files.AssetsAssetFilesDO;
import com.datamaster.module.assets.dal.mapper.assetchild.files.AssetsAssetFilesMapper;
import com.datamaster.module.assets.service.assetchild.files.IAssetsAssetFilesService;

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
 * @date 2025-06-26
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class AssetsAssetFilesServiceImpl  extends ServiceImpl<AssetsAssetFilesMapper,AssetsAssetFilesDO> implements IAssetsAssetFilesService {
    @Resource
    private AssetsAssetFilesMapper AssetsAssetFilesMapper;

    @Override
    public PageResult<AssetsAssetFilesDO> getAssetFilesPage(AssetsAssetFilesPageReqVO pageReqVO) {
        return AssetsAssetFilesMapper.selectPage(pageReqVO);
    }

    @Override
    public Long createAssetFiles(AssetsAssetFilesSaveReqVO createReqVO) {
        AssetsAssetFilesDO dictType = BeanUtils.toBean(createReqVO, AssetsAssetFilesDO.class);
        AssetsAssetFilesMapper.insert(dictType);
        return dictType.getId();
    }

    @Override
    public int updateAssetFiles(AssetsAssetFilesSaveReqVO updateReqVO) {
        // 相关校验

        // 更新数据资产-文件服务
        AssetsAssetFilesDO updateObj = BeanUtils.toBean(updateReqVO, AssetsAssetFilesDO.class);
        return AssetsAssetFilesMapper.updateById(updateObj);
    }
    @Override
    public int removeAssetFiles(Collection<Long> idList) {
        // 批量删除数据资产-文件服务
        return AssetsAssetFilesMapper.deleteBatchIds(idList);
    }

    @Override
    public AssetsAssetFilesDO getAssetFilesById(Long id) {
        return AssetsAssetFilesMapper.selectById(id);
    }

    @Override
    public List<AssetsAssetFilesDO> getAssetFilesList() {
        return AssetsAssetFilesMapper.selectList();
    }

    @Override
    public Map<Long, AssetsAssetFilesDO> getAssetFilesMap() {
        List<AssetsAssetFilesDO> AssetsAssetFilesList = AssetsAssetFilesMapper.selectList();
        return AssetsAssetFilesList.stream()
                .collect(Collectors.toMap(
                        AssetsAssetFilesDO::getId,
                        AssetsAssetFilesDO -> AssetsAssetFilesDO,
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
        public String importAssetFiles(List<AssetsAssetFilesRespVO> importExcelList, boolean isUpdateSupport, String operName) {
            if (StringUtils.isNull(importExcelList) || importExcelList.size() == 0) {
                throw new ServiceException("");
            }

            int successNum = 0;
            int failureNum = 0;
            List<String> successMessages = new ArrayList<>();
            List<String> failureMessages = new ArrayList<>();

            for (AssetsAssetFilesRespVO respVO : importExcelList) {
                try {
                    AssetsAssetFilesDO AssetsAssetFilesDO = BeanUtils.toBean(respVO, AssetsAssetFilesDO.class);
                    Long AssetsAssetFilesId = respVO.getId();
                    if (isUpdateSupport) {
                        if (AssetsAssetFilesId != null) {
                            AssetsAssetFilesDO existingAssetFiles = AssetsAssetFilesMapper.selectById(AssetsAssetFilesId);
                            if (existingAssetFiles != null) {
                                AssetsAssetFilesMapper.updateById(AssetsAssetFilesDO);
                                successNum++;
                                successMessages.add("ID " + AssetsAssetFilesId + " -");
                            } else {
                                failureNum++;
                                failureMessages.add("ID " + AssetsAssetFilesId + " -");
                            }
                        } else {
                            failureNum++;
                            failureMessages.add("ID");
                        }
                    } else {
                        QueryWrapper<AssetsAssetFilesDO> queryWrapper = new QueryWrapper<>();
                        queryWrapper.eq("id", AssetsAssetFilesId);
                        AssetsAssetFilesDO existingAssetFiles = AssetsAssetFilesMapper.selectOne(queryWrapper);
                        if (existingAssetFiles == null) {
                            AssetsAssetFilesMapper.insert(AssetsAssetFilesDO);
                            successNum++;
                            successMessages.add("ID " + AssetsAssetFilesId + " -");
                        } else {
                            failureNum++;
                            failureMessages.add("ID " + AssetsAssetFilesId + " -");
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
