package com.mind.links.netty.mqtt.mqttServer;

import com.alibaba.fastjson.JSON;
import com.mind.links.netty.mqtt.mqttInitializer.MqttServerChannelInitializer;
import com.mind.links.netty.mqtt.config.BrokerProperties;
import com.mind.links.netty.mqtt.mqttStore.cache.DestroyCache;
import com.mind.links.netty.mqtt.mqttStore.cache.MsgCache;
import com.mind.links.netty.mqtt.mqttStore.service.impl.SessionServiceImpl;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.net.InetAddress;
import java.net.InetSocketAddress;

/**
 * 启动Broker
 *
 * @author qidingliang
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class MqttBrokerServer implements IMqttBrokerServer {

    private final BrokerProperties brokerProperties;

    private final MqttServerChannelInitializer mqttServerChannelInitializer;

    private final DestroyCache destroyCache;;

    private EventLoopGroup bossGroup;

    private EventLoopGroup workerGroup;



    @Override
    public void start() throws Exception {
        log.info("Initializing {} MQTT Broker ...", "[" + JSON.toJSONString(brokerProperties) + "]");
        bossGroup = brokerProperties.getUseEpoll() ? new EpollEventLoopGroup() : new NioEventLoopGroup();
        workerGroup = brokerProperties.getUseEpoll() ? new EpollEventLoopGroup() : new NioEventLoopGroup();
        mqttServer();
    }

    @Override
    public void mqttServer() throws Exception {
        String address = InetAddress.getLocalHost().getHostAddress();
        try {
            ServerBootstrap bootstrap = new ServerBootstrap()
                    .group(bossGroup, workerGroup)
                    .channel(brokerProperties.getUseEpoll() ? EpollServerSocketChannel.class : NioServerSocketChannel.class)
                    .localAddress(new InetSocketAddress(brokerProperties.getPort()))
                    .childHandler(mqttServerChannelInitializer)
                    .option(ChannelOption.SO_BACKLOG, brokerProperties.getSoBacklog())
                    // 保持长连接，2小时无数据激活心跳机制
                    .childOption(ChannelOption.SO_KEEPALIVE, true);
            // 绑定端口，开始接收进来的连接
            ChannelFuture future = bootstrap.bind().sync();
            log.info("===netty服务器开始监听端口：" + address + ":" + brokerProperties.getPort());
            // 关闭channel和块，直到它被关闭
            future.channel().closeFuture().sync();
        } catch (Exception e) {
            e.printStackTrace();
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

    /**
     * 重启程序先清空该brokerId下的旧缓存
     */
    @PostConstruct
    public void init() {
        destroyCache
                .deleteAllCacheForBroker()
                .subscribe();
    }

    /**
     * 销毁 不做缓存清空，选择在程序重新启动时清空
     */
    @Override
    @PreDestroy
    public void destroy() {
        bossGroup.shutdownGracefully();
        workerGroup.shutdownGracefully();
        log.warn("Shutdown succeed 已释放brokerId为 {} 的所有缓存  ...", "[" + brokerProperties.getId() + "]");
    }

}
