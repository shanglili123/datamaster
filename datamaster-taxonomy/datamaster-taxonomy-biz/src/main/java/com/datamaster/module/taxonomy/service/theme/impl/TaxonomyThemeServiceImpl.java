

package com.datamaster.module.taxonomy.service.theme.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.yulichang.wrapper.MPJLambdaWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.common.exception.ServiceException;
import com.datamaster.common.utils.StringUtils;
import com.datamaster.common.utils.object.BeanUtils;
import com.datamaster.module.taxonomy.controller.admin.theme.vo.TaxonomyThemePageReqVO;
import com.datamaster.module.taxonomy.controller.admin.theme.vo.TaxonomyThemeRespVO;
import com.datamaster.module.taxonomy.controller.admin.theme.vo.TaxonomyThemeSaveReqVO;
import com.datamaster.module.taxonomy.dal.dataobject.theme.TaxonomyThemeDO;
import com.datamaster.module.taxonomy.dal.mapper.theme.TaxonomyThemeMapper;
import com.datamaster.module.taxonomy.service.theme.ITaxonomyThemeService;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
/**
 * 主题Service业务层处理
 *
 * @author DATAMASTER
 * @date 2025-01-20
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class TaxonomyThemeServiceImpl  extends ServiceImpl<TaxonomyThemeMapper,TaxonomyThemeDO> implements ITaxonomyThemeService {
    @Resource
    private TaxonomyThemeMapper TaxonomyThemeMapper;

    @Override
    public PageResult<TaxonomyThemeDO> getAttThemePage(TaxonomyThemePageReqVO pageReqVO) {
        return TaxonomyThemeMapper.selectPage(pageReqVO);
    }

    @Override
    public List<TaxonomyThemeDO> getAttThemeListByReqVO(TaxonomyThemePageReqVO reqVO) {
        MPJLambdaWrapper<TaxonomyThemeDO> wrapper = new MPJLambdaWrapper<>();
        wrapper.selectAll(TaxonomyThemeDO.class)
                .like(StringUtils.isNotBlank(reqVO.getName()), TaxonomyThemeDO::getName, reqVO.getName());
        return TaxonomyThemeMapper.selectList(wrapper);
    }

    @Override
    public Long createAttTheme(TaxonomyThemeSaveReqVO createReqVO) {
        TaxonomyThemeDO dictType = BeanUtils.toBean(createReqVO, TaxonomyThemeDO.class);
        TaxonomyThemeMapper.insert(dictType);
        return dictType.getId();
    }

    @Override
    public int updateAttTheme(TaxonomyThemeSaveReqVO updateReqVO) {
        // 相关校验

        // 更新主题
        TaxonomyThemeDO updateObj = BeanUtils.toBean(updateReqVO, TaxonomyThemeDO.class);
        return TaxonomyThemeMapper.updateById(updateObj);
    }
    @Override
    public int removeAttTheme(Collection<Long> idList) {
        // 批量删除主题
        return TaxonomyThemeMapper.deleteBatchIds(idList);
    }

    @Override
    public TaxonomyThemeDO getAttThemeById(Long id) {
        return TaxonomyThemeMapper.selectById(id);
    }

    @Override
    public List<TaxonomyThemeDO> getAttThemeList() {
        return TaxonomyThemeMapper.selectList();
    }

    @Override
    public Map<Long, TaxonomyThemeDO> getAttThemeMap() {
        List<TaxonomyThemeDO> TaxonomyThemeList = TaxonomyThemeMapper.selectList();
        return TaxonomyThemeList.stream()
                .collect(Collectors.toMap(
                        TaxonomyThemeDO::getId,
                        TaxonomyThemeDO -> TaxonomyThemeDO,
                        // 保留已存在的值
                        (existing, replacement) -> existing
                ));
    }


    /**
     * 导入主题数据
     *
     * @param importExcelList 主题数据列表
     * @param isUpdateSupport 是否更新支持，如果已存在，则进行更新数据
     * @param operName 操作用户
     * @return 结果
     */
    @Override
    public String importAttTheme(List<TaxonomyThemeRespVO> importExcelList, boolean isUpdateSupport, String operName) {
        if (StringUtils.isNull(importExcelList) || importExcelList.size() == 0) {
            throw new ServiceException("导入数据不能为空！");
        }

        int successNum = 0;
        int failureNum = 0;
        List<String> successMessages = new ArrayList<>();
        List<String> failureMessages = new ArrayList<>();

        for (TaxonomyThemeRespVO respVO : importExcelList) {
            try {
                TaxonomyThemeDO TaxonomyThemeDO = BeanUtils.toBean(respVO, TaxonomyThemeDO.class);
                Long TaxonomyThemeId = respVO.getId();
                if (isUpdateSupport) {
                    if (TaxonomyThemeId != null) {
                        TaxonomyThemeDO existingAttTheme = TaxonomyThemeMapper.selectById(TaxonomyThemeId);
                        if (existingAttTheme != null) {
                            TaxonomyThemeMapper.updateById(TaxonomyThemeDO);
                            successNum++;
                            successMessages.add("数据更新成功，ID为 " + TaxonomyThemeId + " 的主题记录。");
                        } else {
                            failureNum++;
                            failureMessages.add("数据更新失败，ID为 " + TaxonomyThemeId + " 的主题记录不存在。");
                        }
                    } else {
                        failureNum++;
                        failureMessages.add("数据更新失败，某条记录的ID不存在。");
                    }
                } else {
                    QueryWrapper<TaxonomyThemeDO> queryWrapper = new QueryWrapper<>();
                    queryWrapper.eq("id", TaxonomyThemeId);
                    TaxonomyThemeDO existingAttTheme = TaxonomyThemeMapper.selectOne(queryWrapper);
                    if (existingAttTheme == null) {
                        TaxonomyThemeMapper.insert(TaxonomyThemeDO);
                        successNum++;
                        successMessages.add("数据插入成功，ID为 " + TaxonomyThemeId + " 的主题记录。");
                    } else {
                        failureNum++;
                        failureMessages.add("数据插入失败，ID为 " + TaxonomyThemeId + " 的主题记录已存在。");
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
