package com.xbz.xproxy;

import cn.hutool.core.date.StopWatch;
import cn.hutool.core.lang.hash.Hash;
import com.google.common.base.Throwables;
import com.xbz.xproxy.pojo.AppConfig;
import com.xbz.xproxy.pojo.DomainIP;
import com.xbz.xproxy.pojo.DomainIPInfo;
import com.xbz.xproxy.util.ConfigUtil;
import com.xbz.xproxy.util.DNSUtil;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * 域名和IP转换器
 */
public class DomainIpConvertor {
    /**
     * domainIP集合
     */
    private static ConcurrentHashMap<String, DomainIP> domainIPMap = new ConcurrentHashMap<>();
    /**
     * 未解析成功域名集合
     */
    private static HashSet<String> noResolveDomainSet = new HashSet<>();

    private static ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(0, new ThreadFactory() {
        private AtomicInteger counter = new AtomicInteger();

        @Override
        public Thread newThread(Runnable r) {
            return new Thread(r, "D2IP_Thread_" + counter.incrementAndGet());
        }
    }, new RejectedExecutionHandler() {
        @Override
        public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {

        }
    });

    /**
     * 根据域名获取DomainIP
     *
     * @param domain
     * @return
     */
    public static DomainIP getDomainIPBy(String domain) {
        return domainIPMap.get(domain);
    }

    /**
     * 获取实际可以的IP<br>
     * 获取规则：<br>
     * 1. 优先使用被指定为首选的IP，无论是否被标记错误次数
     * 2. 选择ttl最小的IP，但是需要过滤掉错误次数超过上限的IP
     *
     * @param domainIP
     * @return
     */
    public static DomainIPInfo getRealAvailableIpInfo(DomainIP domainIP) {
        if (domainIP == null) {
            return null;
        }
        List<DomainIPInfo> ipList = domainIP.getIpList();
        if (ipList == null || ipList.isEmpty()) {
            return null;
        }

        DomainIPInfo preIpInfo = null;
        for (DomainIPInfo ipInfo : ipList) {
            // 优先使用 配置为首选的IP
            if (ipInfo.isPreferred()) {
                return ipInfo;
            }
            // 判断错误连接次数,超过指定错误次数后，此IP不再被使用
            if (ipInfo.getConnectErrTimes().get() > 5) {
                continue;
            }
            // 否则优先使用ttl最小的IP
            if (preIpInfo == null || preIpInfo.getTtl().compareTo(ipInfo.getTtl()) > 0) {
                // 更换IP
                preIpInfo = ipInfo;
            }
        }
        return preIpInfo;
    }

    /**
     * 启动转换器
     */
    public void start() {
        // 定时任务1：进行域名->IP的转换工作,每次间隔10分钟
        executor.scheduleWithFixedDelay(this::doConvertTask, 0, 5, TimeUnit.MINUTES);
        // 定时任务2：进行未解析域名->IP的高频检查，每次间隔10秒
        executor.scheduleWithFixedDelay(this::doNoResolveDomainTask, 10, 10, TimeUnit.SECONDS);
    }

    /**
     * 检测未解析成功的域名集合进行高频检测
     */
    private void doNoResolveDomainTask() {
        StopWatch stopWatch = new StopWatch("检测未探测到IP的域名");
        try {
            stopWatch.start("检测是否有待处理数据");
            if (noResolveDomainSet.isEmpty()) {
                return;
            }
            stopWatch.stop();
            stopWatch.start("执行检测");
            AppConfig appConfig = ConfigUtil.getAppConfig();
            List<String> dnsList = appConfig.getDnsList();
            for (String domain : noResolveDomainSet) {
                DomainIP domainIP = parseDomainIPInfo(appConfig, dnsList, domain);
                List<DomainIPInfo> ipList = domainIP.getIpList();
                if (ipList != null && !ipList.isEmpty()) {
                    domainIPMap.put(domain, domainIP);
                    noResolveDomainSet.remove(domain);
                }
            }
            stopWatch.stop();
        } catch (Exception e) {
            System.err.println("任务处理异常\n" + Throwables.getStackTraceAsString(e));
        } finally {
            if (stopWatch.isRunning()) {
                stopWatch.stop();
            }
            System.out.println(stopWatch.prettyPrint());
        }
    }

    /**
     * 进行转换工作
     */
    private void doConvertTask() {
        StopWatch stopWatch = new StopWatch("域名IP检测");
        try {
            stopWatch.start("域名检测");
            List<String> domains = ConfigUtil.readProxyDomains();
            AppConfig appConfig = ConfigUtil.getAppConfig();
            List<String> dnsList = appConfig.getDnsList();
            if (domains.isEmpty()) {
                return;
            }
            for (String domain : domains) {
                if (noResolveDomainSet.contains(domain)) {
                    continue;
                }
                //            System.out.println("探测域名[" + domain + "]...开始");
                DomainIP domainIP = parseDomainIPInfo(appConfig, dnsList, domain);
                List<DomainIPInfo> ipList = domainIP.getIpList();
                if (ipList != null && !ipList.isEmpty()) {
                    domainIPMap.put(domain, domainIP);
                } else {
                    // 未检测到可用IP
                    domainIPMap.remove(domain);
                    noResolveDomainSet.add(domain);
                }
                //            System.out.println("探测域名[" + domain + "]...结束");
            }
            stopWatch.stop();
            System.out.println("===========当前域名IP转换服务数据======================");
            domainIPMap.forEach((k, v) -> {
                System.out.println(v.getPrettyPrint());
            });
            if(!noResolveDomainSet.isEmpty()){
                System.out.println("-----------未转换成功的域名列表("+noResolveDomainSet.size()+")------------------------");
                for (String domain : noResolveDomainSet) {
                    System.out.println(domain);
                }

            }
            System.out.println("===========当前域名IP转换服务数据======================");
        } catch (Exception e) {
            System.err.println("任务执行异常" + Throwables.getStackTraceAsString(e));
        } finally {
            if (stopWatch.isRunning()) {
                stopWatch.stop();
            }
            System.out.println(stopWatch.prettyPrint());
        }
    }

    public DomainIP parseDomainIPInfo(AppConfig appConfig, List<String> dnsList, String domain) {
        // 检查是否是用户用户的IP域名
        Map<String, List<String>> domainIPsMap = appConfig.getFixedDomainIPsMap();
        List<String> fixedIps = domainIPsMap.get(domain);
        if (fixedIps != null && !fixedIps.isEmpty()) {
            HashSet<DomainIPInfo> ipSet = new HashSet<>();
            List<CompletableFuture<Void>> futures = dnsList.stream().map(dns -> {
                return CompletableFuture.runAsync(() -> {
                    try {
                        for (String ip : fixedIps) {
                            // 检查IP是否可达
                            DomainIPInfo ipInfo = DNSUtil.getDomainIPInfo(domain, ip);
                            if (ipInfo != null) {
                                ipSet.add(ipInfo);
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            }).collect(Collectors.toCollection(ArrayList::new));
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
            if (ipSet.isEmpty()) {
                System.err.printf("警告：域名%s配置IP：%s.已不可用！系统将自动检索可用IP进行使用，请及时更新IP！\n", domain, Arrays.toString(fixedIps.toArray(new String[0])));
                return DNSUtil.domain2IP(dnsList, domain);
            } else {
                DomainIP domainIP = new DomainIP();
                domainIP.setDomain(domain);
                for (DomainIPInfo ipInfo : ipSet) {
                    ipInfo.setPreferred(true);
                }
                domainIP.setIpList(new ArrayList<>(ipSet));
                return domainIP;
            }
        } else {
            return DNSUtil.domain2IP(dnsList, domain);
        }
    }

    public void stop() {
        System.out.println("探测域名服务停止！");
        executor.shutdownNow();
    }
}
