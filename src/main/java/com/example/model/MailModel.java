package com.example.model;

/**
 * Created by guanxinquan on 15-3-9.
 */
public class MailModel {

    private Integer id;

    private Integer size;

    private Integer mailboxId;

    private String content;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getSize() {
        return size;
    }

    public void setSize(Integer size) {
        this.size = size;
    }

    public Integer getMailboxId() {
        return mailboxId;
    }

    public void setMailboxId(Integer mailboxId) {
        this.mailboxId = mailboxId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
