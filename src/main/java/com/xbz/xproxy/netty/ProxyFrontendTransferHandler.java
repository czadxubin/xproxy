package com.xbz.xproxy.netty;

import com.xbz.xproxy.ProxyServerApplication;
import com.xbz.xproxy.util.HttpUtils;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import io.netty.util.AttributeKey;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.StringTokenizer;

import static com.xbz.xproxy.netty.ProxyFrontendConnectHandler.CLIENT_CTX_KEY;

public class ProxyFrontendTransferHandler extends SimpleChannelInboundHandler<ByteBuf> {

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
        ClientChannelContext clientCtx = ctx.channel().attr(CLIENT_CTX_KEY).get();
        if (clientCtx != null && clientCtx.isTargetConnected()) {
            clientCtx.forwardToTarget(msg);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        super.exceptionCaught(ctx, cause);
        System.err.println("ProxyFrontendTransferHandler...error!");
        cause.printStackTrace(System.err);
    }
}