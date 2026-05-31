

package com.datamaster.module.taxonomy.service.Tag.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.common.core.text.Convert;
import com.datamaster.common.exception.ServiceException;
import com.datamaster.common.utils.StringUtils;
import com.datamaster.common.utils.object.BeanUtils;
import com.datamaster.module.taxonomy.api.Tag.dto.TaxonomyTagRespDTO;
import com.datamaster.module.taxonomy.api.service.cat.tag.ITaxonomyTagApiService;
import com.datamaster.module.taxonomy.controller.admin.tag.vo.TaxonomyTagPageReqVO;
import com.datamaster.module.taxonomy.controller.admin.tag.vo.TaxonomyTagRespVO;
import com.datamaster.module.taxonomy.controller.admin.tag.vo.TaxonomyTagSaveReqVO;
import com.datamaster.module.taxonomy.dal.dataobject.Rel.TaxonomyTagAssetRelDO;
import com.datamaster.module.taxonomy.dal.dataobject.Tag.TaxonomyTagDO;
import com.datamaster.module.taxonomy.dal.dataobject.cat.TaxonomyTagCatDO;
import com.datamaster.module.taxonomy.dal.mapper.Tag.TaxonomyTagMapper;
import com.datamaster.module.taxonomy.service.Rel.ITaxonomyTagAssetRelService;
import com.datamaster.module.taxonomy.service.Tag.ITaxonomyTagService;
import com.datamaster.module.taxonomy.service.cat.ITaxonomyTagCatService;
import com.datamaster.mybatis.core.query.LambdaQueryWrapperX;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 标签管理Service业务层处理
 *
 * @author DATAMASTER
 * @date 2025-07-11
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class TaxonomyTagServiceImpl extends ServiceImpl<TaxonomyTagMapper, TaxonomyTagDO> implements ITaxonomyTagService, ITaxonomyTagApiService {
    @Resource
    private TaxonomyTagMapper TaxonomyTagMapper;
    @Resource
    private ITaxonomyTagAssetRelService TaxonomyTagAssetRelService;
    @Resource
    private ITaxonomyTagCatService TaxonomyTagCatService;

    @Override
    public PageResult<TaxonomyTagDO> getAttTagPage(TaxonomyTagPageReqVO pageReqVO) {
        // 资产打标过滤该资产已经打过标的信息
        if (pageReqVO.getAeestId() != null) {
            LambdaQueryWrapperX<TaxonomyTagAssetRelDO> TaxonomyTagAssetRelDOLambdaQueryWrapperX = new LambdaQueryWrapperX<>();
            TaxonomyTagAssetRelDOLambdaQueryWrapperX.eqIfPresent(TaxonomyTagAssetRelDO::getAssetId, pageReqVO.getAeestId());
            TaxonomyTagAssetRelDOLambdaQueryWrapperX.eqIfPresent(TaxonomyTagAssetRelDO::getDelFlag, "0");
            List<TaxonomyTagAssetRelDO> list = TaxonomyTagAssetRelService.list(TaxonomyTagAssetRelDOLambdaQueryWrapperX);
            Long[] tagIds = new Long[list.size()];
            for (int i = 0; i < list.size(); i++) {
                TaxonomyTagAssetRelDO TaxonomyTagAssetRelDO = list.get(i);
                tagIds[i] = Convert.toLong(TaxonomyTagAssetRelDO.getTagId());
            }
            pageReqVO.setIds(tagIds);
        }
        PageResult<TaxonomyTagDO> TaxonomyTagDOPageResult = TaxonomyTagMapper.selectPage(pageReqVO);

        return TaxonomyTagDOPageResult;
    }

    @Override
    public Long createAttTag(TaxonomyTagSaveReqVO createReqVO) {
        TaxonomyTagDO dictType = BeanUtils.toBean(createReqVO, TaxonomyTagDO.class);
        Map<String, TaxonomyTagCatDO> collect2 = TaxonomyTagCatService.list().stream().collect(Collectors.toMap(s -> s.getCode(), Function.identity()));
        TaxonomyTagCatDO TaxonomyTagCatDO = collect2.get(dictType.getCatCode());
        if (TaxonomyTagCatDO == null) {
            throw new ServiceException("标签类目不存在");
        }
        dictType.setCatName(TaxonomyTagCatDO.getName());
        TaxonomyTagMapper.insert(dictType);
        return dictType.getId();
    }

    @Override
    public int updateAttTag(TaxonomyTagSaveReqVO updateReqVO) {
        // 相关校验

        // 更新标签管理
        TaxonomyTagDO updateObj = BeanUtils.toBean(updateReqVO, TaxonomyTagDO.class);
        String catCode = updateObj.getCatCode();
        if(StringUtils.isNotEmpty(catCode)){
            Map<String, TaxonomyTagCatDO> collect2 = TaxonomyTagCatService.list().stream().collect(Collectors.toMap(s -> s.getCode(), Function.identity()));
            TaxonomyTagCatDO TaxonomyTagCatDO = collect2.get(catCode);
            if (TaxonomyTagCatDO == null) {
                throw new ServiceException("标签类目不存在");
            }
            if (TaxonomyTagCatDO != null) {
                updateObj.setCatName(TaxonomyTagCatDO.getName());
            }
        }
        return TaxonomyTagMapper.updateById(updateObj);
    }

    @Override
    public int removeAttTag(Collection<Long> idList) {
        // 批量删除标签管理
        for (Long l : idList) {
            LambdaQueryWrapperX<TaxonomyTagAssetRelDO> queryWrapperX = new LambdaQueryWrapperX<>();
            queryWrapperX.eqIfPresent(TaxonomyTagAssetRelDO::getTagId, l);
            queryWrapperX.eqIfPresent(TaxonomyTagAssetRelDO::getDelFlag, "0");
            List<TaxonomyTagAssetRelDO> collect = TaxonomyTagAssetRelService.list(queryWrapperX);
            if (collect != null && collect.size() > 0) {
                throw new ServiceException("存在资产信息，不允许删除");
            }
        }

        return TaxonomyTagMapper.deleteBatchIds(idList);
    }

    @Override
    public TaxonomyTagRespVO getAttTagById(Long id) {
        TaxonomyTagDO TaxonomyTagDO = TaxonomyTagMapper.selectById(id);
        if (TaxonomyTagDO == null) {
            return new TaxonomyTagRespVO();
        }
        LambdaQueryWrapperX<TaxonomyTagAssetRelDO> queryWrapperX = new LambdaQueryWrapperX<>();
        queryWrapperX.eqIfPresent(TaxonomyTagAssetRelDO::getTagId, id);
        queryWrapperX.eqIfPresent(TaxonomyTagAssetRelDO::getDelFlag, "0");
        List<TaxonomyTagAssetRelDO> collect = TaxonomyTagAssetRelService.list(queryWrapperX);
        List<Long> list = new ArrayList<>();
        for (TaxonomyTagAssetRelDO TaxonomyTagAssetRelDO : collect) {
            list.add(Convert.toLong(TaxonomyTagAssetRelDO.getAssetId()));
        }
        TaxonomyTagRespVO bean = BeanUtils.toBean(TaxonomyTagDO, TaxonomyTagRespVO.class);
        bean.setAeestId(list);
        return bean;
    }

    @Override
    public List<TaxonomyTagDO> getAttTagList() {
        return TaxonomyTagMapper.selectList();
    }

    @Override
    public Map<Long, TaxonomyTagDO> getAttTagMap() {
        List<TaxonomyTagDO> TaxonomyTagList = TaxonomyTagMapper.selectList();
        return TaxonomyTagList.stream()
                .collect(Collectors.toMap(
                        TaxonomyTagDO::getId,
                        TaxonomyTagDO -> TaxonomyTagDO,
                        // 保留已存在的值
                        (existing, replacement) -> existing
                ));
    }


    /**
     * 导入标签管理数据
     *
     * @param importExcelList 标签管理数据列表
     * @param isUpdateSupport 是否更新支持，如果已存在，则进行更新数据
     * @param operName        操作用户
     * @return 结果
     */
    @Override
    public String importAttTag(List<TaxonomyTagRespVO> importExcelList, boolean isUpdateSupport, String operName) {
        if (StringUtils.isNull(importExcelList) || importExcelList.size() == 0) {
            throw new ServiceException("导入数据不能为空！");
        }

        int successNum = 0;
        int failureNum = 0;
        List<String> successMessages = new ArrayList<>();
        List<String> failureMessages = new ArrayList<>();

        for (TaxonomyTagRespVO respVO : importExcelList) {
            try {
                TaxonomyTagDO TaxonomyTagDO = BeanUtils.toBean(respVO, TaxonomyTagDO.class);
                Long TaxonomyTagId = respVO.getId();
                if (isUpdateSupport) {
                    if (TaxonomyTagId != null) {
                        TaxonomyTagDO existingAttTag = TaxonomyTagMapper.selectById(TaxonomyTagId);
                        if (existingAttTag != null) {
                            TaxonomyTagMapper.updateById(TaxonomyTagDO);
                            successNum++;
                            successMessages.add("数据更新成功，ID为 " + TaxonomyTagId + " 的标签管理记录。");
                        } else {
                            failureNum++;
                            failureMessages.add("数据更新失败，ID为 " + TaxonomyTagId + " 的标签管理记录不存在。");
                        }
                    } else {
                        failureNum++;
                        failureMessages.add("数据更新失败，某条记录的ID不存在。");
                    }
                } else {
                    QueryWrapper<TaxonomyTagDO> queryWrapper = new QueryWrapper<>();
                    queryWrapper.eq("id", TaxonomyTagId);
                    TaxonomyTagDO existingAttTag = TaxonomyTagMapper.selectOne(queryWrapper);
                    if (existingAttTag == null) {
                        TaxonomyTagMapper.insert(TaxonomyTagDO);
                        successNum++;
                        successMessages.add("数据插入成功，ID为 " + TaxonomyTagId + " 的标签管理记录。");
                    } else {
                        failureNum++;
                        failureMessages.add("数据插入失败，ID为 " + TaxonomyTagId + " 的标签管理记录已存在。");
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
    public Long getCountByCatCode(String catCode) {
        return TaxonomyTagMapper.selectCount(Wrappers.lambdaQuery(TaxonomyTagDO.class)
                .likeRight(TaxonomyTagDO::getCatCode, catCode));
    }

    @Override
    public List<TaxonomyTagRespDTO> getApiList() {
        List<TaxonomyTagDO> TaxonomyTagDOS = TaxonomyTagMapper.selectList();
        return BeanUtils.toBean(TaxonomyTagDOS, TaxonomyTagRespDTO.class);
    }

    @Override
    public int updateCatCode(String oldCatCode, String newCatCode) {
        return TaxonomyTagMapper.updateCatCode(oldCatCode,newCatCode);
    }
}
