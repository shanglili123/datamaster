

package com.datamaster.module.taxonomy.service.cat.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.common.exception.ServiceException;
import com.datamaster.common.utils.StringUtils;
import com.datamaster.common.utils.YouBianCodeUtil;
import com.datamaster.common.utils.object.BeanUtils;
import com.datamaster.module.taxonomy.api.cat.dto.TaxonomyApiCatReqDTO;
import com.datamaster.module.taxonomy.api.cat.dto.TaxonomyApiCatRespDTO;
import com.datamaster.module.taxonomy.api.service.cat.ITaxonomyApiCatApiService;
import com.datamaster.module.taxonomy.controller.admin.cat.vo.TaxonomyApiCatPageReqVO;
import com.datamaster.module.taxonomy.controller.admin.cat.vo.TaxonomyApiCatRespVO;
import com.datamaster.module.taxonomy.controller.admin.cat.vo.TaxonomyApiCatSaveReqVO;
import com.datamaster.module.taxonomy.dal.dataobject.cat.TaxonomyApiCatDO;
import com.datamaster.module.taxonomy.dal.dataobject.cat.TaxonomyTaskCatDO;
import com.datamaster.module.taxonomy.dal.mapper.cat.TaxonomyApiCatMapper;
import com.datamaster.module.taxonomy.service.cat.ITaxonomyApiCatService;
import com.datamaster.module.service.api.service.api.ServiceApiService;
import com.datamaster.mybatis.core.query.LambdaQueryWrapperX;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 数据服务类目管理Service业务层处理
 *
 * @author DATAMASTER
 * @date 2025-03-11
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class TaxonomyApiCatServiceImpl extends ServiceImpl<TaxonomyApiCatMapper, TaxonomyApiCatDO> implements ITaxonomyApiCatService, ITaxonomyApiCatApiService {
    @Resource
    private TaxonomyApiCatMapper TaxonomyApiCatMapper;
    @Resource
    private ServiceApiService serviceApiService;

    @Override
    public PageResult<TaxonomyApiCatDO> getAttApiCatPage(TaxonomyApiCatPageReqVO pageReqVO) {
        return TaxonomyApiCatMapper.selectPage(pageReqVO);
    }

    @Override
    public Long createAttApiCat(TaxonomyApiCatSaveReqVO createReqVO) {
        TaxonomyApiCatDO dictType = BeanUtils.toBean(createReqVO, TaxonomyApiCatDO.class);
        dictType.setCode(createCode(createReqVO.getParentId(), null));
        TaxonomyApiCatMapper.insert(dictType);
        return dictType.getId();
    }

    @Override
    public int updateAttApiCat(TaxonomyApiCatSaveReqVO updateReqVO) {
        TaxonomyApiCatDO catDO = baseMapper.selectById(updateReqVO.getId());
        if (catDO == null) {
            return 0;
        }
        if (Boolean.FALSE.equals(updateReqVO.getValidFlag())) {
            Long countData = serviceApiService.getCountByCatCode(catDO.getCode());
            if (countData > 0) {
                throw new ServiceException("存在API服务，不允许禁用");
            }
            baseMapper.updateValidFlag(catDO.getCode(), updateReqVO.getValidFlag());
        } else if (Boolean.TRUE.equals(updateReqVO.getValidFlag())) {
            TaxonomyApiCatDO parent = baseMapper.selectById(catDO.getParentId());
            if (parent != null && Boolean.FALSE.equals(parent.getValidFlag())) {
                throw new ServiceException("须先启用父级");
            }
        }
        // 更新数据服务类目管理
        TaxonomyApiCatDO updateObj = BeanUtils.toBean(updateReqVO, TaxonomyApiCatDO.class);
        return TaxonomyApiCatMapper.updateById(updateObj);
    }

    @Override
    public int removeAttApiCat(Collection<Long> idList) {
        List<TaxonomyApiCatDO> TaxonomyApiCatDOS = baseMapper.selectBatchIds(idList);
        for (TaxonomyApiCatDO catDO : TaxonomyApiCatDOS) {
            Long countData = serviceApiService.getCountByCatCode(catDO.getCode());
            if (countData > 0) {
                throw new ServiceException("存在API服务，不允许删除");
            }
        }
        // 批量删除数据服务类目管理
        return TaxonomyApiCatMapper.deleteBatchIds(idList);
    }

    @Override
    public TaxonomyApiCatDO getAttApiCatById(Long id) {
        return TaxonomyApiCatMapper.selectById(id);
    }

    @Override
    public List<TaxonomyApiCatDO> getAttApiCatList() {
        return TaxonomyApiCatMapper.selectList();
    }

    @Override
    public List<TaxonomyApiCatDO> getAttApiCatList(TaxonomyApiCatPageReqVO reqVO) {
        LambdaQueryWrapperX<TaxonomyApiCatDO> queryWrapperX = new LambdaQueryWrapperX<>();
        queryWrapperX.likeIfPresent(TaxonomyApiCatDO::getName, reqVO.getName())
                .eqIfPresent(TaxonomyApiCatDO::getParentId, reqVO.getParentId())
                .eqIfPresent(TaxonomyApiCatDO::getSortOrder, reqVO.getSortOrder())
                .eqIfPresent(TaxonomyApiCatDO::getDescription, reqVO.getDescription())
                .eqIfPresent(TaxonomyApiCatDO::getCode, reqVO.getCode())
                .eqIfPresent(TaxonomyApiCatDO::getCreateTime, reqVO.getCreateTime())
                .eq(reqVO.getValidFlag() != null, TaxonomyApiCatDO::getValidFlag, Boolean.TRUE.equals(reqVO.getValidFlag()) ? "1" : "0")
                .orderByAsc(TaxonomyApiCatDO::getSortOrder);
        return TaxonomyApiCatMapper.selectList(queryWrapperX);
    }

    @Override
    public Map<Long, TaxonomyApiCatDO> getAttApiCatMap() {
        List<TaxonomyApiCatDO> TaxonomyApiCatList = TaxonomyApiCatMapper.selectList();
        return TaxonomyApiCatList.stream()
                .collect(Collectors.toMap(
                        TaxonomyApiCatDO::getId,
                        TaxonomyApiCatDO -> TaxonomyApiCatDO,
                        // 保留已存在的值
                        (existing, replacement) -> existing
                ));
    }

    @Override
    public String createCode(Long parentId, String parentCode) {
        String categoryCode = null;
        /*
         * 分成三种情况
         * 1.数据库无数据 调用YouBianCodeUtil.getNextYouBianCode(null);
         * 2.添加子节点，无兄弟元素 YouBianCodeUtil.getSubYouBianCode(parentCode,null);
         * 3.添加子节点有兄弟元素 YouBianCodeUtil.getNextYouBianCode(lastCode);
         * */
        //找同类 确定上一个最大的code值
        LambdaQueryWrapper<TaxonomyApiCatDO> query = new LambdaQueryWrapper<TaxonomyApiCatDO>()
                .eq(TaxonomyApiCatDO::getParentId, parentId)
                .likeRight(StringUtils.isNotBlank(parentCode), TaxonomyApiCatDO::getCode, parentCode)
                .isNotNull(TaxonomyApiCatDO::getCode)
                .orderByDesc(TaxonomyApiCatDO::getCode);
        List<TaxonomyApiCatDO> list = baseMapper.selectList(query);
        if (list == null || list.size() == 0) {
            if (parentId == 0) {
                //情况1
                categoryCode = YouBianCodeUtil.getNextYouBianCode(null);
            } else {
                //情况2
                TaxonomyApiCatDO parent = baseMapper.selectById(parentId);
                categoryCode = YouBianCodeUtil.getSubYouBianCode(parent.getCode(), null);
            }
        } else {
            //情况3
            categoryCode = YouBianCodeUtil.getNextYouBianCode(list.get(0).getCode());
        }
        return categoryCode;
    }

    /**
     * 导入数据服务类目管理数据
     *
     * @param importExcelList 数据服务类目管理数据列表
     * @param isUpdateSupport 是否更新支持，如果已存在，则进行更新数据
     * @param operName        操作用户
     * @return 结果
     */
    @Override
    public String importAttApiCat(List<TaxonomyApiCatRespVO> importExcelList, boolean isUpdateSupport, String operName) {
        if (StringUtils.isNull(importExcelList) || importExcelList.size() == 0) {
            throw new ServiceException("导入数据不能为空！");
        }

        int successNum = 0;
        int failureNum = 0;
        List<String> successMessages = new ArrayList<>();
        List<String> failureMessages = new ArrayList<>();

        for (TaxonomyApiCatRespVO respVO : importExcelList) {
            try {
                TaxonomyApiCatDO TaxonomyApiCatDO = BeanUtils.toBean(respVO, TaxonomyApiCatDO.class);
                Long TaxonomyApiCatId = respVO.getId();
                if (isUpdateSupport) {
                    if (TaxonomyApiCatId != null) {
                        TaxonomyApiCatDO existingAttApiCat = TaxonomyApiCatMapper.selectById(TaxonomyApiCatId);
                        if (existingAttApiCat != null) {
                            TaxonomyApiCatMapper.updateById(TaxonomyApiCatDO);
                            successNum++;
                            successMessages.add("数据更新成功，ID为 " + TaxonomyApiCatId + " 的数据服务类目管理记录。");
                        } else {
                            failureNum++;
                            failureMessages.add("数据更新失败，ID为 " + TaxonomyApiCatId + " 的数据服务类目管理记录不存在。");
                        }
                    } else {
                        failureNum++;
                        failureMessages.add("数据更新失败，某条记录的ID不存在。");
                    }
                } else {
                    QueryWrapper<TaxonomyApiCatDO> queryWrapper = new QueryWrapper<>();
                    queryWrapper.eq("id", TaxonomyApiCatId);
                    TaxonomyApiCatDO existingAttApiCat = TaxonomyApiCatMapper.selectOne(queryWrapper);
                    if (existingAttApiCat == null) {
                        TaxonomyApiCatMapper.insert(TaxonomyApiCatDO);
                        successNum++;
                        successMessages.add("数据插入成功，ID为 " + TaxonomyApiCatId + " 的数据服务类目管理记录。");
                    } else {
                        failureNum++;
                        failureMessages.add("数据插入失败，ID为 " + TaxonomyApiCatId + " 的数据服务类目管理记录已存在。");
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
    public List<TaxonomyApiCatRespDTO> getAttApiCatList(TaxonomyApiCatReqDTO TaxonomyApiCatReqDTO) {
        LambdaQueryWrapperX<TaxonomyApiCatDO> queryWrapperX = new LambdaQueryWrapperX<>();
        queryWrapperX.likeIfPresent(TaxonomyApiCatDO::getName, TaxonomyApiCatReqDTO.getName())
                .eqIfPresent(TaxonomyApiCatDO::getParentId, TaxonomyApiCatReqDTO.getParentId())
                .eqIfPresent(TaxonomyApiCatDO::getSortOrder, TaxonomyApiCatReqDTO.getSortOrder())
                .eqIfPresent(TaxonomyApiCatDO::getDescription, TaxonomyApiCatReqDTO.getDescription())
                .eqIfPresent(TaxonomyApiCatDO::getCode, TaxonomyApiCatReqDTO.getCode())
                .orderByAsc(TaxonomyApiCatDO::getSortOrder);
        List<TaxonomyApiCatDO> TaxonomyApiCatDOS = TaxonomyApiCatMapper.selectList(queryWrapperX);
        if (CollectionUtils.isNotEmpty(TaxonomyApiCatDOS)) {
            List<TaxonomyApiCatRespDTO> TaxonomyApiCatRespDTOS = new ArrayList<>();
            for (int i = 0; i < TaxonomyApiCatDOS.size(); i++) {
                TaxonomyApiCatDO TaxonomyApiCatDO = TaxonomyApiCatDOS.get(i);
                TaxonomyApiCatRespDTO TaxonomyApiCatRespDTO = BeanUtils.toBean(TaxonomyApiCatDO, TaxonomyApiCatRespDTO.class);
                TaxonomyApiCatRespDTOS.add(TaxonomyApiCatRespDTO);
            }
            return TaxonomyApiCatRespDTOS;
        }
        return Collections.emptyList();
    }
}
