package com.xbz.xproxy.util;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpResponseStatus;

import java.nio.charset.StandardCharsets;

public class HttpUtils {
    public static void sendError(ChannelHandlerContext ctx, HttpResponseStatus status) {
        String response = "HTTP/1.1 " + status.code() + "\r\n" +
                "Content-Length: 0\r\n" +
                "Connection: close\r\n\r\n";
        ctx.writeAndFlush(Unpooled.copiedBuffer(response, StandardCharsets.US_ASCII));
        ctx.close();
    }
}
