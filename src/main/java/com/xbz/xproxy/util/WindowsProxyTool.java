package com.xbz.xproxy.util;

import java.io.IOException;

public class WindowsProxyTool {

    /**
     * 设置Windows系统HTTP代理（增强版）
     * @param proxyHost        代理服务器地址（如 192.168.1.100）
     * @param proxyPort        代理端口（如 8080）
     * @param username         代理认证用户名（可选，无认证则传null）
     * @param password         代理认证密码（可选，无认证则传null）
     * @param bypassLocal      是否跳过本地地址（true=启用"对本地地址不使用代理"）
     * @param customBypassList 自定义排除列表（格式：地址1;地址2，如 "localhost;192.168.*"）
     * @throws ProxyOperationException 当操作失败时抛出异常
     */
    public static void setProxy(
            String proxyHost, int proxyPort,
            String username, String password,
            boolean bypassLocal, String customBypassList)
            throws ProxyOperationException {

        // 基础代理设置（保持原有逻辑）
        String proxyServer = String.format("%s:%d", proxyHost, proxyPort);
        executeRegistryCommand(
                "reg add \"HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Internet Settings\" /v ProxyEnable /t REG_DWORD /d 1 /f",
                "启用代理失败");
        executeRegistryCommand(
                String.format("reg add \"HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Internet Settings\" /v ProxyServer /t REG_SZ /d \"%s\" /f", proxyServer),
                "设置代理服务器失败");

        // 处理代理认证提示
        if (username != null && !username.isEmpty()) {
            System.out.println("警告：Java无法直接设置系统级代理认证，请按以下步骤手动操作：");
            System.out.println("1. 打开控制面板 -> 用户账户 -> 凭据管理器");
            System.out.println("2. 添加Windows凭据 -> 输入代理服务器地址、用户名和密码");
        }

        // 设置代理排除规则
        String bypassList = buildBypassList(bypassLocal, customBypassList);
        if (bypassList != null) {
            executeRegistryCommand(
                    String.format("reg add \"HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Internet Settings\" /v ProxyOverride /t REG_SZ /d \"%s\" /f", bypassList),
                    "设置代理排除规则失败");
        }

        // 刷新系统设置
        executeRegistryCommand("ipconfig /flushdns", "刷新DNS缓存失败");
        System.out.println("系统代理设置成功！");
    }

    // 构建排除列表（私有方法）
    private static String buildBypassList(boolean bypassLocal, String customList) {
        StringBuilder bypass = new StringBuilder();

        // 添加本地地址排除规则（默认行为）
        if (bypassLocal) {
            bypass.append("<local>"); // 等价于 "localhost;127.*;10.*;172.16.*;192.168.*;..."
        }

        // 添加自定义排除规则
        if (customList != null && !customList.trim().isEmpty()) {
            if (bypass.length() > 0) {
                bypass.append(";");
            }
            bypass.append(customList.replace(",", ";")); // 兼容逗号分隔输入
        }

        return bypass.length() == 0 ? null : bypass.toString();
    }

    /**
     * 清除Windows系统HTTP代理（需管理员权限）
     * @throws ProxyOperationException 当操作失败时抛出异常
     */
    public static void clearProxy() throws ProxyOperationException {
        // 禁用代理
        executeRegistryCommand(
            "reg add \"HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Internet Settings\" /v ProxyEnable /t REG_DWORD /d 0 /f",
            "禁用代理失败"
        );

        // 清除代理服务器
        executeRegistryCommand(
            "reg delete \"HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Internet Settings\" /v ProxyServer /f",
            "清除代理服务器失败"
        );

        // 刷新DNS缓存
        executeRegistryCommand(
            "ipconfig /flushdns",
            "刷新DNS缓存失败"
        );

        System.out.println("系统代理已清除！");
    }

    // 执行注册表命令（私有方法）
    private static void executeRegistryCommand(String command, String errorMessage) throws ProxyOperationException {
        try {
            Process process = Runtime.getRuntime().exec(command);
            int exitCode = process.waitFor();
            
            if (exitCode != 0) {
                throw new ProxyOperationException(errorMessage + " (错误码: " + exitCode + ")");
            }
        } catch (IOException | InterruptedException e) {
            throw new ProxyOperationException(errorMessage, e);
        }
    }

    // 自定义异常类
    public static class ProxyOperationException extends Exception {
        public ProxyOperationException(String message) {
            super(message);
        }

        public ProxyOperationException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    // 使用示例
    public static void main(String[] args) {
        // 设置代理（需管理员权限）
        // setProxy("192.168.1.100", 8080, "username", "password");

        // 清除代理（需管理员权限）
        // clearProxy();

    }
}