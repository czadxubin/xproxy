package com.xbz.xproxy;

import com.xbz.xproxy.pojo.AppConfig;
import com.xbz.xproxy.pojo.DomainIP;
import com.xbz.xproxy.util.ConfigUtil;
import com.xbz.xproxy.util.DNSUtil;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 域名和IP转换器
 */
public class DomainIpConvertor {
    /**
     * domainIP集合
     */
    private static ConcurrentHashMap<String, DomainIP> domainIPMap = new ConcurrentHashMap<>();

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
     * 启动转换器
     */
    public void start() {
        // 定时任务1：进行域名->IP的转换工作,每次间隔10分钟
        executor.scheduleWithFixedDelay(this::doConvertTask, 0, 5, TimeUnit.MINUTES);
    }

    /**
     * 进行转换工作
     */
    private void doConvertTask() {
        System.out.println("域名IP探测...开始");
        List<String> domains = ConfigUtil.readProxyDomains();
        AppConfig appConfig = ConfigUtil.getAppConfig();
        List<String> dnsList = appConfig.getDnsList();
        if (domains.isEmpty()) {
            return;
        }
        for (String domain : domains) {
            System.out.println("探测域名[" + domain + "]...开始");
            List<DomainIP> domainIPs = DNSUtil.domain2IP(dnsList, domain);
            if (!domainIPs.isEmpty()) {
                // 按照ttl排序，取ttl最小
                domainIPs.sort(Comparator.comparing(DomainIP::getTtl));
                DomainIP domainIP = domainIPs.get(0);
                domainIPMap.put(domain, domainIP);
            }
            System.out.println("探测域名[" + domain + "]...结束");
        }
        System.out.println("域名IP探测...结束");
        System.out.println("===========当前域名IP转换服务数据======================");
        domainIPMap.forEach((k, v) -> {
            System.out.println(k + "|" + v.getIp() + "|" + v.getTtl());
        });
        System.out.println("===========当前域名IP转换服务数据======================");
    }

}
