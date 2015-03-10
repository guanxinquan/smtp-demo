package com.example.pop3;

import com.example.mapper.MailBoxMapper;
import com.example.mapper.MailMapper;
import com.example.mapper.UserMapper;
import com.example.model.MailBoxModel;
import com.example.model.MailModel;
import com.example.model.UserModel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.StringBufferInputStream;
import java.io.StringReader;
import java.util.List;

/**
 * Created by guanxinquan on 15-3-10.
 */
public class POP3ServerHandler extends SimpleChannelInboundHandler<String>{


    private static final String hello = "+OK Welcome to coremail Mail Pop3 Server (examplecoms)";

    private static final String LINE_END = "\r\n";

    private Transaction transaction;

    private ChannelHandlerContext ctx;

    private String user;

    private String password;

    private List<MailModel> mails;

    private Long totolSize = 0l;

    private UserModel userModel;

    private MailBoxModel mailBoxModel;

    private static ApplicationContext context;

    private static MailBoxMapper mailBoxMapper;

    private static MailMapper mailMapper;

    private static UserMapper userMapper;

    private static final String[] capa = {"TOP","USER","UIDL"};

    static {
        context = new ClassPathXmlApplicationContext("spring-context.xml");
        mailBoxMapper = context.getBean(MailBoxMapper.class);
        mailMapper = context.getBean(MailMapper.class);
        userMapper = context.getBean(UserMapper.class);
    }


    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        this.ctx = ctx;
        ctx.writeAndFlush(hello+LINE_END);
        transaction = Transaction.INIT;
    }

    @Override
    protected void messageReceived(ChannelHandlerContext ctx, String msg) throws Exception {
        if(msg.toUpperCase().startsWith("CAPA")){
            writeResponse("+OK Capability list follows");
            for(String s :capa){
                writeResponse(s);
            }
            writeResponse(".");

        }else if(transaction == Transaction.INIT && msg.toUpperCase().startsWith("USER")){
            if(msg.split(" ").length ==2 ){
                user = msg.split(" ")[1];
                transaction = Transaction.USER;
                writeResponse("+OK core mail");
            }
        }else if(transaction == Transaction.USER && msg.toUpperCase().startsWith("PASS")){
            if(msg.split(" ").length == 2){
                password = msg.split(" ")[1];

                if(doLogin(user,password)) {
                    writeResponse("+OK "+mails.size()+" message(s) ["+totolSize+" byte(s)]");
                    transaction = Transaction.TRANSACTION;
                }else{
                    ctx.write("-ERR Unable to log on"+LINE_END).addListener(ChannelFutureListener.CLOSE);
                }
            }
        }else if(transaction == Transaction.TRANSACTION){
            doInTransaction(msg);
        }
    }

    private boolean doLogin(String user, String password) {
        userModel = userMapper.selectUserByName(user);

        if(userModel == null ||!userModel.getPassword().equals(password)){
            return false;
        }

        mailBoxModel = mailBoxMapper.selectMailbox("INBOX", userModel.getId());



        mails = mailMapper.selectMails(mailBoxModel.getId());

        for(MailModel mm : mails){
            totolSize += mm.getSize();
        }

        return true;
    }

    private void doInTransaction(String msg) {
        if(msg.toUpperCase().startsWith("STAT")){
            writeResponse("+OK "+mails.size()+" "+totolSize);
        }else if(msg.toUpperCase().startsWith("LIST")){
            String[] args = msg.split(" ");
            if(args.length == 1){
                writeResponse("+OK "+mails.size()+" messages ("+totolSize+" octets)");
                for(int i = 0 ; i < mails.size();i++){
                    writeResponse((i+1)+" "+mails.get(i).getSize());
                }
                writeResponse(".");
            }else{
                Integer index = Integer.valueOf(args[1]);
                if(index -1 < mails.size()){
                    writeResponse("+OK "+index+" " + mails.get(index-1).getSize());
                }else{
                    writeResponse("-ERR no such message, only "+mails.size()+" messages in maildrop");
                }
            }
        }else if(msg.toUpperCase().startsWith("RETR")){
            Integer index = Integer.valueOf(msg.split(" ")[1]);
            writeResponse("+OK "+mails.get(index -1).getSize());
            MailModel mail = mailMapper.selectMailById(mails.get(index -1).getId());
            writeResponse(mail.getContent());
            writeResponse(".");
        }else if(msg.toUpperCase().startsWith("DELE")){
            Integer index = Integer.valueOf(msg.split(" ")[1]);
            if(index -1 < mails.size()) {
                writeResponse("+OK message " + index + " deleted");
            }else{
                writeResponse("-ERR message already deleted");
            }
        }else if(msg.toUpperCase().startsWith("RSET")){
            writeResponse("+OK maildrop has "+mails.size()+" messages ("+totolSize+" octets)");
        }else if(msg.toUpperCase().startsWith("NOOP")){
            writeResponse("+OK");
        }else if(msg.toUpperCase().equals("QUIT")){
            ctx.write("+OK dewey POP3 server signing off (maildrop empty)"+LINE_END).addListener(ChannelFutureListener.CLOSE);
        }else if(msg.toUpperCase().startsWith("TOP")){
            String[] args = msg.split(" ");
            int index = Integer.valueOf(args[1]) - 1;
            int length = Integer.valueOf(args[2]);

            MailModel mailModel = mailMapper.selectMailById(mails.get(index).getId());
            StringReader reader = new StringReader(mailModel.getContent());
            String line = null;
            BufferedReader br= new BufferedReader(reader);
            int contentReadLength = -1;
            try {
                while ((line = br.readLine()) != null){
                    if(contentReadLength == -1 && "".equals(line)){
                        writeResponse(line);
                        contentReadLength = 0;
                    }else if(contentReadLength == -1){
                        writeResponse(line);
                    }else if(contentReadLength < length){
                        writeResponse(line);
                        contentReadLength++;
                    }else{
                        writeResponse(".");
                        break;
                    }
                }
            }catch (Exception e){

            }
        }else if(msg.toUpperCase().startsWith("UIDL")){
            String[] args = msg.split(" ");
            if(args.length == 1){
                writeResponse("+OK");

                for(int i = 0 ; i < mails.size() ; i++){
                    MailModel m = mails.get(i);
                    writeResponse(i+" "+m.getId());
                }
            }else{
                int index = Integer.valueOf(args[1]);
                if(index < mails.size()){
                    writeResponse("+OK "+index + " "+mails.get(index-1).getId());
                }else{
                    writeResponse("-ERR no such message, only "+mails.size()+" messages in maildrop");
                }
            }
        } else{
            writeResponse("-ERR unsupport command");
        }


    }


    private void writeResponse(String msg){
        ctx.write(msg+LINE_END);
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.flush();
    }
}

enum Transaction{
    INIT,USER,TRANSACTION
}

