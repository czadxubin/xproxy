package com.xbz.xproxy.pojo;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(of = {"domain"})
public class DomainIP {
    private String domain;

    private String ip;

    /**
     * 本地检测IP连通性ttl
     */
    private Integer ttl;
}
