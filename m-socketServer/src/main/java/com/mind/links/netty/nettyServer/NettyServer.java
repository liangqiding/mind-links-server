package com.mind.links.netty.nettyServer;

import annotation.Desc;
import com.mind.links.netty.nettyInitializer.NettyServerChannelInitializer;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import javax.annotation.PreDestroy;
import java.net.InetAddress;
import java.net.InetSocketAddress;

/**
 * Netty配置类
 *
 * @author qiding
 */
@Component
@Slf4j
@Desc("Netty配置类")
@RequiredArgsConstructor
public class NettyServer {

    @Desc("react模式 主线程池")
    private final EventLoopGroup BOSS = new NioEventLoopGroup();

    @Desc("IO 操作线程池")
    private final EventLoopGroup WORKER = new NioEventLoopGroup();

    @Desc("通讯模式")
    private final Class<? extends ServerChannel> COMMUNICATION_MODE = NioServerSocketChannel.class;

    @Desc("编码解码器")
    private final NettyServerChannelInitializer channelInitializer;

    @Desc("临时存放已完成三次握手的请求的队列的最大长度")
    private static final Integer BACKLOG = 128;

    @Value("${netty.port}")
    @Desc("监听端口")
    private Integer port;

    public void start() throws Exception {
        String address = InetAddress.getLocalHost().getHostAddress();
        try {
            ServerBootstrap bootstrap = new ServerBootstrap()
                    .group(BOSS, WORKER)
                    .channel(COMMUNICATION_MODE)
                    .localAddress(new InetSocketAddress(port))
                    .childHandler(channelInitializer)
                    .option(ChannelOption.SO_BACKLOG, BACKLOG)
                    // 保持长连接，2小时无数据激活心跳机制
                    .childOption(ChannelOption.SO_KEEPALIVE, true);
            // 绑定端口，开始接收进来的连接
            ChannelFuture future = bootstrap.bind().sync();
            log.info("===netty服务器开始监听端口：" + address + ":" + port+",临时存放已完成三次握手的请求的队列的最大长度"+BACKLOG);
            // 关闭channel和块，直到它被关闭
            future.channel().closeFuture().sync();
        } catch (Exception e) {
            e.printStackTrace();
            BOSS.shutdownGracefully();
            WORKER.shutdownGracefully();
        }
    }

    /**
     * 销毁
     */
    @PreDestroy
    public void destroy() {
        BOSS.shutdownGracefully().syncUninterruptibly();
        WORKER.shutdownGracefully().syncUninterruptibly();
        log.info("Close cim server success!!!");
    }
}

