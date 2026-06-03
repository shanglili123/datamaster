

package com.datamaster.quality.controller.da;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.datamaster.common.core.domain.AjaxResult;
import com.datamaster.common.enums.ExecuteType;
import com.datamaster.quality.dal.dataobject.asset.AssetDO;
import com.datamaster.quality.service.asset.IAssetService;
import com.datamaster.redis.service.IRedisService;

import javax.annotation.Resource;
import java.util.List;

/**
 * <P>
 * 用途:
 * </p>
 *
 * @author: FXB
 * @create: 2025-07-17 10:30
 **/
@RestController
@RequestMapping("/test")
public class TestController {

    @Autowired
    private IRedisService redisService;

    @Resource
    private IAssetService assetService;

    @PostMapping("/test2")
    public AjaxResult test2() {
        redisService.set("test", "1", 1200);
        return AjaxResult.success("测试成功>>>>" + redisService.get("test"));
    }

    @PostMapping("/test3")
    public AjaxResult test3() {
        List<AssetDO> list = assetService.list(Wrappers.lambdaQuery(AssetDO.class)
                .eq(AssetDO::getId, "198"));
        return AjaxResult.success(list);
    }
}
