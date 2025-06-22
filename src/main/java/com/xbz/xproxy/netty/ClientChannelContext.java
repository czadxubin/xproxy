package com.xbz.xproxy.netty;

import com.xbz.xproxy.pojo.DomainIPInfo;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.handler.codec.http.HttpObject;
import io.netty.util.ReferenceCountUtil;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ClientChannelContext {
    private String originHost;
    private DomainIPInfo targetIpInfo;
    @Getter
    private final ChannelFuture connectFuture;
    private final Channel clientChannel;
    private final Channel targetChannel;
    private final List<Object> pendingData = new ArrayList<>();

    public ClientChannelContext(String originHost,DomainIPInfo targetIpInfo,Channel clientChannel, ChannelFuture connectFuture) {
        this.originHost = originHost;
        this.targetIpInfo = targetIpInfo;
        this.clientChannel = clientChannel;
        this.connectFuture = connectFuture;
        this.targetChannel = connectFuture.channel();
    }

    public boolean isTargetConnected() {
        return targetChannel.isActive();
    }

    public void addPendingData(HttpObject obj) {
        pendingData.add(obj);
    }

    public void flushPendingData() {
        for (Object obj : pendingData) {
            forwardToTarget(obj);
        }
        pendingData.clear();
    }

    public void forwardToTarget(Object obj) {
        targetChannel.writeAndFlush(ReferenceCountUtil.retain(obj));
    }

    public void closeTargetChannel() {
        if (targetChannel != null && targetChannel.isActive()) {
            targetChannel.close();
        }
    }
}