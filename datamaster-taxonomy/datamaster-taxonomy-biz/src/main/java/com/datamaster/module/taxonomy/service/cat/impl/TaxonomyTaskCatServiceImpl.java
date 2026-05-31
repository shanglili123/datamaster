

package com.datamaster.module.taxonomy.service.cat.impl;

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
import com.datamaster.common.utils.YouBianCodeUtil;
import com.datamaster.common.utils.object.BeanUtils;
import com.datamaster.module.taxonomy.api.cat.dto.TaxonomyTaskCatReqDTO;
import com.datamaster.module.taxonomy.api.cat.dto.TaxonomyTaskCatRespDTO;
import com.datamaster.module.taxonomy.api.service.cat.ITaxonomyTaskCatApiService;
import com.datamaster.module.taxonomy.controller.admin.cat.vo.TaxonomyTaskCatPageReqVO;
import com.datamaster.module.taxonomy.controller.admin.cat.vo.TaxonomyTaskCatRespVO;
import com.datamaster.module.taxonomy.controller.admin.cat.vo.TaxonomyTaskCatSaveReqVO;
import com.datamaster.module.taxonomy.dal.dataobject.cat.TaxonomyModelCatDO;
import com.datamaster.module.taxonomy.dal.dataobject.cat.TaxonomyTaskCatDO;
import com.datamaster.module.taxonomy.dal.mapper.cat.TaxonomyTaskCatMapper;
import com.datamaster.module.taxonomy.service.cat.ITaxonomyTaskCatService;
import com.datamaster.mybatis.core.query.LambdaQueryWrapperX;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 数据集成任务类目管理Service业务层处理
 *
 * @author DATAMASTER
 * @date 2025-03-11
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class TaxonomyTaskCatServiceImpl extends ServiceImpl<TaxonomyTaskCatMapper, TaxonomyTaskCatDO> implements ITaxonomyTaskCatService, ITaxonomyTaskCatApiService {
    @Resource
    private TaxonomyTaskCatMapper TaxonomyTaskCatMapper;

    @Override
    public PageResult<TaxonomyTaskCatDO> getAttTaskCatPage(TaxonomyTaskCatPageReqVO pageReqVO) {
        return TaxonomyTaskCatMapper.selectPage(pageReqVO);
    }

    @Override
    public Long createAttTaskCat(TaxonomyTaskCatSaveReqVO createReqVO) {
        TaxonomyTaskCatDO dictType = BeanUtils.toBean(createReqVO, TaxonomyTaskCatDO.class);
        dictType.setCode(createCode(createReqVO.getParentId(), null));
        TaxonomyTaskCatMapper.insert(dictType);
        return dictType.getId();
    }

    @Override
    public List<TaxonomyTaskCatRespDTO> getAttTaskCatApiList(TaxonomyTaskCatReqDTO reqVO) {
        MPJLambdaWrapper<TaxonomyTaskCatDO> wrapper = new MPJLambdaWrapper<>();
        wrapper.selectAll(TaxonomyTaskCatDO.class)
                .like(StringUtils.isNotBlank(reqVO.getName()), TaxonomyTaskCatDO::getName, reqVO.getName());
        List<TaxonomyTaskCatDO> TaxonomyTaskCatDOS = TaxonomyTaskCatMapper.selectList(wrapper);
        return BeanUtils.toBean(TaxonomyTaskCatDOS, TaxonomyTaskCatRespDTO.class);
    }

    @Override
    public int updateAttTaskCat(TaxonomyTaskCatSaveReqVO updateReqVO) {
        // 相关校验

        // 更新数据集成任务类目管理
        TaxonomyTaskCatDO updateObj = BeanUtils.toBean(updateReqVO, TaxonomyTaskCatDO.class);
        return TaxonomyTaskCatMapper.updateById(updateObj);
    }

    @Override
    public int removeAttTaskCat(Collection<Long> idList) {
        // 批量删除数据集成任务类目管理
        return TaxonomyTaskCatMapper.deleteBatchIds(idList);
    }

    @Override
    public TaxonomyTaskCatDO getAttTaskCatById(Long id) {
        return TaxonomyTaskCatMapper.selectById(id);
    }

    @Override
    public List<TaxonomyTaskCatDO> getAttTaskCatList() {
        return TaxonomyTaskCatMapper.selectList();
    }

    @Override
    public List<TaxonomyTaskCatDO> getAttTaskCatList(TaxonomyTaskCatPageReqVO reqVO) {
        LambdaQueryWrapperX<TaxonomyTaskCatDO> queryWrapperX = new LambdaQueryWrapperX<>();
        queryWrapperX.likeIfPresent(TaxonomyTaskCatDO::getName, reqVO.getName())
                .eqIfPresent(TaxonomyTaskCatDO::getParentId, reqVO.getParentId())
                .eqIfPresent(TaxonomyTaskCatDO::getSortOrder, reqVO.getSortOrder())
                .eqIfPresent(TaxonomyTaskCatDO::getDescription, reqVO.getDescription())
                .eqIfPresent(TaxonomyTaskCatDO::getCode, reqVO.getCode())
                .eqIfPresent(TaxonomyTaskCatDO::getCreateTime, reqVO.getCreateTime())
                .eqIfPresent(TaxonomyTaskCatDO::getProjectId,reqVO.getProjectId())
                .eqIfPresent(TaxonomyTaskCatDO::getProjectCode,reqVO.getProjectCode())
                .eq(reqVO.getValidFlag() != null, TaxonomyTaskCatDO::getValidFlag, Boolean.TRUE.equals(reqVO.getValidFlag()) ? "1" : "0")
                .orderByAsc(TaxonomyTaskCatDO::getSortOrder);
        return TaxonomyTaskCatMapper.selectList(queryWrapperX);
    }

    @Override
    public Map<Long, TaxonomyTaskCatDO> getAttTaskCatMap() {
        List<TaxonomyTaskCatDO> TaxonomyTaskCatList = TaxonomyTaskCatMapper.selectList();
        return TaxonomyTaskCatList.stream()
                .collect(Collectors.toMap(
                        TaxonomyTaskCatDO::getId,
                        TaxonomyTaskCatDO -> TaxonomyTaskCatDO,
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
        LambdaQueryWrapper<TaxonomyTaskCatDO> query = new LambdaQueryWrapper<TaxonomyTaskCatDO>()
                .eq(TaxonomyTaskCatDO::getParentId, parentId)
                .likeRight(StringUtils.isNotBlank(parentCode), TaxonomyTaskCatDO::getCode, parentCode)
                .isNotNull(TaxonomyTaskCatDO::getCode)
                .orderByDesc(TaxonomyTaskCatDO::getCode);
        List<TaxonomyTaskCatDO> list = baseMapper.selectList(query);
        if (list == null || list.size() == 0) {
            if (parentId == 0) {
                //情况1
                categoryCode = YouBianCodeUtil.getNextYouBianCode(null);
            } else {
                //情况2
                TaxonomyTaskCatDO parent = baseMapper.selectById(parentId);
                categoryCode = YouBianCodeUtil.getSubYouBianCode(parent.getCode(), null);
            }
        } else {
            //情况3
            categoryCode = YouBianCodeUtil.getNextYouBianCode(list.get(0).getCode());
        }
        return categoryCode;
    }

    /**
     * 导入数据集成任务类目管理数据
     *
     * @param importExcelList 数据集成任务类目管理数据列表
     * @param isUpdateSupport 是否更新支持，如果已存在，则进行更新数据
     * @param operName        操作用户
     * @return 结果
     */
    @Override
    public String importAttTaskCat(List<TaxonomyTaskCatRespVO> importExcelList, boolean isUpdateSupport, String operName) {
        if (StringUtils.isNull(importExcelList) || importExcelList.size() == 0) {
            throw new ServiceException("导入数据不能为空！");
        }

        int successNum = 0;
        int failureNum = 0;
        List<String> successMessages = new ArrayList<>();
        List<String> failureMessages = new ArrayList<>();

        for (TaxonomyTaskCatRespVO respVO : importExcelList) {
            try {
                TaxonomyTaskCatDO TaxonomyTaskCatDO = BeanUtils.toBean(respVO, TaxonomyTaskCatDO.class);
                Long TaxonomyTaskCatId = respVO.getId();
                if (isUpdateSupport) {
                    if (TaxonomyTaskCatId != null) {
                        TaxonomyTaskCatDO existingAttTaskCat = TaxonomyTaskCatMapper.selectById(TaxonomyTaskCatId);
                        if (existingAttTaskCat != null) {
                            TaxonomyTaskCatMapper.updateById(TaxonomyTaskCatDO);
                            successNum++;
                            successMessages.add("数据更新成功，ID为 " + TaxonomyTaskCatId + " 的数据集成任务类目管理记录。");
                        } else {
                            failureNum++;
                            failureMessages.add("数据更新失败，ID为 " + TaxonomyTaskCatId + " 的数据集成任务类目管理记录不存在。");
                        }
                    } else {
                        failureNum++;
                        failureMessages.add("数据更新失败，某条记录的ID不存在。");
                    }
                } else {
                    QueryWrapper<TaxonomyTaskCatDO> queryWrapper = new QueryWrapper<>();
                    queryWrapper.eq("id", TaxonomyTaskCatId);
                    TaxonomyTaskCatDO existingAttTaskCat = TaxonomyTaskCatMapper.selectOne(queryWrapper);
                    if (existingAttTaskCat == null) {
                        TaxonomyTaskCatMapper.insert(TaxonomyTaskCatDO);
                        successNum++;
                        successMessages.add("数据插入成功，ID为 " + TaxonomyTaskCatId + " 的数据集成任务类目管理记录。");
                    } else {
                        failureNum++;
                        failureMessages.add("数据插入失败，ID为 " + TaxonomyTaskCatId + " 的数据集成任务类目管理记录已存在。");
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
}
