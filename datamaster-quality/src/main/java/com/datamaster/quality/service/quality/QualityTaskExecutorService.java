

package com.datamaster.quality.service.quality;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import com.datamaster.quality.controller.quality.vo.CheckErrorDataReqDTO;
import com.datamaster.quality.controller.quality.vo.QualityRuleQueryReqDTO;
import com.datamaster.quality.controller.quality.vo.ValidationSqlResult;
import com.datamaster.quality.dal.dataobject.quality.CheckErrorData;

public interface QualityTaskExecutorService {
    public void executeTask(String taskId);


    public ValidationSqlResult generateValidationValidDataSql(QualityRuleQueryReqDTO queryReqDTO);

    public ValidationSqlResult generateValidationErrorDataSql(QualityRuleQueryReqDTO queryReqDTO);

    /**
     * 错误数据分页查询
     *
     * @param of
     * @return
     */
    Page<CheckErrorData> pageErrorData(PageRequest of, CheckErrorDataReqDTO checkErrorDataReqDTO);

    boolean updateErrorData( CheckErrorDataReqDTO checkErrorDataReqDTO);

    String generateDataCheck(QualityRuleQueryReqDTO queryReqDTO);
}
