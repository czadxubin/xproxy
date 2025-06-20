package com.xbz.xproxy;

import cn.hutool.core.io.IoUtil;
import com.google.common.base.Throwables;
import com.xbz.xproxy.exception.BusinessException;
import com.xbz.xproxy.exception.IllegalArgsException;
import com.xbz.xproxy.netty.ProxyServerStatusEnum;
import com.xbz.xproxy.netty.SimpleNettySocketHttpProxy;
import com.xbz.xproxy.util.ConfigUtil;
import com.xbz.xproxy.util.DNSUtil;
import com.xbz.xproxy.util.WindowsProxyTool;
import lombok.SneakyThrows;
import org.icmp4j.util.TimeUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Hello world!
 */
public class ProxyServerApplication {
    /**
     * 应用过期时间
     */
    private static final String APP_INVALIDATE = "2025-06-31 23:59:59.999";
    private Thread nettyThread;
    private SimpleNettySocketHttpProxy httpProxy;

    public static void main(String[] args) {
        // 注册关闭钩子
        Runtime.getRuntime().addShutdownHook(new AppShutdownHook());
        // 解析输入参数
        Map<String, String> argsMap;
        try {
            argsMap = parseArgs(args);
        } catch (IllegalArgsException e) {
            System.err.println(e.getErrMsg());
            return;
        }
        int port = Integer.parseInt(argsMap.getOrDefault("port", "8080"));
        String host = argsMap.getOrDefault("host", "0.0.0.0");

        // 校验软件可用性
        try {
            isAppAvailable();
        } catch (BusinessException e) {
            String errMsg = e.getErrMsg();
            System.err.println(errMsg);
            return;
        }
        SimpleNettySocketHttpProxy httpProxy = new SimpleNettySocketHttpProxy(host, port);
        httpProxy.startProxyServer();

        while (httpProxy.getStatus() == ProxyServerStatusEnum.PREPARE) {
            try {
                TimeUnit.MILLISECONDS.sleep(100);
            } catch (InterruptedException e) {

            }
        }
        // 代理服务启动异常
        if (ProxyServerStatusEnum.RUNNING != httpProxy.getStatus()) {
            System.exit(1);
            return;
        }
        // 代理服务器启动成功
        // 开启IP检测
        httpProxy.startIpDetectedService();

        if (ConfigUtil.isWindows()) {
            boolean autoProxyError = false;
            // 如果是windows平台自动打开代理
            boolean autoProxy = Boolean.parseBoolean(argsMap.getOrDefault("auto_proxy", "false"));
            StringBuilder autoProxySb = new StringBuilder();
            autoProxySb.append("*************************************************************\n");
            if (!autoProxy) {
                autoProxySb.append("当前自动开启代理未启用，如需自动开启系统级http代理服务\n可在启动程序时添加参数：--auto_proxy=true").append("\n");
            } else {
                autoProxySb.append("windows平台自动开启http服务检测：auto_proxy=");
                autoProxySb.append(autoProxy).append("\n");
                String proxyHost = host;
                if ("0.0.0.0".equals(host)) {
                    proxyHost = "127.0.0.1";
                }
                try {
                    WindowsProxyTool.setProxy(proxyHost, port, null, null, true, "localhost;127.0.0.1;");
                    autoProxySb.append("自动开启系统http代理成功！代理服务地址：").append(proxyHost).append(":").append(port).append("\n");
                } catch (Exception e) {
                    autoProxyError = true;
                    autoProxySb.append("自动开启系统http代理失败！代理服务地址：").append(proxyHost).append(":").append(port).append("\n");
                    autoProxySb.append("错误信息如下\n").append(Throwables.getStackTraceAsString(e));
                }
            }
            autoProxySb.append("*************************************************************\n");
            if (autoProxyError) {
                System.err.println(autoProxySb);
            } else {
                System.out.println(autoProxySb);
            }
        }

    }

    public static void closeWindowsHttpProxy() {
        try {
            WindowsProxyTool.clearProxy();
            System.err.println("windows系统代理已关闭！");
        } catch (WindowsProxyTool.ProxyOperationException e) {
            System.err.println("windows系统代理关闭异常，请手动关闭！");
        }

    }

    @SneakyThrows
    private static void isAppAvailable() {
        // 是否已过期
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        Date invalidateDate = df.parse(APP_INVALIDATE);
        if (new Date().after(invalidateDate)) {
            throw new BusinessException("抱歉您的应用程序服务已过期，继续使用请联系攻城狮！");
        }

        // TODO 是否具备授权许可->加密算法实现单设备使用限制
    }

    private static Map<String, String> parseArgs(String[] args) {
        Map<String, String> argsMap = new HashMap<>();
        List<String> unknownArgsList = new ArrayList<>();
        for (String arg : args) {
            if (arg.startsWith("--")) {
                String argStr = arg.replaceAll("^--", "");
                String[] split = argStr.split("=");
                if (split.length == 2) {
                    argsMap.put(split[0], split[1]);
                } else {
                    unknownArgsList.add(arg + ":不满足key=val格式！");
                }
            } else {
                unknownArgsList.add(arg + ":参数需要--为前缀，如--port=80");
            }
        }
        if (!unknownArgsList.isEmpty()) {
            throw new IllegalArgsException(unknownArgsList);
        }
        return argsMap;
    }
}
