package com.example.smtp;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoop;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoop;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.timeout.IdleStateHandler;

import java.util.List;

/**
 * Created by guanxinquan on 15-3-9.
 */
public class SenderMail {

    public void sendMail(final String from, final List<String> rcpts,String domain,final String body){
        String domainMX = "163mx01.mxmail.netease.com";


        EventLoopGroup g = new NioEventLoopGroup();
        try {
            Bootstrap b = new Bootstrap();
            b.group(g);
            b.channel(NioSocketChannel.class);
            b.handler(new ChannelInitializer<NioSocketChannel>(){

                @Override
                protected void initChannel(NioSocketChannel ch) throws Exception {
                    ChannelPipeline pipeline = ch.pipeline();
                    pipeline.addLast(new DelimiterBasedFrameDecoder(8192, Unpooled.wrappedBuffer(new byte[]{'\r', '\n'})));
                    pipeline.addLast(new StringEncoder());
                    pipeline.addLast(new StringDecoder());
                    pipeline.addLast(new IdleStateHandler(10, 10, 10));
                    pipeline.addLast(new SenderMailHandler(from,rcpts,body));
                }
            });

            b.connect(domainMX/*"localhost"*/, 25).sync().channel().closeFuture().sync();
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            g.shutdownGracefully();
        }
    }



}
