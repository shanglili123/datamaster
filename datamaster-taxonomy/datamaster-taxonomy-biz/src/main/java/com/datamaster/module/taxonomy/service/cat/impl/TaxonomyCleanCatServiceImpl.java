

package com.datamaster.module.taxonomy.service.cat.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.extern.slf4j.Slf4j;
import javax.annotation.Resource;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.common.core.text.Convert;
import com.datamaster.common.exception.ServiceException;
import com.datamaster.common.utils.StringUtils;
import com.datamaster.common.utils.YouBianCodeUtil;
import com.datamaster.common.utils.object.BeanUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.datamaster.module.taxonomy.controller.admin.cat.vo.TaxonomyCleanCatPageReqVO;
import com.datamaster.module.taxonomy.controller.admin.cat.vo.TaxonomyCleanCatRespVO;
import com.datamaster.module.taxonomy.controller.admin.cat.vo.TaxonomyCleanCatSaveReqVO;
import com.datamaster.module.taxonomy.dal.dataobject.cat.TaxonomyCleanCatDO;
import com.datamaster.module.taxonomy.dal.mapper.cat.TaxonomyCleanCatMapper;
import com.datamaster.module.taxonomy.service.cat.ITaxonomyCleanCatService;
import com.datamaster.module.taxonomy.service.rule.ITaxonomyCleanRuleService;
import com.datamaster.mybatis.core.query.LambdaQueryWrapperX;

/**
 * 清洗规则类目Service业务层处理
 *
 * @author DATAMASTER
 * @date 2025-08-11
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class TaxonomyCleanCatServiceImpl  extends ServiceImpl<TaxonomyCleanCatMapper,TaxonomyCleanCatDO> implements ITaxonomyCleanCatService {
    @Resource
    private TaxonomyCleanCatMapper TaxonomyCleanCatMapper;
    @Resource
    private ITaxonomyCleanRuleService TaxonomyCleanRuleService;

    @Override
    public PageResult<TaxonomyCleanCatDO> getAttCleanCatPage(TaxonomyCleanCatPageReqVO pageReqVO) {
        return TaxonomyCleanCatMapper.selectPage(pageReqVO);
    }

    @Override
    public Long createAttCleanCat(TaxonomyCleanCatSaveReqVO createReqVO) {
        TaxonomyCleanCatDO dictType = BeanUtils.toBean(createReqVO, TaxonomyCleanCatDO.class);
        dictType.setCode(createCode(createReqVO.getParentId(), null));
        TaxonomyCleanCatMapper.insert(dictType);
        return dictType.getId();
    }

    @Override
    public int updateAttCleanCat(TaxonomyCleanCatSaveReqVO updateReqVO) {
        TaxonomyCleanCatDO catDO = TaxonomyCleanCatMapper.selectById(updateReqVO.getId());
        if (catDO == null) {
            return 0;
        }
        if (Boolean.FALSE.equals(updateReqVO.getValidFlag())) {
            Long countData = TaxonomyCleanRuleService.getCount(catDO.getCode());
            if (countData > 0) {
                throw new ServiceException("存在清洗规则模型，不允许禁用");
            }
            TaxonomyCleanCatMapper.updateValidFlag(catDO.getCode(), updateReqVO.getValidFlag());
        } else if (Boolean.TRUE.equals(updateReqVO.getValidFlag())) {
            TaxonomyCleanCatDO parent = TaxonomyCleanCatMapper.selectById(catDO.getParentId());
            if (parent != null && Boolean.FALSE.equals(parent.getValidFlag())) {
                throw new ServiceException("须先启用父级");
            }
        }        // 更新清洗规则类目
        TaxonomyCleanCatDO updateObj = BeanUtils.toBean(updateReqVO, TaxonomyCleanCatDO.class);
        return TaxonomyCleanCatMapper.updateById(updateObj);
    }
    @Override
    public int removeAttCleanCat(Long idList) {
        // 批量删除清洗规则类目
        int count = 0;
        TaxonomyCleanCatDO cat = baseMapper.selectById(idList);

        //判断是否存在数据
        if (TaxonomyCleanRuleService.getCount(cat.getCode()) > 0) {
            throw new RuntimeException("存在清洗规则模型，不允许删除");
        }

        if (cat != null) {
            count += baseMapper.delete(Wrappers.lambdaQuery(TaxonomyCleanCatDO.class)
                    .likeRight(TaxonomyCleanCatDO::getCode, cat.getCode()));
        }
        return count;
    }

    @Override
    public TaxonomyCleanCatDO getAttCleanCatById(Long id) {
        return TaxonomyCleanCatMapper.selectById(id);
    }

    @Override
    public List<TaxonomyCleanCatDO> getAttCleanCatList(TaxonomyCleanCatPageReqVO TaxonomyCleanCat) {
        LambdaQueryWrapperX<TaxonomyCleanCatDO> queryWrapperX = new LambdaQueryWrapperX<>();
        queryWrapperX.likeIfPresent(TaxonomyCleanCatDO::getName, TaxonomyCleanCat.getName())
                .likeRightIfPresent(TaxonomyCleanCatDO::getCode, TaxonomyCleanCat.getCode())
                .eq(TaxonomyCleanCat.getValidFlag() != null, "valid_flag", Boolean.TRUE.equals(TaxonomyCleanCat.getValidFlag()) ? "1" : "0")
                .orderByAsc(TaxonomyCleanCatDO::getSortOrder);
        return TaxonomyCleanCatMapper.selectList(queryWrapperX);
    }

    @Override
    public List<TaxonomyCleanCatDO> getAttCleanCatList() {
        return TaxonomyCleanCatMapper.selectList();
    }

    @Override
    public Map<Long, TaxonomyCleanCatDO> getAttCleanCatMap() {
        List<TaxonomyCleanCatDO> TaxonomyCleanCatList = TaxonomyCleanCatMapper.selectList();
        return TaxonomyCleanCatList.stream()
                .collect(Collectors.toMap(
                        TaxonomyCleanCatDO::getId,
                        TaxonomyCleanCatDO -> TaxonomyCleanCatDO,
                        // 保留已存在的值
                        (existing, replacement) -> existing
                ));
    }


    /**
     * 导入清洗规则类目数据
     *
     * @param importExcelList 清洗规则类目数据列表
     * @param isUpdateSupport 是否更新支持，如果已存在，则进行更新数据
     * @param operName 操作用户
     * @return 结果
     */
    @Override
    public String importAttCleanCat(List<TaxonomyCleanCatRespVO> importExcelList, boolean isUpdateSupport, String operName) {
        if (StringUtils.isNull(importExcelList) || importExcelList.size() == 0) {
            throw new ServiceException("导入数据不能为空！");
        }

        int successNum = 0;
        int failureNum = 0;
        List<String> successMessages = new ArrayList<>();
        List<String> failureMessages = new ArrayList<>();

        for (TaxonomyCleanCatRespVO respVO : importExcelList) {
            try {
                TaxonomyCleanCatDO TaxonomyCleanCatDO = BeanUtils.toBean(respVO, TaxonomyCleanCatDO.class);
                Long TaxonomyCleanCatId = respVO.getId();
                if (isUpdateSupport) {
                    if (TaxonomyCleanCatId != null) {
                        TaxonomyCleanCatDO existingAttCleanCat = TaxonomyCleanCatMapper.selectById(TaxonomyCleanCatId);
                        if (existingAttCleanCat != null) {
                            TaxonomyCleanCatMapper.updateById(TaxonomyCleanCatDO);
                            successNum++;
                            successMessages.add("数据更新成功，ID为 " + TaxonomyCleanCatId + " 的清洗规则类目记录。");
                        } else {
                            failureNum++;
                            failureMessages.add("数据更新失败，ID为 " + TaxonomyCleanCatId + " 的清洗规则类目记录不存在。");
                        }
                    } else {
                        failureNum++;
                        failureMessages.add("数据更新失败，某条记录的ID不存在。");
                    }
                } else {
                    QueryWrapper<TaxonomyCleanCatDO> queryWrapper = new QueryWrapper<>();
                    queryWrapper.eq("id", TaxonomyCleanCatId);
                    TaxonomyCleanCatDO existingAttCleanCat = TaxonomyCleanCatMapper.selectOne(queryWrapper);
                    if (existingAttCleanCat == null) {
                        TaxonomyCleanCatMapper.insert(TaxonomyCleanCatDO);
                        successNum++;
                        successMessages.add("数据插入成功，ID为 " + TaxonomyCleanCatId + " 的清洗规则类目记录。");
                    } else {
                        failureNum++;
                        failureMessages.add("数据插入失败，ID为 " + TaxonomyCleanCatId + " 的清洗规则类目记录已存在。");
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
        LambdaQueryWrapper<TaxonomyCleanCatDO> query = new LambdaQueryWrapper<TaxonomyCleanCatDO>()
                .eq(TaxonomyCleanCatDO::getParentId, parentId)
                .likeRight(StringUtils.isNotBlank(parentCode), TaxonomyCleanCatDO::getCode, parentCode)
                .isNotNull(TaxonomyCleanCatDO::getCode)
                .orderByDesc(TaxonomyCleanCatDO::getCode);
        List<TaxonomyCleanCatDO> list = baseMapper.selectList(query);
        if (list == null || list.size() == 0) {
            if (parentId == 0) {
                //情况1
                categoryCode = YouBianCodeUtil.getNextYouBianCode(null);
            } else {
                //情况2
                TaxonomyCleanCatDO parent = baseMapper.selectById(parentId);
                categoryCode = YouBianCodeUtil.getSubYouBianCode(parent.getCode(), null);
            }
        } else {
            //情况3
            categoryCode = YouBianCodeUtil.getNextYouBianCode(list.get(0).getCode());
        }
        return categoryCode;
    }
}
