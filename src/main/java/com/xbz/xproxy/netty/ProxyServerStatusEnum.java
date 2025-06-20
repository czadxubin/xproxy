package com.xbz.xproxy.netty;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
@AllArgsConstructor
public enum ProxyServerStatusEnum {
    /**
     * 启动中
     */
    PREPARE(0, "启动中"),
    /**
     * 启动完成，运行中
     */
    RUNNING(1, "运行中"),
    /**
     * 标记为停止
     */
    STOPPING(2, "已中断"),

    /**
     * 已停止
     */
    STOPPED(99, "已中断"),
    ;
    static List<ProxyServerStatusEnum> FINAL_STATUSES = new ArrayList<>();

    static {
        FINAL_STATUSES.add(STOPPED);
    }

    private final int code;
    private final String name;


    public boolean isFinal() {
        return FINAL_STATUSES.contains(this);
    }
}
