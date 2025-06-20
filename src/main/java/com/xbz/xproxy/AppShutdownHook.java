package com.xbz.xproxy;

import com.xbz.xproxy.util.ConfigUtil;

import static com.xbz.xproxy.ProxyServerApplication.closeWindowsHttpProxy;

public class AppShutdownHook extends Thread{
    @Override
    public void run() {
        if(ConfigUtil.isWindows()){
            closeWindowsHttpProxy();
        }
    }
}
