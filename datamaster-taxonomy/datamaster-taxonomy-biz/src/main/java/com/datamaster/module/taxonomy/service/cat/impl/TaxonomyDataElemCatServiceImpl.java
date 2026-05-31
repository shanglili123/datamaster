

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
import com.datamaster.module.taxonomy.controller.admin.cat.vo.TaxonomyDataElemCatPageReqVO;
import com.datamaster.module.taxonomy.controller.admin.cat.vo.TaxonomyDataElemCatRespVO;
import com.datamaster.module.taxonomy.controller.admin.cat.vo.TaxonomyDataElemCatSaveReqVO;
import com.datamaster.module.taxonomy.dal.dataobject.cat.TaxonomyDataElemCatDO;
import com.datamaster.module.taxonomy.dal.dataobject.cat.TaxonomyTaskCatDO;
import com.datamaster.module.taxonomy.dal.mapper.cat.TaxonomyDataElemCatMapper;
import com.datamaster.module.taxonomy.service.cat.ITaxonomyDataElemCatService;
import com.datamaster.module.standards.api.service.dataElem.IDataElemApiService;
import com.datamaster.mybatis.core.query.LambdaQueryWrapperX;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 数据元类目管理Service业务层处理
 *
 * @author DATAMASTER
 * @date 2025-01-20
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class TaxonomyDataElemCatServiceImpl extends ServiceImpl<TaxonomyDataElemCatMapper, TaxonomyDataElemCatDO> implements ITaxonomyDataElemCatService {
    @Resource
    private TaxonomyDataElemCatMapper TaxonomyDataElemCatMapper;
    @Resource
    private IDataElemApiService dataElemApiService;

    @Override
    public PageResult<TaxonomyDataElemCatDO> getAttDataElemCatPage(TaxonomyDataElemCatPageReqVO pageReqVO) {
        return TaxonomyDataElemCatMapper.selectPage(pageReqVO);
    }

    @Override
    public Long createAttDataElemCat(TaxonomyDataElemCatSaveReqVO createReqVO) {
        TaxonomyDataElemCatDO dictType = BeanUtils.toBean(createReqVO, TaxonomyDataElemCatDO.class);
        dictType.setCode(createCode(createReqVO.getParentId(), null));
        TaxonomyDataElemCatMapper.insert(dictType);
        return dictType.getId();
    }

    @Override
    public int updateAttDataElemCat(TaxonomyDataElemCatSaveReqVO updateReqVO) {
        TaxonomyDataElemCatDO TaxonomyDataElemCatDO = TaxonomyDataElemCatMapper.selectById(updateReqVO.getId());
        if (TaxonomyDataElemCatDO == null) {
            return 0;
        }
        // 更新数据元类目管理
        TaxonomyDataElemCatDO updateObj = BeanUtils.toBean(updateReqVO, TaxonomyDataElemCatDO.class);
        if (Boolean.FALSE.equals(updateReqVO.getValidFlag())) {
            Long countData = dataElemApiService.getCountByCatCode(TaxonomyDataElemCatDO.getCode());
            if (countData > 0) {
                throw new ServiceException("存在数据元，不允许禁用");
            }
            TaxonomyDataElemCatMapper.updateValidFlag(TaxonomyDataElemCatDO.getCode(), updateReqVO.getValidFlag());
        } else if (Boolean.TRUE.equals(updateReqVO.getValidFlag())) {
            TaxonomyDataElemCatDO parent = TaxonomyDataElemCatMapper.selectById(TaxonomyDataElemCatDO.getParentId());
            if (parent != null && Boolean.FALSE.equals(parent.getValidFlag())) {
                throw new ServiceException("须先启用父级");
            }
        }
        return TaxonomyDataElemCatMapper.updateById(updateObj);
    }

    @Override
    public int removeAttDataElemCat(Collection<Long> idList) {
        int count = 0;
        for (Long id : idList) {
            TaxonomyDataElemCatDO cat = baseMapper.selectById(id);
            //判断是否存在数据资产
            if (dataElemApiService.getCountByCatCode(cat.getCode()) > 0) {
                throw new ServiceException("存在数据元，不允许删除");
            }
            if (cat != null) {
                count += baseMapper.delete(Wrappers.lambdaQuery(TaxonomyDataElemCatDO.class)
                        .likeRight(TaxonomyDataElemCatDO::getCode, cat.getCode()));
            }
        }
        return count;
    }

    @Override
    public TaxonomyDataElemCatDO getAttDataElemCatById(Long id) {
        return TaxonomyDataElemCatMapper.selectById(id);
    }

    @Override
    public List<TaxonomyDataElemCatDO> getAttDataElemCatList() {
        return TaxonomyDataElemCatMapper.selectList();
    }

    @Override
    public List<TaxonomyDataElemCatDO> getAttDataElemCatList(TaxonomyDataElemCatPageReqVO reqVO) {
        LambdaQueryWrapperX<TaxonomyDataElemCatDO> queryWrapperX = new LambdaQueryWrapperX<>();
        queryWrapperX.likeIfPresent(TaxonomyDataElemCatDO::getName, reqVO.getName())
                .eqIfPresent(TaxonomyDataElemCatDO::getParentId, reqVO.getParentId())
                .eqIfPresent(TaxonomyDataElemCatDO::getSortOrder, reqVO.getSortOrder())
                .eqIfPresent(TaxonomyDataElemCatDO::getDescription, reqVO.getDescription())
                .likeRightIfPresent(TaxonomyDataElemCatDO::getCode, reqVO.getCode())
                .eqIfPresent(TaxonomyDataElemCatDO::getCreateTime, reqVO.getCreateTime())
                .eq(reqVO.getValidFlag() != null, TaxonomyDataElemCatDO::getValidFlag, Boolean.TRUE.equals(reqVO.getValidFlag()) ? "1" : "0")
                .orderByAsc(TaxonomyDataElemCatDO::getSortOrder);
        return TaxonomyDataElemCatMapper.selectList(queryWrapperX);
    }

    @Override
    public Map<Long, TaxonomyDataElemCatDO> getAttDataElemCatMap() {
        List<TaxonomyDataElemCatDO> TaxonomyDataElemCatList = TaxonomyDataElemCatMapper.selectList();
        return TaxonomyDataElemCatList.stream()
                .collect(Collectors.toMap(
                        TaxonomyDataElemCatDO::getId,
                        TaxonomyDataElemCatDO -> TaxonomyDataElemCatDO,
                        // 保留已存在的值
                        (existing, replacement) -> existing
                ));
    }


    /**
     * 导入数据元类目管理数据
     *
     * @param importExcelList 数据元类目管理数据列表
     * @param isUpdateSupport 是否更新支持，如果已存在，则进行更新数据
     * @param operName        操作用户
     * @return 结果
     */
    @Override
    public String importAttDataElemCat(List<TaxonomyDataElemCatRespVO> importExcelList, boolean isUpdateSupport, String operName) {
        if (StringUtils.isNull(importExcelList) || importExcelList.size() == 0) {
            throw new ServiceException("导入数据不能为空！");
        }

        int successNum = 0;
        int failureNum = 0;
        List<String> successMessages = new ArrayList<>();
        List<String> failureMessages = new ArrayList<>();

        for (TaxonomyDataElemCatRespVO respVO : importExcelList) {
            try {
                TaxonomyDataElemCatDO TaxonomyDataElemCatDO = BeanUtils.toBean(respVO, TaxonomyDataElemCatDO.class);
                Long TaxonomyDataElemCatId = respVO.getId();
                if (isUpdateSupport) {
                    if (TaxonomyDataElemCatId != null) {
                        TaxonomyDataElemCatDO existingAttDataElemCat = TaxonomyDataElemCatMapper.selectById(TaxonomyDataElemCatId);
                        if (existingAttDataElemCat != null) {
                            TaxonomyDataElemCatMapper.updateById(TaxonomyDataElemCatDO);
                            successNum++;
                            successMessages.add("数据更新成功，ID为 " + TaxonomyDataElemCatId + " 的数据元类目管理记录。");
                        } else {
                            failureNum++;
                            failureMessages.add("数据更新失败，ID为 " + TaxonomyDataElemCatId + " 的数据元类目管理记录不存在。");
                        }
                    } else {
                        failureNum++;
                        failureMessages.add("数据更新失败，某条记录的ID不存在。");
                    }
                } else {
                    QueryWrapper<TaxonomyDataElemCatDO> queryWrapper = new QueryWrapper<>();
                    queryWrapper.eq("id", TaxonomyDataElemCatId);
                    TaxonomyDataElemCatDO existingAttDataElemCat = TaxonomyDataElemCatMapper.selectOne(queryWrapper);
                    if (existingAttDataElemCat == null) {
                        TaxonomyDataElemCatMapper.insert(TaxonomyDataElemCatDO);
                        successNum++;
                        successMessages.add("数据插入成功，ID为 " + TaxonomyDataElemCatId + " 的数据元类目管理记录。");
                    } else {
                        failureNum++;
                        failureMessages.add("数据插入失败，ID为 " + TaxonomyDataElemCatId + " 的数据元类目管理记录已存在。");
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
        LambdaQueryWrapper<TaxonomyDataElemCatDO> query = new LambdaQueryWrapper<TaxonomyDataElemCatDO>()
                .eq(TaxonomyDataElemCatDO::getParentId, parentId)
                .likeRight(StringUtils.isNotBlank(parentCode), TaxonomyDataElemCatDO::getCode, parentCode)
                .isNotNull(TaxonomyDataElemCatDO::getCode)
                .orderByDesc(TaxonomyDataElemCatDO::getCode);
        List<TaxonomyDataElemCatDO> list = baseMapper.selectList(query);
        if (list == null || list.size() == 0) {
            if (parentId == 0) {
                //情况1
                categoryCode = YouBianCodeUtil.getNextYouBianCode(null);
            } else {
                //情况2
                TaxonomyDataElemCatDO parent = baseMapper.selectById(parentId);
                categoryCode = YouBianCodeUtil.getSubYouBianCode(parent.getCode(), null);
            }
        } else {
            //情况3
            categoryCode = YouBianCodeUtil.getNextYouBianCode(list.get(0).getCode());
        }
        return categoryCode;
    }
}
