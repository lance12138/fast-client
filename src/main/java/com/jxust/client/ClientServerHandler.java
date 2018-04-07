package com.jxust.client;

import com.jxust.protobuf.FastMessage;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClientServerHandler extends SimpleChannelInboundHandler<Object> {

    private static final Logger logger = LoggerFactory.getLogger(ClientServerHandler.class);

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {

        if(msg instanceof FastMessage.FastRes) {
         logger.info("get server info:{}",msg);
        }
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        FastMessage.LoginReq.Builder builder = FastMessage.LoginReq.newBuilder();
        builder.setChatId("547773097");
        builder.setPassword("123456");
        ctx.channel().writeAndFlush(builder.build());
    }

}
