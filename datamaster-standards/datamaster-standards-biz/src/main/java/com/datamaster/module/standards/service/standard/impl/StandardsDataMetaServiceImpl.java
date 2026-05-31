package com.datamaster.module.standards.service.standard.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.datamaster.module.standards.dal.dataobject.standard.StandardsDataMetaDO;
import com.datamaster.module.standards.dal.mapper.standard.StandardsDataMetaMapper;
import com.github.yulichang.wrapper.MPJLambdaWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.common.exception.ServiceException;
import com.datamaster.common.utils.object.BeanUtils;
import com.datamaster.module.standards.controller.admin.standard.vo.StandardsDataElemPageReqVO;
import com.datamaster.module.standards.controller.admin.standard.vo.StandardsDataElemSaveReqVO;
import com.datamaster.mybatis.config.MasterDataSourceConfig;
import com.datamaster.module.standards.service.standard.IStandardsDataMetaService;
import com.datamaster.module.catalog.api.service.column.CatalogColumnApiService;

import java.util.List;

/**
 * 数据元Service业务层处理
 *
 * @author DATAMASTER
 * @date 2025-01-21
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
@RequiredArgsConstructor
public class StandardsDataMetaServiceImpl extends ServiceImpl<StandardsDataMetaMapper, StandardsDataMetaDO> implements IStandardsDataMetaService {

    private final StandardsDataMetaMapper mapper;
    private final CatalogColumnApiService columnApiService;

    @Override
    public PageResult<StandardsDataMetaDO> getDgDataElemPage(StandardsDataElemPageReqVO pageReqVO) {
        return mapper.selectPage(pageReqVO);
    }

    @Override
    public List<StandardsDataMetaDO> getDgDataElemList(StandardsDataElemPageReqVO reqVO) {
        return mapper.selectList(reqVO);
    }

    @Override
    public Long createDgDataElem(StandardsDataElemSaveReqVO createReqVO) {
        StandardsDataMetaDO dictType = BeanUtils.toBean(createReqVO, StandardsDataMetaDO.class);
        mapper.insert(dictType);
        return dictType.getId();
    }

    @Override
    public int updateDgDataElem(StandardsDataElemSaveReqVO updateReqVO) {
        StandardsDataMetaDO updateObj = BeanUtils.toBean(updateReqVO, StandardsDataMetaDO.class);
        return mapper.updateById(updateObj);
    }

    @Override
    public int removeDgDataElem(List<Long> idList) {
        boolean exists = columnApiService.existsByDataElemIds(idList);
        if (exists) {
            throw new ServiceException("被字段元数据引用，不可删除");
        }
        return mapper.deleteBatchIds(idList);
    }

    @Override
    public StandardsDataMetaDO getDgDataElemById(Long id) {
        MPJLambdaWrapper<StandardsDataMetaDO> lambdaWrapper = new MPJLambdaWrapper();
        lambdaWrapper.selectAll(StandardsDataMetaDO.class)
                .select("t2.NAME AS catName", "t3.NICK_NAME AS personChargeName")
                .leftJoin("STD_DATA_ELEM_CAT t2 on t.CAT_CODE = t2.CODE AND t2.DEL_FLAG = '0'")
                .leftJoin("SYSTEM_USER t3 on t.PERSON_CHARGE = " + ("mysql".equals(MasterDataSourceConfig.getDatabaseType()) ? "CAST(t3.USER_ID AS CHAR)" : "CAST(t3.USER_ID AS VARCHAR)") + " AND t3.DEL_FLAG = '0'")
                .eq(StandardsDataMetaDO::getId, id);
        StandardsDataMetaDO StandardsDataElemDO = mapper.selectJoinOne(StandardsDataMetaDO.class, lambdaWrapper);
        return StandardsDataElemDO;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public Boolean updateStatus(Long id, Long status) {
        return this.update(Wrappers.lambdaUpdate(StandardsDataMetaDO.class)
                .eq(StandardsDataMetaDO::getId, id)
                .set(StandardsDataMetaDO::getStatus, status));
    }

}
