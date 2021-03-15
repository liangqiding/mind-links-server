package com.mind.links.netty.mqtt.mqttHandler;

import com.mind.links.netty.mqtt.common.MqttMsgTypeEnum;
import com.mind.links.netty.mqtt.mqttStore.service.impl.SessionServiceImpl;
import exception.LinksExceptionTcp;
import io.netty.channel.*;
import io.netty.handler.codec.mqtt.*;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.AttributeKey;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;


/**
 * MQTT消息处理
 *
 * @author qiding
 */
@ChannelHandler.Sharable
@Slf4j
@Component
@RequiredArgsConstructor
public class MqttBrokerHandler extends SimpleChannelInboundHandler<MqttMessage> {

    private final SessionServiceImpl sessionServiceImpl;

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, MqttMessage msg) throws Exception {
        log.debug("channelRead0:" + ctx.channel());
        sessionServiceImpl.sessionCount().subscribe(connectionCount -> log.info("===============当前总连接数========{}", connectionCount));
        Mono.just(ctx.channel())
                .filter(channel -> this.decoderResultIsSuccess(ctx, msg))
                .switchIfEmpty(LinksExceptionTcp.errors("channelRead0-解码器异常,只支持mqtt协议报文"))
                .flatMap(channel -> MqttMsgTypeEnum.msgHandler(msg.fixedHeader().messageType().value(), channel, msg))
                .onErrorResume(LinksExceptionTcp::errors)
                .subscribe(channel -> log.debug("事件{},处理完成{}", msg.fixedHeader().messageType(), channel));
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent idleStateEvent = (IdleStateEvent) evt;
            if (idleStateEvent.state() == IdleState.ALL_IDLE) {
                Channel channel = ctx.channel();
                String clientId = (String) channel.attr(AttributeKey.valueOf("clientId")).get();
                // 发送遗嘱消息
                log.info("发送遗嘱消息" + clientId);
                ctx.close();
            }
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }

    /**
     * 解码器校验
     */
    private boolean decoderResultIsSuccess(ChannelHandlerContext ctx, MqttMessage msg) {
        if (msg.decoderResult().isFailure()) {
            Throwable cause = msg.decoderResult().cause();
            if (cause instanceof MqttUnacceptableProtocolVersionException) {
                ctx.writeAndFlush(MqttMessageFactory.newMessage(
                        new MqttFixedHeader(MqttMessageType.CONNACK, false, MqttQoS.AT_MOST_ONCE, false, 0),
                        new MqttConnAckVariableHeader(MqttConnectReturnCode.CONNECTION_REFUSED_UNACCEPTABLE_PROTOCOL_VERSION, false),
                        null));
            } else if (cause instanceof MqttIdentifierRejectedException) {
                ctx.writeAndFlush(MqttMessageFactory.newMessage(
                        new MqttFixedHeader(MqttMessageType.CONNACK, false, MqttQoS.AT_MOST_ONCE, false, 0),
                        new MqttConnAckVariableHeader(MqttConnectReturnCode.CONNECTION_REFUSED_IDENTIFIER_REJECTED, false),
                        null));
            }
            return false;
        }
        return true;
    }

}
