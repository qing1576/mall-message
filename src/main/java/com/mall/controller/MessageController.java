package com.mall.controller;

import com.mall.model.EmailModel;
import com.mall.service.MessageService;
import com.mall.util.GsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * <b><u>MessageController功能说明：</u></b>
 * <p></p>
 *
 * @author
 * 2024/11/7 09:39
 */
@RestController
@RequestMapping("/message")
public class MessageController {

    private static final Logger LOGGER = LoggerFactory.getLogger(MessageController.class);

    private MessageService messageService;

    @Autowired
    public void setMessageService(MessageService messageService) {
        this.messageService = messageService;
    }

    @PostMapping("/sendEmail")
    public void sendEmail(@RequestBody EmailModel emailModel) {
        LOGGER.info("开始发送邮件：{}", GsonUtil.toJson(emailModel));
        messageService.sendEmail(emailModel);
        LOGGER.info("邮件发送成功！");
    }
}
