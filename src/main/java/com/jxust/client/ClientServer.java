package com.jxust.client;

import com.jxust.codec.FastDecoder;
import com.jxust.codec.FastEncoder;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;

import static com.jxust.protobuf.FastMessage.*;
@Service
public class ClientServer {

    private static final Logger logger = LoggerFactory.getLogger(ClientServer.class);

    @Value("${fast.server.host}")
    private String host;

    @Value("${fast.server.port:8899}")
    private int port;
    @Value("${fast.server.connect.timeout:10000}")
    private int timeout;
    @Value("${fast.server.connect.retry.time:5}")
    private int retryTime;

    private volatile int reConnectTime=0;
    private EventLoopGroup eventLoopGroup = new NioEventLoopGroup(Runtime.getRuntime().availableProcessors()*2);

    @PostConstruct
    public void start(){
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(eventLoopGroup)
                .channel(NioSocketChannel.class)
                .remoteAddress(host,port)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS,timeout);
        bootstrap.handler(new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel socketChannel) throws Exception {
                ChannelPipeline pipeline = socketChannel.pipeline();
                pipeline.addLast(new FastDecoder())
                        .addLast(new FastEncoder())
                        .addLast(new ClientServerHandler());
            }
        });

        ChannelFuture future = bootstrap.connect();

        future.addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture channelFuture) throws Exception {
                if(!channelFuture.isSuccess()) {
                    if(reConnectTime>=retryTime) {
                        logger.warn("client failed to connect to server host,reconnect time beyond max time:{}",retryTime);
                        return;
                    }

                    logger.warn("client failed to connect to server host:{},port:{},retryTime:{}",host,port,reConnectTime);
                    logger.info("after {} millis ti retry connect",getConnectInterval());
                    Thread.sleep(getConnectInterval());
                    eventLoopGroup.execute(new Runnable() {
                        @Override
                        public void run() {
                            start();
                        }
                    });
                    incrementReconnectTime();
                    return;
                }

                Channel channel = channelFuture.channel();
                logger.info("client connect to server on host:{},port:{} success!",host,port);
                resetReconnectTime();
                channel.closeFuture().addListener(new ChannelFutureListener() {
                    @Override
                    public void operationComplete(ChannelFuture channelFuture) throws Exception {
                        logger.warn("client connect to server have interrupt，start reconnect");
                        if(reConnectTime>=retryTime) {
                            logger.warn("client failed to connect to server host,reconnect time beyond max time:{}",retryTime);
                        }

                        logger.warn("client failed to connect to server host:{},port:{},retryTime:{}",host,port,reConnectTime);
                        logger.info("after {} millis ti retry connect",getConnectInterval());
                        Thread.sleep(getConnectInterval());
                        eventLoopGroup.execute(new Runnable() {
                            @Override
                            public void run() {
                                start();
                            }
                        });
                        incrementReconnectTime();
                        return;
                    }
                });


            }
        });
    }

    private synchronized void resetReconnectTime(){
        reConnectTime=0;
    }

    private synchronized void incrementReconnectTime(){
        reConnectTime++;
    }

    private int getConnectInterval(){
        return reConnectTime*1000;
    }
}
