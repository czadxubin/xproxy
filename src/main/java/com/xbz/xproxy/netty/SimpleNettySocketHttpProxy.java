package com.xbz.xproxy.netty;

import com.xbz.xproxy.DomainIpConvertor;
import com.xbz.xproxy.exception.BusinessException;
import com.xbz.xproxy.util.NetUtil;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpDecoderConfig;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.logging.LoggingHandler;
import lombok.Getter;

import java.net.SocketException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;


public class SimpleNettySocketHttpProxy {
    /**
     * http请求解析handler名称
     */
    public static final String CHANNEL_HANDLER_HTTP_REQUEST_DECODER = HttpRequestDecoder.class.getSimpleName();
    public static final String CHANNEL_HANDLER_PROXY_FRONTEND_CONNECT = ProxyFrontendConnectHandler.class.getSimpleName();
    public static final String CHANNEL_HANDLER_PROXY_FRONTEND_TRANSFER = ProxyFrontendTransferHandler.class.getSimpleName();
    private int port;
    private String host;
    private List<String> allBindIpList;
    private DomainIpConvertor domainIpConvertor = new DomainIpConvertor();
    private Thread nettyThread;

    /**
     * 服务状态，默认为启动中
     */
    @Getter
    private volatile ProxyServerStatusEnum status = ProxyServerStatusEnum.PREPARE;

    public SimpleNettySocketHttpProxy(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public SimpleNettySocketHttpProxy() {
        this("0.0.0.0", 8080);
    }

    /**
     * 启动代理
     */
    public void startProxyServer() {
        nettyThread = new Thread(this::doStartProxyServer);
        nettyThread.start();
    }

    private HttpRequestDecoder createHttpRequestDecoder() {
        HttpDecoderConfig httpDecoderConfig = new HttpDecoderConfig();
        // 请求行最大长度
        httpDecoderConfig.setMaxInitialLineLength(4096);
        // 头部总大小限制
        httpDecoderConfig.setMaxHeaderSize(65536);
        // 分块传输最大块大小
        httpDecoderConfig.setMaxChunkSize(8192);
        // 是否验证非法头部字符
        httpDecoderConfig.setValidateHeaders(false);
        return new HttpRequestDecoder(httpDecoderConfig);
    }

    private void doStartProxyServer() {
        // bossGroup 处理连接请求，workerGroup 处理I/O操作
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)  // 使用NIO传输
                    .option(ChannelOption.SO_BACKLOG, 128)  // 连接队列大小
                    .childOption(ChannelOption.SO_KEEPALIVE, true)  // 保持连接
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            // 配置处理器链
                            ChannelPipeline p = ch.pipeline();
                            p.addLast(CHANNEL_HANDLER_HTTP_REQUEST_DECODER, createHttpRequestDecoder());
                            p.addLast(CHANNEL_HANDLER_PROXY_FRONTEND_CONNECT, new ProxyFrontendConnectHandler());
                            p.addLast(CHANNEL_HANDLER_PROXY_FRONTEND_TRANSFER, new ProxyFrontendTransferHandler());

                            p.addLast(new LoggingHandler());
                        }
                    });
            ChannelFuture f = b.bind(host, port);
            this.status = ProxyServerStatusEnum.PREPARE;

            long timeoutMillis = 30_000; // 30秒
            long startTime = System.currentTimeMillis();

            while (!f.isDone()) {
                try {
                    if (f.await(100)) { // 等待最多100ms
                        break;
                    }
                    // 检查是否超时
                    if (System.currentTimeMillis() - startTime > timeoutMillis) {
                        interruptProxyServer();
                        throw new BusinessException("代理服务器绑定端口[" + port + "]超时！");
                    }
                } catch (InterruptedException e) {
                    // 服务被停止
                    if (this.status == ProxyServerStatusEnum.STOPPING) {
                        System.out.println("代理服务器状态为【" + this.status.getName() + "】,准备停止服务");
                        break;
                    } else {
                        System.out.println("绑定等待过程中线程被中断，继续等待...");

                    }
                }
            }
            if (this.status == ProxyServerStatusEnum.PREPARE) {
                // 此时绑定一定完成了
                if (!f.isSuccess()) {
                    throw new BusinessException("代理服务器" + "[" + host + ":" + port + "]" + "绑定端口失败！", f.cause());
                }
                // 记录所有绑定地址
                this.allBindIpList = getAllBindIpList();
                System.out.println("代理服务绑定的所有IP列表：" + allBindIpList);
                this.status = ProxyServerStatusEnum.RUNNING;
                System.out.println("HTTP代理服务器" + "[" + host + ":" + port + "]" + "启动成功！");
                try {
                    f.channel().closeFuture().sync();
                } catch (InterruptedException e) {
                    System.err.println("HTTP代理服务器--主线程接收到中断异常，即将关闭...");
                }
            }
        } finally {
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
            this.status = ProxyServerStatusEnum.STOPPED;
        }

    }

    public void interruptProxyServer() {
        // 如果状态为启动中，将状态先标记为停止中
        if (this.status == ProxyServerStatusEnum.PREPARE) {
            this.status = ProxyServerStatusEnum.STOPPING;
        }
        // 如何线程活跃，打断线程
        if (nettyThread.isAlive()) {
            nettyThread.interrupt();
        }
    }

    public void startIpDetectedService() {
        // 启动域名IP检测
        domainIpConvertor.start();
        System.out.println("域名IP检测器已启动！");
    }

    public void stopAll() {
        interruptProxyServer();
        domainIpConvertor.stop();
    }

    /**
     * 判断当前请求地址是否为本代理服务
     *
     * @return
     */
    public boolean isProxySelf(String host, int port) {
        if (port != this.port) {
            return false;
        }
        return allBindIpList.contains(host);
    }

    private List<String> getAllBindIpList() {
        HashSet<String> ipSet = new HashSet<String>();
        if ("0.0.0.0".equals(host)) {
            try {
                ipSet.addAll(NetUtil.getAllLocalIPAddresses(false, false));
            } catch (SocketException e) {
                throw new RuntimeException(e);
            }
        } else {
            ipSet.add(this.host);
        }
        return new ArrayList<>(ipSet);
    }
}
