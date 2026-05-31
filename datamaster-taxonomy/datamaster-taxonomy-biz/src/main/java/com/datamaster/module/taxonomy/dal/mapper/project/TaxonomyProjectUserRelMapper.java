

package com.datamaster.module.taxonomy.dal.mapper.project;

import com.github.yulichang.wrapper.MPJLambdaWrapper;
import org.apache.commons.lang3.StringUtils;
import com.datamaster.common.core.page.PageResult;
import com.datamaster.module.taxonomy.controller.admin.project.vo.TaxonomyProjectUserRelPageReqVO;
import com.datamaster.module.taxonomy.dal.dataobject.project.TaxonomyProjectUserRelDO;
import com.datamaster.mybatis.config.MasterDataSourceConfig;
import com.datamaster.mybatis.core.mapper.BaseMapperX;

import java.util.Arrays;

/**
 * 项目与用户关联关系Mapper接口
 *
 * @author DATAMASTER
 * @date 2025-02-11
 */
public interface TaxonomyProjectUserRelMapper extends BaseMapperX<TaxonomyProjectUserRelDO> {

    default PageResult<TaxonomyProjectUserRelDO> selectPage(TaxonomyProjectUserRelPageReqVO reqVO) {
        // 定义排序的字段（防止 SQL 注入，与数据库字段名称一致）
        MPJLambdaWrapper<TaxonomyProjectUserRelDO> lambdaWrapper = new MPJLambdaWrapper();
        String sql = "";
	        if(StringUtils.equals("mysql", MasterDataSourceConfig.getDatabaseType())){
	            sql = "(SELECT GROUP_CONCAT(r.ROLE_NAME) AS roleStr FROM system_role r JOIN system_user_role ur on r.role_id = ur.role_id WHERE u.user_id = ur.user_id" +
	                    " and r.PROJECT_ID = '"+reqVO.getProjectId() +"'"+
	                    ") AS roleStr";
	        }else if(StringUtils.equals("dm8",MasterDataSourceConfig.getDatabaseType())){
	            sql = "(SELECT WM_CONCAT(r.ROLE_NAME) AS roleStr FROM system_role r JOIN system_user_role ur on r.role_id = ur.role_id WHERE u.user_id = ur.user_id" +
	                    " and r.PROJECT_ID = '"+reqVO.getProjectId() +"'"+
	                    ") AS roleStr";
	        }else{
	            sql = "(SELECT string_agg(r.ROLE_NAME, ',') AS roleStr FROM system_role r JOIN system_user_role ur on r.role_id = ur.role_id WHERE u.user_id = ur.user_id" +
	                    " and r.PROJECT_ID = '"+reqVO.getProjectId() +"'"+
	                    ") AS roleStr";
	        }
        lambdaWrapper.selectAll(TaxonomyProjectUserRelDO.class)
                .select(sql)
                .select("u.nick_name AS nickName, u.user_name AS userName , u.phonenumber AS phoneNumber, u.status AS status ,d.dept_name AS deptName")
	                .innerJoin("SYSTEM_USER u on t.user_id = u.user_id AND u.del_flag = '0'")
	                .leftJoin("SYSTEM_DEPT d on u.dept_id = d.dept_id AND d.del_flag = '0'")
                .eq(reqVO.getProjectId() != null, TaxonomyProjectUserRelDO::getProjectId, reqVO.getProjectId())
                .eq(reqVO.getUserId() != null, TaxonomyProjectUserRelDO::getUserId, reqVO.getUserId())
                .like(StringUtils.isNotBlank(reqVO.getUserName()), "u.user_name", reqVO.getUserName())
                .like(StringUtils.isNotBlank(reqVO.getNickName()), "u.nick_name", reqVO.getNickName())
                .like(StringUtils.isNotBlank(reqVO.getPhoneNumber()), "u.phonenumber", reqVO.getPhoneNumber())
                .between(reqVO.getStartTime() != null && reqVO.getEndTime() != null,
                        TaxonomyProjectUserRelDO::getCreateTime, reqVO.getStartTime(), reqVO.getEndTime())
                .orderByStr(StringUtils.isNotBlank(reqVO.getOrderByColumn()),
                        StringUtils.equals("asc", reqVO.getIsAsc()),
                        StringUtils.isNotBlank(reqVO.getOrderByColumn())
                                ? Arrays.asList(reqVO.getOrderByColumn().split(","))
                                : null);

        return selectJoinPage(reqVO, TaxonomyProjectUserRelDO.class, lambdaWrapper);
    }
}
