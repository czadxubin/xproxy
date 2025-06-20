package com.xbz.xproxy.netty;

import com.xbz.xproxy.util.ProxyUtils;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

public class ProxyBackendHandler extends SimpleChannelInboundHandler<ByteBuf> {

    private final Channel inboundChannel;

    public ProxyBackendHandler(Channel inboundChannel) {
        this.inboundChannel = inboundChannel;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) {
        inboundChannel.writeAndFlush(msg.retain());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        ProxyUtils.closeOnFlush(inboundChannel);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        System.out.println("ProxyBackend Error");
        cause.printStackTrace();
        ProxyUtils.closeOnFlush(ctx.channel());
    }
}