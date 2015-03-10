package com.example.smtp;

import com.example.mapper.MailBoxMapper;
import com.example.mapper.MailMapper;
import com.example.mapper.UserMapper;
import com.example.model.MailBoxModel;
import com.example.model.UserModel;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.util.List;

/**
 * Created by guanxinquan on 15-3-9.
 */
public class SMTPTransfer {

    private static  ApplicationContext context;

    private static MailBoxMapper mailBoxMapper;

    private static MailMapper mailMapper;

    private static UserMapper userMapper;
    static {
        context = new ClassPathXmlApplicationContext("spring-context.xml");
        mailBoxMapper = context.getBean(MailBoxMapper.class);
        mailMapper = context.getBean(MailMapper.class);
        userMapper = context.getBean(UserMapper.class);
    }
    public void saveMessage(String from,List<String> rcpts,String data){
        UserModel userModel = userMapper.selectUserByName(from);
        if(userModel != null){
            saveMessageToSender(userModel.getId(),data);
        }
        saveMessageToReceiver(rcpts, data);
        //first save message to user's Send
        //MailBoxModel model = mailBoxMapper.selectMailbox("sender",)
    }

    private void saveMessageToReceiver(List<String> rcpts, String data) {
        for(String rcpt: rcpts){
            saveMessageToReceiver(rcpt, data);
        }

    }

    private void saveMessageToReceiver(String rcpt,String data){
        UserModel user = userMapper.selectUserByName(rcpt);
        if(user != null){
            MailBoxModel inbox = getMailBox(user.getId(),"inbox");
            mailMapper.createMail(inbox.getId(),data.length(),data);

        }else{
            return;
        }
    }

    private void saveMessageToSender(Integer userId ,String data) {
        MailBoxModel model = getMailBox(userId,"sender");
        mailMapper.createMail(model.getId(),data.length(),data);
    }

    private MailBoxModel getMailBox(Integer userId,String mailBoxName){
        MailBoxModel model = mailBoxMapper.selectMailbox(mailBoxName, userId);
        if(model == null){
            mailBoxMapper.createMailbox(mailBoxName,userId);
            model = mailBoxMapper.selectMailbox(mailBoxName,userId);
        }
        return model;
    }
}
