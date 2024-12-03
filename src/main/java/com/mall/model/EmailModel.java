package com.mall.model;

/**
 * <b><u>EmailModel功能说明：</u></b>
 * <p>发送邮件对象</p>
 * @author
 * 2024/11/7 09:46
 */
public class EmailModel {

    /**
     * 收件人
     */
    private String toEmail;

    /**
     * 标题
     */
    private String subject;

    /**
     * 内容
     */
    private String context;

    public String getToEmail() {
        return toEmail;
    }

    public void setToEmail(String toEmail) {
        this.toEmail = toEmail;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getContext() {
        return context;
    }

    public void setContext(String context) {
        this.context = context;
    }
}
