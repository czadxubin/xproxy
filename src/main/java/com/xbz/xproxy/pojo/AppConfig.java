package com.xbz.xproxy.pojo;

import lombok.Data;

import java.util.List;


@Data
public class AppConfig {

    /**
     * dns列表
     */
    private List<String> dnsList;

}
