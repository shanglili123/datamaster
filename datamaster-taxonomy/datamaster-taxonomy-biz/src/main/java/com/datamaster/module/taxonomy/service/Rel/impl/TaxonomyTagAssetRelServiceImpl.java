

package com.datamaster.module.taxonomy.service.Rel.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.common.core.text.Convert;
import com.datamaster.common.exception.ServiceException;
import com.datamaster.common.utils.StringUtils;
import com.datamaster.common.utils.object.BeanUtils;
import com.datamaster.module.taxonomy.api.Rel.dto.TaxonomyTagAssetRelReqDTO;
import com.datamaster.module.taxonomy.api.Rel.dto.TaxonomyTagAssetRelRespDTO;
import com.datamaster.module.taxonomy.api.service.cat.tagRel.ITaxonomyTagAssetRelApiService;
import com.datamaster.module.taxonomy.controller.admin.tag.vo.TaxonomyTagRespVO;
import com.datamaster.module.taxonomy.controller.admin.tag.vo.TaxonomyTagSaveReqVO;
import com.datamaster.module.taxonomy.controller.admin.tagAssetRel.vo.TaxonomyTagAssetRelPageReqVO;
import com.datamaster.module.taxonomy.controller.admin.tagAssetRel.vo.TaxonomyTagAssetRelRespVO;
import com.datamaster.module.taxonomy.controller.admin.tagAssetRel.vo.TaxonomyTagAssetRelSaveReqVO;
import com.datamaster.module.taxonomy.dal.dataobject.Rel.TaxonomyTagAssetRelDO;
import com.datamaster.module.taxonomy.dal.dataobject.Tag.TaxonomyTagDO;
import com.datamaster.module.taxonomy.dal.mapper.Rel.TaxonomyTagAssetRelMapper;
import com.datamaster.module.taxonomy.service.Rel.ITaxonomyTagAssetRelService;
import com.datamaster.module.taxonomy.service.Tag.ITaxonomyTagService;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 标签与资产关联关系Service业务层处理
 *
 * @author DATAMASTER
 * @date 2025-07-11
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class TaxonomyTagAssetRelServiceImpl  extends ServiceImpl<TaxonomyTagAssetRelMapper,TaxonomyTagAssetRelDO> implements ITaxonomyTagAssetRelService , ITaxonomyTagAssetRelApiService {
    @Resource
    private TaxonomyTagAssetRelMapper TaxonomyTagAssetRelMapper;

    @Resource
    private ITaxonomyTagService TaxonomyTagService;

    @Override
    public PageResult<TaxonomyTagAssetRelDO> getAttTagAssetRelPage(TaxonomyTagAssetRelPageReqVO pageReqVO) {
        return TaxonomyTagAssetRelMapper.selectPage(pageReqVO);
    }

    @Override
    public Long createAttTagAssetRel(TaxonomyTagAssetRelSaveReqVO createReqVO) {

        TaxonomyTagAssetRelMapper.delete("asset_id", createReqVO.getAssetId());
        List<String> tagIds = createReqVO.getTagIds();
        // 添加关系表，更新标签关联资产数量
        for (String tagId : tagIds) {
            TaxonomyTagAssetRelDO relDo = new TaxonomyTagAssetRelDO();
            relDo.setAssetId(createReqVO.getAssetId());
            relDo.setTagId(tagId);
            TaxonomyTagAssetRelMapper.insert(relDo);
            Long l = TaxonomyTagAssetRelMapper.selectCount("tag_id", tagId);
            TaxonomyTagDO TaxonomyTagDO = new TaxonomyTagDO();
            TaxonomyTagDO.setId(Convert.toLong(tagId));
            TaxonomyTagDO.setAeestCount(l);
            TaxonomyTagSaveReqVO bean = BeanUtils.toBean(TaxonomyTagDO, TaxonomyTagSaveReqVO.class);
            TaxonomyTagService.updateAttTag(bean);
        }
        return 1L;
    }

    @Override
    public int updateAttTagAssetRel(TaxonomyTagAssetRelSaveReqVO updateReqVO) {
        // 相关校验

        // 更新标签与资产关联关系
        TaxonomyTagAssetRelDO updateObj = BeanUtils.toBean(updateReqVO, TaxonomyTagAssetRelDO.class);
        return TaxonomyTagAssetRelMapper.updateById(updateObj);
    }
    @Override
    public int removeAttTagAssetRel(Collection<Long> idList) {
        // 批量删除标签与资产关联关系
        return TaxonomyTagAssetRelMapper.deleteBatchIds(idList);
    }

    @Override
    public TaxonomyTagAssetRelDO getAttTagAssetRelById(Long id) {
        return TaxonomyTagAssetRelMapper.selectById(id);
    }

    @Override
    public List<TaxonomyTagAssetRelDO> getAttTagAssetRelList() {
        return TaxonomyTagAssetRelMapper.selectList();
    }

    @Override
    public Map<Long, TaxonomyTagAssetRelDO> getAttTagAssetRelMap() {
        List<TaxonomyTagAssetRelDO> TaxonomyTagAssetRelList = TaxonomyTagAssetRelMapper.selectList();
        return TaxonomyTagAssetRelList.stream()
                .collect(Collectors.toMap(
                        TaxonomyTagAssetRelDO::getId,
                        TaxonomyTagAssetRelDO -> TaxonomyTagAssetRelDO,
                        // 保留已存在的值
                        (existing, replacement) -> existing
                ));
    }


        /**
         * 导入标签与资产关联关系数据
         *
         * @param importExcelList 标签与资产关联关系数据列表
         * @param isUpdateSupport 是否更新支持，如果已存在，则进行更新数据
         * @param operName 操作用户
         * @return 结果
         */
        @Override
        public String importAttTagAssetRel(List<TaxonomyTagAssetRelRespVO> importExcelList, boolean isUpdateSupport, String operName) {
            if (StringUtils.isNull(importExcelList) || importExcelList.size() == 0) {
                throw new ServiceException("导入数据不能为空！");
            }

            int successNum = 0;
            int failureNum = 0;
            List<String> successMessages = new ArrayList<>();
            List<String> failureMessages = new ArrayList<>();

            for (TaxonomyTagAssetRelRespVO respVO : importExcelList) {
                try {
                    TaxonomyTagAssetRelDO TaxonomyTagAssetRelDO = BeanUtils.toBean(respVO, TaxonomyTagAssetRelDO.class);
                    Long TaxonomyTagAssetRelId = respVO.getId();
                    if (isUpdateSupport) {
                        if (TaxonomyTagAssetRelId != null) {
                            TaxonomyTagAssetRelDO existingAttTagAssetRel = TaxonomyTagAssetRelMapper.selectById(TaxonomyTagAssetRelId);
                            if (existingAttTagAssetRel != null) {
                                TaxonomyTagAssetRelMapper.updateById(TaxonomyTagAssetRelDO);
                                successNum++;
                                successMessages.add("数据更新成功，ID为 " + TaxonomyTagAssetRelId + " 的标签与资产关联关系记录。");
                            } else {
                                failureNum++;
                                failureMessages.add("数据更新失败，ID为 " + TaxonomyTagAssetRelId + " 的标签与资产关联关系记录不存在。");
                            }
                        } else {
                            failureNum++;
                            failureMessages.add("数据更新失败，某条记录的ID不存在。");
                        }
                    } else {
                        QueryWrapper<TaxonomyTagAssetRelDO> queryWrapper = new QueryWrapper<>();
                        queryWrapper.eq("id", TaxonomyTagAssetRelId);
                        TaxonomyTagAssetRelDO existingAttTagAssetRel = TaxonomyTagAssetRelMapper.selectOne(queryWrapper);
                        if (existingAttTagAssetRel == null) {
                            TaxonomyTagAssetRelMapper.insert(TaxonomyTagAssetRelDO);
                            successNum++;
                            successMessages.add("数据插入成功，ID为 " + TaxonomyTagAssetRelId + " 的标签与资产关联关系记录。");
                        } else {
                            failureNum++;
                            failureMessages.add("数据插入失败，ID为 " + TaxonomyTagAssetRelId + " 的标签与资产关联关系记录已存在。");
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
    public int removeAttTagAssetRel(Long id, TaxonomyTagAssetRelPageReqVO TaxonomyTagAssetRel) {
        TaxonomyTagRespVO TaxonomyTagById = TaxonomyTagService.getAttTagById(Convert.toLong(TaxonomyTagAssetRel.getTagId()));
        TaxonomyTagById.setAeestCount(TaxonomyTagById.getAeestCount() - 1);
        TaxonomyTagSaveReqVO bean = BeanUtils.toBean(TaxonomyTagById, TaxonomyTagSaveReqVO.class);
        TaxonomyTagService.updateAttTag(bean);
        return TaxonomyTagAssetRelMapper.deleteById(id);
    }

    @Override
    public List<TaxonomyTagAssetRelRespDTO> getApiList(TaxonomyTagAssetRelReqDTO TaxonomyApiCatReqDTO) {
        List<TaxonomyTagAssetRelDO> TaxonomyTagAssetRelDOS = TaxonomyTagAssetRelMapper.selectList();
        return BeanUtils.toBean(TaxonomyTagAssetRelDOS, TaxonomyTagAssetRelRespDTO.class);
    }

    @Override
    public void deleteRelByUpdateTag(Long assetId) {
        List<TaxonomyTagAssetRelDO> TaxonomyTagAssetRelDOS = TaxonomyTagAssetRelMapper.selectList("asset_id", assetId);
        Map<Long, TaxonomyTagDO> collect = TaxonomyTagService.list().stream().collect(Collectors.toMap(s -> s.getId(), Function.identity()));
        for (TaxonomyTagAssetRelDO TaxonomyTagAssetRelDO : TaxonomyTagAssetRelDOS) {
            TaxonomyTagDO TaxonomyTagDO1 = collect.get(Convert.toLong(TaxonomyTagAssetRelDO.getTagId()));
            TaxonomyTagDO1.setAeestCount(TaxonomyTagDO1.getAeestCount() - 1);
            TaxonomyTagSaveReqVO bean = BeanUtils.toBean(TaxonomyTagDO1, TaxonomyTagSaveReqVO.class);
            TaxonomyTagService.updateAttTag(bean);
        }
    }
}
