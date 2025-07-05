package com.xbz.xproxy.pojo;

import lombok.Data;

import java.util.List;
import java.util.Map;


@Data
public class AppConfig {

    /**
     * dns列表
     */
    private List<String> dnsList;
    /**
     * 固定的域名和IP列表
     */
    private Map<String, List<String>> fixedDomainIPsMap;
}
