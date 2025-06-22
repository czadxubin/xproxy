package com.xbz.xproxy.util;

import com.xbz.xproxy.pojo.DomainIP;
import com.xbz.xproxy.pojo.DomainIPInfo;
import org.icmp4j.IcmpPingResponse;
import org.icmp4j.IcmpPingUtil;
import org.xbill.DNS.ARecord;
import org.xbill.DNS.Lookup;
import org.xbill.DNS.SimpleResolver;
import org.xbill.DNS.Type;
import org.xbill.DNS.Record;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class DNSUtil {
    /**
     * 域名转IP
     *
     * @param dns
     * @param domain
     * @return
     */
    public static List<String> domain2IP(String dns, String domain) {
        List<String> ips = new ArrayList<>();
        try {
            Lookup lookup = new Lookup(domain, Type.A);
            SimpleResolver resolver = new SimpleResolver(dns); // 指定DNS服务器
            lookup.setResolver(resolver);

            Record[] records = lookup.run();
            if (lookup.getResult() == Lookup.SUCCESSFUL) {
                for (Record rec : records) {
                    ARecord aRecord = (ARecord) rec;
                    ips.add(aRecord.getAddress().getHostAddress());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();

        }
        return ips;
    }

    public static DomainIPInfo getDomainIPInfo(String domain, String ip) {
        // 得到ttl
        try {
            IcmpPingResponse icmpPingResponse = IcmpPingUtil.executePingRequest(ip, 32, 500);
            int ttl = icmpPingResponse.getTtl();
            if (ttl > 0) {
                DomainIPInfo ipInfo = new DomainIPInfo();
                ipInfo.setDomain(domain);
                ipInfo.setIp(ip);
                ipInfo.setTtl(ttl);
                return ipInfo;
            }

        } catch (Exception e) {
            System.err.println("ttl检查[" + domain + "(" + ip + ")]异常！");
            e.printStackTrace(System.err);
        }
        return null;
    }

    /**
     * 多dns域名IP解析
     *
     * @param dnsList
     * @param domain
     * @return
     */
    public static DomainIP domain2IP(List<String> dnsList, String domain) {
        HashSet<DomainIPInfo> ipSet = new HashSet<>();
        List<CompletableFuture<Void>> futures = dnsList.stream().map(dns -> {
            return CompletableFuture.runAsync(() -> {
                try {
                    List<String> ipList = domain2IP(dns, domain);
                    for (String ip : ipList) {
                        // 检查IP是否可达
                        DomainIPInfo ipInfo = getDomainIPInfo(domain, ip);
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
        List<DomainIPInfo> ipInfoList = new ArrayList<>(ipSet);
        // 根据ttl排序
        ipInfoList.sort(Comparator.comparing(DomainIPInfo::getTtl));
        DomainIP domainIP = new DomainIP();
        domainIP.setDomain(domain);
        domainIP.setIpList(ipInfoList);
        return domainIP;
    }

}
