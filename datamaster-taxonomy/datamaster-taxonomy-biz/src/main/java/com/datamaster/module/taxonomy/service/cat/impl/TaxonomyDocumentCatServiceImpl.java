

package com.datamaster.module.taxonomy.service.cat.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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
import com.datamaster.module.taxonomy.controller.admin.cat.vo.TaxonomyDocumentCatPageReqVO;
import com.datamaster.module.taxonomy.controller.admin.cat.vo.TaxonomyDocumentCatSaveReqVO;
import com.datamaster.module.taxonomy.dal.dataobject.cat.TaxonomyDocumentCatDO;
import com.datamaster.module.taxonomy.dal.mapper.cat.TaxonomyDocumentCatMapper;
import com.datamaster.module.taxonomy.service.cat.ITaxonomyDocumentCatService;
import com.datamaster.module.standards.api.service.document.IStandardsDocumentApiService;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 标准信息分类管理Service业务层处理
 *
 * @author DATAMASTER
 * @date 2025-08-21
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class TaxonomyDocumentCatServiceImpl extends ServiceImpl<TaxonomyDocumentCatMapper, TaxonomyDocumentCatDO> implements ITaxonomyDocumentCatService {
    @Resource
    private IStandardsDocumentApiService standardsDocumentApiService;
    @Resource
    private TaxonomyDocumentCatMapper TaxonomyDocumentCatMapper;

    @Override
    public PageResult<TaxonomyDocumentCatDO> getAttDocumentCatPage(TaxonomyDocumentCatPageReqVO pageReqVO) {
        return TaxonomyDocumentCatMapper.selectPage(pageReqVO);
    }

    @Override
    public List<TaxonomyDocumentCatDO> getAttDocumentCatList(TaxonomyDocumentCatPageReqVO reqVO) {
        MPJLambdaWrapper<TaxonomyDocumentCatDO> mpjLambdaWrapper = new MPJLambdaWrapper();
        mpjLambdaWrapper.selectAll(TaxonomyDocumentCatDO.class)
                .like(StringUtils.isNotEmpty(reqVO.getName()), TaxonomyDocumentCatDO::getName, reqVO.getName())
                .eq(reqVO.getParentId() != null, TaxonomyDocumentCatDO::getParentId, reqVO.getParentId())
                .eq(reqVO.getSortOrder() != null, TaxonomyDocumentCatDO::getSortOrder, reqVO.getSortOrder())
                .eq(reqVO.getValidFlag() != null, TaxonomyDocumentCatDO::getValidFlag, Boolean.TRUE.equals(reqVO.getValidFlag()) ? "1" : "0")
                .eq(StringUtils.isNotEmpty(reqVO.getDescription()), TaxonomyDocumentCatDO::getDescription, reqVO.getDescription())
                .eq(StringUtils.isNotEmpty(reqVO.getCode()), TaxonomyDocumentCatDO::getCode, reqVO.getCode());
        return TaxonomyDocumentCatMapper.selectList(mpjLambdaWrapper);
    }

    @Override
    public Long createAttDocumentCat(TaxonomyDocumentCatSaveReqVO createReqVO) {
        TaxonomyDocumentCatDO dictType = BeanUtils.toBean(createReqVO, TaxonomyDocumentCatDO.class);
        dictType.setCode(createCode(createReqVO.getParentId(), null));
        TaxonomyDocumentCatMapper.insert(dictType);
        return dictType.getId();
    }

    @Override
    public int updateAttDocumentCat(TaxonomyDocumentCatSaveReqVO updateReqVO) {
        TaxonomyDocumentCatDO catDO = baseMapper.selectById(updateReqVO.getId());
        if (catDO == null) {
            return 0;
        }
        if (Boolean.FALSE.equals(updateReqVO.getValidFlag())) {
            Long countData = standardsDocumentApiService.getCountByCatCode(catDO.getCode());
            if (countData > 0) {
                throw new ServiceException("存在标准，不允许禁用");
            }
            baseMapper.updateValidFlag(catDO.getCode(), updateReqVO.getValidFlag());
        } else if (Boolean.TRUE.equals(updateReqVO.getValidFlag())) {
            TaxonomyDocumentCatDO parent = baseMapper.selectById(catDO.getParentId());
            if (parent != null && Boolean.FALSE.equals(parent.getValidFlag())) {
                throw new ServiceException("须先启用父级");
            }
        }
        // 更新标准信息分类管理
        TaxonomyDocumentCatDO updateObj = BeanUtils.toBean(updateReqVO, TaxonomyDocumentCatDO.class);
        return baseMapper.updateById(updateObj);
    }

    @Override
    public int removeAttDocumentCat(Long id) {
        TaxonomyDocumentCatDO catDO = baseMapper.selectById(id);
        Long countData = standardsDocumentApiService.getCountByCatCode(catDO.getCode());
        if (countData > 0) {
            throw new ServiceException("存在标准，不允许删除");
        }
        // 单独删除标准信息分类管理
        return baseMapper.deleteById(id);
    }

    @Override
    public TaxonomyDocumentCatDO getAttDocumentCatById(Long id) {
        return TaxonomyDocumentCatMapper.selectById(id);
    }

    @Override
    public List<TaxonomyDocumentCatDO> getAttDocumentCatList() {
        return TaxonomyDocumentCatMapper.selectList();
    }

    @Override
    public Map<Long, TaxonomyDocumentCatDO> getAttDocumentCatMap() {
        List<TaxonomyDocumentCatDO> TaxonomyDocumentCatList = TaxonomyDocumentCatMapper.selectList();
        return TaxonomyDocumentCatList.stream()
                .collect(Collectors.toMap(
                        TaxonomyDocumentCatDO::getId,
                        TaxonomyDocumentCatDO -> TaxonomyDocumentCatDO,
                        // 保留已存在的值
                        (existing, replacement) -> existing
                ));
    }

    @Override
    public boolean hasChildByAttDocumentCatId(Long id) {
        return TaxonomyDocumentCatMapper.selectCount(TaxonomyDocumentCatDO::getParentId, id) > 0;
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
        LambdaQueryWrapper<TaxonomyDocumentCatDO> query = new LambdaQueryWrapper<TaxonomyDocumentCatDO>()
                .eq(TaxonomyDocumentCatDO::getParentId, parentId)
                .likeRight(StringUtils.isNotBlank(parentCode), TaxonomyDocumentCatDO::getCode, parentCode)
                .isNotNull(TaxonomyDocumentCatDO::getCode)
                .orderByDesc(TaxonomyDocumentCatDO::getCode);
        List<TaxonomyDocumentCatDO> list = baseMapper.selectList(query);
        if (list == null || list.size() == 0) {
            if (parentId == 0) {
                //情况1
                categoryCode = YouBianCodeUtil.getNextYouBianCode(null);
            } else {
                //情况2
                TaxonomyDocumentCatDO parent = baseMapper.selectById(parentId);
                categoryCode = YouBianCodeUtil.getSubYouBianCode(parent.getCode(), null);
            }
        } else {
            //情况3
            categoryCode = YouBianCodeUtil.getNextYouBianCode(list.get(0).getCode());
        }
        return categoryCode;
    }
}
