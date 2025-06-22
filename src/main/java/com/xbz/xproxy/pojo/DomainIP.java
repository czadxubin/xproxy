package com.xbz.xproxy.pojo;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(of = {"domain"})
public class DomainIP {
    private String domain;

    private List<DomainIPInfo> ipList;

    public String getPrettyPrint() {
        StringBuilder sb = new StringBuilder();
        sb.append(domain).append("-->");
        if (ipList != null && !ipList.isEmpty()) {
            for (int i = 0; i < ipList.size(); i++) {
                DomainIPInfo ipInfo = ipList.get(i);
                int connectErrTimes = ipInfo.getConnectErrTimes().get();
                String ip = ipInfo.getIp();
                Integer ttl = ipInfo.getTtl();
                boolean preferred = ipInfo.isPreferred();
                sb.append("[");
                sb.append(ip).append(preferred?"（首选）":"");
                sb.append(",ttl=").append(ttl);
                sb.append(",connectErrTimes=").append(connectErrTimes);
                sb.append("]");
                if (i != ipList.size() - 1) {
                    sb.append(",");
                }
            }
        }
        return sb.toString();
    }
}
