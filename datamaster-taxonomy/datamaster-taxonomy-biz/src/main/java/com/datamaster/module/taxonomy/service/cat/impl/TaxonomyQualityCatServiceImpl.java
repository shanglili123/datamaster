

package com.datamaster.module.taxonomy.service.cat.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import javax.annotation.Resource;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.common.exception.ServiceException;
import com.datamaster.common.utils.StringUtils;
import com.datamaster.common.utils.YouBianCodeUtil;
import com.datamaster.common.utils.object.BeanUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.datamaster.module.taxonomy.controller.admin.cat.vo.TaxonomyQualityCatPageReqVO;
import com.datamaster.module.taxonomy.controller.admin.cat.vo.TaxonomyQualityCatRespVO;
import com.datamaster.module.taxonomy.controller.admin.cat.vo.TaxonomyQualityCatSaveReqVO;
import com.datamaster.module.taxonomy.dal.dataobject.cat.TaxonomyQualityCatDO;
import com.datamaster.module.taxonomy.dal.mapper.cat.TaxonomyQualityCatMapper;
import com.datamaster.module.taxonomy.service.cat.ITaxonomyQualityCatService;
import com.datamaster.module.collector.api.service.qa.CollectorQualityTaskApiService;
import com.datamaster.mybatis.core.query.LambdaQueryWrapperX;

/**
 * 数据质量类目Service业务层处理
 *
 * @author DATAMASTER
 * @date 2025-07-19
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class TaxonomyQualityCatServiceImpl  extends ServiceImpl<TaxonomyQualityCatMapper,TaxonomyQualityCatDO> implements ITaxonomyQualityCatService {
    @Resource
    private TaxonomyQualityCatMapper TaxonomyQualityCatMapper;
    @Resource
    private CollectorQualityTaskApiService taskApiService;

    @Override
    public PageResult<TaxonomyQualityCatDO> getAttQualityCatPage(TaxonomyQualityCatPageReqVO pageReqVO) {
        return TaxonomyQualityCatMapper.selectPage(pageReqVO);
    }

    @Override
    public Long createAttQualityCat(TaxonomyQualityCatSaveReqVO createReqVO) {
        TaxonomyQualityCatDO dictType = BeanUtils.toBean(createReqVO, TaxonomyQualityCatDO.class);
        dictType.setCode(createCode(createReqVO.getParentId(), null));
        TaxonomyQualityCatMapper.insert(dictType);
        return dictType.getId();
    }

    @Override
    public int updateAttQualityCat(TaxonomyQualityCatSaveReqVO updateReqVO) {
        TaxonomyQualityCatDO catDO = TaxonomyQualityCatMapper.selectById(updateReqVO.getId());
        if (catDO == null) {
            return 0;
        }
        if (Boolean.FALSE.equals(updateReqVO.getValidFlag())) {
            Long countData = taskApiService.getCountByCatCode(catDO.getCode());
            if (countData > 0) {
                throw new ServiceException("存在数据质量任务，不允许禁用");
            }
            TaxonomyQualityCatMapper.updateValidFlag(catDO.getCode(), updateReqVO.getValidFlag());
        } else if (Boolean.TRUE.equals(updateReqVO.getValidFlag())) {
            TaxonomyQualityCatDO parent = TaxonomyQualityCatMapper.selectById(catDO.getParentId());
            if (parent != null && Boolean.FALSE.equals(parent.getValidFlag())) {
                throw new ServiceException("须先启用父级");
            }
        }
        // 更新数据质量类目
        TaxonomyQualityCatDO updateObj = BeanUtils.toBean(updateReqVO, TaxonomyQualityCatDO.class);
        return TaxonomyQualityCatMapper.updateById(updateObj);
    }
    @Override
    public int removeAttQualityCat(Collection<Long> idList) {
        //判断是否存在数据
        List<TaxonomyQualityCatDO> TaxonomyQualityCatDOS = TaxonomyQualityCatMapper.selectBatchIds(idList);
        for (TaxonomyQualityCatDO cat : TaxonomyQualityCatDOS) {
            if (taskApiService.getCountByCatCode(cat.getCode()) > 0) {
                throw new ServiceException("存在数据质量任务，不允许删除");
            }
        }
        // 批量删除数据质量类目
        return TaxonomyQualityCatMapper.deleteBatchIds(idList);
    }

    @Override
    public TaxonomyQualityCatDO getAttQualityCatById(Long id) {
        return TaxonomyQualityCatMapper.selectById(id);
    }

    @Override
    public List<TaxonomyQualityCatDO> getAttQualityCatList(TaxonomyQualityCatPageReqVO reqVO) {
        LambdaQueryWrapperX<TaxonomyQualityCatDO> queryWrapperX = new LambdaQueryWrapperX<>();
        queryWrapperX.likeIfPresent(TaxonomyQualityCatDO::getName, reqVO.getName())
                .likeRightIfPresent(TaxonomyQualityCatDO::getCode, reqVO.getCode())
                .eqIfPresent(TaxonomyQualityCatDO::getProjectId, reqVO.getProjectId())
                .eq(reqVO.getValidFlag() != null, TaxonomyQualityCatDO::getValidFlag, Boolean.TRUE.equals(reqVO.getValidFlag()) ? "1" : "0")
                .orderByAsc(TaxonomyQualityCatDO::getSortOrder);
        return TaxonomyQualityCatMapper.selectList(queryWrapperX);
    }

    @Override
    public Map<Long, TaxonomyQualityCatDO> getAttQualityCatMap() {
        List<TaxonomyQualityCatDO> TaxonomyQualityCatList = TaxonomyQualityCatMapper.selectList();
        return TaxonomyQualityCatList.stream()
                .collect(Collectors.toMap(
                        TaxonomyQualityCatDO::getId,
                        TaxonomyQualityCatDO -> TaxonomyQualityCatDO,
                        // 保留已存在的值
                        (existing, replacement) -> existing
                ));
    }


    /**
     * 导入数据质量类目数据
     *
     * @param importExcelList 数据质量类目数据列表
     * @param isUpdateSupport 是否更新支持，如果已存在，则进行更新数据
     * @param operName 操作用户
     * @return 结果
     */
    @Override
    public String importAttQualityCat(List<TaxonomyQualityCatRespVO> importExcelList, boolean isUpdateSupport, String operName) {
        if (StringUtils.isNull(importExcelList) || importExcelList.size() == 0) {
            throw new ServiceException("导入数据不能为空！");
        }

        int successNum = 0;
        int failureNum = 0;
        List<String> successMessages = new ArrayList<>();
        List<String> failureMessages = new ArrayList<>();

        for (TaxonomyQualityCatRespVO respVO : importExcelList) {
            try {
                TaxonomyQualityCatDO TaxonomyQualityCatDO = BeanUtils.toBean(respVO, TaxonomyQualityCatDO.class);
                Long TaxonomyQualityCatId = respVO.getId();
                if (isUpdateSupport) {
                    if (TaxonomyQualityCatId != null) {
                        TaxonomyQualityCatDO existingAttQualityCat = TaxonomyQualityCatMapper.selectById(TaxonomyQualityCatId);
                        if (existingAttQualityCat != null) {
                            TaxonomyQualityCatMapper.updateById(TaxonomyQualityCatDO);
                            successNum++;
                            successMessages.add("数据更新成功，ID为 " + TaxonomyQualityCatId + " 的数据质量类目记录。");
                        } else {
                            failureNum++;
                            failureMessages.add("数据更新失败，ID为 " + TaxonomyQualityCatId + " 的数据质量类目记录不存在。");
                        }
                    } else {
                        failureNum++;
                        failureMessages.add("数据更新失败，某条记录的ID不存在。");
                    }
                } else {
                    QueryWrapper<TaxonomyQualityCatDO> queryWrapper = new QueryWrapper<>();
                    queryWrapper.eq("id", TaxonomyQualityCatId);
                    TaxonomyQualityCatDO existingAttQualityCat = TaxonomyQualityCatMapper.selectOne(queryWrapper);
                    if (existingAttQualityCat == null) {
                        TaxonomyQualityCatMapper.insert(TaxonomyQualityCatDO);
                        successNum++;
                        successMessages.add("数据插入成功，ID为 " + TaxonomyQualityCatId + " 的数据质量类目记录。");
                    } else {
                        failureNum++;
                        failureMessages.add("数据插入失败，ID为 " + TaxonomyQualityCatId + " 的数据质量类目记录已存在。");
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
        LambdaQueryWrapper<TaxonomyQualityCatDO> query = new LambdaQueryWrapper<TaxonomyQualityCatDO>()
                .eq(TaxonomyQualityCatDO::getParentId, parentId)
                .likeRight(StringUtils.isNotBlank(parentCode), TaxonomyQualityCatDO::getCode, parentCode)
                .isNotNull(TaxonomyQualityCatDO::getCode)
                .orderByDesc(TaxonomyQualityCatDO::getCode);
        List<TaxonomyQualityCatDO> list = baseMapper.selectList(query);
        if (list == null || list.size() == 0) {
            if (parentId == 0) {
                //情况1
                categoryCode = YouBianCodeUtil.getNextYouBianCode(null);
            } else {
                //情况2
                TaxonomyQualityCatDO parent = baseMapper.selectById(parentId);
                categoryCode = YouBianCodeUtil.getSubYouBianCode(parent.getCode(), null);
            }
        } else {
            //情况3
            categoryCode = YouBianCodeUtil.getNextYouBianCode(list.get(0).getCode());
        }
        return categoryCode;
    }
}
