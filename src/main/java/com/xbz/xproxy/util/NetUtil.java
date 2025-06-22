package com.xbz.xproxy.util;

import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

public class NetUtil {
    /**
     * 检测指定端口是否开放（带超时控制）
     * @param host 目标主机地址（IP或域名）
     * @param port 目标端口号（0-65535）
     * @param timeout 超时时间（毫秒），建议500-3000
     * @return true=端口开放，false=端口关闭/不可达
     */
    public static boolean isPortOpen(String host, int port, int timeout) {
        try (Socket socket = new Socket()) {
            // 设置连接超时时间
            socket.connect(new InetSocketAddress(host, port), timeout);
            return true; // 连接成功说明端口开放
        } catch (SocketTimeoutException e) {
            // System.out.println("连接超时: " + host + ":" + port);
            return false;
        } catch (ConnectException e) {
            // System.out.println("连接被拒绝: " + host + ":" + port);
            return false;
        } catch (UnknownHostException e) {
            // System.out.println("未知主机: " + host);
            return false;
        } catch (IOException e) {
            // System.out.println("IO异常: " + e.getMessage());
            return false;
        }
    }

    /**
     * 获取本地所有IP地址
     * @param excludeLookupBack 是否排除回环地址（如127.0.0.1）
     * @param includeIPv6     是否包含IPv6地址
     * @return 过滤后的IP地址列表
     * @throws SocketException 当网络接口访问出错时抛出
     */
    public static List<String> getAllLocalIPAddresses(
            boolean excludeLookupBack,
            boolean includeIPv6) throws SocketException {

        List<String> ipAddresses = new ArrayList<>();
        Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();

        while (interfaces.hasMoreElements()) {
            NetworkInterface iface = interfaces.nextElement();

            // 可选：排除未启用/虚拟接口
            // if (!iface.isUp() || iface.isVirtual()) continue;

            Enumeration<InetAddress> addresses = iface.getInetAddresses();
            while (addresses.hasMoreElements()) {
                InetAddress addr = addresses.nextElement();

                // 过滤条件
                if (excludeLookupBack && addr.isLoopbackAddress()) {
                    continue;
                }

                if (!includeIPv6 && addr.getHostAddress().contains(":")) {
                    continue; // 排除IPv6地址
                }

                ipAddresses.add(addr.getHostAddress());
            }
        }
        return ipAddresses;
    }
}
