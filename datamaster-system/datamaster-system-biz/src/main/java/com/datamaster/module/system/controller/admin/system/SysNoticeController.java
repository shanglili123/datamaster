

package com.datamaster.module.system.controller.admin.system;

import java.util.Date;
import java.util.List;

import cn.hutool.core.date.DateUtil;
import com.github.pagehelper.PageHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import com.datamaster.common.annotation.Log;
import com.datamaster.common.core.controller.BaseController;
import com.datamaster.common.core.domain.AjaxResult;
import com.datamaster.common.core.page.PageDomain;
import com.datamaster.common.core.page.TableDataInfo;
import com.datamaster.common.core.page.TableSupport;
import com.datamaster.common.enums.BusinessType;
import com.datamaster.common.utils.object.BeanUtils;
import com.datamaster.common.utils.sql.SqlUtil;
import com.datamaster.module.system.controller.admin.system.message.vo.MessagePageReqVO;
import com.datamaster.module.system.controller.admin.system.message.websocket.WebSocketMessageServer;
import com.datamaster.module.system.dal.dataobject.message.MessageDO;
import com.datamaster.module.system.dal.mapper.message.MessageTemplateMapper;
import com.datamaster.module.system.domain.SysNotice;
import com.datamaster.module.system.service.ISysNoticeService;
import com.datamaster.module.system.service.message.IMessageService;
import com.datamaster.module.system.service.message.impl.MessageServiceImpl;

import javax.annotation.Resource;

import static com.datamaster.common.utils.SecurityUtils.getLoginUser;

/**
 * 公告 信息操作处理
 *
 * @author DATAMASTER
 */
@RestController
@RequestMapping("/system/notice")
public class SysNoticeController extends BaseController
{
    @Autowired
    private ISysNoticeService noticeService;
    @Resource
    private WebSocketMessageServer webSocketMessageServer;
    @Resource
    private IMessageService messageService;

    /**
     * 获取通知公告列表
     */
    @GetMapping("/list")
    public TableDataInfo list(SysNotice notice)
    {
        startPage();
        List<SysNotice> list = noticeService.selectNoticeList(notice);
        return getDataTable(list);
    }


    /**
     * 获取通知公告列表(排序后)
     */
//    @PreAuthorize("@ss.hasPermi('system:notice:list')")
    @GetMapping("/sortList")
    public TableDataInfo sortList(SysNotice notice)
    {
        PageDomain var0 = TableSupport.buildPageRequest();
        Integer var1 = var0.getPageNum();
        Integer var2 = var0.getPageSize();
        String var3 = SqlUtil.escapeOrderBySql("top_flag desc, create_time desc");
        Boolean var4 = var0.getReasonable();
        PageHelper.startPage(var1, var2, var3).setReasonable(var4);
        List<SysNotice> list = noticeService.selectNoticeList(notice);
        return getDataTable(list);
    }


    /**
     * 获取弹框公告
     * @return
     */
    @GetMapping("/alertNotice")
    public AjaxResult alertNotice()
    {
        SysNotice noticeQo = new SysNotice();
        noticeQo.setAlertFlag(1);
        noticeQo.setStatus("1");
        noticeQo.getParams().put("efftectTime", DateUtil.now());
        List<SysNotice> list = noticeService.selectNoticeList(noticeQo);
        if (list.size() > 0){
            return success(list.get(0));
        }
        return success();
    }


    /**
     * 根据通知公告编号获取详细信息
     */
    @PreAuthorize("@ss.hasPermi('system:notice:query')")
    @GetMapping(value = "/one")
    public AjaxResult getInfo(@RequestParam Long noticeId)
    {
        return success(noticeService.selectNoticeById(noticeId));
    }
    /**
     * 新增通知公告
     */
    @PreAuthorize("@ss.hasPermi('system:notice:add')")
    @Log(title = "通知公告", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody SysNotice notice)
    {
        //测试 消息通知
        MessagePageReqVO messagePageReqVO = new MessagePageReqVO();
        messagePageReqVO.setContent(notice.getNoticeContent());
        messagePageReqVO.setTitle(notice.getNoticeTitle());
        messagePageReqVO.setEntityType(Integer.valueOf(notice.getNoticeType()));
        messagePageReqVO.setCreateTime(new Date());
        webSocketMessageServer.broadcastMessage(messagePageReqVO);
        notice.setCreateBy(getUsername());

        return toAjax(noticeService.insertNotice(notice));
    }

    /**
     * 修改通知公告
     */
    @PreAuthorize("@ss.hasPermi('system:notice:edit')")
    @Log(title = "通知公告", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody SysNotice notice)
    {
        notice.setUpdateBy(getUsername());
        MessagePageReqVO messagePageReqVO = new MessagePageReqVO();
        messagePageReqVO.setContent(notice.getNoticeContent());
        messagePageReqVO.setTitle(notice.getNoticeTitle());
        messagePageReqVO.setEntityType(Integer.valueOf(notice.getNoticeType()));
        messagePageReqVO.setCreateTime(new Date());
        webSocketMessageServer.broadcastMessage(messagePageReqVO);

        MessageDO messageDO = new MessageDO();
        // 设置模版基本数据
        messageDO.setCategory(Integer.valueOf(0));
        messageDO.setMsgLevel(Integer.valueOf(0));
        messageDO.setTitle("测试");
        // 实际消息
        messageDO.setContent("测试内容");

//        messageDO.setCreatorId(getLoginUser().getUserId());
//        messageDO.setCreateBy(getLoginUser().getUser().getNickName());
        boolean save = messageService.save(messageDO);

        return toAjax(noticeService.updateNotice(notice));
    }

    /**
     * 删除通知公告
     */
    @PreAuthorize("@ss.hasPermi('system:notice:remove')")
    @Log(title = "通知公告", businessType = BusinessType.DELETE)
    @DeleteMapping("/{noticeIds}")
    public AjaxResult remove(@PathVariable Long[] noticeIds)
    {
        return toAjax(noticeService.deleteNoticeByIds(noticeIds));
    }
}
