package com.xbz.xproxy.netty;

import com.xbz.xproxy.ProxyServerApplication;
import com.xbz.xproxy.pojo.DomainIPInfo;
import com.xbz.xproxy.util.HttpUtils;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import io.netty.util.AttributeKey;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.StringTokenizer;

import static com.xbz.xproxy.DomainIpConvertor.getDomainIPBy;
import static com.xbz.xproxy.DomainIpConvertor.getRealAvailableIpInfo;

public class ProxyFrontendConnectHandler extends SimpleChannelInboundHandler<HttpObject> {

    public static final AttributeKey<ClientChannelContext> CLIENT_CTX_KEY =
            AttributeKey.newInstance("clientChannelContext");

    // 当前是否是本地请求
    private boolean isLocalRequest = false;
    // 用于拼接完整请求
    private FullHttpRequest fullHttpRequest = null;

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, HttpObject msg) throws Exception {
        try {
            ClientChannelContext clientCtx = ctx.channel().attr(CLIENT_CTX_KEY).get();
            if (msg instanceof HttpRequest request) {
                String host = request.headers().get(HttpHeaderNames.HOST);
                if (host == null) {
                    HttpUtils.sendError(ctx, HttpResponseStatus.BAD_REQUEST);
                    return;
                }

                StringTokenizer tokenizer = new StringTokenizer(host, ":");
                String hostName = tokenizer.nextToken();
                int port = tokenizer.hasMoreTokens() ? Integer.parseInt(tokenizer.nextToken()) : 80;

                // 判断是否是发给本代理服务器的请求
                if (isLocalRequest(hostName, port)) {
                    isLocalRequest = true;

                    // 构建一个空 body 的 FullHttpRequest
                    fullHttpRequest = new DefaultFullHttpRequest(
                            request.protocolVersion(),
                            request.method(),
                            request.uri(),
                            Unpooled.buffer(0),
                            request.headers(),
                            EmptyHttpHeaders.INSTANCE
                    );
                    fullHttpRequest.retain(); // 保留引用计数

                    // 如果是 FullHttpRequest，直接处理 body
                    if (msg instanceof FullHttpRequest fullRequest) {
                        fullHttpRequest.content().writeBytes(fullRequest.content());
                        handleInternalRequest(ctx, fullHttpRequest);
                        fullHttpRequest.release();
                        isLocalRequest = false;
                        fullHttpRequest = null;
                    }
                } else {
                    isLocalRequest = false;

                    // CONNECT 方法处理 HTTPS 隧道
                    if (HttpMethod.CONNECT.equals(request.method())) {
                        handleConnect(ctx, request);
                    } else {
                        handleForwardedRequest(ctx, request, hostName, port);
                    }
                }
            } else if (msg instanceof HttpContent httpContent) {
                // 如果是本地请求，继续收集内容
                if (isLocalRequest) {
                    if (fullHttpRequest != null) {
                        fullHttpRequest.content().writeBytes(httpContent.content());

                        if (httpContent instanceof LastHttpContent) {
                            handleInternalRequest(ctx, fullHttpRequest);
                            fullHttpRequest.release();
                            isLocalRequest = false;
                            fullHttpRequest = null;
                        }
                    }
                } else {
                    if (clientCtx != null) {
                        if (!clientCtx.isTargetConnected()) {
                            ChannelFuture connectFuture = clientCtx.getConnectFuture();
                            if (httpContent instanceof LastHttpContent) {
                                connectFuture.addListener((ChannelFutureListener) future -> {
                                    try {
                                        if (future.isSuccess()) {
                                            if (ctx.channel().isActive()) {
                                                // 浏览器发送CONNECT 成功响应
                                                ChannelPipeline pipeline = ctx.pipeline();
                                                pipeline.remove(SimpleNettySocketHttpProxy.CHANNEL_HANDLER_HTTP_REQUEST_DECODER);
                                                pipeline.remove(SimpleNettySocketHttpProxy.CHANNEL_HANDLER_PROXY_FRONTEND_CONNECT);
                                            }
                                            // 响应客户端 CONNECT 成功
                                            try {
                                                ctx.writeAndFlush(Unpooled.copiedBuffer(
                                                        "HTTP/1.1 200 Connection Established\r\n\r\n",
                                                        StandardCharsets.US_ASCII)).sync();
                                            } catch (InterruptedException e) {

                                            }
                                            ctx.channel().config().setAutoRead(true);
                                            future.channel().config().setAutoRead(true);
                                            clientCtx.flushPendingData();
                                        } else {
                                            HttpUtils.sendError(ctx, HttpResponseStatus.BAD_GATEWAY);
                                            ctx.close();
                                        }
                                    } finally {
                                    }
                                });
                            } else {
                                clientCtx.addPendingData(httpContent);
                            }
                        } else {
                            clientCtx.forwardToTarget(httpContent);
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }
    }

    // 判断是否是发送给本代理服务器的请求
    private boolean isLocalRequest(String hostName, int port) {
        return ProxyServerApplication.httpProxy.isProxySelf(hostName, port);
    }

    // 处理本代理服务器的请求（如 /health）
    private void handleInternalRequest(ChannelHandlerContext ctx, FullHttpRequest request) {
//        String uri = request.uri();
//
//        if ("/health".equals(uri)) {
//            FullHttpResponse response = new DefaultFullHttpResponse(
//                    HttpVersion.HTTP_1_1,
//                    HttpResponseStatus.OK,
//                    Unpooled.copiedBuffer("OK".getBytes()));
//            HttpUtils.setKeepAliveAndHeaders(response, request);
//            ctx.writeAndFlush(response);
//        } else if ("/info".equals(uri)) {
//            FullHttpResponse response = new DefaultFullHttpResponse(
//                    HttpVersion.HTTP_1_1,
//                    HttpResponseStatus.OK,
//                    Unpooled.copiedBuffer("This is a proxy server".getBytes()));
//            HttpUtils.setKeepAliveAndHeaders(response, request);
//            ctx.writeAndFlush(response);
//        } else {
//            HttpUtils.sendError(ctx, HttpResponseStatus.NOT_FOUND);
//        }
    }

    // 处理 CONNECT 隧道（HTTPS）
    private void handleConnect(ChannelHandlerContext ctx, HttpRequest request) {
        String[] parts = request.uri().split(":");
        String host = parts[0];
        int port = parts.length > 1 ? Integer.parseInt(parts[1]) : 443;
        // 获取目标访问IP信息
        DomainIPInfo targetIpInfo = getRealAvailableIpInfo(getDomainIPBy(host));
        // 得到目标访问host
        String targetHost = targetIpInfo != null ? targetIpInfo.getIp() : host;


        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(ctx.channel().eventLoop())
                .channel(ctx.channel().getClass())
                .handler(new ProxyBackendHandler(ctx.channel()))
                .option(ChannelOption.AUTO_READ, false)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)  // 关键优化：设置5秒连接超时
                // 不开启keep-alive
                .option(ChannelOption.SO_KEEPALIVE, false)            // 推荐添加：保持连接活跃
                .option(ChannelOption.TCP_NODELAY, true)             // 推荐添加：禁用Nagle算法
        ;

        ChannelFuture connectFuture = bootstrap.connect(targetHost, port);
        ClientChannelContext clientCtx = new ClientChannelContext(host, targetIpInfo, ctx.channel(), connectFuture);
        ctx.channel().attr(CLIENT_CTX_KEY).set(clientCtx);
    }

    // 转发到目标服务器
    private void handleForwardedRequest(ChannelHandlerContext ctx, HttpRequest request, String host, int port) {
        // 获取目标访问IP信息
        DomainIPInfo targetIpInfo = getRealAvailableIpInfo(getDomainIPBy(host));
        // 得到目标访问host
        String targetHost = targetIpInfo != null ? targetIpInfo.getIp() : host;

        InetSocketAddress targetAddress = new InetSocketAddress(targetHost, port);

        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(ctx.channel().eventLoop())
                .channel(ctx.channel().getClass())
                .handler(new ProxyBackendHandler(ctx.channel()))
                .option(ChannelOption.AUTO_READ, false);

        ChannelFuture connectFuture = bootstrap.connect(targetAddress);
        ClientChannelContext clientCtx = new ClientChannelContext(targetHost, targetIpInfo, ctx.channel(), connectFuture);
        ctx.channel().attr(CLIENT_CTX_KEY).set(clientCtx);

        connectFuture.addListener((ChannelFutureListener) future -> {
            if (future.isSuccess()) {
                clientCtx.addPendingData(request);
                clientCtx.flushPendingData();
            } else {
                HttpUtils.sendError(ctx, HttpResponseStatus.BAD_GATEWAY);
                ctx.close();
            }
        });

        if (!(request instanceof FullHttpRequest)) {
            clientCtx.addPendingData(request);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        super.exceptionCaught(ctx, cause);
        System.err.println("ProxyFrontendConnectHandler...error!");
        cause.printStackTrace(System.err);
        ctx.close();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        ClientChannelContext clientCtx = ctx.channel().attr(CLIENT_CTX_KEY).get();
        if (clientCtx != null) {
            clientCtx.closeTargetChannel();
        }
        ctx.fireChannelInactive();
    }
}