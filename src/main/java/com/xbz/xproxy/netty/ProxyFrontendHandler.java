package com.xbz.xproxy.netty;

import cn.hutool.core.net.NetUtil;
import com.google.common.base.Throwables;
import com.xbz.xproxy.DomainIpConvertor;
import com.xbz.xproxy.pojo.DomainIP;
import com.xbz.xproxy.util.DNSUtil;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ProxyFrontendHandler extends SimpleChannelInboundHandler<ByteBuf> {

    private volatile boolean isConnectHandled = false;
    private Channel outboundChannel;

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf in) throws Exception {
        if (isConnectHandled) {
            // CONNECT 已处理，直接转发数据
            if (outboundChannel != null && outboundChannel.isActive()) {
                outboundChannel.writeAndFlush(in.retain());
            }
            return;
        }

        // 将 ByteBuf 转换为字符串尝试解析方法
        int readerIndex = in.readerIndex();
        byte[] headerBytes = new byte[Math.min(in.readableBytes(), 1024)];
        in.markReaderIndex();
        in.readBytes(headerBytes);
        String headerStr = new String(headerBytes, StandardCharsets.US_ASCII);
        in.resetReaderIndex();

        if (headerStr.startsWith("CONNECT")) {
            handleConnect(ctx, in);
        } else {
            handleHttpRequest(ctx, in);
        }
    }

    private void handleConnect(ChannelHandlerContext ctx, ByteBuf in) {
        int readerIndex = in.readerIndex();
        byte[] bytes = new byte[in.readableBytes()];
        in.readBytes(bytes);
        String request = new String(bytes, StandardCharsets.US_ASCII);

        // 解析 CONNECT example.com:443 HTTP/1.1
        Pattern pattern = Pattern.compile("^CONNECT\\s+([^\\s]+)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(request);
        if (!matcher.find()) {
            sendError(ctx, "400 Bad Request");
            ctx.close();
            return;
        }

        String hostPort = matcher.group(1);
        String[] parts = hostPort.split(":");
        String host = parts[0];
        int port = parts.length > 1 ? Integer.parseInt(parts[1]) : 443;

        // 处理host,转IP
        host = convertHostToAvailableIp(host);

        // 建立到目标服务器的连接
        Bootstrap b = new Bootstrap();
        b.group(ctx.channel().eventLoop())
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ch.pipeline().addLast(new ProxyBackendHandler(ctx.channel()));
                    }
                });

        ChannelFuture f = b.connect(host, port);
        String finalHost = host;
        f.addListener((ChannelFutureListener) future -> {
            if (future.isSuccess()) {
                System.out.println("隧道[" + finalHost + ":" + port + "]建立成功！\n");
                // 响应客户端 CONNECT 成功
                ctx.writeAndFlush(Unpooled.copiedBuffer(
                        "HTTP/1.1 200 Connection Established\r\n\r\n",
                        StandardCharsets.US_ASCII)).sync();
                isConnectHandled = true;
            } else {
                System.err.println("隧道[" + finalHost + ":" + port + "]建立失败！\n" + Throwables.getStackTraceAsString(future.cause()));
                ctx.close();
            }
        });
        outboundChannel = f.channel();
    }

    /**
     * 域名转IP
     *
     * @param host
     * @return
     */
    private String convertHostToAvailableIp(String host) {
        if (isDomainName(host)) {
            // 查看域名-IP配置表是否存在对应IP
            String ip = findByDomainIp(host);
            if (ip != null) {
                boolean ping = false;
                try {
                    NetUtil.ping(ip, 1000);
                    ping = true;
                } catch (Exception e) {
                    System.err.println(host + "[" + ip + "]无法ping通！继续使用域名访问。");
                    ping = false;
                }
                return ping ? ip : host;
            }
        }
        return host;
    }

    private String findByDomainIp(String host) {
//        if(host.equals("github.com")){
//            return "140.82.116.4";
////            return "20.27.177.113";
//        } else if (host.equals("netflix.com")) {
//            return "207.45.72.1";
//        }
        DomainIP domainIP = DomainIpConvertor.getDomainIPBy(host);
        if (domainIP != null) {
            return domainIP.getIp();
        }
        return null;
    }

    private void handleHttpRequest(ChannelHandlerContext ctx, ByteBuf in) {
        // TODO: 手动解析 HTTP GET/POST 请求并转发
        // 可以提取 Host 头，构造新请求发往目标服务器
        // 此处略过简化，只做 CONNECT 示例
        sendError(ctx, "501 Not Implemented");
        ctx.close();
    }

    private void sendError(ChannelHandlerContext ctx, String code) {
        String response = "HTTP/1.1 " + code + "\r\n" +
                "Content-Length: 0\r\n" +
                "Connection: close\r\n\r\n";
        ctx.writeAndFlush(Unpooled.copiedBuffer(response, StandardCharsets.US_ASCII));
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        if (outboundChannel != null) {
            outboundChannel.close();
        }
        ctx.close();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        System.out.println("ProxyFrontend Error");
        cause.printStackTrace();
        ctx.close();
    }

    public static boolean isDomainName(String input) {
        String domainPattern = "^((?!-)[A-Za-z0-9-]{1,63}(?<!-)\\.)+[A-Za-z]{2,6}$";
        return input.matches(domainPattern);
    }
}