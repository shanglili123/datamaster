

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
import com.datamaster.module.taxonomy.controller.admin.cat.vo.TaxonomyAssetCatPageReqVO;
import com.datamaster.module.taxonomy.controller.admin.cat.vo.TaxonomyAssetCatRespVO;
import com.datamaster.module.taxonomy.controller.admin.cat.vo.TaxonomyAssetCatSaveReqVO;
import com.datamaster.module.taxonomy.dal.dataobject.cat.TaxonomyAssetCatDO;
import com.datamaster.module.taxonomy.dal.mapper.cat.TaxonomyAssetCatMapper;
import com.datamaster.module.taxonomy.service.cat.ITaxonomyAssetCatService;
import com.datamaster.module.assets.api.service.asset.IAssetsAssetApiOutService;
import com.datamaster.mybatis.core.query.LambdaQueryWrapperX;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 数据资产类目管理Service业务层处理
 *
 * @author DATAMASTER
 * @date 2025-01-20
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class TaxonomyAssetCatServiceImpl extends ServiceImpl<TaxonomyAssetCatMapper, TaxonomyAssetCatDO> implements ITaxonomyAssetCatService {
    @Resource
    private TaxonomyAssetCatMapper TaxonomyAssetCatMapper;

    @Resource
    private IAssetsAssetApiOutService assetsAssetApiService;

    @Override
    public PageResult<TaxonomyAssetCatDO> getAttAssetCatPage(TaxonomyAssetCatPageReqVO pageReqVO) {
        return TaxonomyAssetCatMapper.selectPage(pageReqVO);
    }

    @Override
    public Long createAttAssetCat(TaxonomyAssetCatSaveReqVO createReqVO) {
        TaxonomyAssetCatDO dictType = BeanUtils.toBean(createReqVO, TaxonomyAssetCatDO.class);
        dictType.setCode(createCode(createReqVO.getParentId(), null));
        TaxonomyAssetCatMapper.insert(dictType);
        return dictType.getId();
    }

    @Override
    public int updateAttAssetCat(TaxonomyAssetCatSaveReqVO updateReqVO) {
        TaxonomyAssetCatDO catDO = baseMapper.selectById(updateReqVO.getId());
        if (catDO == null) {
            return 0;
        }
        if (Boolean.FALSE.equals(updateReqVO.getValidFlag())) {
            Long countData = assetsAssetApiService.getCountByCatCode(catDO.getCode());
            if (countData > 0) {
                throw new ServiceException("存在数据资产，不允许禁用");
            }
            baseMapper.updateValidFlag(catDO.getCode(), updateReqVO.getValidFlag());
        } else if (Boolean.TRUE.equals(updateReqVO.getValidFlag())) {
            TaxonomyAssetCatDO parent = baseMapper.selectById(catDO.getParentId());
            if (parent != null && Boolean.FALSE.equals(parent.getValidFlag())) {
                throw new ServiceException("须先启用父级");
            }
        }
        // 更新数据资产类目管理
        TaxonomyAssetCatDO updateObj = BeanUtils.toBean(updateReqVO, TaxonomyAssetCatDO.class);
        return TaxonomyAssetCatMapper.updateById(updateObj);
    }

    @Override
    public int removeAttAssetCat(Collection<Long> idList) {
        int count = 0;
        for (Long id : idList) {
            TaxonomyAssetCatDO cat = baseMapper.selectById(id);
            //判断是否存在数据资产
            if (assetsAssetApiService.getCountByCatCode(cat.getCode()) > 0) {
                throw new ServiceException("存在数据资产，不允许删除");
            }
            if (cat != null) {
                count += baseMapper.delete(Wrappers.lambdaQuery(TaxonomyAssetCatDO.class)
                        .likeRight(TaxonomyAssetCatDO::getCode, cat.getCode()));
            }
        }
        return count;
    }


    @Override
    public TaxonomyAssetCatDO getAttAssetCatById(Long id) {
        return TaxonomyAssetCatMapper.selectById(id);
    }

    @Override
    public List<TaxonomyAssetCatDO> getAttAssetCatList() {
        return TaxonomyAssetCatMapper.selectList();
    }

    @Override
    public List<TaxonomyAssetCatDO> getAttAssetCatList(TaxonomyAssetCatPageReqVO reqVO) {
        LambdaQueryWrapperX<TaxonomyAssetCatDO> queryWrapperX = new LambdaQueryWrapperX<>();
        queryWrapperX.likeIfPresent(TaxonomyAssetCatDO::getName, reqVO.getName())
                .likeRightIfPresent(TaxonomyAssetCatDO::getCode, reqVO.getCode())
                .eqIfPresent(TaxonomyAssetCatDO::getProjectId, reqVO.getProjectId())
                .eq(reqVO.getValidFlag() != null, TaxonomyAssetCatDO::getValidFlag, Boolean.TRUE.equals(reqVO.getValidFlag()) ? "1" : "0")
                .orderByAsc(TaxonomyAssetCatDO::getSortOrder);
        return TaxonomyAssetCatMapper.selectList(queryWrapperX);
    }

    @Override
    public Map<Long, TaxonomyAssetCatDO> getAttAssetCatMap() {
        List<TaxonomyAssetCatDO> TaxonomyAssetCatList = TaxonomyAssetCatMapper.selectList();
        return TaxonomyAssetCatList.stream()
                .collect(Collectors.toMap(
                        TaxonomyAssetCatDO::getId,
                        TaxonomyAssetCatDO -> TaxonomyAssetCatDO,
                        // 保留已存在的值
                        (existing, replacement) -> existing
                ));
    }


    /**
     * 导入数据资产类目管理数据
     *
     * @param importExcelList 数据资产类目管理数据列表
     * @param isUpdateSupport 是否更新支持，如果已存在，则进行更新数据
     * @param operName        操作用户
     * @return 结果
     */
    @Override
    public String importAttAssetCat(List<TaxonomyAssetCatRespVO> importExcelList, boolean isUpdateSupport, String operName) {
        if (StringUtils.isNull(importExcelList) || importExcelList.size() == 0) {
            throw new ServiceException("导入数据不能为空！");
        }

        int successNum = 0;
        int failureNum = 0;
        List<String> successMessages = new ArrayList<>();
        List<String> failureMessages = new ArrayList<>();

        for (TaxonomyAssetCatRespVO respVO : importExcelList) {
            try {
                TaxonomyAssetCatDO TaxonomyAssetCatDO = BeanUtils.toBean(respVO, TaxonomyAssetCatDO.class);
                Long TaxonomyAssetCatId = respVO.getId();
                if (isUpdateSupport) {
                    if (TaxonomyAssetCatId != null) {
                        TaxonomyAssetCatDO existingAttAssetCat = TaxonomyAssetCatMapper.selectById(TaxonomyAssetCatId);
                        if (existingAttAssetCat != null) {
                            TaxonomyAssetCatMapper.updateById(TaxonomyAssetCatDO);
                            successNum++;
                            successMessages.add("数据更新成功，ID为 " + TaxonomyAssetCatId + " 的数据资产类目管理记录。");
                        } else {
                            failureNum++;
                            failureMessages.add("数据更新失败，ID为 " + TaxonomyAssetCatId + " 的数据资产类目管理记录不存在。");
                        }
                    } else {
                        failureNum++;
                        failureMessages.add("数据更新失败，某条记录的ID不存在。");
                    }
                } else {
                    QueryWrapper<TaxonomyAssetCatDO> queryWrapper = new QueryWrapper<>();
                    queryWrapper.eq("id", TaxonomyAssetCatId);
                    TaxonomyAssetCatDO existingAttAssetCat = TaxonomyAssetCatMapper.selectOne(queryWrapper);
                    if (existingAttAssetCat == null) {
                        TaxonomyAssetCatMapper.insert(TaxonomyAssetCatDO);
                        successNum++;
                        successMessages.add("数据插入成功，ID为 " + TaxonomyAssetCatId + " 的数据资产类目管理记录。");
                    } else {
                        failureNum++;
                        failureMessages.add("数据插入失败，ID为 " + TaxonomyAssetCatId + " 的数据资产类目管理记录已存在。");
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
        LambdaQueryWrapper<TaxonomyAssetCatDO> query = new LambdaQueryWrapper<TaxonomyAssetCatDO>()
                .eq(TaxonomyAssetCatDO::getParentId, parentId)
                .likeRight(StringUtils.isNotBlank(parentCode), TaxonomyAssetCatDO::getCode, parentCode)
                .isNotNull(TaxonomyAssetCatDO::getCode)
                .orderByDesc(TaxonomyAssetCatDO::getCode);
        List<TaxonomyAssetCatDO> list = baseMapper.selectList(query);
        if (list == null || list.size() == 0) {
            if (parentId == 0) {
                //情况1
                categoryCode = YouBianCodeUtil.getNextYouBianCode(null);
            } else {
                //情况2
                TaxonomyAssetCatDO parent = baseMapper.selectById(parentId);
                categoryCode = YouBianCodeUtil.getSubYouBianCode(parent.getCode(), null);
            }
        } else {
            //情况3
            categoryCode = YouBianCodeUtil.getNextYouBianCode(list.get(0).getCode());
        }
        return categoryCode;
    }

}
