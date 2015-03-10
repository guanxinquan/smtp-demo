package com.example.smtp;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import org.apache.james.mime4j.dom.*;
import org.apache.james.mime4j.message.DefaultMessageBuilder;
import org.apache.james.mime4j.message.MessageImpl;
import org.apache.james.mime4j.message.SimpleContentHandler;
import org.springframework.mail.MailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;

import javax.activation.MimetypesFileTypeMap;
import javax.mail.BodyPart;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by guanxinquan on 15-3-5.
 */
public class SMTPServerHandler extends SimpleChannelInboundHandler<String>{

    private static final String[] ehlos = {"250 mail"/*,"250-SIZE 73400320"*/};

    private static final int DEFAULT_MAIL_MAX_SIZE = 73400320;

    private static final SMTPTransfer transfer = new SMTPTransfer();

    private String reversePath = "";

    private List<String> forwardPaths;

    private StringBuilder data;

    boolean isData;

    boolean isHello = false;

    Transaction transaction = Transaction.INIT;

    private static final String LINE_END = "\r\n";

    private ChannelHandlerContext ctx;

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        this.ctx = ctx;
        writeResponse("220 example.com Anti-spam GT for Coremail System (examplecom[20140526])");
        resetSession();
        ctx.flush();
    }

    @Override
    protected void messageReceived(ChannelHandlerContext ctx, String msg) throws Exception {
        String command = null;

        System.err.println(msg);

        if(transaction != Transaction.DATA) {

            if ("".equals(msg) || msg.length() < 4) {
                writeResponse("500 Error: bad syntax");
            }
            command = msg.substring(0, 4).toUpperCase();
            if ("HELO".equals(command) || "EHLO".equals(command)) {
                for (String ehlo : ehlos) {
                    writeResponse(ehlo);
                }
                isHello = true;
            } else if ("NOOP".equals(command)) {
                writeResponse("250 OK");
            }else if ("RSET".equals(command)) {
                rsetCommand();
            }else if ("QUIT".equals(command)) {
                ChannelFuture future = ctx.write("221 Bye" + LINE_END);
                future.addListener(ChannelFutureListener.CLOSE);
            }else if ("SIZE".equals(command)) {
                System.err.println("size command");
            }else {
                if(!isHello){
                    writeResponse("503 Error: send HELO/EHLO first");
                    return;
                }
                if ("MAIL".equals(command)) {
                    mailCommand(msg);

                } else if ("RCPT".equals(command)) {
                    rcptCommand(msg);
                } else if ("DATA".equals(command)) {
                    if(transaction != Transaction.RCPT){
                        writeResponse("503 bad sequence of commands");
                        return;
                    }else {
                        transaction = Transaction.DATA;
                        writeResponse("354 End data with <CR><LF>.<CR><LF>");
                    }
                }  else if ("VERY".equals(command)) {

                }  else {
                    writeResponse("500 Error: bad syntax");
                }
            }
        }else{
            dataCommand(msg);
        }
    }

    private void dataCommand(String msg){

        if("".equals(msg)){
            data.append(LINE_END);
        }else if(msg.startsWith(".")){
            if(msg.length() == 1){
                isData = false;
                dataFinish();
            }else{
                data.append(msg);
                data.append(LINE_END);
            }
        }else{
            data.append(msg);
            data.append(LINE_END);
        }
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.flush();
    }

    private void rsetCommand() {
        resetSession();
        writeResponse("250 OK");
    }

    private void dataFinish() {
        System.out.println("reversePath: " + reversePath);
        for(String rcpt:forwardPaths){
            System.out.println("forwordPath: "+rcpt);
        }

        System.err.println(data.toString());
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        ByteArrayInputStream inputStream = new ByteArrayInputStream(data.toString().getBytes());

        //System.out.println(mimeMessage.getAllHeaders());

        writeResponse("250 Mail OK queued as mx14,QMCowED5_0Vx1fZU+qpsDA--.663S2 1425462770");


        transfer.saveMessage(reversePath,forwardPaths,data.toString());

//        SenderMail senderMail = new SenderMail();
//        senderMail.sendMail(reversePath,forwardPaths,"localhost",data.toString());

        //MimetypesFileTypeMap mimetypesFileTypeMap = new MimetypesFileTypeMap(inputStream);

        //mimetypesFileTypeMap.


//        MessageBuilder messageBuilder = new DefaultMessageBuilder();
//        Message message = null; //= new MessageImpl(inputStream);
//        try {
//           message =  messageBuilder.parseMessage(inputStream);
//        }catch (Exception e){
//
//        }
//
//        boolean isMultipart = message.isMultipart();
//        Body body = message.getBody();
//
//        if(body instanceof TextBody){
//            TextBody tbody = (TextBody) body;
//            try {
//                tbody.writeTo(System.out);
//            }catch (Exception e){
//                e.printStackTrace();
//            }
//        }
//
//        if(body instanceof Multipart){
//            Multipart mbody = (Multipart) body;
//
//            List<Entity> entities = mbody.getBodyParts();
//
//            for(Entity e : entities){
//                String dispositionType = e.getDispositionType();
//                Body eBody = e.getBody();
//
//
//            }
//        }


        resetSession();

    }

    private void rcptCommand(String msg) {
        if(transaction != Transaction.MAIL && transaction != Transaction.RCPT){
            writeResponse("503 bad sequence of commands");
        }else {
            String rcptAddress = getMailAddress(msg);
            forwardPaths.add(rcptAddress);
            transaction = Transaction.RCPT;

            writeResponse("250 Mail OK");
        }
    }

    private void mailCommand(String msg) {

        reversePath = getMailAddress(msg);

        int begin = 0;
        int end = 0;
        begin = msg.indexOf("size=");
        if(begin > 0) {
            end = msg.indexOf(' ', begin);
            String paramSize = msg.substring(begin,end);
            if(Integer.valueOf(paramSize) > DEFAULT_MAIL_MAX_SIZE){
                writeResponse("552 Requested mail action aborted: exceeded mailsize limit");
                return;
            }
        }
        transaction = Transaction.MAIL;
        ///ctx.write("250 Mail OK\n\r");
        writeResponse("250 Mail OK");
    }

    private String getMailAddress(String msg){

        int begin = msg.indexOf('<');
        int end = msg.indexOf('>');
        if(begin > 0 && end > 0)
            return msg.substring(begin+1,end);
        else
            return null;
    }

    private void resetSession(){
        String reversePath = null;
        forwardPaths = new ArrayList<String>();
        data = new StringBuilder();
        isData = false;
        transaction = Transaction.INIT;
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if(evt instanceof IdleStateEvent){
            IdleStateEvent e = (IdleStateEvent) evt;
            if(e.state() == IdleState.READER_IDLE){
                ctx.close();
            }else if(e.state() == IdleState.WRITER_IDLE){

            }else if(e.state() == IdleState.ALL_IDLE){

            }
        }
    }

    private void writeResponse(String msg){
        ctx.write(msg+LINE_END);
    }

}
