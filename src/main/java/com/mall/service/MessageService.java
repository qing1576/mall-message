package com.mall.service;

import com.mall.model.EmailModel;

/**
 * <b><u>MessageService功能说明：</u></b>
 * <p></p>
 * @author
 * 2024/11/7 09:52
 */
public interface MessageService {

    /**
     * 发送邮件
     * @param emailModel    邮件对象
     */
    public void sendEmail(EmailModel emailModel);
}
