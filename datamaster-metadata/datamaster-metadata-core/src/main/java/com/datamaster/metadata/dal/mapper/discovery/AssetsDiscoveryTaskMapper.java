package com.datamaster.metadata.dal.mapper.discovery;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.github.yulichang.wrapper.MPJLambdaWrapper;
import org.apache.commons.lang3.StringUtils;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.metadata.controller.discovery.vo.AssetsDiscoveryTaskPageReqVO;
import com.datamaster.metadata.dal.dataobject.discovery.AssetsDiscoveryTaskDO;
import com.datamaster.mybatis.core.mapper.BaseMapperX;

import java.util.Arrays;

/**
 * Mapper
 *
 * @author DATAMASTER
 * @date 2025-02-11
 */
public interface AssetsDiscoveryTaskMapper extends BaseMapperX<AssetsDiscoveryTaskDO> {

    default PageResult<AssetsDiscoveryTaskDO> selectPage(AssetsDiscoveryTaskPageReqVO reqVO) {
        MPJLambdaWrapper<AssetsDiscoveryTaskDO> lambdaWrapper = new MPJLambdaWrapper();
        lambdaWrapper.selectAll(AssetsDiscoveryTaskDO.class)
                .select("t2.NAME AS catName")
                .leftJoin("TAX_CATEGORY t2 on t.CAT_CODE = t2.CODE AND t2.DEL_FLAG = '0' AND t2.CAT_TYPE = 'TASK'")
                .eq(reqVO.getDatasourceId() != null, AssetsDiscoveryTaskDO::getDatasourceId, reqVO.getDatasourceId())
                .like(StringUtils.isNotBlank(reqVO.getName()), AssetsDiscoveryTaskDO::getName, reqVO.getName())
                .likeRight(StringUtils.isNotBlank(reqVO.getCatCode()), AssetsDiscoveryTaskDO::getCatCode, reqVO.getCatCode())
                .eq(StringUtils.isNotBlank(reqVO.getStatus()), AssetsDiscoveryTaskDO::getStatus, reqVO.getStatus())
                .eq(reqVO.getContactId() != null, AssetsDiscoveryTaskDO::getContactId, reqVO.getContactId())
                .orderByStr(StringUtils.isNotBlank(reqVO.getOrderByColumn()), StringUtils.equals("asc", reqVO.getIsAsc()), StringUtils.isNotBlank(reqVO.getOrderByColumn()) ? Arrays.asList(reqVO.getOrderByColumn().split(",")) : null);
        if (StringUtils.isBlank(reqVO.getOrderByColumn())) {
            lambdaWrapper.orderByDesc(AssetsDiscoveryTaskDO::getLastExecuteTime)
                    .orderByDesc(AssetsDiscoveryTaskDO::getUpdateTime)
                    .orderByDesc(AssetsDiscoveryTaskDO::getId);
        }

        return selectJoinPage(reqVO, AssetsDiscoveryTaskDO.class, lambdaWrapper);

    }

    /**
     *  CAT_CODE  CAT_CODE
     *
     * @param oldCatCode
     * @param newCatCode
     * @return
     */
    default int updateCatCode(String oldCatCode, String newCatCode) {
        return this.update(
                null,
                Wrappers.<AssetsDiscoveryTaskDO>lambdaUpdate()
                        .set(AssetsDiscoveryTaskDO::getCatCode, newCatCode)
                        .eq(AssetsDiscoveryTaskDO::getDelFlag, "0")
                        .eq(AssetsDiscoveryTaskDO::getCatCode, oldCatCode)
        );
    }
}
