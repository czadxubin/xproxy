package com.xbz.xproxy.netty;

import com.xbz.xproxy.DomainIpConvertor;
import com.xbz.xproxy.exception.BusinessException;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import lombok.Getter;


public class SimpleNettySocketHttpProxy {
    private int port;
    private String host;
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
                            p.addLast(new ProxyFrontendHandler());              // 业务处理器
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
}
