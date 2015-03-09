package com.example.smtp;

import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;

import java.util.List;

/**
 * Created by guanxinquan on 15-3-9.
 */
public class SenderMailHandler extends SimpleChannelInboundHandler<String>{

    private Transaction transaction = Transaction.INIT;

    private ChannelHandlerContext ctx ;

    private static final String LINE_END = "\r\n";

    private String from;

    private List<String> rcpts;

    private String data;

    public SenderMailHandler(String from, List<String> rcpts, String data) {
        this.from = from;
        this.rcpts = rcpts;
        this.data = data;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        this.ctx = ctx;
    }

    @Override
    protected void messageReceived(ChannelHandlerContext ctx, String msg) throws Exception {

        System.err.println(msg);

        if(transaction == Transaction.INIT){
            initTransaction(msg);
        }else if(transaction == Transaction.HELO){
            heloTransaction(msg);
        }else if(transaction == Transaction.MAIL){
            mailTransaction(msg);
        }else if(transaction == Transaction.RCPT){
            rcptTransaction(msg);
        }else if(transaction == Transaction.DATA){
            dataTransaction(msg);
        }else if(transaction == Transaction.DATA_TRAN){
            dataTranTransaction(msg);
        }else if(transaction == Transaction.QUIT){
            quitTransaction(msg);
        }
    }

    private void quitTransaction(String msg) {
        if(msg.startsWith("221")){
            ctx.close();
        }else{
            resetAndFinish();
        }
    }

    private void dataTranTransaction(String msg) {
        if(msg.startsWith("250")){
            transaction = Transaction.QUIT;
            writeCommand("quit");
        }else{
            resetAndFinish();
        }
    }

    private void dataTransaction(String msg) {
        if(msg.startsWith("354")){
            transaction = Transaction.DATA_TRAN;
            ctx.write(data);
            writeCommand(".");
        }else{
            resetAndFinish();
        }
    }

    private void rcptTransaction(String msg) {
        if(msg.startsWith("250")){
            transaction = Transaction.DATA;
            writeCommand("data");
        }
    }

    private void mailTransaction(String msg) {
        if(msg.startsWith("250")){
            transaction = Transaction.RCPT;
            writeCommand("rcpt to:<"+rcpts.get(0)+">");

        }else{
            resetAndFinish();
        }
    }

    private void heloTransaction(String msg) {
        if(msg.startsWith("250 ")){//以250+sp为开始的，表示是echo的最后一行
            transaction = Transaction.MAIL;
            writeCommand("mail from:<" + from + ">");
        }else if(msg.startsWith("250-")){//以250-开始的表示后面还有额外的信息
            System.err.println(msg);
        }else{
            resetAndFinish();
        }
    }

    private void initTransaction(String msg) {
        if(msg.startsWith("220")){
            writeCommand("ehlo "+ "woshiguanxinquan@g.com");
            transaction = Transaction.HELO;
        }else{
            resetAndFinish();
        }
    }

    private void resetAndFinish() {
        ctx.write("rset"+LINE_END).addListener(ChannelFutureListener.CLOSE);
    }

    private void writeCommand(String msg){
        ctx.write(msg+LINE_END);
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.flush();
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

}
