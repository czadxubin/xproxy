package com.xbz.xproxy.util;

import java.io.IOException;
import java.net.*;

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
}
