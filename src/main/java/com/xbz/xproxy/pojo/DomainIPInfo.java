package com.xbz.xproxy.pojo;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * IP信息类<br>
 * 属性解读<br>
 * <code>isPreferred</code>:是否为首选IP，需要维护首选IP列表（后续支持，由用户添加），如果当前IP是首选IP，那么将忽略<code>连接错误次数 connectErrTimes</code>
 * 强制IP选择使用此IP。<br>
 * <code>connectErrTimes</code>:连接错误次数,当前IP被用于进行连接时，记录此IP被访问时连接错误发生次数，当连接成功时，重置错误次数为0。该错误次数被用于判断当前IP是否应该被继续使用
 */
@EqualsAndHashCode(of = "ip")
@Data
public class DomainIPInfo {
    private String domain;

    private String ip;

    private Integer ttl;

    /**
     * 是否位首选IP
     */
    private boolean isPreferred = false;

    /**
     * 连接错误次数，默认0
     */
    private AtomicInteger connectErrTimes = new AtomicInteger(0);

    public void increConnectErrTimes() {
        connectErrTimes.incrementAndGet();
    }

    public void resetConnectErrTimes() {
        connectErrTimes.set(0);
    }
}
