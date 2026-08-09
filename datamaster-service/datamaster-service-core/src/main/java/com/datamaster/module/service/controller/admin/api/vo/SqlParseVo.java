

package com.datamaster.module.service.controller.admin.api.vo;

import lombok.Data;
import com.datamaster.module.service.dal.dataobject.dto.ReqParam;
import com.datamaster.module.service.dal.dataobject.dto.ResParam;

import java.io.Serializable;
import java.util.List;

@Data
public class SqlParseVo implements Serializable {

    private static final long serialVersionUID=1L;

    private List<ReqParam> reqParams;
    private List<ResParam> resParams;
}
