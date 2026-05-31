

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
import com.datamaster.module.taxonomy.api.cat.dto.TaxonomyDataDevCatReqDTO;
import com.datamaster.module.taxonomy.api.cat.dto.TaxonomyDataDevCatRespDTO;
import com.datamaster.module.taxonomy.api.service.cat.ITaxonomyDataDevCatApiService;
import com.datamaster.module.taxonomy.controller.admin.cat.vo.TaxonomyDataDevCatPageReqVO;
import com.datamaster.module.taxonomy.controller.admin.cat.vo.TaxonomyDataDevCatRespVO;
import com.datamaster.module.taxonomy.controller.admin.cat.vo.TaxonomyDataDevCatSaveReqVO;
import com.datamaster.module.taxonomy.dal.dataobject.cat.TaxonomyDataDevCatDO;
import com.datamaster.module.taxonomy.dal.dataobject.cat.TaxonomyTaskCatDO;
import com.datamaster.module.taxonomy.dal.mapper.cat.TaxonomyDataDevCatMapper;
import com.datamaster.module.taxonomy.service.cat.ITaxonomyDataDevCatService;
import com.datamaster.mybatis.core.query.LambdaQueryWrapperX;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 数据开发类目管理Service业务层处理
 *
 * @author DATAMASTER
 * @date 2025-03-11
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class TaxonomyDataDevCatServiceImpl extends ServiceImpl<TaxonomyDataDevCatMapper, TaxonomyDataDevCatDO> implements ITaxonomyDataDevCatService, ITaxonomyDataDevCatApiService {
    @Resource
    private TaxonomyDataDevCatMapper TaxonomyDataDevCatMapper;

    @Override
    public PageResult<TaxonomyDataDevCatDO> getAttDataDevCatPage(TaxonomyDataDevCatPageReqVO pageReqVO) {
        return TaxonomyDataDevCatMapper.selectPage(pageReqVO);
    }

    @Override
    public Long createAttDataDevCat(TaxonomyDataDevCatSaveReqVO createReqVO) {
        TaxonomyDataDevCatDO dictType = BeanUtils.toBean(createReqVO, TaxonomyDataDevCatDO.class);
        dictType.setCode(createCode(createReqVO.getParentId(), null));
        TaxonomyDataDevCatMapper.insert(dictType);
        return dictType.getId();
    }

    @Override
    public int updateAttDataDevCat(TaxonomyDataDevCatSaveReqVO updateReqVO) {
        // 相关校验

        // 更新数据开发类目管理
        TaxonomyDataDevCatDO updateObj = BeanUtils.toBean(updateReqVO, TaxonomyDataDevCatDO.class);
        return TaxonomyDataDevCatMapper.updateById(updateObj);
    }

    @Override
    public List<TaxonomyDataDevCatRespDTO> getAttDataDevCatApiList(TaxonomyDataDevCatReqDTO reqVO) {
        MPJLambdaWrapper<TaxonomyDataDevCatDO> wrapper = new MPJLambdaWrapper<>();
        wrapper.selectAll(TaxonomyDataDevCatDO.class)
                .like(StringUtils.isNotBlank(reqVO.getName()), TaxonomyDataDevCatDO::getName, reqVO.getName());
        List<TaxonomyDataDevCatDO> TaxonomyTaskCatDOS = TaxonomyDataDevCatMapper.selectList(wrapper);
        return BeanUtils.toBean(TaxonomyTaskCatDOS, TaxonomyDataDevCatRespDTO.class);
    }

    @Override
    public int removeAttDataDevCat(Collection<Long> idList) {
        // 批量删除数据开发类目管理
        return TaxonomyDataDevCatMapper.deleteBatchIds(idList);
    }

    @Override
    public TaxonomyDataDevCatDO getAttDataDevCatById(Long id) {
        return TaxonomyDataDevCatMapper.selectById(id);
    }

    @Override
    public List<TaxonomyDataDevCatDO> getAttDataDevCatList() {
        return TaxonomyDataDevCatMapper.selectList();
    }

    @Override
    public List<TaxonomyDataDevCatDO> getAttDataDevCatList(TaxonomyDataDevCatPageReqVO reqVO) {
        LambdaQueryWrapperX<TaxonomyDataDevCatDO> queryWrapperX = new LambdaQueryWrapperX<>();
        queryWrapperX.likeIfPresent(TaxonomyDataDevCatDO::getName, reqVO.getName())
                .likeRightIfPresent(TaxonomyDataDevCatDO::getCode, reqVO.getCode())
                .eqIfPresent(TaxonomyDataDevCatDO::getProjectId,reqVO.getProjectId())
                .eqIfPresent(TaxonomyDataDevCatDO::getProjectCode,reqVO.getProjectCode())
                .eq(reqVO.getValidFlag() != null, TaxonomyDataDevCatDO::getValidFlag, Boolean.TRUE.equals(reqVO.getValidFlag()) ? "1" : "0")
                .orderByAsc(TaxonomyDataDevCatDO::getSortOrder);
        return TaxonomyDataDevCatMapper.selectList(queryWrapperX);
    }

    @Override
    public Map<Long, TaxonomyDataDevCatDO> getAttDataDevCatMap() {
        List<TaxonomyDataDevCatDO> TaxonomyDataDevCatList = TaxonomyDataDevCatMapper.selectList();
        return TaxonomyDataDevCatList.stream()
                .collect(Collectors.toMap(
                        TaxonomyDataDevCatDO::getId,
                        TaxonomyDataDevCatDO -> TaxonomyDataDevCatDO,
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
        LambdaQueryWrapper<TaxonomyDataDevCatDO> query = new LambdaQueryWrapper<TaxonomyDataDevCatDO>()
                .eq(TaxonomyDataDevCatDO::getParentId, parentId)
                .likeRight(StringUtils.isNotBlank(parentCode), TaxonomyDataDevCatDO::getCode, parentCode)
                .isNotNull(TaxonomyDataDevCatDO::getCode)
                .orderByDesc(TaxonomyDataDevCatDO::getCode);
        List<TaxonomyDataDevCatDO> list = baseMapper.selectList(query);
        if (list == null || list.size() == 0) {
            if (parentId == 0) {
                //情况1
                categoryCode = YouBianCodeUtil.getNextYouBianCode(null);
            } else {
                //情况2
                TaxonomyDataDevCatDO parent = baseMapper.selectById(parentId);
                categoryCode = YouBianCodeUtil.getSubYouBianCode(parent.getCode(), null);
            }
        } else {
            //情况3
            categoryCode = YouBianCodeUtil.getNextYouBianCode(list.get(0).getCode());
        }
        return categoryCode;
    }


    /**
     * 导入数据开发类目管理数据
     *
     * @param importExcelList 数据开发类目管理数据列表
     * @param isUpdateSupport 是否更新支持，如果已存在，则进行更新数据
     * @param operName        操作用户
     * @return 结果
     */
    @Override
    public String importAttDataDevCat(List<TaxonomyDataDevCatRespVO> importExcelList, boolean isUpdateSupport, String operName) {
        if (StringUtils.isNull(importExcelList) || importExcelList.size() == 0) {
            throw new ServiceException("导入数据不能为空！");
        }

        int successNum = 0;
        int failureNum = 0;
        List<String> successMessages = new ArrayList<>();
        List<String> failureMessages = new ArrayList<>();

        for (TaxonomyDataDevCatRespVO respVO : importExcelList) {
            try {
                TaxonomyDataDevCatDO TaxonomyDataDevCatDO = BeanUtils.toBean(respVO, TaxonomyDataDevCatDO.class);
                Long TaxonomyDataDevCatId = respVO.getId();
                if (isUpdateSupport) {
                    if (TaxonomyDataDevCatId != null) {
                        TaxonomyDataDevCatDO existingAttDataDevCat = TaxonomyDataDevCatMapper.selectById(TaxonomyDataDevCatId);
                        if (existingAttDataDevCat != null) {
                            TaxonomyDataDevCatMapper.updateById(TaxonomyDataDevCatDO);
                            successNum++;
                            successMessages.add("数据更新成功，ID为 " + TaxonomyDataDevCatId + " 的数据开发类目管理记录。");
                        } else {
                            failureNum++;
                            failureMessages.add("数据更新失败，ID为 " + TaxonomyDataDevCatId + " 的数据开发类目管理记录不存在。");
                        }
                    } else {
                        failureNum++;
                        failureMessages.add("数据更新失败，某条记录的ID不存在。");
                    }
                } else {
                    QueryWrapper<TaxonomyDataDevCatDO> queryWrapper = new QueryWrapper<>();
                    queryWrapper.eq("id", TaxonomyDataDevCatId);
                    TaxonomyDataDevCatDO existingAttDataDevCat = TaxonomyDataDevCatMapper.selectOne(queryWrapper);
                    if (existingAttDataDevCat == null) {
                        TaxonomyDataDevCatMapper.insert(TaxonomyDataDevCatDO);
                        successNum++;
                        successMessages.add("数据插入成功，ID为 " + TaxonomyDataDevCatId + " 的数据开发类目管理记录。");
                    } else {
                        failureNum++;
                        failureMessages.add("数据插入失败，ID为 " + TaxonomyDataDevCatId + " 的数据开发类目管理记录已存在。");
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
