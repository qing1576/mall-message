package com.mall.service.impl;

import com.mall.model.EmailModel;
import com.mall.service.MessageService;
import com.mall.util.AsyncExecutorUtil;
import com.mall.util.GsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * <b><u>MessageServiceImpl功能说明：</u></b>
 * <p></p>
 *
 * @author
 * 2024/11/7 09:53
 */
@Service
public class MessageServiceImpl implements MessageService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MessageServiceImpl.class);

    @Value("${spring.mail.username}")
    private String fromUser;

    private JavaMailSender javaMailSender;

    @Autowired
    public void setJavaMailSender(JavaMailSender javaMailSender) {
        this.javaMailSender = javaMailSender;
    }

    @Override
    public void sendEmail(EmailModel emailModel) {
        AsyncExecutorUtil.execute(() -> {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromUser);
            message.setTo(emailModel.getToEmail());
            message.setSubject(emailModel.getSubject());
            message.setText(emailModel.getContext());
            try {
                javaMailSender.send(message);
                LOGGER.info("邮件发送成功：{}", GsonUtil.toJson(emailModel));
            } catch (Exception e) {
                LOGGER.error("邮件发送失败", e);
            }
        });
    }
}
