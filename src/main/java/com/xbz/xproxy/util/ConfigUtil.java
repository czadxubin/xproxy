package com.xbz.xproxy.util;

import cn.hutool.core.net.URLDecoder;
import com.xbz.xproxy.pojo.AppConfig;
import lombok.SneakyThrows;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class ConfigUtil {
    /**
     * 应用配置属性，全局唯一
     */
    private static volatile Properties APP_CONFIG = null;

    /**
     * 读取应用配置
     *
     * @return
     */
    public static Properties readApplicationConfig() {
        if (APP_CONFIG == null) {
            synchronized (ConfigUtil.class) {
                if (APP_CONFIG == null) {
                    // 读取应用内配置
                    Properties props;
                    InputStream appIn = ConfigUtil.class.getResourceAsStream("/application.properties");
                    try (BufferedReader br = new BufferedReader(new InputStreamReader(appIn, StandardCharsets.UTF_8))) {
                        props = new Properties();
                        props.load(br);
                    } catch (Exception e) {
                        System.out.println("应用配置读取失败！");
                        throw new RuntimeException(e);
                    } finally {
                        try {
                            appIn.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    APP_CONFIG = props;
                }
            }
        }
        return APP_CONFIG;
    }

    /**
     * 获取app配置bean
     *
     * @return
     */
    public static AppConfig getAppConfig() {
        AppConfig appConfig = new AppConfig();
        Properties appProps = readApplicationConfig();
        String dnsListVal = appProps.getProperty("dns.list");
        List<String> dnsList = Arrays.stream(dnsListVal.split(",")).collect(Collectors.toCollection(ArrayList::new));
        appConfig.setDnsList(dnsList);
        return appConfig;
    }

    /**
     * 读取需要代理的域名列表
     *
     * @return
     */
    @SneakyThrows
    public static List<String> readProxyDomains() {
        HashSet<String> domainSet = new HashSet<String>();
        // 读取App内domain列表
        InputStream appIn = ConfigUtil.class.getResourceAsStream("/proxy_domains");
        loadDomainList(appIn, domainSet);
        // 读取应用外部domain列表
        String appDirectory = getAppDirectory();
        File file = new File(appDirectory, "proxy_domains");
        if (file.exists() && file.isFile() && file.length() > 0) {
            loadDomainList(new FileInputStream(file), domainSet);
        }
        return new ArrayList<>(domainSet);
    }

    private static void loadDomainList(InputStream appIn, HashSet<String> domainSet) {
        String line;
        try (BufferedReader br = new BufferedReader(new InputStreamReader(appIn, StandardCharsets.UTF_8))) {
            while ((line = br.readLine()) != null) {
                domainSet.add(line.trim());
            }
        } catch (Exception e) {
            System.out.println("读取代理域名列表文件发生错误！");
            throw new RuntimeException(e);
        } finally {
            try {
                appIn.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    /**
     * 获取应用程序所在目录路径（兼容JAR运行和IDE运行）
     */
    private static String getAppDirectory() throws IOException {
        // 获取当前类的保护域（适用于JAR和类文件）
        String codeLocation = ConfigUtil.class
                .getProtectionDomain()
                .getCodeSource()
                .getLocation()
                .getPath();

        // 解码URL编码（处理空格等特殊字符）
        String decodedPath = URLDecoder.decode(codeLocation, StandardCharsets.UTF_8)
                .replaceAll("\\\\", "/"); // 替换混合斜杠;

        // windows路径处理开头
        if (isWindows()) {
            decodedPath = decodedPath.replaceFirst("^/", "");
        }

        // 获取父目录路径
        Path path = Paths.get(decodedPath).toRealPath().getParent();

        // 如果是IDE运行（target/classes目录），使用工作目录
        if (path.toString().contains("target" + File.separator + "classes")) {
            return System.getProperty("user.dir");
        }

        return path.toString();
    }

    /**
     * 判断当前操作系统是否为Windows
     */
    public static boolean isWindows() {
        return System.getProperty("os.name").startsWith("Windows");
    }
}
