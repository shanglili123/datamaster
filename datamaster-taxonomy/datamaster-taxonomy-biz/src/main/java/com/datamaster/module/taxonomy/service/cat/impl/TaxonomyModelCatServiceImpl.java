

package com.datamaster.module.taxonomy.service.cat.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.common.exception.ServiceException;
import com.datamaster.common.utils.StringUtils;
import com.datamaster.common.utils.YouBianCodeUtil;
import com.datamaster.common.utils.object.BeanUtils;
import com.datamaster.module.taxonomy.controller.admin.cat.vo.TaxonomyModelCatPageReqVO;
import com.datamaster.module.taxonomy.controller.admin.cat.vo.TaxonomyModelCatRespVO;
import com.datamaster.module.taxonomy.controller.admin.cat.vo.TaxonomyModelCatSaveReqVO;
import com.datamaster.module.taxonomy.dal.dataobject.cat.TaxonomyModelCatDO;
import com.datamaster.module.taxonomy.dal.mapper.cat.TaxonomyModelCatMapper;
import com.datamaster.module.taxonomy.service.cat.ITaxonomyModelCatService;
import com.datamaster.module.standards.api.service.model.IStandardsModelApiService;
import com.datamaster.mybatis.core.query.LambdaQueryWrapperX;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 逻辑模型类目管理Service业务层处理
 *
 * @author DATAMASTER
 * @date 2025-01-20
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class TaxonomyModelCatServiceImpl extends ServiceImpl<TaxonomyModelCatMapper, TaxonomyModelCatDO> implements ITaxonomyModelCatService {
    @Resource
    private TaxonomyModelCatMapper TaxonomyModelCatMapper;

    @Resource
    private IStandardsModelApiService standardsModelApiService;

    @Override
    public PageResult<TaxonomyModelCatDO> getAttModelCatPage(TaxonomyModelCatPageReqVO pageReqVO) {
        return TaxonomyModelCatMapper.selectPage(pageReqVO);
    }

    @Override
    public Long createAttModelCat(TaxonomyModelCatSaveReqVO createReqVO) {
        TaxonomyModelCatDO dictType = BeanUtils.toBean(createReqVO, TaxonomyModelCatDO.class);
        dictType.setCode(createCode(createReqVO.getParentId(), null));
        TaxonomyModelCatMapper.insert(dictType);
        return dictType.getId();
    }

    @Override
    public int updateAttModelCat(TaxonomyModelCatSaveReqVO updateReqVO) {
        TaxonomyModelCatDO catDO = baseMapper.selectById(updateReqVO.getId());
        if (catDO == null) {
            return 0;
        }
        if (Boolean.FALSE.equals(updateReqVO.getValidFlag())) {
            Long countData = standardsModelApiService.getCountByCatCode(catDO.getCode());
            if (countData > 0) {
                throw new ServiceException("存在逻辑模型，不允许禁用");
            }
            baseMapper.updateValidFlag(catDO.getCode(), updateReqVO.getValidFlag());
        } else if (Boolean.TRUE.equals(updateReqVO.getValidFlag())) {
            TaxonomyModelCatDO parent = baseMapper.selectById(catDO.getParentId());
            if (parent != null && Boolean.FALSE.equals(parent.getValidFlag())) {
                throw new ServiceException("须先启用父级");
            }
        }
        // 更新逻辑模型类目管理
        TaxonomyModelCatDO updateObj = BeanUtils.toBean(updateReqVO, TaxonomyModelCatDO.class);
        return baseMapper.updateById(updateObj);
    }

    @Override
    public int removeAttModelCat(Collection<Long> idList) {
        int count = 0;
        for (Long id : idList) {
            TaxonomyModelCatDO cat = baseMapper.selectById(id);
            //判断是否存在数据
            if (standardsModelApiService.getCountByCatCode(cat.getCode()) > 0) {
                throw new ServiceException("存在逻辑模型，不允许删除");
            }
            if (cat != null) {
                count += baseMapper.delete(Wrappers.lambdaQuery(TaxonomyModelCatDO.class)
                        .likeRight(TaxonomyModelCatDO::getCode, cat.getCode()));
            }
        }
        return count;
    }

    @Override
    public int removeAttModelCat(Long id) {
        int count = 0;
        TaxonomyModelCatDO cat = baseMapper.selectById(id);
        //判断是否存在数据
        if (standardsModelApiService.getCountByCatCode(cat.getCode()) > 0) {
            throw new ServiceException("存在逻辑模型，不允许删除");
        }
        if (cat != null) {
            count += baseMapper.delete(Wrappers.lambdaQuery(TaxonomyModelCatDO.class)
                    .likeRight(TaxonomyModelCatDO::getCode, cat.getCode()));
        }
        return count;
    }

    @Override
    public TaxonomyModelCatDO getAttModelCatById(Long id) {
        return TaxonomyModelCatMapper.selectById(id);
    }

    @Override
    public List<TaxonomyModelCatDO> getAttModelCatList() {
        return TaxonomyModelCatMapper.selectList();
    }

    @Override
    public List<TaxonomyModelCatDO> getAttModelCatList(TaxonomyModelCatPageReqVO reqVO) {
        LambdaQueryWrapperX<TaxonomyModelCatDO> queryWrapperX = new LambdaQueryWrapperX<>();
        queryWrapperX.likeIfPresent(TaxonomyModelCatDO::getName, reqVO.getName())
                .likeRightIfPresent(TaxonomyModelCatDO::getCode, reqVO.getCode())
                .eq(reqVO.getValidFlag() != null, TaxonomyModelCatDO::getValidFlag, Boolean.TRUE.equals(reqVO.getValidFlag()) ? "1" : "0")
                .orderByAsc(TaxonomyModelCatDO::getSortOrder);
        return TaxonomyModelCatMapper.selectList(queryWrapperX);
    }

    @Override
    public Map<Long, TaxonomyModelCatDO> getAttModelCatMap() {
        List<TaxonomyModelCatDO> TaxonomyModelCatList = TaxonomyModelCatMapper.selectList();
        return TaxonomyModelCatList.stream()
                .collect(Collectors.toMap(
                        TaxonomyModelCatDO::getId,
                        TaxonomyModelCatDO -> TaxonomyModelCatDO,
                        // 保留已存在的值
                        (existing, replacement) -> existing
                ));
    }


    /**
     * 导入逻辑模型类目管理数据
     *
     * @param importExcelList 逻辑模型类目管理数据列表
     * @param isUpdateSupport 是否更新支持，如果已存在，则进行更新数据
     * @param operName        操作用户
     * @return 结果
     */
    @Override
    public String importAttModelCat(List<TaxonomyModelCatRespVO> importExcelList, boolean isUpdateSupport, String operName) {
        if (StringUtils.isNull(importExcelList) || importExcelList.size() == 0) {
            throw new ServiceException("导入数据不能为空！");
        }

        int successNum = 0;
        int failureNum = 0;
        List<String> successMessages = new ArrayList<>();
        List<String> failureMessages = new ArrayList<>();

        for (TaxonomyModelCatRespVO respVO : importExcelList) {
            try {
                TaxonomyModelCatDO TaxonomyModelCatDO = BeanUtils.toBean(respVO, TaxonomyModelCatDO.class);
                Long TaxonomyModelCatId = respVO.getId();
                if (isUpdateSupport) {
                    if (TaxonomyModelCatId != null) {
                        TaxonomyModelCatDO existingAttModelCat = TaxonomyModelCatMapper.selectById(TaxonomyModelCatId);
                        if (existingAttModelCat != null) {
                            TaxonomyModelCatMapper.updateById(TaxonomyModelCatDO);
                            successNum++;
                            successMessages.add("数据更新成功，ID为 " + TaxonomyModelCatId + " 的逻辑模型类目管理记录。");
                        } else {
                            failureNum++;
                            failureMessages.add("数据更新失败，ID为 " + TaxonomyModelCatId + " 的逻辑模型类目管理记录不存在。");
                        }
                    } else {
                        failureNum++;
                        failureMessages.add("数据更新失败，某条记录的ID不存在。");
                    }
                } else {
                    QueryWrapper<TaxonomyModelCatDO> queryWrapper = new QueryWrapper<>();
                    queryWrapper.eq("id", TaxonomyModelCatId);
                    TaxonomyModelCatDO existingAttModelCat = TaxonomyModelCatMapper.selectOne(queryWrapper);
                    if (existingAttModelCat == null) {
                        TaxonomyModelCatMapper.insert(TaxonomyModelCatDO);
                        successNum++;
                        successMessages.add("数据插入成功，ID为 " + TaxonomyModelCatId + " 的逻辑模型类目管理记录。");
                    } else {
                        failureNum++;
                        failureMessages.add("数据插入失败，ID为 " + TaxonomyModelCatId + " 的逻辑模型类目管理记录已存在。");
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
    public String createCode(Long parentId, String parentCode) {
        String categoryCode = null;
        /*
         * 分成三种情况
         * 1.数据库无数据 调用YouBianCodeUtil.getNextYouBianCode(null);
         * 2.添加子节点，无兄弟元素 YouBianCodeUtil.getSubYouBianCode(parentCode,null);
         * 3.添加子节点有兄弟元素 YouBianCodeUtil.getNextYouBianCode(lastCode);
         * */
        //找同类 确定上一个最大的code值
        LambdaQueryWrapper<TaxonomyModelCatDO> query = new LambdaQueryWrapper<TaxonomyModelCatDO>()
                .eq(TaxonomyModelCatDO::getParentId, parentId)
                .likeRight(StringUtils.isNotBlank(parentCode), TaxonomyModelCatDO::getCode, parentCode)
                .isNotNull(TaxonomyModelCatDO::getCode)
                .orderByDesc(TaxonomyModelCatDO::getCode);
        List<TaxonomyModelCatDO> list = baseMapper.selectList(query);
        if (list == null || list.size() == 0) {
            if (parentId == 0) {
                //情况1
                categoryCode = YouBianCodeUtil.getNextYouBianCode(null);
            } else {
                //情况2
                TaxonomyModelCatDO parent = baseMapper.selectById(parentId);
                categoryCode = YouBianCodeUtil.getSubYouBianCode(parent.getCode(), null);
            }
        } else {
            //情况3
            categoryCode = YouBianCodeUtil.getNextYouBianCode(list.get(0).getCode());
        }
        return categoryCode;
    }
}
