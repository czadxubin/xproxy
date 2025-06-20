package com.xbz.xproxy.util;

import com.xbz.xproxy.pojo.DomainIP;
import org.checkerframework.checker.units.qual.A;
import org.icmp4j.Icmp4jUtil;
import org.icmp4j.IcmpPingResponse;
import org.icmp4j.IcmpPingUtil;
import org.xbill.DNS.ARecord;
import org.xbill.DNS.Lookup;
import org.xbill.DNS.SimpleResolver;
import org.xbill.DNS.Type;
import org.xbill.DNS.Record;

import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.util.ArrayList;
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

    public static DomainIP getTtl(String domain, String ip) {
        DomainIP domainIP = new DomainIP();
        domainIP.setDomain(domain);
        domainIP.setIp(ip);

        // 得到ttl
        try {
            IcmpPingResponse icmpPingResponse = IcmpPingUtil.executePingRequest(ip, 32, 500);
            int ttl = icmpPingResponse.getTtl();
            if (ttl > 0) {
                domainIP.setTtl(ttl);
            }

        } catch (Exception e) {
            System.err.println("ttl检查[" + domain + "(" + ip + ")]异常！");
            e.printStackTrace(System.err);
        }
        return domainIP;
    }

    /**
     * 多dns域名IP解析
     *
     * @param dnsList
     * @param domain
     * @return
     */
    public static List<DomainIP> domain2IP(List<String> dnsList, String domain) {
        HashSet<DomainIP> ipSet = new HashSet<>();
        List<CompletableFuture<Void>> futures = dnsList.stream().map(dns -> {
            return CompletableFuture.runAsync(() -> {
                try {
                    List<String> ipList = domain2IP(dns, domain);
                    for (String ip : ipList) {
                        // 检查IP是否可达
                        DomainIP domainIP = getTtl(domain, ip);
                        if (domainIP.getTtl() != null) {
                            ipSet.add(domainIP);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }).collect(Collectors.toCollection(ArrayList::new));
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        return new ArrayList<>(ipSet);
    }

}
